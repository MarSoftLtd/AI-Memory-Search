package com.bliss.aimemorysearch.ai;

import java.util.List;

public interface ReasoningStrategy {

    List<ReasoningStep> expand(
            SemanticKnowledgeGraph graph,
            SemanticConcept concept,
            SemanticReasoningOptions options
    );
}
