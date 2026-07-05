package com.bliss.aimemorysearch.ai.concept;
public class Concept {

    private final String id;
    private final String englishLabel;
    private final float confidence;

    public Concept(String id, String englishLabel, float confidence) {
        this.id = id;
        this.englishLabel = englishLabel;
        this.confidence = confidence;
    }

    public String getId() {
        return id;
    }

    public String getEnglishLabel() {
        return englishLabel;
    }

    public float getConfidence() {
        return confidence;
    }
}