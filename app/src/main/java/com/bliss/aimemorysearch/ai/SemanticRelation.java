package com.bliss.aimemorysearch.ai;

public final class SemanticRelation {

    private final String sourceConceptId;
    private final SemanticRelationType type;
    private final String targetConceptId;

    public SemanticRelation(
            String sourceConceptId,
            SemanticRelationType type,
            String targetConceptId
    ) {
        this.sourceConceptId =
                sourceConceptId != null
                        ? sourceConceptId
                        : "";
        this.type =
                type;
        this.targetConceptId =
                targetConceptId != null
                        ? targetConceptId
                        : "";
    }

    public String getSourceConceptId() {
        return sourceConceptId;
    }

    public SemanticRelationType getType() {
        return type;
    }

    public String getTargetConceptId() {
        return targetConceptId;
    }
}
