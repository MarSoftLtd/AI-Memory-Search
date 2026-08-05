package com.bliss.aimemorysearch.ai.evidence;

public final class ImageEvidence {

    private final int candidateCount;
    private final int acceptedCandidateCount;
    private final float bestClipScore;
    private final float averageClipScore;
    private final int metadataMatches;
    private final int filenameMatches;
    private final int ocrMatches;
    private final float pivotAgreement;
    private final boolean semanticAvailable;
    private final boolean metadataAvailable;
    private final boolean ocrAvailable;

    public ImageEvidence(
            int candidateCount,
            int acceptedCandidateCount,
            float bestClipScore,
            float averageClipScore,
            int metadataMatches,
            int filenameMatches,
            int ocrMatches,
            float pivotAgreement,
            boolean semanticAvailable,
            boolean metadataAvailable,
            boolean ocrAvailable
    ) {
        this.candidateCount = candidateCount;
        this.acceptedCandidateCount = acceptedCandidateCount;
        this.bestClipScore = bestClipScore;
        this.averageClipScore = averageClipScore;
        this.metadataMatches = metadataMatches;
        this.filenameMatches = filenameMatches;
        this.ocrMatches = ocrMatches;
        this.pivotAgreement = pivotAgreement;
        this.semanticAvailable = semanticAvailable;
        this.metadataAvailable = metadataAvailable;
        this.ocrAvailable = ocrAvailable;
    }

    public int getCandidateCount() { return candidateCount; }
    public int getAcceptedCandidateCount() { return acceptedCandidateCount; }
    public float getBestClipScore() { return bestClipScore; }
    public float getAverageClipScore() { return averageClipScore; }
    public int getMetadataMatches() { return metadataMatches; }
    public int getFilenameMatches() { return filenameMatches; }
    public int getOcrMatches() { return ocrMatches; }
    public float getPivotAgreement() { return pivotAgreement; }
    public boolean isSemanticAvailable() { return semanticAvailable; }
    public boolean isMetadataAvailable() { return metadataAvailable; }
    public boolean isOcrAvailable() { return ocrAvailable; }
}
