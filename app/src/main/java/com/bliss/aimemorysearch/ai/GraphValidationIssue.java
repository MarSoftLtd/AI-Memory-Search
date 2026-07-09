package com.bliss.aimemorysearch.ai;

public final class GraphValidationIssue {

    private final GraphValidationIssueType type;
    private final String conceptId;
    private final String token;
    private final SemanticRelation relation;
    private final String message;

    public GraphValidationIssue(
            GraphValidationIssueType type,
            String conceptId,
            String token,
            SemanticRelation relation,
            String message
    ) {
        this.type =
                type;
        this.conceptId =
                conceptId;
        this.token =
                token;
        this.relation =
                relation;
        this.message =
                message;
    }

    public GraphValidationIssueType getType() {
        return type;
    }

    public String getConceptId() {
        return conceptId;
    }

    public String getToken() {
        return token;
    }

    public SemanticRelation getRelation() {
        return relation;
    }

    public String getMessage() {
        return message;
    }
}
