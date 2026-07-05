package com.bliss.aimemorysearch.ai.multilingual;

public final class EmbeddingUtils {

    private EmbeddingUtils() {
    }

    public static void normalize(float[] vector) {

        float norm = 0f;

        for (float v : vector) {
            norm += v * v;
        }

        norm = (float) Math.sqrt(norm);

        if (norm == 0f) {
            return;
        }

        for (int i = 0; i < vector.length; i++) {
            vector[i] /= norm;
        }
    }
}