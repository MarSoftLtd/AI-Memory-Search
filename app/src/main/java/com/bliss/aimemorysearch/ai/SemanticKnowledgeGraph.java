package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SemanticKnowledgeGraph {

    private final List<SemanticConcept> allConcepts;
    private final List<SemanticRelation> allRelations;
    private final Map<String, SemanticConcept> conceptsById;
    private final Map<String, SemanticConcept> conceptsByToken;
    private final Map<String, List<SemanticRelation>> relationsBySourceConceptId;

    public SemanticKnowledgeGraph(
            List<SemanticConcept> allConcepts,
            List<SemanticRelation> allRelations,
            Map<String, SemanticConcept> conceptsById,
            Map<String, SemanticConcept> conceptsByToken,
            Map<String, List<SemanticRelation>> relationsBySourceConceptId
    ) {
        this.allConcepts =
                immutableList(
                        allConcepts
                );
        this.allRelations =
                immutableList(
                        allRelations
                );
        this.conceptsById =
                immutableMap(
                        conceptsById
                );
        this.conceptsByToken =
                immutableMap(
                        conceptsByToken
                );
        this.relationsBySourceConceptId =
                immutableRelationMap(
                        relationsBySourceConceptId
                );
    }

    public SemanticConcept getConceptById(
            String conceptId
    ) {
        if (conceptId == null) {
            return null;
        }

        return conceptsById.get(
                conceptId.trim()
        );
    }

    public SemanticConcept getConceptByToken(
            String token
    ) {
        if (token == null) {
            return null;
        }

        return conceptsByToken.get(
                token.trim()
        );
    }

    public List<SemanticConcept> getAllConcepts() {
        return allConcepts;
    }

    public List<SemanticRelation> getAllRelations() {
        return allRelations;
    }

    public List<SemanticRelation> getRelations(
            String sourceConceptId
    ) {
        if (sourceConceptId == null) {
            return Collections.emptyList();
        }

        List<SemanticRelation> relations =
                relationsBySourceConceptId.get(
                        sourceConceptId.trim()
                );

        if (relations == null) {
            return Collections.emptyList();
        }

        return relations;
    }

    private static <T> List<T> immutableList(
            List<T> values
    ) {
        if (values == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(
                new ArrayList<>(
                        values
                )
        );
    }

    private static <T> Map<String, T> immutableMap(
            Map<String, T> values
    ) {
        if (values == null) {
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(
                new LinkedHashMap<>(
                        values
                )
        );
    }

    private static Map<String, List<SemanticRelation>> immutableRelationMap(
            Map<String, List<SemanticRelation>> values
    ) {
        if (values == null) {
            return Collections.emptyMap();
        }

        Map<String, List<SemanticRelation>> result =
                new LinkedHashMap<>();

        for (Map.Entry<String, List<SemanticRelation>> entry : values.entrySet()) {
            result.put(
                    entry.getKey(),
                    immutableList(
                            entry.getValue()
                    )
            );
        }

        return Collections.unmodifiableMap(
                result
        );
    }
}
