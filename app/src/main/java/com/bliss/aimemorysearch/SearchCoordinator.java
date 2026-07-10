package com.bliss.aimemorysearch;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.bliss.aimemorysearch.ai.ChunkSemanticSearchEngine;
import com.bliss.aimemorysearch.ai.ImageSemanticSearchEngine;
import com.bliss.aimemorysearch.ai.MobileClipTextEmbeddingEngine;
import com.bliss.aimemorysearch.ai.MultilingualQueryNormalizer;
import com.bliss.aimemorysearch.ai.QueryUnderstandingEngine;
import com.bliss.aimemorysearch.ai.SearchAnalysis;
import com.bliss.aimemorysearch.ai.SearchRequest;
import com.bliss.aimemorysearch.db.AppDatabase;
import com.bliss.aimemorysearch.db.FileEntity;

import java.util.List;

public final class SearchCoordinator {

    public interface Callback {

        void onSearchCompleted(
                List<FileEntity> finalResults
        );
    }

    private final Context context;
    private final Handler mainHandler;

    public SearchCoordinator(
            Context context
    ) {
        this.context =
                context.getApplicationContext();
        this.mainHandler =
                new Handler(
                        Looper.getMainLooper()
                );
    }

    public void search(
            String query,
            Callback callback
    ) {
        android.util.Log.e(
                "SEARCH_ENTRY",
                "MAINACTIVITY SEARCH CALLED = " + query
        );
        if (query == null || query.trim().isEmpty()) {

            return;
        }

        new Thread(() -> {

            SearchRequest
                    queryRequest =
                    QueryUnderstandingEngine.createSearchRequest(
                            query.trim()
                    );
            SearchAnalysis
                    queryAnalysis =
                    QueryUnderstandingEngine.createSearchAnalysis(
                            queryRequest
                    );
            List<ChunkSemanticSearchEngine.ChunkResult>
                    chunkResults =
                    ChunkSemanticSearchEngine.search(
                            context,
                            queryRequest,
                            queryAnalysis
                    );
            boolean prefixDocumentCandidate =
                    hasStrongVocabularyPrefixExpansion(
                            queryRequest,
                            queryAnalysis
                    );

            if (
                    queryAnalysis.isImageIntent()
                            &&
                            !queryAnalysis.isDocumentIntent()
                            &&
                            !prefixDocumentCandidate
            ) {

                chunkResults =
                        new java.util.ArrayList<>();
            }

            final String[] clipQueryHolder =
                    new String[]{
                            query.trim()
                    };

            java.util.concurrent.CountDownLatch normalizerLatch =
                    new java.util.concurrent.CountDownLatch(1);

            MultilingualQueryNormalizer
                    .getInstance(context)
                    .normalizeForClip(
                            query.trim(),
                            new MultilingualQueryNormalizer.Callback() {
                                @Override
                                public void onReady(
                                        String originalQuery,
                                        String englishClipQuery
                                ) {
                                    clipQueryHolder[0] =
                                            englishClipQuery;

                                    normalizerLatch.countDown();
                                }

                                @Override
                                public void onError(
                                        String originalQuery
                                ) {
                                    normalizerLatch.countDown();
                                }
                            }
                    );

            try {
                normalizerLatch.await(
                        20,
                        java.util.concurrent.TimeUnit.SECONDS
                );
            } catch (Exception e) {
                e.printStackTrace();
            }

            SearchRequest
                    imageSearchRequest =
                    QueryUnderstandingEngine.createSearchRequest(
                            clipQueryHolder[0]
                    );
            SearchAnalysis
                    imageSearchAnalysis =
                    QueryUnderstandingEngine.createSearchAnalysis(
                            imageSearchRequest
                    );

            boolean imagePrefixDocumentCandidate =
                    hasStrongVocabularyPrefixExpansion(
                            imageSearchRequest,
                            imageSearchAnalysis
                    );

            float[] imageQueryEmbedding =
                    MobileClipTextEmbeddingEngine
                            .getInstance()
                            .generateEmbedding(
                                    clipQueryHolder[0]
                            );

            ImageSemanticSearchEngine imageEngine =
                    new ImageSemanticSearchEngine(
                            context
                    );

            List<ImageSemanticSearchEngine.SearchResult>
                    imageResults =
                    imageEngine.search(
                            imageQueryEmbedding,
                            10
                    );
            for (
                    ImageSemanticSearchEngine.SearchResult r
                    : imageResults
            ) {

                android.util.Log.e(
                        "IMAGE_RAW",
                        r.file.name
                                + " | "
                                + r.score
                );
            }
            android.util.Log.d(
                    "IMAGE_SEARCH",
                    "RESULTS = "
                            + imageResults.size()
            );

            java.util.LinkedHashMap<String, FileEntity>
                    uniqueFiles =
                    new java.util.LinkedHashMap<>();

            AppDatabase database =
                    AppDatabase.getInstance(
                            context
                    );

            SearchExplanationHolder.clear();

            for (
                    ChunkSemanticSearchEngine.ChunkResult result
                    : chunkResults
            ) {

                if (result == null) {
                    continue;
                }

                if (result.chunk == null) {
                    continue;
                }

                String path =
                        result.chunk.filePath;

                if (path == null) {
                    continue;
                }

                if (
                        uniqueFiles.containsKey(path)
                ) {
                    continue;
                }

                FileEntity entity =
                        database.fileDao()
                                .getFileByPath(path);

                if (entity != null) {
                    android.util.Log.e(
                            "CHUNK_RESULT",
                            entity.name
                                    + " | "
                                    + entity.type
                                    + " | "
                                    + result.score
                    );
                    uniqueFiles.put(
                            path,
                            entity
                    );

                    SearchExplanationHolder.snippets.put(
                            path,
                            result.matchedSnippet
                    );
                    android.util.Log.d(
                            "SNIPPET_UI",
                            result.chunk.fileName
                                    + " => "
                                    + result.matchedSnippet
                    );
                    SearchExplanationHolder.scores.put(
                            path,
                            result.score
                    );
                }
            }

            java.util.Map<String, Float> ranking =
                    new java.util.HashMap<>();

            for (
                    ChunkSemanticSearchEngine.ChunkResult result
                    : chunkResults
            ) {

                if (
                        result == null
                                ||
                                result.chunk == null
                                ||
                                result.chunk.filePath == null
                ) {
                    continue;
                }

                Float existingScore =
                        ranking.get(
                                result.chunk.filePath
                        );

                if (
                        existingScore == null
                ) {

                    ranking.put(
                            result.chunk.filePath,
                            result.score
                    );

                } else {

                    ranking.put(
                            result.chunk.filePath,
                            Math.max(
                                    existingScore,
                                    result.score
                            )
                    );
                }
                android.util.Log.e(
                        "RANK_BUILD",
                        result.chunk.fileName
                                + " | "
                                + result.chunk.filePath
                                + " | "
                                + result.score
                );
            }

            for (
                    ImageSemanticSearchEngine.SearchResult result
                    : imageResults
            ) {

                if (
                        result == null
                                ||
                                result.file == null
                ) {
                    continue;
                }

                android.util.Log.e(
                        "IMAGE_FILTER",
                        result.file.name
                                + " | OCR="
                                + (
                                result.file.ocrText == null
                                        ? "NULL"
                                        : result.file.ocrText.length()
                        )
                );
                android.util.Log.e(
                        "IMAGE_OCR",
                        result.file.name
                                + " -> "
                                + result.file.ocrText
                );
                float imageScore;

                if (
                        (
                                imageSearchAnalysis.isDocumentIntent()
                                        &&
                                        !imageSearchAnalysis.isImageIntent()
                        )
                                ||
                                imagePrefixDocumentCandidate
                )
                {
                    String searchableText =
                            (
                                    String.valueOf(result.file.name)
                                            + " "
                                            + String.valueOf(result.file.ocrText)
                                            + " "
                                            + String.valueOf(result.file.path)
                            )
                                    .toLowerCase(
                                            java.util.Locale.ROOT
                                    );

                    boolean matched = false;
                    android.util.Log.e(
                            "TOKEN_CHECK",
                            result.file.name
                                    + " | TOKENS="
                                    + imageSearchRequest.getQueryTokens()
                    );
                    for (String token : imageSearchRequest.getQueryTokens())
                    {
                        if (
                                token == null
                                        ||
                                        token.trim().length() < 3
                        )
                        {
                            continue;
                        }

                        if (
                                searchableText.contains(
                                        token.toLowerCase(
                                                java.util.Locale.ROOT
                                        )
                                )
                        )
                        {
                            matched = true;
                            break;
                        }
                    }
                    android.util.Log.e(
                            "OCR_MATCH",
                            result.file.name
                                    + " => "
                                    + matched
                    );
                    android.util.Log.e(
                            "TOKEN_MATCH",
                            result.file.name
                                    + " => "
                                    + matched
                    );
                    android.util.Log.e(
                            "TOKEN_MATCH",
                            result.file.name
                                    + " => "
                                    + matched
                    );
                    android.util.Log.e(
                            "SEARCHABLE_TEXT",
                            result.file.name
                                    + " => "
                                    + searchableText
                    );
                    if (!matched)

                    {
                        continue;
                    }

                    imageScore = result.score * 12f;
                }
                else if (
                        imageSearchAnalysis.isImageIntent()
                                &&
                                !imageSearchAnalysis.isDocumentIntent()
                )
                {
                    imageScore =
                            result.score * 80f;
                }
                else
                {
                    imageScore =
                            result.score * 20f;
                }

                Float existing =
                        ranking.get(
                                result.file.path
                        );

                if (existing == null) {

                    ranking.put(
                            result.file.path,
                            imageScore
                    );

                } else {

                    ranking.put(
                            result.file.path,
                            Math.max(
                                    existing,
                                    imageScore
                            )
                    );
                }

                SearchExplanationHolder.snippets.put(
                        result.file.path,
                        "Image match â€¢ CLIP vision score: "
                                + result.score
                );

                uniqueFiles.put(
                        result.file.path,
                        result.file
                );
            }
            for (
                    java.util.Map.Entry<String, Float> e
                    : ranking.entrySet()
            )
            {
                android.util.Log.e(
                        "FINAL_RANKING",
                        e.getKey()
                                + " = "
                                + e.getValue()
                );
            }
            float maxScore = 0f;

            for (Float score : ranking.values()) {

                if (score != null && score > maxScore) {
                    maxScore = score;
                }
            }

            float minAcceptedScore;

            if (
                    queryAnalysis.isDocumentIntent()
                            &&
                            !queryAnalysis.isImageIntent()
            ) {

                minAcceptedScore =
                        maxScore * 0.50f;

            } else {

                minAcceptedScore =
                        maxScore * 0.15f;
            }

            float adaptiveGap =
                    maxScore * 0.35f;

            List<FileEntity> finalResults =
                    new java.util.ArrayList<>();

            for (FileEntity file : uniqueFiles.values()) {

                float score =
                        ranking.getOrDefault(
                                file.path,
                                0f
                        );

                boolean passesMinimum =
                        score >= minAcceptedScore;

                boolean closeToBest =
                        (maxScore - score) <= adaptiveGap;
                android.util.Log.e(
                        "MAIN_FILTER",
                        file.name
                                + " | score=" + score
                                + " | max=" + maxScore
                                + " | minAccepted=" + minAcceptedScore
                                + " | gap=" + adaptiveGap
                                + " | passMin=" + passesMinimum
                                + " | close=" + closeToBest
                );
                if (
                        passesMinimum
                                &&
                                closeToBest
                ) {

                    finalResults.add(file);
                }
            }

            java.util.Collections.sort(
                    finalResults,
                    (a, b) -> Float.compare(
                            ranking.getOrDefault(
                                    b.path,
                                    0f
                            ),
                            ranking.getOrDefault(
                                    a.path,
                                    0f
                            )
                    )
            );

            SearchResultsHolder.results =
                    finalResults;

            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onSearchCompleted(
                            finalResults
                    );
                }
            });

        }).start();
    }

    private boolean hasStrongVocabularyPrefixExpansion(
            SearchRequest searchRequest,
            SearchAnalysis searchAnalysis
    ) {

        if (
                searchRequest == null
                        ||
                        searchAnalysis == null
                        ||
                        searchRequest.getQueryTokens() == null
                        ||
                        searchAnalysis.getSemanticTokens() == null
        ) {
            return false;
        }

        for (String token : searchRequest.getQueryTokens()) {

            if (
                    token == null
                            ||
                            token.length() < 4
            ) {
                continue;
            }

            int strongMatches = 0;

            for (String semanticToken : searchAnalysis.getSemanticTokens()) {

                if (
                        semanticToken == null
                ) {
                    continue;
                }

                if (
                        semanticToken.startsWith(token)
                                &&
                                semanticToken.length() >= token.length() + 2
                                &&
                                !semanticToken.equals(token)
                ) {

                    strongMatches++;
                }

                if (strongMatches >= 2) {
                    return true;
                }
            }
        }

        return false;
    }
}
