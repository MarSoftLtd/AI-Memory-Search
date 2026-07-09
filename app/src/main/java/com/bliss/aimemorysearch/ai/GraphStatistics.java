package com.bliss.aimemorysearch.ai;

public final class GraphStatistics {

    private final int totalConcepts;
    private final int totalRelations;
    private final int totalTokens;
    private final int conceptsWithoutRelations;
    private final int conceptsWithoutTokens;
    private final int maxOutgoingRelations;
    private final double averageOutgoingRelations;
    private final double graphDensity;
    private final long generatedAt;

    public GraphStatistics(
            int totalConcepts,
            int totalRelations,
            int totalTokens,
            int conceptsWithoutRelations,
            int conceptsWithoutTokens,
            int maxOutgoingRelations,
            double averageOutgoingRelations,
            double graphDensity,
            long generatedAt
    ) {
        this.totalConcepts =
                totalConcepts;
        this.totalRelations =
                totalRelations;
        this.totalTokens =
                totalTokens;
        this.conceptsWithoutRelations =
                conceptsWithoutRelations;
        this.conceptsWithoutTokens =
                conceptsWithoutTokens;
        this.maxOutgoingRelations =
                maxOutgoingRelations;
        this.averageOutgoingRelations =
                averageOutgoingRelations;
        this.graphDensity =
                graphDensity;
        this.generatedAt =
                generatedAt;
    }

    public int getTotalConcepts() {
        return totalConcepts;
    }

    public int getTotalRelations() {
        return totalRelations;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public int getConceptsWithoutRelations() {
        return conceptsWithoutRelations;
    }

    public int getConceptsWithoutTokens() {
        return conceptsWithoutTokens;
    }

    public int getMaxOutgoingRelations() {
        return maxOutgoingRelations;
    }

    public double getAverageOutgoingRelations() {
        return averageOutgoingRelations;
    }

    public double getGraphDensity() {
        return graphDensity;
    }

    public long getGeneratedAt() {
        return generatedAt;
    }
}
