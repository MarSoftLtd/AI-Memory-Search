package com.bliss.aimemorysearch.ai;

import java.util.List;

public interface ConceptRelationRepository {

    List<SemanticRelation> getRelations(
            String conceptId
    );

    List<SemanticRelation> getAllRelations();
}
