package com.bliss.aimemorysearch.ai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public final class BreadthFirstReasoningStrategy implements ReasoningStrategy {

    @Override
    public List<ReasoningStep> expand(
            SemanticKnowledgeGraph graph,
            SemanticConcept concept,
            SemanticReasoningOptions options
    ) {
        List<ReasoningStep> steps =
                new ArrayList<>();

        if (
                graph == null
                        ||
                        concept == null
                        ||
                        options == null
        ) {
            return steps;
        }

        Queue<ReasoningNode> queue =
                new ArrayDeque<>();
        Set<String> visitedConceptIds =
                new HashSet<>();

        if (!options.isAllowCycles()) {
            visitedConceptIds.add(
                    normalize(
                            concept.getId()
                    )
            );
        }

        queue.add(
                new ReasoningNode(
                        concept,
                        0
                )
        );

        int expandedConceptCount =
                options.isIncludeOriginalConcept()
                        ? 1
                        : 0;

        while (
                !queue.isEmpty()
                        &&
                        withinLimit(
                                expandedConceptCount,
                                options
                        )
        ) {
            ReasoningNode node =
                    queue.remove();

            if (node.depth >= options.getMaxReasoningDepth()) {
                continue;
            }

            List<SemanticRelation> relations =
                    graph.getRelations(
                            node.concept.getId()
                    );

            for (SemanticRelation relation : relations) {
                if (
                        !withinLimit(
                                expandedConceptCount,
                                options
                        )
                ) {
                    break;
                }

                if (
                        !isRelationEnabled(
                                relation,
                                options
                        )
                ) {
                    continue;
                }

                SemanticConcept targetConcept =
                        graph.getConceptById(
                                relation.getTargetConceptId()
                        );

                if (targetConcept == null) {
                    continue;
                }

                String targetConceptId =
                        normalize(
                                targetConcept.getId()
                        );

                if (
                        !options.isAllowCycles()
                                &&
                                visitedConceptIds.contains(
                                        targetConceptId
                                )
                ) {
                    continue;
                }

                int nextDepth =
                        node.depth + 1;

                steps.add(
                        new ReasoningStep(
                                node.concept,
                                targetConcept,
                                relation,
                                nextDepth
                        )
                );
                expandedConceptCount++;

                if (!options.isAllowCycles()) {
                    visitedConceptIds.add(
                            targetConceptId
                    );
                }

                queue.add(
                        new ReasoningNode(
                                targetConcept,
                                nextDepth
                        )
                );
            }
        }

        return steps;
    }

    private static boolean isRelationEnabled(
            SemanticRelation relation,
            SemanticReasoningOptions options
    ) {
        if (
                relation == null
                        ||
                        relation.getType() == null
        ) {
            return false;
        }

        switch (relation.getType()) {
            case SAME_AS:
                return options.isIncludeSameAs();
            case PARENT:
                return options.isIncludeParentRelations();
            case CHILD:
                return options.isIncludeChildRelations();
            case RELATED:
                return options.isIncludeRelatedRelations();
            default:
                return false;
        }
    }

    private static boolean withinLimit(
            int expandedConceptCount,
            SemanticReasoningOptions options
    ) {
        return options.getMaxExpandedConcepts() <= 0
                ||
                expandedConceptCount < options.getMaxExpandedConcepts();
    }

    private static String normalize(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private static final class ReasoningNode {

        private final SemanticConcept concept;
        private final int depth;

        private ReasoningNode(
                SemanticConcept concept,
                int depth
        ) {
            this.concept =
                    concept;
            this.depth =
                    depth;
        }
    }
}
