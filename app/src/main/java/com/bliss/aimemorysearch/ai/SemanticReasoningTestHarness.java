package com.bliss.aimemorysearch.ai;

import android.util.Log;

import java.util.Objects;

public final class SemanticReasoningTestHarness {

    private static final String TAG =
            "SEMANTIC_REASONING";

    private final SemanticKnowledgeGraph graph;
    private final SemanticReasoningEngine engine;

    public SemanticReasoningTestHarness(
            SemanticKnowledgeGraph graph,
            SemanticReasoningEngine engine
    ) {
        this.graph =
                Objects.requireNonNull(
                        graph,
                        "graph"
                );
        this.engine =
                Objects.requireNonNull(
                        engine,
                        "engine"
                );
    }

    public String run(
            String conceptId
    ) {
        SemanticConcept concept =
                graph.getConceptById(
                        conceptId
                );

        return run(
                concept
        );
    }

    public String run(
            SemanticConcept concept
    ) {
        return formatResult(
                engine.expand(
                        concept
                )
        );
    }

    public void printResult(
            String conceptId
    ) {
        Log.d(
                TAG,
                run(
                        conceptId
                )
        );
    }

    public void printResult(
            SemanticConcept concept
    ) {
        Log.d(
                TAG,
                run(
                        concept
                )
        );
    }

    public String formatResult(
            SemanticReasoningResult result
    ) {
        StringBuilder builder =
                new StringBuilder();

        builder
                .append("Concept:")
                .append('\n')
                .append(formatConcept(
                        result.getOriginalConcept()
                ))
                .append('\n')
                .append('\n')
                .append("Expanded:")
                .append('\n');

        appendOriginalConcept(
                builder,
                result
        );

        for (ReasoningStep step : result.getReasoningSteps()) {
            appendStep(
                    builder,
                    step
            );
        }

        return builder.toString();
    }

    private static void appendOriginalConcept(
            StringBuilder builder,
            SemanticReasoningResult result
    ) {
        if (result.getOriginalConcept() == null) {
            return;
        }

        builder
                .append("Depth 0")
                .append('\n')
                .append(formatConcept(
                        result.getOriginalConcept()
                ))
                .append('\n')
                .append('\n');
    }

    private static void appendStep(
            StringBuilder builder,
            ReasoningStep step
    ) {
        if (step == null) {
            return;
        }

        builder
                .append("Depth ")
                .append(step.getDepth())
                .append('\n')
                .append(formatConcept(
                        step.getSourceConcept()
                ))
                .append(" --")
                .append(formatRelationType(
                        step.getRelation()
                ))
                .append("--> ")
                .append(formatConcept(
                        step.getTargetConcept()
                ))
                .append('\n')
                .append('\n');
    }

    private static String formatConcept(
            SemanticConcept concept
    ) {
        if (concept == null) {
            return "<missing>";
        }

        return concept.getId();
    }

    private static String formatRelationType(
            SemanticRelation relation
    ) {
        if (
                relation == null
                        ||
                        relation.getType() == null
        ) {
            return "UNKNOWN";
        }

        return relation.getType().name();
    }
}
