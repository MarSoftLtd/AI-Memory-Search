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
import com.bliss.aimemorysearch.ai.QueryUnderstandingEngine;
import com.bliss.aimemorysearch.ai.SearchAnalysis;
import com.bliss.aimemorysearch.ai.SearchRequest;
import com.bliss.aimemorysearch.ai.evidence.DocumentEvidence;
import com.bliss.aimemorysearch.ai.evidence.DocumentConfidence;
import com.bliss.aimemorysearch.ai.evidence.DocumentConfidenceCalculator;
import com.bliss.aimemorysearch.ai.evidence.ImageEvidence;
import com.bliss.aimemorysearch.ai.concepts.ConceptGraph;
import com.bliss.aimemorysearch.ai.concepts.AtomicConjunctionStrategy;
import com.bliss.aimemorysearch.ai.concepts.AtomicMultilingualAlignment;
import com.bliss.aimemorysearch.ai.concepts.Interpretation;
import com.bliss.aimemorysearch.ai.concepts.InterpretationExecutionResult;
import com.bliss.aimemorysearch.ai.concepts.InterpretationEvaluator;
import com.bliss.aimemorysearch.ai.concepts.InterpretationEngine;
import com.bliss.aimemorysearch.ai.concepts.MatchTier;
import com.bliss.aimemorysearch.ai.concepts.RetrievalBudget;
import com.bliss.aimemorysearch.ai.concepts.RetrievalContext;
import com.bliss.aimemorysearch.ai.concepts.SemanticConceptExtractor;
import com.bliss.aimemorysearch.ai.results.AdaptiveResultCutoff;
import com.bliss.aimemorysearch.db.AppDatabase;
import com.bliss.aimemorysearch.db.FileEntity;
import com.bliss.aimemorysearch.db.EmailEntity;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SearchCoordinator {

    private static final AtomicBoolean SEARCH_IN_PROGRESS =
            new AtomicBoolean(false);
    private static final float CANONICAL_RELATIVE_THRESHOLD = 0.70f;
    private static final int PIVOT_ORIGINAL = 1;
    private static final int PIVOT_ALIAS = 1 << 1;
    private static final int PIVOT_TRANSLATION = 1 << 2;
    private static final int EVIDENCE_LEXICAL = 1 << 3;
    private static final int EVIDENCE_CANONICAL = 1 << 4;

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

            android.util.Log.d(
                    "MULTILINGUAL_PIPELINE",
                    "2. Detected language family: "
                            + queryRequest.getSelectedLanguageFamily()
            );

            java.util.HashMap<String, Integer> pivotSources =
                    new java.util.HashMap<>();
            List<String> queryPivots =
                    buildQueryPivots(
                            query.trim(),
                            queryRequest,
                            pivotSources
                    );
            String primaryPivot =
                    queryPivots.get(0);

            queryRequest.setTranslatedQuery(
                    primaryPivot
            );
            if (isAccuracyDiagnostic(query)) {
                com.bliss.aimemorysearch.ai.AiPackageInfo translationPackage =
                        com.bliss.aimemorysearch.ai.AiPlatform
                                .getPackageManager()
                                .findInstalledPackage(
                                        com.bliss.aimemorysearch.ai.AiPackageType.TRANSLATION,
                                        queryRequest.getSelectedLanguageFamily()
                                );
                android.util.Log.i(
                        "ACCURACY_DIAG",
                        "query-route"
                                + " | original=" + query
                                + " | detectedLanguage="
                                + queryRequest.getDetectedLanguage()
                                + " | selectedFamily="
                                + queryRequest.getSelectedLanguageFamily()
                                + " | package="
                                + (translationPackage == null
                                ? "none"
                                : translationPackage.getPackageId()
                                + "/" + translationPackage.getVersion())
                                + " | pivots=" + queryPivots
                );
            }

            android.util.Log.d(
                    "MULTILINGUAL_PIPELINE",
                    "3. English query pivots: " + queryPivots
            );
            SearchAnalysis
                    queryAnalysis =
                    QueryUnderstandingEngine.createSearchAnalysis(
                            queryRequest
                    );
            android.util.Log.d("MULTILINGUAL_PIPELINE", "QueryUnderstandingEngine output: normalized="
                    + queryRequest.getNormalizedQuery() + " | tokens=" + queryRequest.getQueryTokens());
            ConceptGraph shadowConceptGraph =
                    SemanticConceptExtractor.extract(
                            queryRequest.getNormalizedQuery()
                    );
            android.util.Log.d(
                    "CONCEPT_GRAPH",
                    shadowConceptGraph.toDiagnosticString()
            );
            List<Interpretation> shadowInterpretations =
                    InterpretationEngine.generate(shadowConceptGraph);
            com.bliss.aimemorysearch.ai.AiPackageInfo documentPackage =
                    com.bliss.aimemorysearch.ai.AiPlatform.getPackageManager()
                            .getActivePackage(com.bliss.aimemorysearch.ai.AiCapability.DOCUMENT_SEARCH);
            com.bliss.aimemorysearch.ai.AiPackageInfo imagePackage =
                    com.bliss.aimemorysearch.ai.AiPlatform.getPackageManager()
                            .getActivePackage(com.bliss.aimemorysearch.ai.AiCapability.IMAGE_SEARCH);
            com.bliss.aimemorysearch.ai.AiPackageInfo languagePackage =
                    com.bliss.aimemorysearch.ai.AiPlatform.getPackageManager()
                            .findInstalledPackage(com.bliss.aimemorysearch.ai.AiPackageType.TRANSLATION,
                                    queryRequest.getSelectedLanguageFamily());
            android.util.Log.d(
                    "QUERY_INTERPRETATIONS",
                    InterpretationEngine.toDiagnosticString(
                            shadowInterpretations
                    )
            );
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
                    "5. Document search pivots: "
                            + queryPivots
            );
            java.util.LinkedHashMap<String,
                    ChunkSemanticSearchEngine.ChunkResult>
                    bestDocumentResultByPath =
                    new java.util.LinkedHashMap<>();
            java.util.HashMap<String, Integer> documentPivotSupport =
                    new java.util.HashMap<>();
            java.util.HashMap<String, Integer> documentSourceMasks =
                    new java.util.HashMap<>();
            java.util.HashMap<String, Float> documentBm25 =
                    new java.util.HashMap<>();
            java.util.HashMap<String, Float> documentSemantic =
                    new java.util.HashMap<>();
            java.util.HashMap<String, Float> documentCoverage =
                    new java.util.HashMap<>();
            java.util.HashMap<String, Float> documentCanonicalCoverage =
                    new java.util.HashMap<>();
            java.util.HashMap<String, Boolean> documentExactMatch =
                    new java.util.HashMap<>();
            java.util.HashMap<String, Boolean> documentSemanticAvailable =
                    new java.util.HashMap<>();
            for (int pivotIndex = 0;
                 pivotIndex < queryPivots.size();
                 pivotIndex++) {
                String pivot = queryPivots.get(pivotIndex);
                int pivotSourceMask = pivotSources.getOrDefault(
                        normalizePivotKey(pivot),
                        0
                );
                SearchRequest pivotRequest;
                SearchAnalysis pivotAnalysis;
                if (pivotIndex == 0) {
                    pivotRequest = queryRequest;
                    pivotAnalysis = queryAnalysis;
                } else {
                    pivotRequest =
                            QueryUnderstandingEngine.createSearchRequest(pivot);
                    pivotRequest.setDetectedLanguage(
                            queryRequest.getDetectedLanguage()
                    );
                    pivotRequest.setSelectedLanguageFamily(
                            queryRequest.getSelectedLanguageFamily()
                    );
                    pivotRequest.setTranslatedQuery(pivot);
                    pivotAnalysis =
                            QueryUnderstandingEngine.createSearchAnalysis(
                                    pivotRequest
                            );
                }
                List<ChunkSemanticSearchEngine.ChunkResult> pivotResults =
                        ChunkSemanticSearchEngine.search(
                                context,
                                pivotRequest,
                                pivotAnalysis,
                                java.util.Collections.singletonList(pivot)
                        );
                android.util.Log.d(
                        "MULTI_PIVOT_SEARCH",
                        "pivot=" + pivot
                                + " | results=" + pivotResults.size()
                );
                for (ChunkSemanticSearchEngine.ChunkResult result
                        : pivotResults) {
                    if (result == null
                            || result.chunk == null
                            || result.chunk.filePath == null) {
                        continue;
                    }
                    documentPivotSupport.put(
                            result.chunk.filePath,
                            documentPivotSupport.getOrDefault(
                                    result.chunk.filePath,
                                    0
                            ) + 1
                    );
                    int sourceMask = pivotSourceMask;
                    if (result.lexicalEvidence) {
                        sourceMask |= EVIDENCE_LEXICAL;
                    }
                    if (result.canonicalEvidence) {
                        sourceMask |= EVIDENCE_CANONICAL;
                    }
                    documentSourceMasks.put(
                            result.chunk.filePath,
                            documentSourceMasks.getOrDefault(
                                    result.chunk.filePath,
                                    0
                            ) | sourceMask
                    );
                    String resultPath = result.chunk.filePath;
                    documentBm25.put(
                            resultPath,
                            Math.max(
                                    documentBm25.getOrDefault(resultPath, 0f),
                                    result.bm25Score
                            )
                    );
                    documentSemantic.put(
                            resultPath,
                            Math.max(
                                    documentSemantic.getOrDefault(resultPath, 0f),
                                    result.semanticScore
                            )
                    );
                    if (result.canonicalOnly) {
                        documentCanonicalCoverage.put(
                                resultPath,
                                Math.max(
                                        documentCanonicalCoverage.getOrDefault(
                                                resultPath, 0f),
                                        result.tokenCoverage
                                )
                        );
                    } else if (result.lexicalEvidence) {
                        documentCoverage.put(
                                resultPath,
                                Math.max(
                                        documentCoverage.getOrDefault(
                                                resultPath, 0f),
                                        result.tokenCoverage
                                )
                        );
                    }
                    documentExactMatch.put(
                            resultPath,
                            documentExactMatch.getOrDefault(resultPath, false)
                                    || result.exactTokenMatch
                    );
                    documentSemanticAvailable.put(
                            resultPath,
                            documentSemanticAvailable.getOrDefault(
                                    resultPath, false)
                                    || result.semanticAvailable
                    );
                    ChunkSemanticSearchEngine.ChunkResult existing =
                            bestDocumentResultByPath.get(
                                    result.chunk.filePath
                            );
                    if (existing == null || result.score > existing.score) {
                        bestDocumentResultByPath.put(
                                result.chunk.filePath,
                                result
                        );
                    }
                }
            }
            chunkResults =
                    new java.util.ArrayList<>(
                            bestDocumentResultByPath.values()
                    );
            float bestDocumentScore = 0f;
            float secondDocumentScore = 0f;
            java.util.LinkedHashMap<String, DocumentEvidence.Provenance>
                    documentProvenance = new java.util.LinkedHashMap<>();
            for (ChunkSemanticSearchEngine.ChunkResult result : chunkResults) {
                if (result.score >= bestDocumentScore) {
                    secondDocumentScore = bestDocumentScore;
                    bestDocumentScore = result.score;
                } else if (result.score > secondDocumentScore) {
                    secondDocumentScore = result.score;
                }
                int sourceMask = documentSourceMasks.getOrDefault(
                        result.chunk.filePath,
                        0
                );
                documentProvenance.put(
                        result.chunk.filePath,
                        new DocumentEvidence.Provenance(
                                (sourceMask & EVIDENCE_LEXICAL) != 0,
                                (sourceMask & EVIDENCE_CANONICAL) != 0,
                                (sourceMask & PIVOT_ORIGINAL) != 0,
                                (sourceMask & PIVOT_TRANSLATION) != 0,
                                (sourceMask & PIVOT_ALIAS) != 0
                        )
                );
            }
            float documentScoreMargin = chunkResults.size() < 2
                    ? 0f
                    : bestDocumentScore - secondDocumentScore;
            boolean prefixDocumentCandidate =
                    hasStrongVocabularyPrefixExpansion(
                            queryRequest,
                            queryAnalysis
                    );

            ImageSemanticSearchEngine imageEngine =
                    new ImageSemanticSearchEngine(
                            context
                    );
            List<ClipHypothesisResult> imageResults =
                    new java.util.ArrayList<>();
            java.util.LinkedHashMap<String, Float> imageClipCosineByPath =
                    new java.util.LinkedHashMap<>();
            java.util.HashMap<String, Integer> imagePivotSupport =
                    new java.util.HashMap<>();
            for (String pivot : queryPivots) {
                SearchRequest imageSearchRequest =
                        QueryUnderstandingEngine.createSearchRequest(
                                pivot
                        );
                imageSearchRequest.setSelectedLanguageFamily(
                        queryRequest.getSelectedLanguageFamily()
                );
                SearchAnalysis imageSearchAnalysis =
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
                                        pivot
                                );
                android.util.Log.d(
                        "MULTILINGUAL_PIPELINE",
                        "6. CLIP Text input: " + pivot
                                + " | embedding created="
                                + (imageQueryEmbedding != null)
                                + " | dimensions="
                                + (imageQueryEmbedding == null
                                ? 0
                                : imageQueryEmbedding.length)
                );
                List<ImageSemanticSearchEngine.SearchResult> pivotResults =
                        imageEngine.search(
                                imageQueryEmbedding,
                                10
                        );
                android.util.Log.d(
                        "MULTILINGUAL_PIPELINE",
                        "7. Image search input: " + pivot
                                + " | results found="
                                + pivotResults.size()
                );
                for (ImageSemanticSearchEngine.SearchResult result : pivotResults) {
                    if (result != null
                            && result.file != null
                            && result.file.path != null) {
                        imageClipCosineByPath.put(
                                result.file.path,
                                Math.max(imageClipCosineByPath.getOrDefault(
                                        result.file.path, -Float.MAX_VALUE),
                                        result.score)
                        );
                        imagePivotSupport.put(
                                result.file.path,
                                imagePivotSupport.getOrDefault(
                                        result.file.path,
                                        0
                                ) + 1
                        );
                    }
                    imageResults.add(
                            new ClipHypothesisResult(
                                    result,
                                    imageSearchRequest,
                                    imageSearchAnalysis,
                                    imagePrefixDocumentCandidate
                            )
                    );
                    android.util.Log.e(
                            "IMAGE_RAW",
                            result.file.name
                                    + " | "
                                    + result.score
                                    + " | pivot="
                                    + pivot
                    );
                }
            }
            boolean translatedQuery =
                    hasTranslatedPivot(
                            query,
                            queryPivots
                    );
            java.util.Set<String> semanticImagePaths =
                    new java.util.HashSet<>();
            float bestClipScore = 0f;
            float totalClipScore = 0f;
            int clipScoreCount = 0;
            int bestImagePivotSupport = 0;
            boolean imageMetadataAvailable = false;
            boolean imageOcrAvailable = false;
            for (ClipHypothesisResult hypothesisResult : imageResults) {
                ImageSemanticSearchEngine.SearchResult result =
                        hypothesisResult.result;
                if (result != null && result.file != null && result.file.path != null) {
                    semanticImagePaths.add(result.file.path);
                    imageMetadataAvailable = imageMetadataAvailable
                            || result.file.name != null
                            || result.file.path != null
                            || result.file.type != null;
                    imageOcrAvailable = imageOcrAvailable
                            || (result.file.ocrText != null
                            && !result.file.ocrText.trim().isEmpty());
                    bestClipScore = Math.max(bestClipScore, result.score);
                    totalClipScore += result.score;
                    clipScoreCount++;
                    bestImagePivotSupport = Math.max(
                            bestImagePivotSupport,
                            imagePivotSupport.getOrDefault(
                                    result.file.path,
                                    0
                            )
                    );
                }
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
            SearchExplanationHolder.setOriginalQuery(query);
            SearchExplanationHolder.setQueryMetadata(
                    new SearchExplanationHolder.QueryMetadata(
                            queryRequest.getNormalizedQuery(),
                            queryRequest.getTranslatedQuery(),
                            queryRequest.getDetectedLanguage(),
                            queryRequest.getWorkingLanguage(),
                            queryRequest.getSelectedLanguageFamily(), queryPivots,
                            InterpretationEngine.toDiagnosticString(shadowInterpretations),
                            "DocumentRuntimeLoader / EmbeddingEngine",
                            packageLabel(documentPackage),
                            "MobileClipTextEmbeddingEngine / ImageSemanticSearchEngine",
                            packageLabel(imagePackage), packageLabel(languagePackage)));
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
                                        + " | canonical=" + queryPivots
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
            java.util.Set<String> acceptedImagePaths =
                    new java.util.HashSet<>();
            int imageMetadataMatches = 0;
            int imageFilenameMatches = 0;
            int imageOcrMatches = 0;

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
                    ClipHypothesisResult hypothesisResult
                    : imageResults
            ) {
                ImageSemanticSearchEngine.SearchResult result =
                        hypothesisResult.result;
                SearchRequest imageSearchRequest =
                        hypothesisResult.searchRequest;
                SearchAnalysis imageSearchAnalysis =
                        hypothesisResult.searchAnalysis;
                boolean imagePrefixDocumentCandidate =
                        hypothesisResult.prefixDocumentCandidate;

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
                    boolean filenameMatched = false;
                    boolean ocrMatched = false;
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
                            String normalizedToken = token.toLowerCase(
                                    java.util.Locale.ROOT
                            );
                            filenameMatched = filenameMatched
                                    || String.valueOf(result.file.name)
                                    .toLowerCase(java.util.Locale.ROOT)
                                    .contains(normalizedToken);
                            ocrMatched = ocrMatched
                                    || String.valueOf(result.file.ocrText)
                                    .toLowerCase(java.util.Locale.ROOT)
                                    .contains(normalizedToken);
                            break;
                        }
                    }
                    if (matched) {
                        imageMetadataMatches++;
                    }
                    if (filenameMatched) {
                        imageFilenameMatches++;
                    }
                    if (ocrMatched) {
                        imageOcrMatches++;
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
                    imageScore = matched
                            ? result.score * 12f
                            : result.score;
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
                acceptedImagePaths.add(result.file.path);
            }
            ImageEvidence imageEvidence =
                    new ImageEvidence(
                            imageResults.size(),
                            acceptedImagePaths.size(),
                            bestClipScore,
                            clipScoreCount == 0
                                    ? 0f
                                    : totalClipScore / clipScoreCount,
                            imageMetadataMatches,
                            imageFilenameMatches,
                            imageOcrMatches,
                            queryPivots.isEmpty()
                                    ? 0f
                                    : (float) bestImagePivotSupport
                                    / queryPivots.size(),
                            clipScoreCount > 0,
                            imageMetadataAvailable,
                            imageOcrAvailable
                    );
            java.util.LinkedHashMap<String, DocumentEvidence>
                    documentEvidenceByPath = new java.util.LinkedHashMap<>();
            for (java.util.Map.Entry<String, DocumentEvidence.Provenance>
                    entry : documentProvenance.entrySet()) {
                String path = entry.getKey();
                DocumentEvidence.Provenance provenance = entry.getValue();
                FileEntity entity = uniqueFiles.get(path);
                boolean metadataAvailable = entity != null
                        && (entity.name != null
                        || entity.path != null
                        || entity.type != null);
                boolean ocrAvailable = entity != null
                        && entity.ocrText != null
                        && !entity.ocrText.trim().isEmpty();
                float finalDocumentScore = ranking.getOrDefault(path, 0f);
                documentEvidenceByPath.put(
                        path,
                        new DocumentEvidence(
                                chunkResults.size(),
                                documentBm25.getOrDefault(path, 0f),
                                documentSemantic.getOrDefault(path, 0f),
                                documentCoverage.getOrDefault(path, 0f),
                                documentCanonicalCoverage.getOrDefault(path, 0f),
                                documentExactMatch.getOrDefault(path, false)
                                        ? 1 : 0,
                                queryPivots.isEmpty()
                                        ? 0f
                                        : (float) documentPivotSupport
                                        .getOrDefault(path, 0)
                                        / queryPivots.size(),
                                documentSemanticAvailable.getOrDefault(
                                        path, false),
                                provenance.isCanonical(),
                                metadataAvailable,
                                ocrAvailable,
                                finalDocumentScore,
                                secondDocumentScore,
                                documentScoreMargin,
                                java.util.Collections.singletonMap(
                                        path, provenance)
                        )
                );
            }
            SearchExplanationHolder.setEvidence(
                    documentEvidenceByPath,
                    imageEvidence
            );
            SearchExplanationHolder.setImageClipCosineByPath(
                    imageClipCosineByPath
            );
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
            float maxDocumentScore = 0f;
            float maxImageScore = 0f;

            for (java.util.Map.Entry<String, Float> entry : ranking.entrySet()) {
                Float score = entry.getValue();
                if (score == null) {
                    continue;
                }
                if (semanticImagePaths.contains(entry.getKey())) {
                    maxImageScore = Math.max(maxImageScore, score);
                } else {
                    maxDocumentScore = Math.max(maxDocumentScore, score);
                }
            }

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
                float modalityMaxScore =
                        semanticImagePaths.contains(file.path)
                                ? maxImageScore
                                : maxDocumentScore;
                float minAcceptedScore =
                        queryAnalysis.isDocumentIntent()
                                && !queryAnalysis.isImageIntent()
                                ? modalityMaxScore * 0.50f
                                : modalityMaxScore * 0.15f;
                float adaptiveGap =
                        modalityMaxScore * 0.35f;

                boolean passesMinimum =
                        score >= minAcceptedScore;

                boolean closeToBest =
                        (modalityMaxScore - score) <= adaptiveGap;
                boolean passesCanonicalPolicy =
                        canonicalOnlyPaths.contains(file.path)
                                && bestCanonicalScore > 0f
                                && score >= bestCanonicalScore
                                * CANONICAL_RELATIVE_THRESHOLD;
                android.util.Log.e(
                        "MAIN_FILTER",
                        file.name
                                + " | score=" + score
                                + " | modalityMax=" + modalityMaxScore
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

            sortFinalResults(
                    finalResults,
                    ranking,
                    semanticImagePaths,
                    maxDocumentScore,
                    maxImageScore
            );

            java.util.LinkedHashMap<String, DocumentConfidence>
                    documentConfidences = new java.util.LinkedHashMap<>();
            for (FileEntity finalResult : finalResults) {
                DocumentEvidence documentEvidence =
                        documentEvidenceByPath.get(finalResult.path);
                DocumentConfidence documentConfidence =
                        DocumentConfidenceCalculator.calculate(
                                documentEvidence,
                                finalResult.path
                        );
                documentConfidences.put(
                        finalResult.path,
                        documentConfidence
                );
                android.util.Log.d(
                        "DOCUMENT_CONFIDENCE",
                        "filename=" + finalResult.name
                                + " | confidence="
                                + diagnosticSignal(
                                documentConfidence.getConfidence())
                                + " | bm25="
                                + diagnosticSignal(documentConfidence.getBm25())
                                + " | semantic="
                                + diagnosticSignal(
                                documentConfidence.getSemantic())
                                + " | coverage="
                                + diagnosticSignal(
                                documentConfidence.getCoverage())
                                + " | canonical="
                                + diagnosticSignal(
                                documentConfidence.getCanonicalCoverage())
                                + " | margin="
                                + diagnosticSignal(
                                documentConfidence.getScoreMargin())
                                + " | availability="
                                + diagnosticAvailability(documentConfidence)
                                + " | provenance="
                                + diagnosticProvenance(
                                documentEvidence == null
                                        ? null
                                        : documentEvidence
                                        .getProvenanceByPath()
                                        .get(finalResult.path))
                );
            }
            SearchExplanationHolder.setDocumentConfidences(
                    documentConfidences
            );
            SearchExplanationHolder.scores.clear();
            SearchExplanationHolder.scores.putAll(ranking);

            java.util.LinkedHashMap<String, SearchExplanationHolder.EmailMetadata>
                    emailMetadataByPath = new java.util.LinkedHashMap<>();
            for (FileEntity finalResult : finalResults) {
                if (finalResult != null
                        && finalResult.path != null
                        && "EMAIL".equalsIgnoreCase(finalResult.type)) {
                    EmailEntity email = database.emailDao()
                            .getById(finalResult.path);
                    if (email != null) {
                        emailMetadataByPath.put(
                                finalResult.path,
                                new SearchExplanationHolder.EmailMetadata(
                                        email.provider, email.account, email.messageId,
                                        email.threadId, email.sender, email.timestamp)
                        );
                    }
                }
            }
            SearchExplanationHolder.setEmailMetadataByPath(
                    emailMetadataByPath
            );

            for (int index = 0; index < finalResults.size(); index++) {
                FileEntity result = finalResults.get(index);
                android.util.Log.d(
                        "SEARCH_TRACE",
                                "final"
                                + " | original=" + query
                                + " | canonical=" + queryPivots
                                + " | rank=" + (index + 1)
                                + " | name=" + result.name
                                + " | type=" + result.type
                                + " | score=" + ranking.getOrDefault(result.path, 0f)
                                + " | path=" + result.path
                );
            }
            android.util.Log.d("MULTILINGUAL_PIPELINE", "10. Final merged search results: " + finalResults.size());

            java.util.LinkedHashMap<String, MatchTier> imageMatchTiers =
                    new java.util.LinkedHashMap<>();
            for (FileEntity finalResult : finalResults) {
                if (finalResult != null
                        && finalResult.path != null
                        && "IMAGE".equalsIgnoreCase(finalResult.type)) {
                    imageMatchTiers.put(
                            finalResult.path,
                            MatchTier.RELATED_MATCH
                    );
                }
            }
            SearchExplanationHolder.setImageMatchTiers(imageMatchTiers);

            if (progressCallback != null) {
                mainHandler.post(() -> progressCallback.onPhase("Opening search results…"));
            }

            java.util.LinkedHashMap<String, Float> imageAgreementByPath =
                    new java.util.LinkedHashMap<>();
            boolean multiConceptInterpretationAvailable = false;

            try {
                android.os.Process.setThreadPriority(
                        android.os.Process.THREAD_PRIORITY_BACKGROUND
                );
                List<AtomicMultilingualAlignment.AlignedInterpretation>
                        alignedInterpretations =
                        AtomicMultilingualAlignment.align(queryPivots);
                android.util.Log.d(
                        "ATOMIC_ALIGNMENT",
                        "approvedPivots=" + queryPivots
                                + " | aligned="
                                + alignedInterpretations.size()
                );
                RetrievalBudget shadowRetrievalBudget =
                        new RetrievalBudget(1, 12, 1, 10);
                AtomicConjunctionStrategy atomicStrategy =
                        new AtomicConjunctionStrategy();
                for (AtomicMultilingualAlignment.AlignedInterpretation aligned
                        : alignedInterpretations) {
                    multiConceptInterpretationAvailable =
                            multiConceptInterpretationAvailable
                                    || aligned.getInterpretation()
                                    .getComponents().size() > 1;
                    RetrievalContext shadowRetrievalContext =
                            new RetrievalContext(
                                    context,
                                    aligned.getConceptGraph()
                                            .getNormalizedQuery(),
                                    java.util.Collections.singletonList(
                                            aligned.getApprovedPivot()
                                    )
                            );
                    List<InterpretationExecutionResult> shadowEvaluations =
                            InterpretationEvaluator.evaluate(
                                    shadowRetrievalContext,
                                    shadowRetrievalBudget,
                                    java.util.Collections.singletonList(
                                            aligned.getInterpretation()
                                    ),
                                    atomicStrategy
                            );
                    for (InterpretationExecutionResult evaluation
                            : shadowEvaluations) {
                        for (InterpretationExecutionResult.RetrievedResult result
                                : evaluation.getRetrievedResults()) {
                            if (result.getModality()
                                    != InterpretationExecutionResult.Modality.IMAGE
                                    || result.getMatchTier() == null
                                    || !imageMatchTiers.containsKey(
                                    result.getPath())) {
                                continue;
                            }
                            imageMatchTiers.put(
                                    result.getPath(),
                                    strongerMatchTier(
                                            imageMatchTiers.get(result.getPath()),
                                            result.getMatchTier()
                                    )
                            );
                            imageAgreementByPath.put(
                                    result.getPath(),
                                    Math.max(
                                            imageAgreementByPath.getOrDefault(
                                                    result.getPath(), 0f),
                                            result.getScore()
                                    )
                            );
                        }
                    }
                    android.util.Log.d(
                            "ATOMIC_ALIGNMENT",
                            "pivot=\"" + aligned.getApprovedPivot() + "\""
                                    + " | normalized=\""
                                    + aligned.getConceptGraph()
                                    .getNormalizedQuery() + "\""
                                    + " | "
                                    + InterpretationEngine.toDiagnosticString(
                                    java.util.Collections.singletonList(
                                            aligned.getInterpretation()
                                    ))
                                    + " | "
                                    + InterpretationEvaluator
                                    .toDiagnosticString(shadowEvaluations)
                    );
                }
                SearchExplanationHolder.setImageMatchTiers(imageMatchTiers);
                SearchExplanationHolder.setImageAgreementByPath(
                        imageAgreementByPath
                );
                for (FileEntity finalResult : finalResults) {
                    MatchTier matchTier = imageMatchTiers.get(finalResult.path);
                    if (matchTier != null) {
                        android.util.Log.d(
                                "MATCH_TIER",
                                "name=" + finalResult.name
                                        + " | tier=" + matchTier
                                        + " | path=" + finalResult.path
                        );
                    }
                }
            } catch (Throwable shadowError) {
                android.util.Log.w(
                        "INTERPRETATION_EVALUATION",
                        "shadow evaluation unavailable",
                        shadowError
                );
            }

            applyAtomicImageValidation(
                    finalResults,
                    multiConceptInterpretationAvailable,
                    imageMatchTiers
            );

            boolean imageOnlyResults = !finalResults.isEmpty();
            for (FileEntity result : finalResults) {
                imageOnlyResults = imageOnlyResults
                        && result != null
                        && "IMAGE".equalsIgnoreCase(result.type);
            }
            boolean originalSingleConceptImagePipeline =
                    !multiConceptInterpretationAvailable;
            AdaptiveResultCutoff.Result cutoff =
                    imageOnlyResults || originalSingleConceptImagePipeline
                            ? AdaptiveResultCutoff.preserveOriginalResults(
                            finalResults)
                            : AdaptiveResultCutoff.evaluate(
                            finalResults,
                            SearchExplanationHolder.scores,
                            documentConfidences,
                            imageMatchTiers,
                            imageAgreementByPath,
                            false
                    );
            SearchResultsHolder.setResults(
                    finalResults,
                    cutoff.getDisplayedCount()
            );
            android.util.Log.d(
                    "ADAPTIVE_CUTOFF",
                    "query=" + query
                            + " | total=" + finalResults.size()
                            + " | displayed=" + cutoff.getDisplayedCount()
                            + " | hidden=" + cutoff.getHiddenCount()
                            + " | hiddenPartial="
                            + SearchResultsHolder
                            .getHiddenPartialMatches().size()
                            + " | hiddenRelated="
                            + SearchResultsHolder
                            .getHiddenRelatedMatches().size()
                            + " | reason=" + cutoff.getReason()
            );
            List<FileEntity> displayedResults = SearchResultsHolder.results;
            completionPosted = mainHandler.post(() -> {
                try {
                    if (callback != null) {
                        callback.onSearchCompleted(displayedResults);
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

    private static String packageLabel(com.bliss.aimemorysearch.ai.AiPackageInfo info) {
        return info == null ? "" : info.getPackageId() + " / " + info.getVersion();
    }

    private static MatchTier strongerMatchTier(
            MatchTier current,
            MatchTier candidate
    ) {
        if (current == MatchTier.BEST_MATCH
                || candidate == MatchTier.BEST_MATCH) {
            return MatchTier.BEST_MATCH;
        }
        if (current == MatchTier.PARTIAL_MATCH
                || candidate == MatchTier.PARTIAL_MATCH) {
            return MatchTier.PARTIAL_MATCH;
        }
        return MatchTier.RELATED_MATCH;
    }

    static void applyAtomicImageValidation(
            List<FileEntity> results,
            boolean multiConceptInterpretationAvailable,
            java.util.Map<String, MatchTier> imageMatchTiers
    ) {
        if (!multiConceptInterpretationAvailable) {
            return;
        }
        results.removeIf(result -> result != null
                && "IMAGE".equalsIgnoreCase(result.type)
                && imageMatchTiers.get(result.path) != MatchTier.BEST_MATCH);
    }

    static void sortFinalResults(
            List<FileEntity> results,
            java.util.Map<String, Float> scores,
            java.util.Set<String> imagePaths,
            float maxDocumentScore,
            float maxImageScore
    ) {
        boolean hasDocuments = false;
        boolean hasImages = false;
        for (FileEntity result : results) {
            if (imagePaths.contains(result.path)) hasImages = true;
            else hasDocuments = true;
        }
        final boolean mixedModalities = hasDocuments && hasImages;
        java.util.Collections.sort(results, (a, b) -> {
            boolean aImage = imagePaths.contains(a.path);
            boolean bImage = imagePaths.contains(b.path);
            if (!mixedModalities || aImage == bImage) {
                return Float.compare(
                        scores.getOrDefault(b.path, 0f),
                        scores.getOrDefault(a.path, 0f)
                );
            }
            return Float.compare(
                    relativeModalityScore(
                            b, scores, imagePaths, maxDocumentScore,
                            maxImageScore),
                    relativeModalityScore(
                            a, scores, imagePaths, maxDocumentScore,
                            maxImageScore)
            );
        });
    }

    private static float relativeModalityScore(
            FileEntity result,
            java.util.Map<String, Float> scores,
            java.util.Set<String> imagePaths,
            float maxDocumentScore,
            float maxImageScore
    ) {
        float rawScore = scores.getOrDefault(result.path, 0f);
        float modalityMaximum = imagePaths.contains(result.path)
                ? maxImageScore : maxDocumentScore;
        return modalityMaximum == 0f
                ? rawScore : rawScore / modalityMaximum;
    }

    private List<String> buildQueryPivots(
            String query,
            SearchRequest request,
            java.util.Map<String, Integer> pivotSources
    ) {
        java.util.LinkedHashMap<String, String> pivots =
                new java.util.LinkedHashMap<>();
        String selectedFamily =
                request.getSelectedLanguageFamily() == null
                        ? ""
                        : request.getSelectedLanguageFamily().trim();
        String detectedLanguage =
                request.getDetectedLanguage() == null
                        ? ""
                        : request.getDetectedLanguage().trim();
        boolean singleToken =
                request.getQueryTokens() == null
                        || request.getQueryTokens().size() <= 1;
        boolean unsupportedDetection =
                !detectedLanguage.isEmpty()
                        && !"en".equalsIgnoreCase(detectedLanguage)
                        && !"und".equalsIgnoreCase(detectedLanguage)
                        && com.bliss.aimemorysearch.ai.TranslationModelRegistry
                        .getModelForLanguage(detectedLanguage) == null;
        boolean ambiguous =
                singleToken
                        || detectedLanguage.isEmpty()
                        || "und".equalsIgnoreCase(detectedLanguage)
                        || unsupportedDetection;

        addPivot(pivots, pivotSources, query, PIVOT_ORIGINAL);
        for (String alias :
                com.bliss.aimemorysearch.ai.QueryAliasRegistry.expand(query)) {
            addPivot(
                    pivots,
                    pivotSources,
                    alias,
                    alias != null
                            && query.trim().equalsIgnoreCase(alias.trim())
                            ? PIVOT_ORIGINAL
                            : PIVOT_ALIAS
            );
        }
        if (!ambiguous) {
            addPivot(
                    pivots,
                    pivotSources,
                    selectedFamily.isEmpty()
                            ? query
                            : translatePivot(query, selectedFamily),
                    selectedFamily.isEmpty()
                            ? PIVOT_ORIGINAL
                            : PIVOT_TRANSLATION
            );
        } else {
            java.util.LinkedHashSet<String> families =
                    new java.util.LinkedHashSet<>();
            if (isInstalledTranslationFamily(selectedFamily)) {
                families.add(selectedFamily);
            }
            for (String family : families) {
                addPivot(
                        pivots,
                        pivotSources,
                        translatePivot(query, family),
                        PIVOT_TRANSLATION
                );
            }
        }

        if (pivots.isEmpty()) {
            addPivot(pivots, pivotSources, query, PIVOT_ORIGINAL);
        }
        List<String> result =
                new java.util.ArrayList<>(pivots.values());
        android.util.Log.d(
                "QUERY_ROUTING",
                "query=" + query
                        + " | ambiguous=" + ambiguous
                        + " | detectedLanguage=" + detectedLanguage
                        + " | selectedFamily=" + selectedFamily
                        + " | pivots=" + result
        );
        return result;
    }

    private boolean isInstalledTranslationFamily(
            String family
    ) {
        if (family == null || family.trim().isEmpty()) {
            return false;
        }
        return com.bliss.aimemorysearch.ai.AiPlatform
                .getPackageManager()
                .findInstalledPackage(
                        com.bliss.aimemorysearch.ai.AiPackageType.TRANSLATION,
                        family.trim()
                ) != null;
    }

    private String translatePivot(
            String query,
            String family
    ) {
        try {
            return com.bliss.aimemorysearch.ai.TranslationEngine
                    .getInstance(context)
                    .translate(query, family);
        } catch (Exception error) {
            android.util.Log.w(
                    "QUERY_ROUTING",
                    "Translation hypothesis failed"
                            + " | family=" + family
                            + " | query=" + query,
                    error
            );
            return "";
        }
    }

    private static void addPivot(
            java.util.LinkedHashMap<String, String> pivots,
            java.util.Map<String, Integer> pivotSources,
            String pivot,
            int source
    ) {
        if (pivot == null || pivot.trim().isEmpty()) {
            return;
        }
        String normalized = normalizePivotKey(pivot);
        pivots.putIfAbsent(normalized, pivot.trim());
        pivotSources.put(
                normalized,
                pivotSources.getOrDefault(normalized, 0) | source
        );
    }

    private static String normalizePivotKey(String pivot) {
        return pivot == null
                ? ""
                : pivot.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static boolean hasTranslatedPivot(
            String originalQuery,
            List<String> pivots
    ) {
        if (originalQuery == null || pivots == null) {
            return false;
        }
        for (String pivot : pivots) {
            if (pivot != null
                    && !originalQuery.trim().equalsIgnoreCase(pivot.trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAccuracyDiagnostic(String query) {
        if (query == null) {
            return false;
        }
        String normalized = query.trim()
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("\\s+", " ");
        return "factura".equals(normalized)
                || "factura apa".equals(normalized)
                || "invoice".equals(normalized);
    }

    private static String diagnosticSignal(
            DocumentConfidence.NormalizedSignal signal
    ) {
        return signal != null && signal.isAvailable()
                ? Float.toString(signal.getValue())
                : "unavailable";
    }

    private static String diagnosticAvailability(
            DocumentConfidence confidence
    ) {
        return "confidence=" + confidence.getConfidence().isAvailable()
                + ",bm25=" + confidence.getBm25().isAvailable()
                + ",semantic=" + confidence.getSemantic().isAvailable()
                + ",coverage=" + confidence.getCoverage().isAvailable()
                + ",canonical="
                + confidence.getCanonicalCoverage().isAvailable()
                + ",exact=" + confidence.getExactMatch().isAvailable()
                + ",margin=" + confidence.getScoreMargin().isAvailable();
    }

    private static String diagnosticProvenance(
            DocumentEvidence.Provenance provenance
    ) {
        if (provenance == null) return "unavailable";
        return "lexical=" + provenance.isLexical()
                + ",canonical=" + provenance.isCanonical()
                + ",original=" + provenance.isOriginal()
                + ",translation=" + provenance.isTranslation()
                + ",alias=" + provenance.isAlias();
    }

    private static final class ClipHypothesisResult {
        final ImageSemanticSearchEngine.SearchResult result;
        final SearchRequest searchRequest;
        final SearchAnalysis searchAnalysis;
        final boolean prefixDocumentCandidate;

        ClipHypothesisResult(
                ImageSemanticSearchEngine.SearchResult result,
                SearchRequest searchRequest,
                SearchAnalysis searchAnalysis,
                boolean prefixDocumentCandidate
        ) {
            this.result = result;
            this.searchRequest = searchRequest;
            this.searchAnalysis = searchAnalysis;
            this.prefixDocumentCandidate = prefixDocumentCandidate;
        }
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
