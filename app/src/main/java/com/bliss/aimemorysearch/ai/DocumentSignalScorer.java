package com.bliss.aimemorysearch.ai;

import com.bliss.aimemorysearch.db.ChunkEntity;

public class DocumentSignalScorer {

/*    public static float calculateSignalScore(
            String[] queryTokens,
            ChunkEntity chunk
    ) {
        {
            if (chunk == null) {
                return 0f;
            }

            String text =
                    chunk.chunkText == null
                            ? ""
                            : chunk.chunkText.toLowerCase();

            String name =
                    chunk.fileName == null
                            ? ""
                            : chunk.fileName.toLowerCase();

            float score = 0f;

            int totalTokens = 0;
            int matchedTokens = 0;

            if (queryTokens != null) {

                for (String token : queryTokens) {

                    if (token == null) {
                        continue;
                    }

                    token = token.trim().toLowerCase();

                    if (token.length() < 2) {
                        continue;
                    }

                    totalTokens++;

                    if (
                            text.contains(token)
                                    ||
                                    name.contains(token)
                    ) {

                        matchedTokens++;
                    }
                }
            }

            if (totalTokens > 0) {

                float coverage =
                        (float) matchedTokens
                                /
                                (float) totalTokens;

                score += coverage * 5.0f;

                if (coverage == 1.0f) {
                    score += 5.0f;
                }
            }

            return score;
        }
    }*/

    public static float calculateSignalScore(
            String[] queryTokens,
            ChunkEntity chunk
    ) {

        return 0f;
    }
}