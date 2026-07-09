package com.bliss.aimemorysearch.ai;

public final class ReasoningStep {

    private final SemanticConcept sourceConcept;
    private final SemanticConcept targetConcept;
    private final SemanticRelation relation;
    private final int depth;

    public ReasoningStep(
            SemanticConcept sourceConcept,
            SemanticConcept targetConcept,
            SemanticRelation relation,
            int depth
    ) {
        this.sourceConcept =
                sourceConcept;
        this.targetConcept =
                targetConcept;
        this.relation =
                relation;
        this.depth =
                depth;
    }

    public SemanticConcept getSourceConcept() {
        return sourceConcept;
    }

    public SemanticConcept getTargetConcept() {
        return targetConcept;
    }

    public SemanticRelation getRelation() {
        return relation;
    }

    public int getDepth() {
        return depth;
    }
}
