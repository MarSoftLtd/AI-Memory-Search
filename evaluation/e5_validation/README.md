# M-E5-VALIDATION offline harness

This harness creates temporary in-memory passage embeddings from the synthetic
Romanian document collection in `dataset.json`. It does not open or modify the
Android production database.

It compares:

- shipped multilingual E5 using `query:` and `passage:` prefixes;
- the current production `EmbeddingEngine` model and tokenizer behavior.

Run from the repository root:

```powershell
python evaluation/e5_validation/evaluate.py
```

The detailed metrics and failure cases are written to `results.json`.
