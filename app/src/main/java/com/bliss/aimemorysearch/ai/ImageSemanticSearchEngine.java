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
        if (
                bestScore < 0.26f
        ) {

            android.util.Log.e(
                    "IMAGE_REJECTED",
                    "BEST SCORE TOO LOW = "
                            + bestScore
            );

            return new ArrayList<>();
        }
        android.util.Log.e(
                "IMAGE_THRESHOLD",
                "BEST SCORE = " + bestScore
        );

        List<SearchResult> filtered =
                new ArrayList<>();

        float adaptiveThreshold =
                Math.max(
                        0.22f,
                        bestScore * 0.85f
                );

        android.util.Log.e(
                "IMAGE_THRESHOLD",
                "THRESHOLD = " + adaptiveThreshold
        );

        for (SearchResult r : results) {

            if (
                    r.score >= adaptiveThreshold
            ) {

                filtered.add(r);

                android.util.Log.e(
                        "IMAGE_ACCEPTED",
                        r.file.name
                                + " | "
                                + r.score
                );
            }
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

        return filtered;
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