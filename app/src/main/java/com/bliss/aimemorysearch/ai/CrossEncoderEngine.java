package com.bliss.aimemorysearch.ai;

public class CrossEncoderEngine {

    public static float rerankScore(
            String query,
            String chunkText,
            float semanticScore,
            float lexicalScore
    ) {

        if (query == null) {
            return 0f;
        }

        if (chunkText == null) {
            return 0f;
        }

        String normalizedQuery =
                query.toLowerCase().trim();

        String normalizedChunk =
                chunkText.toLowerCase();

        float score = 0f;

        score += semanticScore * 0.45f;

        score += lexicalScore * 0.55f;

        String[] queryTokens =
                normalizedQuery.split("\\s+");

        int exactMatches = 0;

        for (String token : queryTokens) {

            token = token.trim();

            if (token.length() < 2) {
                continue;
            }

            if (
                    normalizedChunk.contains(token)
            ) {

                exactMatches++;
            }
        }

        if (queryTokens.length > 0) {

            float tokenCoverage =
                    (float) exactMatches
                            /
                            (float) queryTokens.length;

            score += tokenCoverage * 1.20f;
        }

        if (
                normalizedChunk.contains(
                        normalizedQuery
                )
        ) {

            score += 2.50f;
        }

        if (
                queryTokens.length >= 2
                        &&
                        exactMatches == queryTokens.length
        ) {

            score += 4.00f;
        }
        int proximityBonus =
                calculateTokenProximity(
                        normalizedChunk,
                        queryTokens
                );

        score += proximityBonus * 0.85f;
        return score;
    }
    private static int calculateTokenProximity(
            String chunk,
            String[] tokens
    ) {

        if (tokens == null) {
            return 0;
        }

        if (tokens.length < 2) {
            return 0;
        }

        int bestScore = 0;

        for (int i = 0; i < tokens.length; i++) {

            for (int j = i + 1; j < tokens.length; j++) {

                String tokenA =
                        tokens[i];

                String tokenB =
                        tokens[j];

                if (tokenA == null || tokenB == null) {
                    continue;
                }

                int indexA =
                        chunk.indexOf(tokenA);

                int indexB =
                        chunk.indexOf(tokenB);

                if (indexA == -1 || indexB == -1) {
                    continue;
                }

                int distance =
                        Math.abs(
                                indexA - indexB
                        );

                if (distance < 30) {

                    bestScore += 4;

                } else if (distance < 80) {

                    bestScore += 2;

                } else if (distance < 150) {

                    bestScore += 1;
                }
            }
        }

        return bestScore;
    }
}