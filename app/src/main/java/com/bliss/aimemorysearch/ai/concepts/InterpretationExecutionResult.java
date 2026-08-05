package com.bliss.aimemorysearch.ai.concepts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable retrieval observations produced by one strategy execution. */
public final class InterpretationExecutionResult {

    private final String interpretationId;
    private final Interpretation.Kind interpretationKind;
    private final String strategyId;
    private final List<RetrievedResult> retrievedResults;
    private final float bestScore;
    private final int resultCount;
    private final int documentCount;
    private final int imageCount;
    private final long latencyMillis;
    private final boolean reused;

    public InterpretationExecutionResult(
            String interpretationId,
            Interpretation.Kind interpretationKind,
            String strategyId,
            List<RetrievedResult> retrievedResults,
            float bestScore,
            int resultCount,
            int documentCount,
            int imageCount,
            long latencyMillis,
            boolean reused
    ) {
        this.interpretationId = interpretationId;
        this.interpretationKind = interpretationKind;
        this.strategyId = strategyId;
        this.retrievedResults = Collections.unmodifiableList(
                new ArrayList<>(retrievedResults)
        );
        this.bestScore = bestScore;
        this.resultCount = resultCount;
        this.documentCount = documentCount;
        this.imageCount = imageCount;
        this.latencyMillis = latencyMillis;
        this.reused = reused;
    }

    public String getInterpretationId() { return interpretationId; }
    public Interpretation.Kind getInterpretationKind() {
        return interpretationKind;
    }
    public String getStrategyId() { return strategyId; }
    public List<RetrievedResult> getRetrievedResults() {
        return retrievedResults;
    }
    public float getBestScore() { return bestScore; }
    public int getResultCount() { return resultCount; }
    public int getDocumentCount() { return documentCount; }
    public int getImageCount() { return imageCount; }
    public long getLatencyMillis() { return latencyMillis; }
    public boolean isReused() { return reused; }

    public InterpretationExecutionResult forInterpretation(
            Interpretation interpretation,
            boolean reusedObservation
    ) {
        return new InterpretationExecutionResult(
                interpretation.getId(),
                interpretation.getKind(),
                strategyId,
                retrievedResults,
                bestScore,
                resultCount,
                documentCount,
                imageCount,
                reusedObservation ? 0L : latencyMillis,
                reusedObservation
        );
    }

    public static final class RetrievedResult {
        private final String path;
        private final Modality modality;
        private final float score;

        public RetrievedResult(String path, Modality modality, float score) {
            this.path = path;
            this.modality = modality;
            this.score = score;
        }

        public String getPath() { return path; }
        public Modality getModality() { return modality; }
        public float getScore() { return score; }
    }

    public enum Modality {
        DOCUMENT,
        IMAGE
    }
}
