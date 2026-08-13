#!/usr/bin/env python3
"""Apples-to-apples evaluation of the Android CLIP ONNX package.

This tool intentionally uses the model and tokenizer assets shipped by the
Android application. It compares only image preprocessing:

  A: Android indexing equivalent: inSampleSize, RGB565, direct 224x224 resize.
  B: Standard CLIP preprocessing: aspect-ratio-preserving resize and center crop.

No MobileCLIP2 model or embedding is used.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import math
import sys
import unicodedata
from pathlib import Path
from typing import Dict, Iterable, List, Sequence, Tuple

import numpy as np
import onnxruntime as ort
from pillow_heif import register_heif_opener
from PIL import Image


register_heif_opener()


IMAGE_SIZE = 224
CONTEXT_LENGTH = 77
START_TOKEN_ID = 49406
END_TOKEN_ID = 49407
CLIP_MEAN = np.asarray([0.48145466, 0.4578275, 0.40821073], dtype=np.float32)
CLIP_STD = np.asarray([0.26862954, 0.26130258, 0.27577711], dtype=np.float32)
IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png", ".webp", ".bmp", ".heic", ".heif"}


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def bytes_to_unicode() -> Dict[int, str]:
    """The byte encoder used by OpenAI CLIP/GPT-2 tokenization."""
    values = list(range(ord("!"), ord("~") + 1))
    values += list(range(ord("¡"), ord("¬") + 1))
    values += list(range(ord("®"), ord("ÿ") + 1))
    chars = list(values)
    extra = 0
    for byte_value in range(256):
        if byte_value not in values:
            values.append(byte_value)
            chars.append(256 + extra)
            extra += 1
    return dict(zip(values, (chr(value) for value in chars)))


def pairs(word: Sequence[str]) -> set[Tuple[str, str]]:
    return set(zip(word, word[1:]))


class AndroidClipTokenizer:
    """Python port of the app's ClipTokenizer, including EOT padding."""

    START_TEXT = "<|startoftext|>"
    END_TEXT = "<|endoftext|>"

    def __init__(self, vocab_path: Path, merges_path: Path) -> None:
        with vocab_path.open("r", encoding="utf-8") as stream:
            self.vocab: Dict[str, int] = json.load(stream)
        self.byte_encoder = bytes_to_unicode()
        self.bpe_ranks: Dict[Tuple[str, str], int] = {}
        with merges_path.open("r", encoding="utf-8") as stream:
            rank = 0
            for raw_line in stream:
                line = raw_line.strip()
                if not line or line.startswith("#"):
                    continue
                fields = line.split()
                if len(fields) == 2:
                    self.bpe_ranks[(fields[0], fields[1])] = rank
                    rank += 1
        self.cache: Dict[str, Tuple[str, ...]] = {}

    @staticmethod
    def _clean(text: str) -> str:
        return " ".join(text.lower().strip().split())

    def _pieces(self, text: str) -> Iterable[str]:
        """Equivalent to the Java Unicode letter/number tokenizer pattern."""
        index = 0
        while index < len(text):
            if text.startswith(self.START_TEXT, index):
                yield self.START_TEXT
                index += len(self.START_TEXT)
                continue
            if text.startswith(self.END_TEXT, index):
                yield self.END_TEXT
                index += len(self.END_TEXT)
                continue
            if text[index].isspace():
                index += 1
                continue

            category = unicodedata.category(text[index])[:1]
            wanted = category if category in {"L", "N"} else "OTHER"
            end = index + 1
            while end < len(text):
                if text.startswith(self.START_TEXT, end) or text.startswith(self.END_TEXT, end):
                    break
                char = text[end]
                if char.isspace():
                    break
                current_category = unicodedata.category(char)[:1]
                current = current_category if current_category in {"L", "N"} else "OTHER"
                if current != wanted:
                    break
                end += 1
            yield text[index:end]
            index = end

    def _bpe(self, token: str) -> Tuple[str, ...]:
        cached = self.cache.get(token)
        if cached is not None:
            return cached
        if not token:
            return tuple()

        word: Tuple[str, ...] = tuple(token[:-1]) + (token[-1] + "</w>",)
        while len(word) > 1:
            candidates = pairs(word)
            best = min(candidates, key=lambda pair: self.bpe_ranks.get(pair, math.inf))
            if best not in self.bpe_ranks:
                break
            first, second = best
            merged: List[str] = []
            index = 0
            while index < len(word):
                try:
                    match = word.index(first, index)
                except ValueError:
                    merged.extend(word[index:])
                    break
                merged.extend(word[index:match])
                index = match
                if index < len(word) - 1 and word[index] == first and word[index + 1] == second:
                    merged.append(first + second)
                    index += 2
                else:
                    merged.append(word[index])
                    index += 1
            word = tuple(merged)
        self.cache[token] = word
        return word

    def encode(self, text: str) -> np.ndarray:
        token_ids: List[int] = [START_TOKEN_ID]
        for piece in self._pieces(self._clean(text)):
            if piece == self.START_TEXT:
                encoded_parts = (piece,)
            elif piece == self.END_TEXT:
                encoded_parts = (piece,)
            else:
                byte_token = "".join(self.byte_encoder[value] for value in piece.encode("utf-8"))
                encoded_parts = self._bpe(byte_token)
            for encoded_part in encoded_parts:
                # This reproduces the app's fallback to the EOT id.
                token_ids.append(self.vocab.get(encoded_part, END_TOKEN_ID))
                if len(token_ids) >= CONTEXT_LENGTH - 1:
                    break
            if len(token_ids) >= CONTEXT_LENGTH - 1:
                break
        token_ids.append(END_TOKEN_ID)

        # ClipTokenizer initializes every unused position with EOT, not zero.
        result = np.full((1, CONTEXT_LENGTH), END_TOKEN_ID, dtype=np.int64)
        result[0, : min(len(token_ids), CONTEXT_LENGTH)] = token_ids[:CONTEXT_LENGTH]
        return result


