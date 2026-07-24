import json
import os
import sys
import time

import psutil

from evaluate import OfficialE5, ProductionEmbeddingCore


def rss_bytes():
    return psutil.Process(os.getpid()).memory_info().rss


def main():
    if len(sys.argv) != 2 or sys.argv[1] not in {"e5", "core"}:
        raise SystemExit("usage: measure_memory.py e5|core")
    baseline = rss_bytes()
    started = time.perf_counter()
    if sys.argv[1] == "e5":
        model = OfficialE5()
        role = "passage"
    else:
        model = ProductionEmbeddingCore()
        role = None
    loaded = rss_bytes()
    vector = model.embed(
        "Factura de apă potabilă pentru luna iunie. Consum 14 metri cubi și total de plată 128 lei.",
        role,
    )
    after_inference = rss_bytes()
    print(json.dumps({
        "model": sys.argv[1],
        "baseline_rss_bytes": baseline,
        "loaded_rss_bytes": loaded,
        "after_inference_rss_bytes": after_inference,
        "load_delta_bytes": loaded - baseline,
        "first_inference_delta_bytes": after_inference - loaded,
        "embedding_dimensions": len(vector),
        "elapsed_ms": (time.perf_counter() - started) * 1000.0,
    }, indent=2))


if __name__ == "__main__":
    main()
