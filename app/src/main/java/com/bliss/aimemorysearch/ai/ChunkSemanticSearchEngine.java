package com.bliss.aimemorysearch.ai;

import android.content.Context;

import com.bliss.aimemorysearch.db.AppDatabase;
import com.bliss.aimemorysearch.db.ChunkEntity;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
public class ChunkSemanticSearchEngine {

    private static final float DEFAULT_MIN_SCORE = 1.10f;
    private static final float SINGLE_TOKEN_MIN_SCORE = 0.65f;
    private static final float TOKEN_SEMANTIC_THRESHOLD =
            0.82f;

    public static List<ChunkResult> search(
            Context context,
            SearchRequest searchRequest,
            SearchAnalysis searchAnalysis
    ) {

        List<ChunkResult> results =
                new ArrayList<>();

        if (context == null) {
            return results;
        }

        if (
                searchRequest == null
                        ||
                        searchAnalysis == null
        ) {
            return results;
        }

        if (
                searchRequest.getNormalizedQuery() == null
                        ||
                        searchRequest.getNormalizedQuery().trim().isEmpty()
        ) {

            return results;
        }

        try {
            long t0 =
                    System.currentTimeMillis();
            String normalizedQuery =
                    normalize(
                            searchRequest.getNormalizedQuery()
                    );

            List<String> allTokens =
                    new ArrayList<>();

            if (
                    searchRequest.getQueryTokens() != null
            ) {

                allTokens.addAll(
                        searchRequest.getQueryTokens()
                );
            }

            if (
                    searchAnalysis.getSemanticTokens() != null
            ) {

                allTokens.addAll(
                        searchAnalysis.getSemanticTokens()
                );
            }

            String[] queryTokens =
                    searchRequest.getQueryTokens().toArray(
                            new String[0]
                    );

            float minScore =
                    DEFAULT_MIN_SCORE;

            if (queryTokens.length == 1) {

                minScore =
                        SINGLE_TOKEN_MIN_SCORE;
            }
            String[] expandedTokens =
                    allTokens.toArray(
                            new String[0]
                    );
            android.util.Log.d(
                    "QUERY_EXPANSION",
                    "QUERY=" + normalizedQuery
                            + " | TOKENS=" + java.util.Arrays.toString(queryTokens)
                            + " | EXPANDED=" + java.util.Arrays.toString(expandedTokens)
            );
            float[] queryEmbedding =
                    searchAnalysis.getEmbedding();

            boolean hasQueryEmbedding =
                    queryEmbedding != null
                            &&
                            queryEmbedding.length > 0;

            List<ChunkEntity> candidateChunks =
                    new ArrayList<>();

            HashSet<String> addedPaths =
                    new HashSet<>();

            HashSet<String> retrievalTokens =
                    new HashSet<>();

            for (String token : expandedTokens) {

                if (
                        token == null
                                ||
                                token.trim().isEmpty()
                ) {

                    continue;
                }

                retrievalTokens.add(
                        normalize(token)
                );
            }

            for (String token : retrievalTokens) {

                if (
                        token == null
                                ||
                                token.trim().isEmpty()
                ) {

                    continue;
                }
                android.util.Log.e(
                        "RETRIEVAL_TOKEN",
                        token
                );
                List<ChunkEntity> partial =
                        AppDatabase
                                .getInstance(context)
                                .chunkDao()
                                .searchByToken(
                                        token,
                                        1000
                                );

                if (partial == null) {
                    continue;
                }
                android.util.Log.e(
                        "RETRIEVAL_COUNT",
                        token + " => " + partial.size()
                );
                for (ChunkEntity chunk : partial) {

                    if (chunk == null) {
                        continue;
                    }

                    String unique =
                            chunk.filePath
                                    + "_"
                                    + chunk.chunkIndex;

                    if (addedPaths.contains(unique)) {
                        continue;
                    }

                    addedPaths.add(unique);

                    candidateChunks.add(chunk);
                }
            }

            if (candidateChunks.isEmpty()) {

                return results;
            }
            android.util.Log.d(
                    "FINAL_RANK",
                    "CANDIDATE CHUNKS = "
                            + candidateChunks.size()
            );
            List<VectorIndexEngine.ScoredChunk>
                    topSemanticChunks =
                    new ArrayList<>();

            if (
                    false
            )
            {
                long semanticStart =
                        System.currentTimeMillis();
                topSemanticChunks =
                        VectorIndexEngine.getTopK(
                                queryEmbedding,
                                candidateChunks,
                                60
                        );

                List<ChunkEntity> semanticCandidates =
                        new ArrayList<>();

                for (
                        VectorIndexEngine.ScoredChunk scored
                        : topSemanticChunks
                ) {

                    if (
                            scored == null
                                    ||
                                    scored.chunk == null
                    ) {

                        continue;
                    }

                    semanticCandidates.add(
                            scored.chunk
                    );
                }

                if (!semanticCandidates.isEmpty()) {

                    candidateChunks =
                            semanticCandidates;
                }
            }

            HashSet<String> seenChunks =
                    new HashSet<>();

            HashMap<String, Float>
                    documentScores =
                    new HashMap<>();

            HashMap<String, Integer>
                    documentHits =
                    new HashMap<>();
            android.util.Log.d(
                    "FINAL_RANK",
                    "START SCORING"
            );
            for (ChunkEntity chunk : candidateChunks) {

                if (chunk == null) {
                    continue;
                }

                if (chunk.chunkText == null) {
                    continue;
                }

                String uniqueKey =
                        chunk.filePath
                                + "_"
                                + chunk.chunkIndex;

                if (
                        seenChunks.contains(uniqueKey)
                ) {

                    continue;
                }

                seenChunks.add(uniqueKey);

                String normalizedChunk =
                        normalize(
                                chunk.chunkText
                        );

                if (
                        normalizedChunk.isEmpty()
                ) {

                    continue;
                }
                float tokenWeightSum = 0f;

                float matchedWeightSum = 0f;

                int validTokens = 0;

                int matchedTokens = 0;

                for (
                        QueryUnderstandingEngine.QueryTokenWeight tokenInfo
                        : searchAnalysis.getTokenWeights()
                ) {

                    if (
                            tokenInfo == null
                                    ||
                                    tokenInfo.token == null
                    ) {

                        continue;
                    }

                    String token =
                            tokenInfo.token;

                    validTokens++;

                    tokenWeightSum +=
                            tokenInfo.weight;

                    boolean matched =
                            containsWholeToken(
                                    normalizedChunk,
                                    token
                            )
                                    ||
                                    containsPrefixToken(
                                            normalizedChunk,
                                            token
                                    )
                                    ||
                                    containsAcronymToken(
                                            normalizedChunk,
                                            token
                                    );

                    if (matched) {

                        matchedTokens++;

                        matchedWeightSum +=
                                tokenInfo.weight;
                    }
                }

                float proximityBoost =
                        calculateTokenProximity(
                                normalizedChunk,
                                queryTokens
                        );
                // BOOST PE TOKENII EXPANDAȚI

                float semanticTokenBoost = 0f;

                String expandedQuery =
                        normalizedQuery;

                for (String expandedToken : expandedTokens) {

                    if (
                            expandedToken == null
                                    ||
                                    expandedToken.trim().isEmpty()
                    ) {
                        continue;
                    }

                    float bestWeight = 0f;

                    for (String originalToken : queryTokens) {

                        float weight =
                                calculatePrefixExpansionWeight(
                                        originalToken,
                                        expandedToken
                                );

                        if (weight > bestWeight) {
                            bestWeight = weight;
                        }
                    }

                    if (bestWeight <= 0f) {
                        continue;
                    }

                    if (
                            containsWholeToken(
                                    normalizedChunk,
                                    expandedToken
                            )
                                    ||
                                    containsPrefixToken(
                                            normalizedChunk,
                                            expandedToken
                                    )
                                    ||
                                    containsAcronymToken(
                                            normalizedChunk,
                                            expandedToken
                                    )
                    ) {

                        semanticTokenBoost +=
                                0.80f * bestWeight;

                        if (bestWeight >= 0.55f) {

                            expandedQuery +=
                                    " "
                                            + expandedToken;
                        }
                    }
                }

                if (validTokens == 0) {
                    continue;
                }

                float tokenCoverage = 0f;

                if (
                        tokenWeightSum > 0f
                ) {

                    tokenCoverage =
                            matchedWeightSum
                                    /
                                    tokenWeightSum;
                }

                if (
                        validTokens >= 2
                                &&
                                matchedTokens < validTokens
                ) {

                    continue;
                }
                if (
                        validTokens >= 2
                                &&
                                !allTokensInSameWindow(
                                        normalizedChunk,
                                        queryTokens,
                                        15
                                )
                ) {

                    continue;
                }
                float lexicalScore =
                        BM25Engine.score(
                                expandedQuery,
                                normalizedChunk
                        );

                if (
                        lexicalScore <= 0f
                ) {

                    continue;
                }

                float semanticScore = 0f;

                if (
                        hasQueryEmbedding
                                &&
                                chunk.embedding != null
                                &&
                                chunk.embedding.length > 0
                ) {

                    float[] chunkEmbedding =
                            EmbeddingUtils
                                    .bytesToFloatArray(
                                            chunk.embedding
                                    );

                    if (
                            chunkEmbedding != null
                                    &&
                                    chunkEmbedding.length > 0
                    ) {

                        semanticScore =
                                VectorUtils
                                        .cosineSimilarity(
                                                queryEmbedding,
                                                chunkEmbedding
                                        );
                    }
                }

                float metadataScore = 0f;

                String lowerName =
                        chunk.fileName == null
                                ? ""
                                : chunk.fileName.toLowerCase();
                
                String lowerPath =
                        chunk.filePath == null
                                ? ""
                                : chunk.filePath.toLowerCase();

                if (
                        lowerName.endsWith(".pdf")
                                ||
                                lowerName.endsWith(".doc")
                                ||
                                lowerName.endsWith(".docx")
                ) {

                    metadataScore += 0.20f;
                }

                if (
                        lowerName.endsWith(".jpg")
                                ||
                                lowerName.endsWith(".jpeg")
                                ||
                                lowerName.endsWith(".png")
                                ||
                                lowerName.endsWith(".webp")
                ) {

                    metadataScore += 0.25f;
                }

                if (
                        lowerPath.contains("download")
                                ||
                                lowerPath.contains("document")
                ) {

                    metadataScore += 0.15f;
                }

                float documentSignalScore =
                        DocumentSignalScorer
                                .calculateSignalScore(
                                        queryTokens,
                                        chunk
                                );

                float finalScore =
                        lexicalScore * 2.80f
                                +
                                semanticScore * 0.55f
                                +
                                semanticTokenBoost
                                +
                                metadataScore
                                +
                                documentSignalScore
                                +
                                proximityBoost;

                finalScore +=
                        tokenCoverage * 2.50f;

                if (
                        tokenCoverage < 1.0f
                ) {

                    finalScore *= 0.55f;
                }

                if (
                        lexicalScore > 2.0f
                ) {

                    finalScore += 1.20f;
                }
                String debugChunkText =
                        normalizedChunk;

                if (
                        debugChunkText.length() > 350
                ) {

                    debugChunkText =
                            debugChunkText.substring(
                                    0,
                                    350
                            );
                }

                android.util.Log.d(
                        "SCORE_BREAKDOWN",
                        chunk.fileName
                                + " | lexical=" + lexicalScore
                                + " | semantic=" + semanticScore
                                + " | tokenBoost=" + semanticTokenBoost
                                + " | metadata=" + metadataScore
                                + " | documentSignal=" + documentSignalScore
                                + " | proximity=" + proximityBoost
                                + " | coverage=" + tokenCoverage
                                + " | final=" + finalScore
                                + " | chunk=" + debugChunkText
                );
                android.util.Log.e(
                        "DOC_SCORE",
                        chunk.fileName
                                + " | lexical="
                                + lexicalScore
                                + " | final="
                                + finalScore
                );
                if (
                        finalScore < minScore
                ) {

                    continue;
                }
                if (
                        matchedTokens == validTokens
                ) {

                    finalScore += 5.0f;

                    if (
                            proximityBoost >= 2.0f
                    ) {

                        finalScore += 6.0f;
                    }

                }
                String documentPath =
                        chunk.filePath;

                if (documentPath == null) {
                    continue;
                }

                Float currentScore =
                        documentScores.get(
                                documentPath
                        );

                if (currentScore == null) {

                    currentScore = finalScore;

                } else {

                    currentScore =
                            Math.max(
                                    currentScore,
                                    finalScore
                            );
                }

                Integer currentHits =
                        documentHits.get(
                                documentPath
                        );

                if (currentHits == null) {
                    currentHits = 0;
                }

                documentScores.put(
                        documentPath,
                        currentScore
                );

                documentHits.put(
                        documentPath,
                        currentHits + 1
                );

                ChunkResult chunkResult =
                        new ChunkResult(
                                chunk,
                                finalScore
                        );

                chunkResult.matchedSnippet =
                        buildSmartSnippet(
                                chunk.chunkText,
                                queryTokens
                        );
                android.util.Log.d(
                        "SMART_SNIPPET",
                        chunk.fileName
                                + " => "
                                + chunkResult.matchedSnippet
                );
                results.add(
                        chunkResult
                );
            }
            HashMap<String, ChunkResult> bestDocumentResults =
                    new HashMap<>();

            for (ChunkResult result : results) {

                if (
                        result == null
                                ||
                                result.chunk == null
                ) {

                    continue;
                }

                String path =
                        result.chunk.filePath;

                if (path == null) {
                    continue;
                }

                Float documentScore =
                        documentScores.get(path);

                Integer hits =
                        documentHits.get(path);

                if (
                        documentScore != null
                ) {

                    result.score =
                            documentScore;
                }

                if (
                        hits != null
                ) {

                    result.score +=
                            Math.min(
                                    hits,
                                    5
                            ) * 0.50f;
                }

                ChunkResult existing =
                        bestDocumentResults.get(path);

                if (
                        existing == null
                                ||
                                result.score > existing.score
                ) {

                    android.util.Log.d(
                            "DOC_KEY",
                            path
                                    + " | "
                                    + result.chunk.fileName
                                    + " | "
                                    + result.score
                    );

                    bestDocumentResults.put(
                            path,
                            result
                    );
                }
            }
            results =
                    new ArrayList<>(
                            bestDocumentResults.values()
                    );
            android.util.Log.d(
                    "FINAL_RANK",
                    "RESULTS AFTER DEDUP = "
                            + results.size()
            );

            android.util.Log.d(
                    "FINAL_RANK",
                    "DOCUMENT SCORES = "
                            + documentScores.size()
            );

            android.util.Log.d(
                    "FINAL_RANK",
                    "DOCUMENT HITS = "
                            + documentHits.size()
            );
            Collections.sort(
                    results,
                    (a, b) ->
                            Float.compare(
                                    b.score,
                                    a.score
                            )
            );
            for (ChunkResult r : results) {

                android.util.Log.e(
                        "BEFORE_FILTER",
                        r.chunk.fileName
                                + " | "
                                + r.score
                );
            }
            if (!results.isEmpty()) {

                float bestScore =
                        results.get(0).score;

                List<ChunkResult> filtered =
                        new ArrayList<>();

                for (ChunkResult r : results) {

                    boolean singleTokenPrefixQuery =
                            queryTokens.length == 1
                                    &&
                                    queryTokens[0] != null
                                    &&
                                    queryTokens[0].length() <= 4;

                    float threshold;

                    if (singleTokenPrefixQuery) {

                        threshold = 0.70f;

                    } else {

                        threshold = 0.80f;
                    }

                    if (
                            r.score >= bestScore * threshold
                    ) {

                        filtered.add(r);
                    }
                }

                results = filtered;
            }
            for (ChunkResult r : results) {

                if (
                        r == null
                                ||
                                r.chunk == null
                ) {

                    continue;
                }

                android.util.Log.d(
                        "FINAL_RANK",
                        r.chunk.fileName
                                + " | "
                                + r.score
                );
            }
            if (results.size() > 10) {

                results =
                        results.subList(
                                0,
                                10
                        );
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return results;}

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
                        "[^\\p{L}\\p{N}]",
                        " "
                );

        normalized =
                normalized.replaceAll(
                        "\\s+",
                        " "
                );

        return normalized.trim();
    }

