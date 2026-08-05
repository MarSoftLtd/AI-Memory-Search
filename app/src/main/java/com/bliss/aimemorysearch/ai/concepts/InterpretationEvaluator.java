package com.bliss.aimemorysearch.ai.concepts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Executes bounded, observational retrieval for structural interpretations. */
public final class InterpretationEvaluator {

    private InterpretationEvaluator() {}

    public static List<InterpretationExecutionResult> evaluate(
            RetrievalContext context,
            RetrievalBudget budget,
            List<Interpretation> interpretations,
            InterpretationRetrievalStrategy strategy
    ) {
        if (context == null
                || budget == null
                || strategy == null
                || interpretations == null
                || interpretations.isEmpty()) {
            return Collections.emptyList();
        }

        int count = Math.min(
                budget.getMaxInterpretations(),
                interpretations.size()
        );
        List<InterpretationExecutionResult> evaluations =
                new ArrayList<>(count);
        Map<String, InterpretationExecutionResult> observationsByKey =
                new LinkedHashMap<>(count);
        int executions = 0;

        for (int index = 0; index < count; index++) {
            Interpretation interpretation = interpretations.get(index);
            if (interpretation.getComponents().size()
                    > budget.getMaxComponents()) {
                continue;
            }
            String key = strategy.executionKey(context, interpretation);
            InterpretationExecutionResult observation =
                    observationsByKey.get(key);
            boolean reused = observation != null;
            if (observation == null) {
                if (executions >= budget.getMaxExecutions()) break;
                observation = strategy.execute(
                        context,
                        budget,
                        interpretation
                );
                observationsByKey.put(key, observation);
                executions++;
            }
            evaluations.add(observation.forInterpretation(
                    interpretation,
                    reused
            ));
        }
        return Collections.unmodifiableList(evaluations);
    }

    public static InterpretationExecutionResult strongest(
            List<InterpretationExecutionResult> evaluations
    ) {
        InterpretationExecutionResult strongest = null;
        for (InterpretationExecutionResult evaluation : evaluations) {
            if (strongest == null
                    || evaluation.getBestScore() > strongest.getBestScore()
                    || (evaluation.getBestScore() == strongest.getBestScore()
                    && evaluation.getResultCount()
                    > strongest.getResultCount())) {
                strongest = evaluation;
            }
        }
        return strongest;
    }

    public static String toDiagnosticString(
            List<InterpretationExecutionResult> evaluations
    ) {
        InterpretationExecutionResult strongest = strongest(evaluations);
        StringBuilder output = new StringBuilder("evaluations=[");
        for (int index = 0; index < evaluations.size(); index++) {
            if (index > 0) output.append(',');
            InterpretationExecutionResult evaluation = evaluations.get(index);
            output.append(evaluation.getInterpretationId())
                    .append('{').append(evaluation.getInterpretationKind())
                    .append(",results=").append(evaluation.getResultCount())
                    .append(",best=").append(evaluation.getBestScore())
                    .append(",documents=").append(evaluation.getDocumentCount())
                    .append(",images=").append(evaluation.getImageCount())
                    .append(",ratio=")
                    .append(evaluation.getDocumentCount()).append(':')
                    .append(evaluation.getImageCount())
                    .append(",latencyMs=")
                    .append(evaluation.getLatencyMillis())
                    .append(",reused=").append(evaluation.isReused())
                    .append('}');
        }
        output.append("] strongest=")
                .append(strongest == null
                        ? "unavailable"
                        : strongest.getInterpretationId()
                        + "/" + strongest.getInterpretationKind());
        return output.toString();
    }

}
