package com.bliss.aimemorysearch.ai;

public class VectorUtils {

    public static float cosineSimilarity(
            float[] a,
            float[] b
    ) {

        if (
                a == null ||
                        b == null
        ) {
            return 0f;
        }

        if (
                a.length != b.length
        ) {
            return 0f;
        }

        float dot = 0f;

        float normA = 0f;

        float normB = 0f;

        for (
                int i = 0;
                i < a.length;
                i++
        ) {

            dot += a[i] * b[i];

            normA += a[i] * a[i];

            normB += b[i] * b[i];
        }

        if (
                normA == 0f ||
                        normB == 0f
        ) {
            return 0f;
        }

        return (float)
                (
                        dot /
                                (
                                        Math.sqrt(normA)
                                                *
                                                Math.sqrt(normB)
                                )
                );
    }
}