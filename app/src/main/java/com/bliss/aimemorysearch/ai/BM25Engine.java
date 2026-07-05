package com.bliss.aimemorysearch.ai;

import java.util.HashMap;
import java.util.Map;

public class BM25Engine {
    private static final float k1 = 1.5f;
    private static final float b = 0.75f;
    public static float score(
            String query,
            String document
    ) {
        if (query == null || document == null) {
            return 0f;
        }
        String[] queryTokens =
                query.toLowerCase().split("\\s+");
        String[] documentTokens =
                document.toLowerCase().split("\\s+");
        if (documentTokens.length == 0) {
            return 0f;
        }
        Map<String, Integer> frequencies =
                new HashMap<>();
        for (String token : documentTokens) {

            token = token.trim();

            if (token.length() < 2) {
                continue;
            }

            Integer count =
                    frequencies.get(token);

            if (count == null) {
                count = 0;
            }

            frequencies.put(
                    token,
                    count + 1
            );
        }
        float score = 0f;
        int matchedTokens = 0;
        float avgDocLength = 120f;
        int docLength =
                documentTokens.length;
        for (String token : queryTokens) {

            token = token.trim();

            if (token.length() < 2) {
                continue;
            }

            int freq =
                    frequencies.containsKey(token)
                            ? frequencies.get(token)
                            : 0;
            if (freq == 0) {

                continue;
            }

            float numerator =
                    freq * (k1 + 1f);

            float denominator =
                    freq
                            +
                            k1 * (
                                    1f
                                            -
                                            b
                                            +
                                            b
                                                    *
                                                    (
                                                            (float) docLength
                                                                    /
                                                                    avgDocLength
                                                    )
                            );

            score +=
                    numerator / denominator;
            matchedTokens++;
        }
        if (
                matchedTokens
                        ==
                        queryTokens.length
        ) {

            score += 3.5f;
        }
        return score;
    }
}