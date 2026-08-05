package com.bliss.aimemorysearch.ai.concepts;

/** Strategy boundary for interpretation-aware retrieval experiments. */
public interface InterpretationRetrievalStrategy {

    String getStrategyId();

    String executionKey(
            RetrievalContext context,
            Interpretation interpretation
    );

    InterpretationExecutionResult execute(
            RetrievalContext context,
            RetrievalBudget budget,
            Interpretation interpretation
    );
}
