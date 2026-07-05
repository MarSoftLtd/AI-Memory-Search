package com.bliss.aimemorysearch.ai.multilingual;

public class EmbeddingResult {

    private final float[] embedding;
    private final long inferenceTimeMs;

    public EmbeddingResult(float[] embedding, long inferenceTimeMs) {
        this.embedding = embedding;
        this.inferenceTimeMs = inferenceTimeMs;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public long getInferenceTimeMs() {
        return inferenceTimeMs;
    }
}