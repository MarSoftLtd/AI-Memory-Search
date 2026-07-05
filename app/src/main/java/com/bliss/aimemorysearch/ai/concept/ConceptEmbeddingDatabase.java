package com.bliss.aimemorysearch.ai.concept;

import java.util.ArrayList;
import java.util.List;

public class ConceptEmbeddingDatabase {

    private final List<ConceptEmbedding> concepts =
            new ArrayList<>();

    public void add(ConceptEmbedding concept) {

        concepts.add(concept);
    }

    public List<ConceptEmbedding> getAll() {

        return concepts;
    }

    public int size() {

        return concepts.size();
    }
}