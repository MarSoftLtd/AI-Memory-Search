package com.bliss.aimemorysearch.ai.concepts;

import android.content.Context;
import android.util.Log;

import com.bliss.aimemorysearch.ai.EmbeddingUtils;
import com.bliss.aimemorysearch.ai.ImageSemanticSearchEngine;
import com.bliss.aimemorysearch.db.AppDatabase;
import com.bliss.aimemorysearch.db.FileEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shadow-only single-scan image retrieval for multiple atomic embeddings. */
final class BatchAtomicImageRetriever {

    private static final String LOG_TAG = "ATOMIC_IMAGE_BATCH";
    private static final float MINIMUM_ADAPTIVE_SCORE = 0.22f;
    private static final float RELATIVE_THRESHOLD = 0.85f;

    private BatchAtomicImageRetriever() {}

    static BatchResult search(
            Context context,
            Map<String, float[]> embeddings,
            int topK
    ) {
        long startedNanos = System.nanoTime();
        if (context == null || embeddings == null || embeddings.isEmpty()) {
            return new BatchResult(Collections.emptyMap(), 0, 0, 0L);
        }

        List<FileEntity> indexedImages = AppDatabase
                .getInstance(context.getApplicationContext())
                .fileDao()
                .getAllIndexedImages();
        Map<String, ComponentState> states = new LinkedHashMap<>(
                embeddings.size()
        );
        for (Map.Entry<String, float[]> entry : embeddings.entrySet()) {
            states.put(
                    entry.getKey(),
                    new ComponentState(entry.getValue())
            );
        }

        List<ComponentState> orderedStates = new ArrayList<>(states.values());
        List<ScoredImage> sharedCandidates = new ArrayList<>(
                indexedImages.size()
        );
        float[] componentScores = new float[orderedStates.size()];
        int ordinal = 0;
        for (FileEntity image : indexedImages) {
            if (image == null
                    || image.imageEmbedding == null
                    || image.imageEmbedding.length == 0) {
                ordinal++;
                continue;
            }
            float[] imageVector = EmbeddingUtils.bytesToFloatArray(
                    image.imageEmbedding
            );
            if (imageVector == null || imageVector.length == 0) {
                ordinal++;
                continue;
            }
            for (int componentIndex = 0;
                 componentIndex < orderedStates.size();
                 componentIndex++) {
                ComponentState state = orderedStates.get(componentIndex);
                float score = cosineSimilarity(state.embedding, imageVector);
                componentScores[componentIndex] = score;
                if (score > state.bestScore) state.bestScore = score;
            }
            sharedCandidates.add(new ScoredImage(
                    image,
                    componentScores.clone(),
                    ordinal
            ));
            ordinal++;
        }

        Map<String, List<ImageSemanticSearchEngine.SearchResult>> results =
                new LinkedHashMap<>(states.size());
        int componentIndex = 0;
        for (Map.Entry<String, ComponentState> entry : states.entrySet()) {
            List<ImageSemanticSearchEngine.SearchResult> accepted =
                    entry.getValue().conjunctionEvidence(
                            sharedCandidates,
                            componentIndex
                    );
            results.put(
                    entry.getKey(),
                    Collections.unmodifiableList(accepted)
            );
            Log.d(
                    LOG_TAG,
                    "component=" + entry.getKey()
                            + " | best=" + entry.getValue().bestScore
                            + " | threshold="
                            + entry.getValue().adaptiveThreshold()
                            + " | sharedCandidates=" + sharedCandidates.size()
                            + " | results=" + accepted.size()
            );
            componentIndex++;
        }
        long elapsedMillis = elapsedMillis(startedNanos);
        Log.d(
                LOG_TAG,
                "scanCount=1"
                        + " | components=" + states.size()
                        + " | indexedImages=" + indexedImages.size()
                        + " | latencyMs=" + elapsedMillis
        );
        return new BatchResult(results, 1, indexedImages.size(), elapsedMillis);
    }

    private static float cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0f;
        float dot = 0f;
        float normA = 0f;
        float normB = 0f;
        for (int index = 0; index < a.length; index++) {
            dot += a[index] * b[index];
            normA += a[index] * a[index];
            normB += b[index] * b[index];
        }
        if (normA == 0f || normB == 0f) return 0f;
        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

    private static long elapsedMillis(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }

    static final class BatchResult {
        private final Map<String,
                List<ImageSemanticSearchEngine.SearchResult>> results;
        private final int corpusScans;
        private final int indexedImageCount;
        private final long latencyMillis;

        BatchResult(
                Map<String, List<ImageSemanticSearchEngine.SearchResult>> results,
                int corpusScans,
                int indexedImageCount,
                long latencyMillis
        ) {
            this.results = Collections.unmodifiableMap(
                    new LinkedHashMap<>(results)
            );
            this.corpusScans = corpusScans;
            this.indexedImageCount = indexedImageCount;
            this.latencyMillis = latencyMillis;
        }

        List<ImageSemanticSearchEngine.SearchResult> resultsFor(String component) {
            List<ImageSemanticSearchEngine.SearchResult> value =
                    results.get(component);
            return value == null ? Collections.emptyList() : value;
        }

        int getCorpusScans() { return corpusScans; }
        int getIndexedImageCount() { return indexedImageCount; }
        long getLatencyMillis() { return latencyMillis; }
    }

    private static final class ComponentState {
        final float[] embedding;
        float bestScore = -999f;

        ComponentState(float[] embedding) {
            this.embedding = embedding;
        }

        float adaptiveThreshold() {
            return Math.max(
                    MINIMUM_ADAPTIVE_SCORE,
                    bestScore * RELATIVE_THRESHOLD
            );
        }

        List<ImageSemanticSearchEngine.SearchResult> conjunctionEvidence(
                Iterable<ScoredImage> sharedCandidates,
                int componentIndex
        ) {
            float threshold = adaptiveThreshold();
            List<ScoredImage> ordered = new ArrayList<>();
            for (ScoredImage candidate : sharedCandidates) {
                ordered.add(candidate);
            }
            ordered.sort((left, right) -> {
                int scoreOrder = Float.compare(
                        right.componentScores[componentIndex],
                        left.componentScores[componentIndex]
                );
                return scoreOrder != 0
                        ? scoreOrder
                        : Integer.compare(left.ordinal, right.ordinal);
            });
            List<ImageSemanticSearchEngine.SearchResult> accepted =
                    new ArrayList<>(ordered.size());
            for (ScoredImage result : ordered) {
                float score = result.componentScores[componentIndex];
                if (score >= threshold) {
                    accepted.add(new ImageSemanticSearchEngine.SearchResult(
                            result.image,
                            score
                    ));
                }
            }
            return accepted;
        }
    }

    private static final class ScoredImage {
        final FileEntity image;
        final float[] componentScores;
        final int ordinal;

        ScoredImage(FileEntity image, float[] componentScores, int ordinal) {
            this.image = image;
            this.componentScores = componentScores;
            this.ordinal = ordinal;
        }
    }
}
