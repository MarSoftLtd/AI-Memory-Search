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

    public List<SemanticConcept> getExpandedConcepts() {
        Map<String, SemanticConcept> concepts =
                new LinkedHashMap<>();

        if (
                includeOriginalConcept
                        &&
                        originalConcept != null
        ) {
            concepts.put(
                    originalConcept.getId(),
                    originalConcept
            );
        }

        for (ReasoningStep step : reasoningSteps) {
            if (
                    step != null
                            &&
                            step.getTargetConcept() != null
            ) {
                concepts.put(
                        step.getTargetConcept().getId(),
                        step.getTargetConcept()
                );
            }
        }

        return Collections.unmodifiableList(
                new ArrayList<>(
                        concepts.values()
                )
        );
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
