package com.bliss.aimemorysearch.ai;

import java.util.List;

public final class GraphStatisticsCalculator {

    public GraphStatistics calculate(
            SemanticKnowledgeGraph graph
    ) {
        if (graph == null) {
            return emptyStatistics();
        }

        List<SemanticConcept> concepts =
                graph.getAllConcepts();
        List<SemanticRelation> relations =
                graph.getAllRelations();

        int totalConcepts =
                concepts.size();
        int totalRelations =
                relations.size();
        int totalTokens =
                countTokens(
                        concepts
                );
        int conceptsWithoutRelations =
                countConceptsWithoutRelations(
                        graph,
                        concepts
                );
        int conceptsWithoutTokens =
                countConceptsWithoutTokens(
                        concepts
                );
        int maxOutgoingRelations =
                calculateMaxOutgoingRelations(
                        graph,
                        concepts
                );
        double averageOutgoingRelations =
                calculateAverageOutgoingRelations(
                        totalConcepts,
                        totalRelations
                );
        double graphDensity =
                calculateGraphDensity(
                        totalConcepts,
                        totalRelations
                );

        return new GraphStatistics(
                totalConcepts,
                totalRelations,
                totalTokens,
                conceptsWithoutRelations,
                conceptsWithoutTokens,
                maxOutgoingRelations,
                averageOutgoingRelations,
                graphDensity,
                System.currentTimeMillis()
        );
    }

    private static int countTokens(
            List<SemanticConcept> concepts
    ) {
        int count =
                0;

        for (SemanticConcept concept : concepts) {
            if (
                    concept != null
                            &&
                            concept.getTokens() != null
            ) {
                count +=
                        concept.getTokens().size();
            }
        }

        return count;
    }

    private static int countConceptsWithoutRelations(
            SemanticKnowledgeGraph graph,
            List<SemanticConcept> concepts
    ) {
        int count =
                0;

        for (SemanticConcept concept : concepts) {
            if (
                    concept != null
                            &&
                            graph.getRelations(
                                    concept.getId()
                            ).isEmpty()
            ) {
                count++;
            }
        }

        return count;
    }

    private static int countConceptsWithoutTokens(
            List<SemanticConcept> concepts
    ) {
        int count =
                0;

        for (SemanticConcept concept : concepts) {
            if (
                    concept == null
                            ||
                            concept.getTokens() == null
                            ||
                            concept.getTokens().isEmpty()
            ) {
                count++;
            }
        }

        return count;
    }

    private static int calculateMaxOutgoingRelations(
            SemanticKnowledgeGraph graph,
            List<SemanticConcept> concepts
    ) {
        int max =
                0;

        for (SemanticConcept concept : concepts) {
            if (concept == null) {
                continue;
            }

            int outgoingRelations =
                    graph.getRelations(
                            concept.getId()
                    ).size();

            if (outgoingRelations > max) {
                max =
                        outgoingRelations;
            }
        }

        return max;
    }

    private static double calculateAverageOutgoingRelations(
            int totalConcepts,
            int totalRelations
    ) {
        if (totalConcepts == 0) {
            return 0.0;
        }

        return (double) totalRelations / totalConcepts;
    }

    private static double calculateGraphDensity(
            int totalConcepts,
            int totalRelations
    ) {
        if (totalConcepts <= 1) {
            return 0.0;
        }

        return (double) totalRelations
                /
                (totalConcepts * (totalConcepts - 1));
    }

    private static GraphStatistics emptyStatistics() {
        return new GraphStatistics(
                0,
                0,
                0,
                0,
                0,
                0,
                0.0,
                0.0,
                System.currentTimeMillis()
        );
    }
}
