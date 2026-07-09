package com.bliss.aimemorysearch.ai;

import java.util.List;
import java.util.Objects;

public final class SemanticReasoningEngine {

    private final SemanticKnowledgeGraph graph;
    private final SemanticReasoningOptions options;
    private final ReasoningStrategy strategy;

    public SemanticReasoningEngine(
            SemanticKnowledgeGraph graph,
            SemanticReasoningOptions options
    ) {
        this.graph =
                Objects.requireNonNull(
                        graph,
                        "graph"
                );
        this.options =
                Objects.requireNonNull(
                        options,
                        "options"
                );
        strategy =
                new BreadthFirstReasoningStrategy();
    }

    public SemanticReasoningResult expand(
            SemanticConcept concept
    ) {
        List<ReasoningStep> steps =
                strategy.expand(
                        graph,
                        concept,
                        options
                );

        return new SemanticReasoningResult(
                concept,
                steps,
                options.isIncludeOriginalConcept()
        );
    }
}
