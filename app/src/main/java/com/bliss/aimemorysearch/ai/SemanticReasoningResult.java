package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SemanticReasoningResult {

    private final SemanticConcept originalConcept;
    private final List<ReasoningStep> reasoningSteps;
    private final boolean includeOriginalConcept;

    public SemanticReasoningResult(
            SemanticConcept originalConcept,
            List<ReasoningStep> reasoningSteps,
            boolean includeOriginalConcept
    ) {
        this.originalConcept =
                originalConcept;
        this.reasoningSteps =
                immutableList(
                        reasoningSteps
                );
        this.includeOriginalConcept =
                includeOriginalConcept;
    }

    public SemanticConcept getOriginalConcept() {
        return originalConcept;
    }

    public List<ReasoningStep> getReasoningSteps() {
        return reasoningSteps;
    }

    public List<SemanticExpansion> getExpansions() {
        Map<String, SemanticExpansion> expansions =
                new LinkedHashMap<>();

        if (
                includeOriginalConcept
                        &&
                        originalConcept != null
        ) {
            expansions.put(
                    originalConcept.getId(),
                    new SemanticExpansion(
                            originalConcept,
                            SemanticExpansionOrigin.USER,
                            0
                    )
            );
        }

        for (ReasoningStep step : reasoningSteps) {
            if (
                    step == null
                            ||
                            step.getTargetConcept() == null
            ) {
                continue;
            }

            expansions.put(
                    step.getTargetConcept().getId(),
                    new SemanticExpansion(
                            step.getTargetConcept(),
                            originFromRelation(
                                    step.getRelation()
                            ),
                            step.getDepth()
                    )
            );
        }

        return Collections.unmodifiableList(
                new ArrayList<>(
                        expansions.values()
                )
        );
    }

    public List<SemanticConcept> getExpandedConcepts() {
        List<SemanticConcept> concepts =
                new ArrayList<>();

        for (SemanticExpansion expansion : getExpansions()) {
            if (
                    expansion != null
                            &&
                            expansion.getConcept() != null
            ) {
                concepts.add(
                        expansion.getConcept()
                );
            }
        }

        return Collections.unmodifiableList(
                concepts
        );
    }

    private static SemanticExpansionOrigin originFromRelation(
            SemanticRelation relation
    ) {
        if (
                relation == null
                        ||
                        relation.getType() == null
        ) {
            return SemanticExpansionOrigin.RELATED;
        }

        switch (relation.getType()) {
            case SAME_AS:
                return SemanticExpansionOrigin.SAME_AS;
            case PARENT:
                return SemanticExpansionOrigin.PARENT;
            case CHILD:
                return SemanticExpansionOrigin.CHILD;
            case RELATED:
                return SemanticExpansionOrigin.RELATED;
            default:
                return SemanticExpansionOrigin.RELATED;
        }
    }

    private static List<ReasoningStep> immutableList(
            List<ReasoningStep> values
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
