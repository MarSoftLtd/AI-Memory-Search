package com.bliss.aimemorysearch;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.bliss.aimemorysearch.ai.ChunkSemanticSearchEngine;
import com.bliss.aimemorysearch.ai.AiCapabilityManager;
import com.bliss.aimemorysearch.ai.CapabilityPlan;
import com.bliss.aimemorysearch.ai.ImageSemanticSearchEngine;
import com.bliss.aimemorysearch.ai.MobileClipTextEmbeddingEngine;
import com.bliss.aimemorysearch.ai.MultilingualQueryNormalizer;
import com.bliss.aimemorysearch.ai.QueryUnderstandingEngine;
import com.bliss.aimemorysearch.ai.SearchAnalysis;
import com.bliss.aimemorysearch.ai.SearchRequest;
import com.bliss.aimemorysearch.db.AppDatabase;
import com.bliss.aimemorysearch.db.FileEntity;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SearchCoordinator {

    private static final AtomicBoolean SEARCH_IN_PROGRESS =
            new AtomicBoolean(false);
    private static final float CANONICAL_RELATIVE_THRESHOLD = 0.70f;

    public interface Callback {

        void onSearchCompleted(
                List<FileEntity> finalResults
        );
    }

    public interface ProgressCallback {
        void onPhase(String phase);
        void onBusy();
        void onFailure(Throwable error);
    }

    private final Context context;
    private final Handler mainHandler;
    private final AiCapabilityManager capabilityManager;
    public SearchCoordinator(
            Context context
    ) {
        this.context =
                context.getApplicationContext();
        this.mainHandler =
                new Handler(
                        Looper.getMainLooper()
                );
        this.capabilityManager =
                AiCapabilityManager.createDefault();
    }
    public void search(
            String query,
            Callback callback
    ) {
        SearchRequest request =
                QueryUnderstandingEngine.createSearchRequest(
                        query == null ? "" : query.trim()
                );
        search(
                request,
                callback
        );
    }
    public void search(
            SearchRequest queryRequest,
            Callback callback
    ) {
        search(queryRequest, null, callback);
    }

    public void search(
            SearchRequest queryRequest,
            ProgressCallback progressCallback,
            Callback callback
    ) {
        String query =
                queryRequest == null
                        ? ""
                        : queryRequest.getOriginalQuery();
        android.util.Log.e(
                "SEARCH_ENTRY",
                "MAINACTIVITY SEARCH CALLED = " + query
        );
        android.util.Log.d("MULTILINGUAL_PIPELINE", "1. User query: " + query);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            if (query == null || query.trim().isEmpty()) {

                return;
            }
        }

        if (!SEARCH_IN_PROGRESS.compareAndSet(false, true)) {
            android.util.Log.d(
                    "SEARCH_ENTRY",
                    "SEARCH IGNORED: another search is already running"
            );
            if (progressCallback != null) {
                mainHandler.post(progressCallback::onBusy);
            }
            return;
        }

        if (progressCallback != null) {
            mainHandler.post(() -> progressCallback.onPhase("Preparing the query…"));
        }

        new Thread(() -> {

            boolean completionPosted = false;

            try {

            final String[] finalQueryHolder =
                    new String[]{
                            query.trim()
                    };

            android.util.Log.d(
                    "MULTILINGUAL_PIPELINE",
                    "2. Detected language family: "
                            + queryRequest.getSelectedLanguageFamily()
            );

            java.util.concurrent.CountDownLatch normalizerLatch =
                    new java.util.concurrent.CountDownLatch(1);

            MultilingualQueryNormalizer
                    .getInstance(context)
                    .normalizeForClip(
                            query.trim(),
                            queryRequest.getSelectedLanguageFamily(),
                            new MultilingualQueryNormalizer.Callback() {
                                @Override
                                public void onReady(
                                        String originalQuery,
                                        String englishQuery
                                ) {
                                    finalQueryHolder[0] = englishQuery;
                                    android.util.Log.d(
                                            "MULTILINGUAL_PIPELINE",
                                            "3. Final translated English query: "
                                                    + englishQuery
                                    );
                                    normalizerLatch.countDown();
                                }

                                @Override
                                public void onError(String originalQuery) {
                                    android.util.Log.w(
                                            "MULTILINGUAL_PIPELINE",
                                            "Normalizer onError fallback; final query remains: "
                                                    + finalQueryHolder[0]
                                    );
                                    normalizerLatch.countDown();
                                }
                            }
                    );

            try {
                boolean completed = normalizerLatch.await(
                        20,
                        java.util.concurrent.TimeUnit.SECONDS
                );
                if (!completed) {
                    android.util.Log.w(
                            "MULTILINGUAL_PIPELINE",
                            "Normalizer timeout fallback; final query remains: "
                                    + finalQueryHolder[0]
                    );
                }
            } catch (Exception e) {
                android.util.Log.e(
                        "MULTILINGUAL_PIPELINE",
                        "Normalizer wait exception; final query remains: "
                                + finalQueryHolder[0],
                        e
                );
            }

            queryRequest.setTranslatedQuery(
                    finalQueryHolder[0]
            );

            android.util.Log.d(
                    "MULTILINGUAL_PIPELINE",
                    "4. Canonical query: " + finalQueryHolder[0]
            );
            SearchAnalysis
                    queryAnalysis =
                    QueryUnderstandingEngine.createSearchAnalysis(
                            queryRequest
                    );
            android.util.Log.d("MULTILINGUAL_PIPELINE", "QueryUnderstandingEngine output: normalized="
                    + queryRequest.getNormalizedQuery() + " | tokens=" + queryRequest.getQueryTokens());
            CapabilityPlan capabilityPlan =
                    capabilityManager.evaluate(
                            queryRequest,
                            queryAnalysis
                    );
            android.util.Log.d(
                    "AI_CAPABILITY",
                    "canContinue="
                            + capabilityPlan.canContinue()
                            + " | mode="
                            + capabilityPlan.getExecutionMode()
                            + " | requirements="
                            + capabilityPlan.getRequirements().size()
            );
            List<ChunkSemanticSearchEngine.ChunkResult>
                    chunkResults;
            android.util.Log.d(
                    "MULTILINGUAL_PIPELINE",
                    "5. Document search input: "
                            + queryRequest.getNormalizedQuery()
            );
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

            SearchRequest
                    imageSearchRequest =
                    QueryUnderstandingEngine.createSearchRequest(
                            finalQueryHolder[0]
                    );
            imageSearchRequest.setSelectedLanguageFamily(
                    queryRequest.getSelectedLanguageFamily()
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
                                    finalQueryHolder[0]
                            );
            android.util.Log.d("MULTILINGUAL_PIPELINE", "6. CLIP Text input: " + finalQueryHolder[0]
                    + " | embedding created=" + (imageQueryEmbedding != null)
                    + " | dimensions=" + (imageQueryEmbedding == null ? 0 : imageQueryEmbedding.length));

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
            boolean translatedQuery =
                    !query.trim().equalsIgnoreCase(finalQueryHolder[0].trim());
            java.util.Set<String> semanticImagePaths =
                    new java.util.HashSet<>();
            for (ImageSemanticSearchEngine.SearchResult result : imageResults) {
                if (result != null && result.file != null && result.file.path != null) {
                    semanticImagePaths.add(result.file.path);
                }
            }
            android.util.Log.d("MULTILINGUAL_PIPELINE", "7. Image search input: " + finalQueryHolder[0]
                    + " | results found=" + imageResults.size());
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
                    if (translatedQuery
                            && "IMAGE".equalsIgnoreCase(entity.type)
                            && !semanticImagePaths.contains(path)) {
                        android.util.Log.d(
                                "SEARCH_TRACE",
                                "lexical-image-suppressed"
                                        + " | original=" + query
                                        + " | canonical=" + finalQueryHolder[0]
                                        + " | name=" + entity.name
                                        + " | path=" + path
                                        + " | lexicalScore=" + result.score
                        );
                        continue;
                    }
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
            java.util.Set<String> canonicalOnlyPaths =
                    new java.util.HashSet<>();

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

                if (translatedQuery
                        && !semanticImagePaths.contains(result.chunk.filePath)) {
                    FileEntity lexicalEntity = database.fileDao()
                            .getFileByPath(result.chunk.filePath);
                    if (lexicalEntity != null
                            && "IMAGE".equalsIgnoreCase(lexicalEntity.type)) {
                        continue;
                    }
                }

                Float existingScore =
                        ranking.get(
                                result.chunk.filePath
                        );

                if (result.canonicalOnly) {
                    canonicalOnlyPaths.add(result.chunk.filePath);
                }

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

            float bestCanonicalScore = 0f;
            for (String path : canonicalOnlyPaths) {
                bestCanonicalScore = Math.max(
                        bestCanonicalScore,
                        ranking.getOrDefault(path, 0f)
                );
            }

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
                boolean passesCanonicalPolicy =
                        canonicalOnlyPaths.contains(file.path)
                                && bestCanonicalScore > 0f
                                && score >= bestCanonicalScore
                                * CANONICAL_RELATIVE_THRESHOLD;
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
                        (passesMinimum && closeToBest)
                                || passesCanonicalPolicy
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
            for (int index = 0; index < finalResults.size(); index++) {
                FileEntity result = finalResults.get(index);
                android.util.Log.d(
                        "SEARCH_TRACE",
                        "final"
                                + " | original=" + query
                                + " | canonical=" + finalQueryHolder[0]
                                + " | rank=" + (index + 1)
                                + " | name=" + result.name
                                + " | type=" + result.type
                                + " | score=" + ranking.getOrDefault(result.path, 0f)
                                + " | path=" + result.path
                );
            }
            android.util.Log.d("MULTILINGUAL_PIPELINE", "10. Final merged search results: " + finalResults.size());

            if (progressCallback != null) {
                mainHandler.post(() -> progressCallback.onPhase("Opening search results…"));
            }

            completionPosted = mainHandler.post(() -> {
                try {
                    if (callback != null) {
                        callback.onSearchCompleted(
                                finalResults
                        );
                    }
                } finally {
                    SEARCH_IN_PROGRESS.set(false);
                }
            });

            } catch (Throwable error) {
                android.util.Log.e(
                        "SEARCH_ENTRY",
                        "SEARCH FAILED",
                        error
                );
                if (progressCallback != null) {
                    mainHandler.post(() -> progressCallback.onFailure(error));
                }
            } finally {
                if (!completionPosted) {
                    SEARCH_IN_PROGRESS.set(false);
                }
            }

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
