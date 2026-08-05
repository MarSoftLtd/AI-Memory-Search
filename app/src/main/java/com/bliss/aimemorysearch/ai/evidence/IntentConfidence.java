package com.bliss.aimemorysearch.ai.evidence;

public final class IntentConfidence {

    private final float documentConfidence;
    private final float imageConfidence;
    private final float mixedConfidence;

    public IntentConfidence(
            float documentConfidence,
            float imageConfidence,
            float mixedConfidence
    ) {
        this.documentConfidence = documentConfidence;
        this.imageConfidence = imageConfidence;
        this.mixedConfidence = mixedConfidence;
    }

    public float getDocumentConfidence() { return documentConfidence; }
    public float getImageConfidence() { return imageConfidence; }
    public float getMixedConfidence() { return mixedConfidence; }
}
