package com.bliss.aimemorysearch.ai.concepts;

/** Immutable Android-first limits for one shadow evaluation. */
public final class RetrievalBudget {

    public static final int HARD_MAX_INTERPRETATIONS = 8;
    public static final int HARD_MAX_COMPONENTS = 12;
    public static final int HARD_MAX_EXECUTIONS = 8;
    public static final int HARD_MAX_RESULTS_PER_MODALITY = 10;

    private final int maxInterpretations;
    private final int maxComponents;
    private final int maxExecutions;
    private final int maxResultsPerModality;

    public RetrievalBudget(
            int maxInterpretations,
            int maxComponents,
            int maxExecutions,
            int maxResultsPerModality
    ) {
        this.maxInterpretations = bounded(
                maxInterpretations,
                HARD_MAX_INTERPRETATIONS
        );
        this.maxComponents = bounded(maxComponents, HARD_MAX_COMPONENTS);
        this.maxExecutions = bounded(maxExecutions, HARD_MAX_EXECUTIONS);
        this.maxResultsPerModality = bounded(
                maxResultsPerModality,
                HARD_MAX_RESULTS_PER_MODALITY
        );
    }

    public static RetrievalBudget shadowDefaults() {
        return new RetrievalBudget(
                HARD_MAX_INTERPRETATIONS,
                HARD_MAX_COMPONENTS,
                1,
                HARD_MAX_RESULTS_PER_MODALITY
        );
    }

    public int getMaxInterpretations() { return maxInterpretations; }
    public int getMaxComponents() { return maxComponents; }
    public int getMaxExecutions() { return maxExecutions; }
    public int getMaxResultsPerModality() {
        return maxResultsPerModality;
    }

    private static int bounded(int requested, int hardMaximum) {
        return Math.max(0, Math.min(requested, hardMaximum));
    }
}
