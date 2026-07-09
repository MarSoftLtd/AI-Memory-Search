package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class KnowledgeGraphBuilder {

    private final SemanticConceptRepository conceptRepository;
    private final ConceptRelationRepository relationRepository;

    public KnowledgeGraphBuilder(
            SemanticConceptRepository conceptRepository,
            ConceptRelationRepository relationRepository
    ) {
        this.conceptRepository =
                Objects.requireNonNull(
                        conceptRepository,
                        "conceptRepository"
                );
        this.relationRepository =
                Objects.requireNonNull(
                        relationRepository,
                        "relationRepository"
                );
    }

    public SemanticKnowledgeGraph build() {
        List<SemanticConcept> allConcepts =
                new ArrayList<>();
        List<SemanticRelation> allRelations =
                new ArrayList<>();
        Map<String, SemanticConcept> conceptsById =
                new LinkedHashMap<>();
        Map<String, SemanticConcept> conceptsByToken =
                new LinkedHashMap<>();
        Map<String, List<SemanticRelation>> relationsBySourceConceptId =
                new LinkedHashMap<>();

        loadConcepts(
                allConcepts,
                conceptsById,
                conceptsByToken
        );
        loadRelations(
                allRelations,
                relationsBySourceConceptId
        );

        return new SemanticKnowledgeGraph(
                allConcepts,
                allRelations,
                conceptsById,
                conceptsByToken,
                relationsBySourceConceptId
        );
    }

    private void loadConcepts(
            List<SemanticConcept> allConcepts,
            Map<String, SemanticConcept> conceptsById,
            Map<String, SemanticConcept> conceptsByToken
    ) {
        List<SemanticConcept> concepts =
                conceptRepository.getAllConcepts();

        if (concepts == null) {
            return;
        }

        for (SemanticConcept concept : concepts) {
            if (concept == null) {
                continue;
            }

            allConcepts.add(
                    concept
            );

            String conceptId =
                    normalizeKey(
                            concept.getId()
                    );

            if (
                    !conceptId.isEmpty()
                            &&
                            !conceptsById.containsKey(
                                    conceptId
                            )
            ) {
                conceptsById.put(
                        conceptId,
                        concept
                );
            }

            for (String token : concept.getTokens()) {
                String normalizedToken =
                        normalizeKey(
                                token
                        );

                if (
                        !normalizedToken.isEmpty()
                                &&
                                !conceptsByToken.containsKey(
                                        normalizedToken
                                )
                ) {
                    conceptsByToken.put(
                            normalizedToken,
                            concept
                    );
                }
            }
        }
    }

    private void loadRelations(
            List<SemanticRelation> allRelations,
            Map<String, List<SemanticRelation>> relationsBySourceConceptId
    ) {
        List<SemanticRelation> relations =
                relationRepository.getAllRelations();

        if (relations == null) {
            return;
        }

        for (SemanticRelation relation : relations) {
            if (relation == null) {
                continue;
            }

            allRelations.add(
                    relation
            );

            String sourceConceptId =
                    normalizeKey(
                            relation.getSourceConceptId()
                    );

            if (sourceConceptId.isEmpty()) {
                continue;
            }

            List<SemanticRelation> sourceRelations =
                    relationsBySourceConceptId.get(
                            sourceConceptId
                    );

            if (sourceRelations == null) {
                sourceRelations =
                        new ArrayList<>();
                relationsBySourceConceptId.put(
                        sourceConceptId,
                        sourceRelations
                );
            }

            sourceRelations.add(
                    relation
            );
        }
    }

    private static String normalizeKey(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}
