package com.bliss.aimemorysearch.ai.concept;

public class ConceptEmbedding {

    private final String id;
    private final String englishLabel;
    private final ConceptType type;
    private final float[] embedding;

    public ConceptEmbedding(
            String id,
            String englishLabel,
            ConceptType type,
            float[] embedding
    ) {

        this.id = id;
        this.englishLabel = englishLabel;
        this.type = type;
        this.embedding = embedding;
    }

    public String getId() {
        return id;
    }

    public String getEnglishLabel() {
        return englishLabel;
    }

    public ConceptType getType() {
        return type;
    }

    public float[] getEmbedding() {
        return embedding;
    }
}