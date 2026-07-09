package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GraphValidator {

    public GraphValidationResult validate(
            SemanticKnowledgeGraph graph
    ) {
        if (graph == null) {
            return new GraphValidationResult(
                    0,
                    0,
                    new ArrayList<>()
            );
        }

        List<GraphValidationIssue> issues =
                new ArrayList<>();
        List<SemanticConcept> concepts =
                graph.getAllConcepts();
        List<SemanticRelation> relations =
                graph.getAllRelations();

        validateConcepts(
                concepts,
                issues
        );
        validateRelations(
                graph,
                relations,
                issues
        );
        validateConceptRelationCoverage(
                graph,
                concepts,
                issues
        );

        return new GraphValidationResult(
                concepts.size(),
                relations.size(),
                issues
        );
    }

    private void validateConcepts(
            List<SemanticConcept> concepts,
            List<GraphValidationIssue> issues
    ) {
        Set<String> seenConceptIds =
                new HashSet<>();
        Set<String> reportedConceptIds =
                new HashSet<>();
        Set<String> seenTokens =
                new HashSet<>();
        Set<String> reportedTokens =
                new HashSet<>();

        for (SemanticConcept concept : concepts) {
            if (concept == null) {
                continue;
            }

            String conceptId =
                    normalize(
                            concept.getId()
                    );

            if (
                    !conceptId.isEmpty()
                            &&
                            !seenConceptIds.add(
                                    conceptId
                            )
                            &&
                            reportedConceptIds.add(
                                    conceptId
                            )
            ) {
                issues.add(
                        issue(
                                GraphValidationIssueType.DUPLICATE_CONCEPT_ID,
                                conceptId,
                                null,
                                null,
                                "Duplicate concept id: " + conceptId
                        )
                );
            }

            List<String> tokens =
                    concept.getTokens();

            if (
                    tokens == null
                            ||
                            tokens.isEmpty()
            ) {
                issues.add(
                        issue(
                                GraphValidationIssueType.CONCEPT_WITHOUT_TOKEN,
                                conceptId,
                                null,
                                null,
                                "Concept has no tokens: " + conceptId
                        )
                );
                continue;
            }

            boolean hasNonEmptyToken =
                    false;

            for (String token : tokens) {
                String normalizedToken =
                        normalize(
                                token
                        );

                if (normalizedToken.isEmpty()) {
                    continue;
                }

                hasNonEmptyToken =
                        true;

                if (
                        !seenTokens.add(
                                normalizedToken
                        )
                                &&
                                reportedTokens.add(
                                        normalizedToken
                                )
                ) {
                    issues.add(
                            issue(
                                    GraphValidationIssueType.DUPLICATE_TOKEN,
                                    conceptId,
                                    normalizedToken,
                                    null,
                                    "Duplicate token: " + normalizedToken
                            )
                    );
                }
            }

            if (!hasNonEmptyToken) {
                issues.add(
                        issue(
                                GraphValidationIssueType.CONCEPT_WITHOUT_TOKEN,
                                conceptId,
                                null,
                                null,
                                "Concept has no non-empty tokens: " + conceptId
                        )
                );
            }
        }
    }

    private void validateRelations(
            SemanticKnowledgeGraph graph,
            List<SemanticRelation> relations,
            List<GraphValidationIssue> issues
    ) {
        for (SemanticRelation relation : relations) {
            if (relation == null) {
                continue;
            }

            String sourceConceptId =
                    normalize(
                            relation.getSourceConceptId()
                    );
            String targetConceptId =
                    normalize(
                            relation.getTargetConceptId()
                    );

            if (
                    sourceConceptId.isEmpty()
                            ||
                            graph.getConceptById(
                                    sourceConceptId
                            ) == null
            ) {
                issues.add(
                        issue(
                                GraphValidationIssueType.MISSING_SOURCE_CONCEPT,
                                sourceConceptId,
                                null,
                                relation,
                                "Missing source concept: " + sourceConceptId
                        )
                );
            }

            if (
                    targetConceptId.isEmpty()
                            ||
                            graph.getConceptById(
                                    targetConceptId
                            ) == null
            ) {
                issues.add(
                        issue(
                                GraphValidationIssueType.MISSING_TARGET_CONCEPT,
                                targetConceptId,
                                null,
                                relation,
                                "Missing target concept: " + targetConceptId
                        )
                );
            }

            if (
                    !sourceConceptId.isEmpty()
                            &&
                            sourceConceptId.equals(
                                    targetConceptId
                            )
            ) {
                issues.add(
                        issue(
                                GraphValidationIssueType.SELF_RELATION,
                                sourceConceptId,
                                null,
                                relation,
                                "Self relation: " + sourceConceptId
                        )
                );
            }
        }
    }

    private void validateConceptRelationCoverage(
            SemanticKnowledgeGraph graph,
            List<SemanticConcept> concepts,
            List<GraphValidationIssue> issues
    ) {
        for (SemanticConcept concept : concepts) {
            if (concept == null) {
                continue;
            }

            String conceptId =
                    normalize(
                            concept.getId()
                    );

            if (
                    !conceptId.isEmpty()
                            &&
                            graph.getRelations(
                                    conceptId
                            ).isEmpty()
            ) {
                issues.add(
                        issue(
                                GraphValidationIssueType.CONCEPT_WITHOUT_RELATIONS,
                                conceptId,
                                null,
                                null,
                                "Concept has no outgoing relations: " + conceptId
                        )
                );
            }
        }
    }

    private static GraphValidationIssue issue(
            GraphValidationIssueType type,
            String conceptId,
            String token,
            SemanticRelation relation,
            String message
    ) {
        return new GraphValidationIssue(
                type,
                conceptId,
                token,
                relation,
                message
        );
    }

    private static String normalize(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}
