package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GraphValidationResult {

    private final int totalConcepts;
    private final int totalRelations;
    private final List<GraphValidationIssue> issues;

    public GraphValidationResult(
            int totalConcepts,
            int totalRelations,
            List<GraphValidationIssue> issues
    ) {
        this.totalConcepts =
                totalConcepts;
        this.totalRelations =
                totalRelations;
        this.issues =
                immutableIssueList(
                        issues
                );
    }

    public int getTotalConcepts() {
        return totalConcepts;
    }

    public int getTotalRelations() {
        return totalRelations;
    }

    public List<GraphValidationIssue> getIssues() {
        return issues;
    }

    public boolean isValid() {
        return issues.isEmpty();
    }

    private static List<GraphValidationIssue> immutableIssueList(
            List<GraphValidationIssue> values
    ) {
        if (values == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(
                new ArrayList<>(
                        values
                )
        );
    }
}
