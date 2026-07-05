package com.bliss.aimemorysearch.ai;

import com.bliss.aimemorysearch.db.ChunkEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class HybridCandidateRetriever {

    public static List<ChunkEntity> retrieveCandidates(
            String query,
            List<ChunkEntity> allChunks,
            List<VectorIndexEngine.ScoredChunk> semanticChunks
    ) {

        List<ChunkEntity> finalCandidates =
                new ArrayList<>();

        HashSet<String> added =
                new HashSet<>();

        if (semanticChunks != null) {

            for (
                    VectorIndexEngine.ScoredChunk scoredChunk
                    : semanticChunks
            ) {

                if (scoredChunk == null) {
                    continue;
                }

                if (scoredChunk.chunk == null) {
                    continue;
                }

                String key =
                        scoredChunk.chunk.filePath
                                + "_"
                                + scoredChunk.chunk.chunkIndex;

                if (added.contains(key)) {
                    continue;
                }

                added.add(key);

                finalCandidates.add(
                        scoredChunk.chunk
                );
            }
        }

        if (
                query == null
                        ||
                        query.trim().isEmpty()
        ) {

            return finalCandidates;
        }

        String normalizedQuery =
                query.toLowerCase();

        String[] tokens =
                normalizedQuery.split("\\s+");

        if (allChunks != null) {

            for (ChunkEntity chunk : allChunks) {

                if (chunk == null) {
                    continue;
                }

                if (chunk.chunkText == null) {
                    continue;
                }

                String lowerChunk =
                        chunk.chunkText.toLowerCase();

                int tokenMatches = 0;

                for (String token : tokens) {

                    token = token.trim();

                    if (token.length() < 2) {
                        continue;
                    }

                    if (
                            lowerChunk.contains(token)
                    ) {

                        tokenMatches++;
                    }
                }

                if (
                        tokens.length >= 2
                                &&
                                tokenMatches < 2
                ) {

                    continue;
                }

                if (
                        tokens.length == 1
                                &&
                                tokenMatches == 0
                ) {

                    continue;
                }

                String key =
                        chunk.filePath
                                + "_"
                                + chunk.chunkIndex;

                if (added.contains(key)) {
                    continue;
                }

                added.add(key);

                finalCandidates.add(chunk);
            }
        }

        return finalCandidates;
    }
}