    private static boolean containsWholeToken(
            String text,
            String token
    ) {

        if (
                text == null
                        ||
                        token == null
        ) {

            return false;
        }

        String[] words =
                text.split("\\s+");

        String cleanToken =
                token
                        .trim()
                        .toLowerCase();

        for (String word : words) {

            if (
                    word != null
                            &&
                            word.equals(cleanToken)
            ) {

                return true;
            }
        }

        return false;
    }
    private static boolean containsPrefixToken(
            String text,
            String token
    ) {

        if (
                text == null
                        ||
                        token == null
        ) {

            return false;
        }

        String cleanToken =
                token.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (cleanToken.length() < 2) {
            return false;
        }

        String[] words =
                text.split("\\s+");

        for (String word : words) {

            if (
                    word != null
                            &&
                            word.length()
                                    >= cleanToken.length()
                            &&
                            word.startsWith(
                                    cleanToken
                            )
            ) {

                return true;
            }
        }

        return false;
    }

    private static boolean containsAcronymToken(
            String text,
            String token
    ) {

        if (
                text == null
                        ||
                        token == null
        ) {

            return false;
        }

        String cleanToken =
                token.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (
                cleanToken.length() < 2
                        ||
                        cleanToken.length() > 5
        ) {

            return false;
        }

        String[] words =
                text.split("\\s+");

        for (
                int i = 0;
                i <= words.length - cleanToken.length();
                i++
        ) {

            StringBuilder acronym =
                    new StringBuilder();

            for (
                    int j = 0;
                    j < cleanToken.length();
                    j++
            ) {

                String word =
                        words[i + j];

                if (
                        word == null
                                ||
                                word.isEmpty()
                ) {

                    continue;
                }

                acronym.append(
                        word.charAt(0)
                );
            }

            if (
                    acronym.toString()
                            .equals(cleanToken)
            ) {

                return true;
            }
        }

        return false;
    }
    private static float calculatePrefixExpansionWeight(
            String originalToken,
            String expandedToken
    ) {

        if (
                originalToken == null
                        ||
                        expandedToken == null
        ) {
            return 0f;
        }

        String original =
                originalToken.trim()
                        .toLowerCase(Locale.ROOT);

        String expanded =
                expandedToken.trim()
                        .toLowerCase(Locale.ROOT);

        if (
                original.isEmpty()
                        ||
                        expanded.isEmpty()
        ) {
            return 0f;
        }

        if (
                expanded.equals(original)
        ) {
            return 1.0f;
        }

        if (
                !expanded.startsWith(original)
        ) {
            return 0f;
        }

        int extraLength =
                expanded.length()
                        - original.length();

        if (extraLength <= 2) {
            return 0.95f;
        }

        if (extraLength <= 4) {
            return 0.75f;
        }

        if (extraLength <= 6) {
            return 0.55f;
        }

        return 0.35f;
    }
    private static SemanticTokenResult semanticTokenMatch(
            String text,
            String token
    ) {

        SemanticTokenResult result =
                new SemanticTokenResult();

        if (
                text == null
                        ||
                        token == null
        ) {

            return result;
        }

        String cleanToken =
                token
                        .trim()
                        .toLowerCase();

        if (
                cleanToken.length() < 3
        ) {

            return result;
        }

        EmbeddingEngine embeddingEngine =
                EmbeddingEngine.getInstance();

        float[] tokenEmbedding =
                embeddingEngine.generateEmbedding(
                        cleanToken
                );

        if (
                tokenEmbedding == null
                        ||
                        tokenEmbedding.length == 0
        ) {

            return result;
        }

        String[] words =
                text.split("\\s+");

        float bestSimilarity = 0f;

        String bestWord = "";

        for (String word : words) {

            if (
                    word == null
                            ||
                            word.length() < 3
            ) {

                continue;
            }

            float[] wordEmbedding =
                    embeddingEngine.generateEmbedding(
                            word
                    );

            if (
                    wordEmbedding == null
                            ||
                            wordEmbedding.length == 0
            ) {

                continue;
            }

            float similarity =
                    VectorUtils.cosineSimilarity(
                            tokenEmbedding,
                            wordEmbedding
                    );

            if (
                    similarity > bestSimilarity
            ) {

                bestSimilarity = similarity;

                bestWord = word;
            }
        }

        if (
                bestSimilarity >= TOKEN_SEMANTIC_THRESHOLD
        ) {

            result.matched = true;

            result.score =
                    bestSimilarity * 1.40f;

            result.bestWord =
                    bestWord;
        }

        return result;
    }