def quantize_rgb565(image: Image.Image) -> Image.Image:
    pixels = np.asarray(image.convert("RGB"), dtype=np.uint8)
    red5 = pixels[:, :, 0] >> 3
    green6 = pixels[:, :, 1] >> 2
    blue5 = pixels[:, :, 2] >> 3
    quantized = np.empty_like(pixels)
    quantized[:, :, 0] = (red5 << 3) | (red5 >> 2)
    quantized[:, :, 1] = (green6 << 2) | (green6 >> 4)
    quantized[:, :, 2] = (blue5 << 3) | (blue5 >> 2)
    return Image.fromarray(quantized, mode="RGB")


def preprocess_android(image: Image.Image, sample_size: int) -> np.ndarray:
    """Equivalent to IndexWorker decode followed by ImageEmbeddingEngine resize.

    BitmapFactory decoder subsampling is codec-dependent. On PC it is reproduced
    deterministically as a bilinear 1/sample_size decode before RGB565 storage.
    EXIF rotation is intentionally not applied, matching BitmapFactory usage.
    """
    rgb = image.convert("RGB")
    if sample_size > 1:
        decoded_size = (
            max(1, rgb.width // sample_size),
            max(1, rgb.height // sample_size),
        )
        rgb = rgb.resize(decoded_size, Image.Resampling.BILINEAR)
    rgb = quantize_rgb565(rgb)
    resized = rgb.resize((IMAGE_SIZE, IMAGE_SIZE), Image.Resampling.BILINEAR)
    return normalized_chw(resized)


def preprocess_clip(image: Image.Image) -> np.ndarray:
    """Standard CLIP resize-short-edge + bicubic + center-crop preprocessing."""
    rgb = image.convert("RGB")
    width, height = rgb.size
    if width <= height:
        resized_width = IMAGE_SIZE
        resized_height = max(IMAGE_SIZE, int(IMAGE_SIZE * height / width))
    else:
        resized_height = IMAGE_SIZE
        resized_width = max(IMAGE_SIZE, int(IMAGE_SIZE * width / height))
    resized = rgb.resize((resized_width, resized_height), Image.Resampling.BICUBIC)
    left = (resized_width - IMAGE_SIZE) // 2
    top = (resized_height - IMAGE_SIZE) // 2
    cropped = resized.crop((left, top, left + IMAGE_SIZE, top + IMAGE_SIZE))
    return normalized_chw(cropped)


def normalized_chw(image: Image.Image) -> np.ndarray:
    pixels = np.asarray(image, dtype=np.float32) / np.float32(255.0)
    pixels = (pixels - CLIP_MEAN) / CLIP_STD
    return np.transpose(pixels, (2, 0, 1))[None, :, :, :].astype(np.float32)


def android_l2_normalize(vector: np.ndarray) -> np.ndarray:
    flat = np.asarray(vector, dtype=np.float32).reshape(-1).copy()
    squared_sum = np.float32(0.0)
    for value in flat:
        squared_sum = np.float32(squared_sum + np.float32(value * value))
    norm = np.float32(math.sqrt(float(squared_sum)))
    if norm > 0:
        flat /= norm
    return flat


def android_cosine(first: np.ndarray, second: np.ndarray) -> float:
    dot = np.float32(0.0)
    first_norm = np.float32(0.0)
    second_norm = np.float32(0.0)
    for first_value, second_value in zip(first, second):
        dot = np.float32(dot + np.float32(first_value * second_value))
        first_norm = np.float32(first_norm + np.float32(first_value * first_value))
        second_norm = np.float32(second_norm + np.float32(second_value * second_value))
    denominator = math.sqrt(float(first_norm)) * math.sqrt(float(second_norm))
    return float(dot / denominator) if denominator > 0 else 0.0


def model_embedding(session: ort.InferenceSession, input_name: str, value: np.ndarray) -> np.ndarray:
    output = session.run(None, {input_name: value})[0]
    return android_l2_normalize(output)


def resolve_images(inputs: Sequence[str]) -> List[Path]:
    results: List[Path] = []
    for raw_input in inputs:
        path = Path(raw_input).expanduser()
        if path.is_dir():
            results.extend(
                candidate for candidate in path.rglob("*")
                if candidate.is_file() and candidate.suffix.lower() in IMAGE_SUFFIXES
            )
        elif path.is_file():
            results.append(path)
        else:
            # Path.glob cannot accept an absolute pattern, so split at its parent.
            parent = path.parent if str(path.parent) else Path(".")
            results.extend(candidate for candidate in parent.glob(path.name) if candidate.is_file())
    unique = {candidate.resolve(): candidate.resolve() for candidate in results}
    return sorted(unique.values(), key=lambda candidate: str(candidate).lower())


def rank_descending(values: Sequence[float]) -> List[int]:
    ordered = sorted(range(len(values)), key=lambda index: (-values[index], index))
    ranks = [0] * len(values)
    for rank, index in enumerate(ordered, start=1):
        ranks[index] = rank
    return ranks


def print_summary(label: str, values: Sequence[float]) -> None:
    scores = np.asarray(values, dtype=np.float64)
    print(
        f"{label}: min={scores.min():.9f} max={scores.max():.9f} "
        f"range={np.ptp(scores):.9f} mean={scores.mean():.9f} std={scores.std():.9f}"
    )


def parse_args() -> argparse.Namespace:
    repository = Path(__file__).resolve().parents[1]
    default_models = repository / "app" / "src" / "main" / "assets" / "models" / "clip"
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--query", default="car", help="Text query (default: car).")
    parser.add_argument(
        "--images", nargs="+", required=True,
        help="Image files, directories, or filename globs. Directories are recursive.",
    )
    parser.add_argument(
        "--model-dir", type=Path, default=default_models,
        help=f"Directory containing the four current CLIP assets (default: {default_models}).",
    )
    parser.add_argument(
        "--android-sample-size", type=int, default=8,
        help="IndexWorker BitmapFactory inSampleSize (default: 8).",
    )
    parser.add_argument("--csv", type=Path, help="Optional detailed CSV output path.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.android_sample_size < 1:
        raise ValueError("--android-sample-size must be at least 1")

    model_dir = args.model_dir.resolve()
    vision_path = model_dir / "vision_model_q4f16.onnx"
    text_path = model_dir / "text_model_uint8.onnx"
    vocab_path = model_dir / "vocab.json"
    merges_path = model_dir / "merges.txt"
    assets = [vision_path, text_path, vocab_path, merges_path]
    missing = [path for path in assets if not path.is_file()]
    if missing:
        raise FileNotFoundError("Missing model/tokenizer assets: " + ", ".join(map(str, missing)))

    images = resolve_images(args.images)
    if not images:
        raise FileNotFoundError("No supported images matched --images")

    print("Assets used (SHA-256 proves the exact package under test):")
    for asset in assets:
        print(f"  {asset.name}: {sha256(asset)}")
    print(f"Query: {args.query!r}")
    print(f"Images: {len(images)}")
    print(f"Android emulation inSampleSize: {args.android_sample_size}")

    providers = ["CPUExecutionProvider"]
    vision_session = ort.InferenceSession(str(vision_path), providers=providers)
    text_session = ort.InferenceSession(str(text_path), providers=providers)
    vision_inputs = {item.name: item for item in vision_session.get_inputs()}
    text_inputs = {item.name: item for item in text_session.get_inputs()}
    if "pixel_values" not in vision_inputs:
        raise RuntimeError(f"Vision model has no pixel_values input: {list(vision_inputs)}")
    if "input_ids" not in text_inputs:
        raise RuntimeError(f"Text model has no input_ids input: {list(text_inputs)}")
    print(f"Vision input: pixel_values {vision_inputs['pixel_values'].shape}")
    print(f"Text input: input_ids {text_inputs['input_ids'].shape}")

    tokenizer = AndroidClipTokenizer(vocab_path, merges_path)
    input_ids = tokenizer.encode(args.query)
    text_embedding = model_embedding(text_session, "input_ids", input_ids)
    print(f"Text embedding dimensions: {text_embedding.size}")
    print(f"input_ids: {input_ids[0].tolist()}")

    rows: List[dict] = []
    for index, image_path in enumerate(images, start=1):
        print(f"[{index}/{len(images)}] {image_path.name}", flush=True)
        with Image.open(image_path) as opened:
            # Do not call ImageOps.exif_transpose: Android BitmapFactory does not.
            opened.load()
            tensor_android = preprocess_android(opened, args.android_sample_size)
            tensor_correct = preprocess_clip(opened)
        embedding_android = model_embedding(vision_session, "pixel_values", tensor_android)
        embedding_correct = model_embedding(vision_session, "pixel_values", tensor_correct)
        score_android = android_cosine(text_embedding, embedding_android)
        score_correct = android_cosine(text_embedding, embedding_correct)
        rows.append(
            {
                "filename": image_path.name,
                "path": str(image_path),
                "score_android_a": score_android,
                "score_correct_b": score_correct,
                "delta_b_minus_a": score_correct - score_android,
            }
        )

    scores_android = [row["score_android_a"] for row in rows]
    scores_correct = [row["score_correct_b"] for row in rows]
    ranks_android = rank_descending(scores_android)
    ranks_correct = rank_descending(scores_correct)
    for row, rank_android, rank_correct in zip(rows, ranks_android, ranks_correct):
        row["rank_android_a"] = rank_android
        row["rank_correct_b"] = rank_correct

    print("\nScores (sorted by Android/A score):")
    print(f"{'A#':>3} {'B#':>3} {'Android A':>12} {'Correct B':>12} {'B-A':>12}  file")
    for row in sorted(rows, key=lambda item: (-item["score_android_a"], item["path"])):
        print(
            f"{row['rank_android_a']:3d} {row['rank_correct_b']:3d} "
            f"{row['score_android_a']:12.9f} {row['score_correct_b']:12.9f} "
            f"{row['delta_b_minus_a']:12.9f}  {row['filename']}"
        )

    print("\nDistribution:")
    print_summary("A Android", scores_android)
    print_summary("B correct", scores_correct)
    range_android = float(np.ptp(np.asarray(scores_android, dtype=np.float64)))
    range_correct = float(np.ptp(np.asarray(scores_correct, dtype=np.float64)))
    if range_android > 0:
        print(f"B/A score-range ratio: {range_correct / range_android:.6f}x")
    else:
        print("B/A score-range ratio: undefined (A range is zero)")
    if len(rows) > 1:
        pearson = float(np.corrcoef(scores_android, scores_correct)[0, 1])
        rank_pearson = float(np.corrcoef(ranks_android, ranks_correct)[0, 1])
        print(f"A/B Pearson correlation: {pearson:.6f}")
        print(f"A/B rank correlation: {rank_pearson:.6f}")

    print(
        "\nInterpretation: B uses the identical quantized Android ONNX models, tokenizer, "
        "normalization and cosine. Any repeatable widening or ranking improvement from A "
        "to B is therefore attributable to preprocessing. Compression that remains in B "
        "belongs to this model package/embedding space for this query and image set."
    )

    if args.csv:
        args.csv.parent.mkdir(parents=True, exist_ok=True)
        fields = [
            "filename", "path", "score_android_a", "score_correct_b",
            "delta_b_minus_a", "rank_android_a", "rank_correct_b",
        ]
        with args.csv.open("w", newline="", encoding="utf-8-sig") as stream:
            writer = csv.DictWriter(stream, fieldnames=fields)
            writer.writeheader()
            writer.writerows(rows)
        print(f"CSV written: {args.csv.resolve()}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as error:
        print(f"ERROR: {error}", file=sys.stderr)
        raise
