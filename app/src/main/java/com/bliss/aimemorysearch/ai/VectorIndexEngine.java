package com.bliss.aimemorysearch.ai;

import com.bliss.aimemorysearch.db.ChunkEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VectorIndexEngine {

    public static List<ScoredChunk> getTopK(
            float[] queryEmbedding,
            List<ChunkEntity> chunks,
            int topK
    ) {

        List<ScoredChunk> scoredChunks =
                new ArrayList<>();

        if (queryEmbedding == null || queryEmbedding.length == 0) {
            return scoredChunks;
        }

        if (chunks == null || chunks.isEmpty()) {
            return scoredChunks;
        }

        for (ChunkEntity chunk : chunks) {

            if (chunk == null) {
                continue;
            }

            if (chunk.embedding == null || chunk.embedding.length == 0) {
                continue;
            }

            float[] chunkEmbedding =
                    EmbeddingUtils.bytesToFloatArray(
                            chunk.embedding
                    );

            if (chunkEmbedding == null || chunkEmbedding.length == 0) {
                continue;
            }

            float score =
                    VectorUtils.cosineSimilarity(
                            queryEmbedding,
                            chunkEmbedding
                    );

            scoredChunks.add(
                    new ScoredChunk(
                            chunk,
                            score
                    )
            );
        }

        Collections.sort(
                scoredChunks,
                (a, b) -> Float.compare(
                        b.score,
                        a.score
                )
        );

        if (scoredChunks.size() > topK) {
            return new ArrayList<>(
                    scoredChunks.subList(
                            0,
                            topK
                    )
            );
        }

        return scoredChunks;
    }

    public static class ScoredChunk {

        public ChunkEntity chunk;
        public float score;

        public ScoredChunk(
                ChunkEntity chunk,
                float score
        ) {

            this.chunk = chunk;
            this.score = score;
        }
    }
}