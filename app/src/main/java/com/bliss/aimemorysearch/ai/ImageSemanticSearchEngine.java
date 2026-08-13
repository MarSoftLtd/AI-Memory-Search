package com.bliss.aimemorysearch.ai;

import android.content.Context;

import com.bliss.aimemorysearch.db.AppDatabase;
import com.bliss.aimemorysearch.db.FileEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImageSemanticSearchEngine {

    private final AppDatabase db;

    public ImageSemanticSearchEngine(
            Context context
    ) {
        db =
                AppDatabase.getInstance(
                        context.getApplicationContext()
                );
    }

    public List<SearchResult> search(
            float[] queryEmbedding,
            int topK
    ) {

        android.util.Log.d("MULTILINGUAL_PIPELINE", "ImageSemanticSearchEngine input: dimensions="
                + (queryEmbedding == null ? 0 : queryEmbedding.length) + " | topK=" + topK);

        List<FileEntity> images =
                db.fileDao()
                        .getAllIndexedImages();

        List<SearchResult> results =
                new ArrayList<>();

        if (
                queryEmbedding == null
                        ||
                        queryEmbedding.length == 0
        ) {

            return results;
        }

        float bestScore = -999f;
        android.util.Log.e(
                "IMAGE_DEBUG",
                "TOTAL IMAGES = " + images.size()
        );
        for (FileEntity image : images) {

            if (
                    image.imageEmbedding == null
                            ||
                            image.imageEmbedding.length == 0
            ) {
                continue;
            }

            float[] imageVector =
                    EmbeddingUtils.bytesToFloatArray(
                            image.imageEmbedding
                    );

            if (
                    imageVector == null
                            ||
                            imageVector.length == 0
            ) {
                continue;
            }

            float score =
                    cosineSimilarity(
                            queryEmbedding,
                            imageVector
                    );
            android.util.Log.e(
                    "IMAGE_SCORE",
                    image.name
                            + " = "
                            + score
            );
            if (score > bestScore) {
                bestScore = score;
            }

            results.add(
                    new SearchResult(
                            image,
                            score
                    )
            );
            android.util.Log.e(
                    "TOP_IMAGE",
                    "===================="
            );

            for (int i = 0; i < Math.min(10, results.size()); i++) {

                SearchResult r = results.get(i);

                android.util.Log.e(
                        "TOP_IMAGE",
                        (i + 1)
                                + " -> "
                                + r.file.name
                                + " = "
                                + r.score
                );
            }
        }

        Collections.sort(
                results,
                (a, b) ->
                        Float.compare(
                                b.score,
                                a.score
                        )
        );
        android.util.Log.e(
                "IMAGE_THRESHOLD",
                "BEST SCORE = " + bestScore
        );

        float adaptiveThreshold =
                Math.max(
                        0.22f,
                        bestScore * 0.85f
                );

        android.util.Log.e(
                "IMAGE_THRESHOLD",
                "THRESHOLD = " + adaptiveThreshold
        );

        List<SearchResult> filtered =
                filterByAdaptiveThreshold(results, bestScore);

        for (SearchResult result : filtered) {
            android.util.Log.e(
                    "IMAGE_ACCEPTED",
                    result.file.name
                            + " | "
                            + result.score
            );
        }

        if (
                filtered.size() > topK
        ) {

            filtered =
                    new ArrayList<>(
                            filtered.subList(
                                    0,
                                    topK
                            )
                    );
        }

        int cutoff = adaptiveImageCutoff(filtered);
        if (cutoff < filtered.size()) {
            android.util.Log.d(
                    "IMAGE_DISTRIBUTION_CUTOFF",
                    "total=" + filtered.size()
                            + " | retained=" + cutoff
                            + " | hidden=" + (filtered.size() - cutoff)
            );
            filtered = new ArrayList<>(filtered.subList(0, cutoff));
        }

        return filtered;
    }

    static List<SearchResult> filterByAdaptiveThreshold(
            List<SearchResult> orderedResults,
            float bestScore
    ) {
        List<SearchResult> filtered = new ArrayList<>();

        float adaptiveThreshold =
                Math.max(
                        0.22f,
                        bestScore * 0.85f
                );

        for (SearchResult r : orderedResults) {

            if (
                    r.score >= adaptiveThreshold
            ) {

                filtered.add(r);

            }
        }
        return filtered;
    }

    static int adaptiveImageCutoff(List<SearchResult> orderedResults) {
        if (orderedResults == null || orderedResults.size() < 3) {
            return orderedResults == null ? 0 : orderedResults.size();
        }

        float totalDrop = 0f;
        float largestDrop = 0f;
        int largestBoundary = -1;
        for (int index = 1; index < orderedResults.size(); index++) {
            float drop = orderedResults.get(index - 1).score
                    - orderedResults.get(index).score;
            if (!Float.isFinite(drop) || drop <= 0f) {
                continue;
            }
            totalDrop += drop;
            if (drop > largestDrop) {
                largestDrop = drop;
                largestBoundary = index;
            }
        }

        if (largestBoundary > 0
                && largestDrop > totalDrop - largestDrop) {
            return largestBoundary;
        }
        return orderedResults.size();
    }

    private float cosineSimilarity(
            float[] a,
            float[] b
    ) {

        if (
                a == null
                        ||
                        b == null
        ) {
            return 0f;
        }

        if (
                a.length != b.length
        ) {
            return 0f;
        }

        float dot = 0f;
        float normA = 0f;
        float normB = 0f;

        for (
                int i = 0;
                i < a.length;
                i++
        ) {

            dot += a[i] * b[i];

            normA += a[i] * a[i];

            normB += b[i] * b[i];
        }

        if (
                normA == 0f
                        ||
                        normB == 0f
        ) {
            return 0f;
        }

        return (float)
                (
                        dot /
                                (
                                        Math.sqrt(normA)
                                                *
                                                Math.sqrt(normB)
                                )
                );
    }

    public static class SearchResult {

        public final FileEntity file;
        public final float score;

        public SearchResult(
                FileEntity file,
                float score
        ) {
            this.file = file;
            this.score = score;
        }
    }
}
