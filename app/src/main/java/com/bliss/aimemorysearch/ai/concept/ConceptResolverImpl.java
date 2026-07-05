package com.bliss.aimemorysearch.ai.concept;

import com.bliss.aimemorysearch.ai.VectorUtils;
import com.bliss.aimemorysearch.ai.multilingual.MultilingualTextEmbeddingEngine;

public class ConceptResolverImpl implements ConceptResolver {

    private final MultilingualTextEmbeddingEngine embeddingEngine;
    private final ConceptEmbeddingDatabase database;

    public ConceptResolverImpl(
            MultilingualTextEmbeddingEngine embeddingEngine,
            ConceptEmbeddingDatabase database
    ) {

        this.embeddingEngine = embeddingEngine;
        this.database = database;
    }

    @Override
    public SearchConcept resolve(String query) throws Exception {

        float[] queryEmbedding =
                embeddingEngine
                        .generateEmbedding(query)
                        .getEmbedding();

        SearchConcept result =
                new SearchConcept();

        float bestScore =
                -Float.MAX_VALUE;

        ConceptEmbedding best =
                null;

        for (ConceptEmbedding concept : database.getAll()) {

            float score =
                    VectorUtils.cosineSimilarity(
                            queryEmbedding,
                            concept.getEmbedding()
                    );

            if (score > bestScore) {

                bestScore =
                        score;

                best =
                        concept;
            }
        }

        if (best == null) {
            return result;
        }

        Concept concept =
                new Concept(
                        best.getId(),
                        best.getEnglishLabel(),
                        bestScore
                );

        switch (best.getType()) {

            case OBJECT:
                result.addObject(concept);
                break;

            case COLOR:
                result.addColor(concept);
                break;

            case PLACE:
                result.addPlace(concept);
                break;

            case ACTION:
                result.addAction(concept);
                break;
        }

        return result;
    }
}