import json
import math
import re
import sys
import time
import unicodedata
from pathlib import Path

import numpy as np
import onnxruntime as ort
import sentencepiece as spm


ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
E5_DIR = ROOT / "package-builder" / "sources" / "e5" / "payload"
CORE_DIR = ROOT / "package-builder" / "sources" / "embedding-core" / "payload"


class OfficialE5:
    def __init__(self):
        self.sp = spm.SentencePieceProcessor(model_file=str(E5_DIR / "sentencepiece.bpe.model"))
        self.session = ort.InferenceSession(str(E5_DIR / "model.onnx"), providers=["CPUExecutionProvider"])

    def embed(self, text, role):
        prefixed = f"{role}: {text.strip()}"
        raw = self.sp.encode(prefixed, out_type=int)[:510]
        ids = np.asarray([[0] + raw + [2]], dtype=np.int64)
        mask = np.ones_like(ids, dtype=np.int64)
        token_embeddings = self.session.run(None, {"input_ids": ids, "attention_mask": mask})[0]
        pooled = (token_embeddings * mask[..., None]).sum(axis=1) / mask.sum(axis=1, keepdims=True)
        vector = pooled[0]
        return vector / np.linalg.norm(vector)


class ProductionEmbeddingCore:
    def __init__(self):
        with (CORE_DIR / "vocab.txt").open(encoding="utf-8") as source:
            self.vocab = {line.rstrip("\r\n"): index for index, line in enumerate(source)}
        self.session = ort.InferenceSession(str(CORE_DIR / "model.onnx"), providers=["CPUExecutionProvider"])

    def _wordpiece(self, word):
        if len(word) > 100:
            return [100]
        output = []
        start = 0
        while start < len(word):
            end = len(word)
            found = None
            while start < end:
                piece = word[start:end]
                if start > 0:
                    piece = "##" + piece
                if piece in self.vocab:
                    found = piece
                    break
                end -= 1
            if found is None:
                return [100]
            output.append(self.vocab[found])
            start = end
        return output

    def embed(self, text, role=None):
        normalized = unicodedata.normalize("NFD", text.lower().strip())
        normalized = "".join(char for char in normalized if unicodedata.category(char) != "Mn")
        normalized = re.sub(r"[^a-z0-9]+", " ", normalized)
        normalized = re.sub(r"\s+", " ", normalized).strip()
        token_ids = [101]
        for word in normalized.split() if normalized else []:
            for token_id in self._wordpiece(word):
                if len(token_ids) >= 127:
                    break
                token_ids.append(token_id)
            if len(token_ids) >= 127:
                break
        token_ids.append(102)
        ids = np.zeros((1, 128), dtype=np.int64)
        mask = np.zeros((1, 128), dtype=np.int64)
        token_types = np.zeros((1, 128), dtype=np.int64)
        ids[0, :len(token_ids)] = token_ids
        mask[0, :len(token_ids)] = 1
        hidden = self.session.run(None, {
            "input_ids": ids,
            "attention_mask": mask,
            "token_type_ids": token_types,
        })[0]
        pooled = (hidden * mask[..., None]).sum(axis=1) / mask.sum(axis=1, keepdims=True)
        vector = pooled[0]
        return vector / np.linalg.norm(vector)


def percentile(values, percentile_value):
    return float(np.percentile(np.asarray(values, dtype=np.float64), percentile_value))


def benchmark_model(name, model, documents, queries):
    passage_times = []
    passage_vectors = []
    for document in documents:
        start = time.perf_counter()
        passage_vectors.append(model.embed(document["text"], "passage"))
        passage_times.append((time.perf_counter() - start) * 1000.0)
    matrix = np.stack(passage_vectors)
    ids = [document["id"] for document in documents]

    query_times = []
    ranks = []
    failures = []
    language_ranks = {}
    for query in queries:
        start = time.perf_counter()
        query_vector = model.embed(query["text"], "query")
        query_times.append((time.perf_counter() - start) * 1000.0)
        scores = matrix @ query_vector
        order = np.argsort(-scores)
        ranked_ids = [ids[index] for index in order]
        rank = ranked_ids.index(query["relevant"]) + 1
        ranks.append(rank)
        language_ranks.setdefault(query["language"], []).append(rank)
        if rank != 1:
            failures.append({
                "query": query["text"],
                "language": query["language"],
                "expected": query["relevant"],
                "rank": rank,
                "expected_score": float(scores[ids.index(query["relevant"])]),
                "top": [{"id": ids[index], "score": float(scores[index])} for index in order[:5]],
            })

    def metrics(values):
        return {
            "count": len(values),
            "recall_at_1": sum(rank <= 1 for rank in values) / len(values),
            "recall_at_5": sum(rank <= 5 for rank in values) / len(values),
            "recall_at_10": sum(rank <= 10 for rank in values) / len(values),
            "mrr": sum(1.0 / rank for rank in values) / len(values),
        }

    return {
        "model": name,
        "metrics": metrics(ranks),
        "metrics_by_language": {language: metrics(values) for language, values in sorted(language_ranks.items())},
        "timing_ms": {
            "passage_total": sum(passage_times),
            "passage_mean": sum(passage_times) / len(passage_times),
            "passage_p50": percentile(passage_times, 50),
            "passage_p95": percentile(passage_times, 95),
            "query_mean": sum(query_times) / len(query_times),
            "query_p50": percentile(query_times, 50),
            "query_p95": percentile(query_times, 95),
        },
        "failures": failures,
    }


def main():
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    if hasattr(sys.stderr, "reconfigure"):
        sys.stderr.reconfigure(encoding="utf-8")

    with (HERE / "dataset.json").open(encoding="utf-8") as source:
        dataset = json.load(source)
    documents = dataset["documents"]
    queries = dataset["queries"]

    started = time.perf_counter()
    e5_started = time.perf_counter()
    e5 = OfficialE5()
    e5_load_ms = (time.perf_counter() - e5_started) * 1000.0
    e5_result = benchmark_model("official_multilingual_e5", e5, documents, queries)
    e5_result["model_load_ms"] = e5_load_ms
    del e5

    core_started = time.perf_counter()
    core = ProductionEmbeddingCore()
    core_load_ms = (time.perf_counter() - core_started) * 1000.0
    core_result = benchmark_model("production_embedding_core", core, documents, queries)
    core_result["model_load_ms"] = core_load_ms

    result = {
        "methodology": {
            "documents": len(documents),
            "queries": len(queries),
            "languages": sorted({query["language"] for query in queries}),
            "e5_query_format": "query: <text>",
            "e5_passage_format": "passage: <text>",
            "ranking": "exhaustive cosine over temporary in-memory passage embeddings",
            "production_database_used": False,
        },
        "models": [e5_result, core_result],
        "raw_embedding_storage_bytes": {
            "per_vector": 384 * 4,
            "all_20_documents": len(documents) * 384 * 4,
            "per_100000_chunks": 100000 * 384 * 4,
            "per_500000_chunks": 500000 * 384 * 4,
        },
        "total_run_seconds": time.perf_counter() - started,
    }
    output = HERE / "results.json"
    output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False, indent=2))
    print(f"Results written to {output}", file=sys.stderr)


if __name__ == "__main__":
    main()
