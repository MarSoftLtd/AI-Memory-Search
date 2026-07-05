package com.bliss.aimemorysearch.ai;

import android.content.Context;
import android.util.Log;

import com.bliss.aimemorysearch.SearchResultsHolder;
import com.bliss.aimemorysearch.db.AppDatabase;
import com.bliss.aimemorysearch.db.FileEntity;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SemanticSearchEngine {

    private static final String TAG =
            "SEMANTIC_SEARCH";

    public static List<FileEntity> search(
            Context context,
            String query
    ) {

        List<FileEntity> finalResults =
                new ArrayList<>();

        if (context == null) {
            return finalResults;
        }

        if (
                query == null
                        ||
                        query.trim().isEmpty()
        ) {
            return finalResults;
        }

        try {

            String normalizedQuery =
                    normalize(query);

            String[] queryTokens =
                    normalizedQuery.split("\\s+");

            EmbeddingEngine embeddingEngine =
                    EmbeddingEngine.getInstance();

            float[] queryEmbedding =
                    embeddingEngine.generateEmbedding(
                            normalizedQuery
                    );
            android.util.Log.d(
                    "IMAGE_TEST",
                    "QUERY VECTOR = "
                            + queryEmbedding.length
            );
            
            if (
                    queryEmbedding == null
                            ||
                            queryEmbedding.length == 0
            ) {

                return finalResults;
            }

            AppDatabase database =
                    AppDatabase.getInstance(context);
            ImageSemanticSearchEngine imageEngine =
                    new ImageSemanticSearchEngine(
                            context
                    );
            List<ImageSemanticSearchEngine.SearchResult>
                    imageResults =
                    imageEngine.search(
                            queryEmbedding,
                            20
                    );
            android.util.Log.d(
                    "IMAGE_FLOW",
                    "SEARCH CALLED"
            );

            android.util.Log.d(
                    "IMAGE_FLOW",
                    "RESULT SIZE = "
                            + imageResults.size()
            );
            android.util.Log.d(
                    "IMAGE_TEST",
                    "IMAGE RESULTS = "
                            + imageResults.size()
            );
            List<FileEntity> allFiles =
                    database.fileDao()
                            .getAllFilesSync();

            if (
                    allFiles == null
                            ||
                            allFiles.isEmpty()
            ) {

                return finalResults;
            }

            /*
             * REAL CANDIDATE FILTERING
             */

            List<FileEntity> candidateFiles =
                    new ArrayList<>();

            for (FileEntity entity : allFiles) {

                if (entity == null) {
                    continue;
                }

                String searchableText =
                        normalize(
                                (
                                        safe(entity.name)
                                                + " "
                                                + safe(entity.ocrText)
                                                + " "
                                                + safe(entity.path)
                                )
                        );

                int matchedTokens = 0;

                int validTokens = 0;

                for (String token : queryTokens) {

                    if (
                            token == null
                                    ||
                                    token.trim().isEmpty()
                    ) {
                        continue;
                    }

                    validTokens++;

                    if (
                            searchableText.contains(token)
                    ) {

                        matchedTokens++;
                    }
                }

                if (validTokens == 0) {
                    continue;
                }

                float coverage =
                        (float) matchedTokens
                                /
                                (float) validTokens;

                /*
                 * STRICT FILTER
                 */

                if (
                        validTokens >= 2
                                &&
                                coverage < 0.60f
                ) {

                    continue;
                }

                candidateFiles.add(entity);
                Log.d(
                        "SEARCH_FILTER",
                        "CANDIDATE PASSED = "
                                + entity.name
                                + " | coverage = "
                                + coverage
                                + " | matched = "
                                + matchedTokens
                                + "/"
                                + validTokens
                );
            }

            List<ScoredFile> scoredFiles =
                    new ArrayList<>();

            for (FileEntity entity : candidateFiles) {

                if (
                        entity.embedding == null
                                ||
                                entity.embedding.length == 0
                ) {
                    continue;
                }

                float[] fileEmbedding =
                        EmbeddingUtils.bytesToFloatArray(
                                entity.embedding
                        );

                if (
                        fileEmbedding == null
                                ||
                                fileEmbedding.length == 0
                ) {
                    continue;
                }

                float semanticScore =
                        VectorUtils.cosineSimilarity(
                                queryEmbedding,
                                fileEmbedding
                        );

                float finalScore =
                        SearchScoringEngine
                                .calculateFinalScore(
                                        normalizedQuery,
                                        entity,
                                        semanticScore
                                );

                /*
                 * HARD THRESHOLD
                 */
                Log.d(
                        "SEARCH_SCORE",
                        entity.name
                                + " | semantic = "
                                + semanticScore
                                + " | final = "
                                + finalScore
                );
                if (finalScore < 1.20f) {
                    continue;
                }

                scoredFiles.add(
                        new ScoredFile(
                                entity,
                                finalScore
                        )
                );

                Log.d(
                        TAG,
                        "PASSED = "
                                + entity.name
                                + " | score = "
                                + finalScore
                );
            }

            Collections.sort(
                    scoredFiles,
                    (a, b) ->
                            Float.compare(
                                    b.similarity,
                                    a.similarity
                            )
            );

            if (scoredFiles.size() > 50) {

                scoredFiles =
                        scoredFiles.subList(
                                0,
                                50
                        );
            }

            for (ScoredFile scoredFile : scoredFiles) {

                finalResults.add(
                        scoredFile.fileEntity
                );

                Log.d(
                        TAG,
                        "FINAL RESULT = "
                                + scoredFile.fileEntity.name
                                + " | score = "
                                + scoredFile.similarity
                );
            }

            SearchResultsHolder.results =
                    finalResults;

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Semantic search failed",
                    e
            );
        }

        return finalResults;
    }

    private static String normalize(
            String text
    ) {

        if (text == null) {
            return "";
        }

        String normalized =
                Normalizer.normalize(
                        text,
                        Normalizer.Form.NFD
                );

        normalized =
                normalized.replaceAll(
                        "\\p{InCombiningDiacriticalMarks}+",
                        ""
                );

        normalized =
                normalized.toLowerCase(
                        Locale.ROOT
                );

        normalized =
                normalized.replaceAll(
                        "[^a-z0-9 ]",
                        " "
                );

        normalized =
                normalized.replaceAll(
                        "\\s+",
                        " "
                );

        return normalized.trim();
    }

    private static String safe(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value;
    }

    private static class ScoredFile {

        FileEntity fileEntity;

        float similarity;

        ScoredFile(
                FileEntity fileEntity,
                float similarity
        ) {

            this.fileEntity =
                    fileEntity;

            this.similarity =
                    similarity;
        }
    }
}