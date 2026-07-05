package com.bliss.aimemorysearch.ai.concept;

import android.content.Context;
import android.util.Log;

import com.bliss.aimemorysearch.ai.EmbeddingEngine;
import com.bliss.aimemorysearch.ai.VectorUtils;

public class ConceptSimilarityTest {

    public static void run(Context context) {

        try {

            EmbeddingEngine engine =
                    EmbeddingEngine.getInstance();

            engine.initialize(context);

            ConceptBinaryLoader loader =
                    new ConceptBinaryLoader();

            ConceptEmbeddingDatabase db =
                    loader.load(context);

            test(engine, db, "dog");
            test(engine, db, "caine");
            test(engine, db, "hund");
            test(engine, db, "chien");
            test(engine, db, "perro");
            test(engine, db, "cane");

        } catch (Exception e) {

            Log.e(
                    "CONCEPT_TEST",
                    "FAILED",
                    e
            );
        }
    }

    private static void test(
            EmbeddingEngine engine,
            ConceptEmbeddingDatabase db,
            String query
    ) throws Exception {

        float[] queryEmbedding =
                engine.generateEmbedding(query);

        String bestLabel = "";
        float bestScore = -999f;

        for (ConceptEmbedding concept : db.getAll()) {

            float score =
                    VectorUtils.cosineSimilarity(
                            queryEmbedding,
                            concept.getEmbedding()
                    );

            if (score > bestScore) {

                bestScore = score;
                bestLabel = concept.getEnglishLabel();
            }
        }

        Log.e(
                "CONCEPT_TEST",
                query
                        + " -> "
                        + bestLabel
                        + " = "
                        + bestScore
        );
    }
}