    private static class SemanticTokenResult {

        boolean matched = false;

        float score = 0f;

        String bestWord = "";
    }

    public static class ChunkResult {

        public ChunkEntity chunk;

        public float score;

        public String matchedSnippet;

        public ChunkResult(
                ChunkEntity chunk,
                float score
        ) {

            this.chunk = chunk;
            this.score = score;

            if (
                    chunk != null
                            &&
                            chunk.chunkText != null
            ) {

                String text =
                        chunk.chunkText
                                .replace("\n", " ")
                                .replace("\r", " ")
                                .trim();

                if (text.length() > 220) {

                    text =
                            text.substring(
                                    0,
                                    220
                            ) + "...";
                }

                matchedSnippet =
                        text;

            } else {

                matchedSnippet = "";
            }
        }
    }
    private static float calculateTokenProximity(
            String text,
            String[] tokens
    ) {

        if (
                text == null
                        ||
                        tokens == null
                        ||
                        tokens.length < 2
        ) {

            return 0f;
        }

        String[] words =
                text.split("\\s+");

        int minDistance =
                Integer.MAX_VALUE;

        for (String tokenA : tokens) {

            for (String tokenB : tokens) {

                if (
                        tokenA.equals(tokenB)
                ) {

                    continue;
                }

                int posA = -1;
                int posB = -1;

                for (
                        int i = 0;
                        i < words.length;
                        i++
                ) {

                    if (
                            posA == -1
                                    &&
                                    words[i].startsWith(tokenA)
                    ) {

                        posA = i;
                    }

                    if (
                            posB == -1
                                    &&
                                    words[i].startsWith(tokenB)
                    ) {

                        posB = i;
                    }
                }

                if (
                        posA >= 0
                                &&
                                posB >= 0
                ) {

                    int distance =
                            Math.abs(
                                    posA - posB
                            );

                    if (
                            distance < minDistance
                    ) {

                        minDistance =
                                distance;
                    }
                }
            }
        }

        if (
                minDistance == Integer.MAX_VALUE
        ) {

            return 0f;
        }

        if (minDistance <= 3) {
            return 4.0f;
        }

        if (minDistance <= 10) {
            return 2.0f;
        }

        if (minDistance <= 25) {
            return 1.0f;
        }

        return 0f;
    }
    private static boolean allTokensInSameWindow(
            String text,
            String[] tokens,
            int windowSize
    ) {

        if (
                text == null
                        ||
                        tokens == null
                        ||
                        tokens.length < 2
        ) {

            return true;
        }

        String[] words =
                text.split("\\s+");

        for (
                int start = 0;
                start < words.length;
                start++
        ) {

            int end =
                    Math.min(
                            start + windowSize,
                            words.length
                    );

            int found = 0;

            for (String token : tokens) {

                boolean tokenFound =
                        false;

                for (
                        int i = start;
                        i < end;
                        i++
                ) {

                    if (
                            words[i].startsWith(token)
                    ) {

                        tokenFound = true;
                        break;
                    }
                }

                if (tokenFound) {
                    found++;
                }
            }

            if (
                    found == tokens.length
            ) {

                return true;
            }
        }

        return false;
    }
    private static String buildSmartSnippet(
            String text,
            String[] queryTokens
    ) {

        if (
                text == null
                        ||
                        text.trim().isEmpty()
        ) {

            return "";
        }

        String cleanText =
                text.replace("\n", " ")
                        .replace("\r", " ")
                        .replaceAll("\\s+", " ")
                        .trim();

        String lowerText =
                cleanText.toLowerCase();

        int bestPosition =
                -1;

        String matchedToken =
                null;

        for (String token : queryTokens) {

            if (
                    token == null
                            ||
                            token.trim().isEmpty()
            ) {

                continue;
            }

            int pos =
                    lowerText.indexOf(
                            token.toLowerCase()
                    );

            if (pos >= 0) {

                bestPosition = pos;
                matchedToken = token;
                break;
            }
        }

        if (bestPosition < 0) {

            if (cleanText.length() > 180) {

                return cleanText.substring(
                        0,
                        180
                ) + "...";
            }

            return cleanText;
        }

        int start =
                Math.max(
                        0,
                        bestPosition - 3
                );

        int end =
                Math.min(
                        cleanText.length(),
                        bestPosition + 140
                );

        String snippet =
                cleanText.substring(
                        start,
                        end
                );

        if (start > 0) {
            snippet = "..." + snippet;
        }

        if (end < cleanText.length()) {
            snippet = snippet + " ...";
        }

        if (matchedToken != null) {

            snippet =
                    snippet.replaceAll(
                            "(?i)"
                                    + java.util.regex.Pattern.quote(
                                    matchedToken
                            ),
                            "【"
                                    + matchedToken.toUpperCase()
                                    + "】"
                    );
        }

        return snippet;
    }
}
