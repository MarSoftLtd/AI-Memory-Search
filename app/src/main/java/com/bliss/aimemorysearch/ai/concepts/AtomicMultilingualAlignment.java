package com.bliss.aimemorysearch.ai.concepts;

import com.bliss.aimemorysearch.ai.QueryUnderstandingEngine;
import com.bliss.aimemorysearch.ai.SearchRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Builds atomic shadow interpretations from approved full-query pivots. */
public final class AtomicMultilingualAlignment {

    private AtomicMultilingualAlignment() {}

    public static List<AlignedInterpretation> align(
            List<String> approvedPivots
    ) {
        if (approvedPivots == null || approvedPivots.isEmpty()) {
            return Collections.emptyList();
        }
        int count = Math.min(
                approvedPivots.size(),
                RetrievalBudget.HARD_MAX_INTERPRETATIONS
        );
        List<AlignedInterpretation> aligned = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            String approvedPivot = approvedPivots.get(index);
            if (approvedPivot == null || approvedPivot.trim().isEmpty()) {
                continue;
            }
            SearchRequest pivotRequest = QueryUnderstandingEngine
                    .createSearchRequest(approvedPivot);
            ConceptGraph graph = SemanticConceptExtractor.extract(
                    pivotRequest.getNormalizedQuery()
            );
            Interpretation atomic = atomicInterpretation(
                    InterpretationEngine.generate(graph)
            );
            if (atomic != null) {
                aligned.add(new AlignedInterpretation(
                        approvedPivot,
                        graph,
                        atomic
                ));
            }
        }
        return Collections.unmodifiableList(aligned);
    }

    private static Interpretation atomicInterpretation(
            List<Interpretation> interpretations
    ) {
        for (Interpretation interpretation : interpretations) {
            if (interpretation.getKind()
                    == Interpretation.Kind.ATOMIC_CONJUNCTION) {
                return interpretation;
            }
        }
        return null;
    }

    public static final class AlignedInterpretation {
        private final String approvedPivot;
        private final ConceptGraph conceptGraph;
        private final Interpretation interpretation;

        AlignedInterpretation(
                String approvedPivot,
                ConceptGraph conceptGraph,
                Interpretation interpretation
        ) {
            this.approvedPivot = approvedPivot;
            this.conceptGraph = conceptGraph;
            this.interpretation = interpretation;
        }

        public String getApprovedPivot() { return approvedPivot; }
        public ConceptGraph getConceptGraph() { return conceptGraph; }
        public Interpretation getInterpretation() { return interpretation; }
    }
}
