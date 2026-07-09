package com.bliss.aimemorysearch.ai;

import java.util.List;

public interface SemanticConceptRepository {

    SemanticConcept findConcept(
            String token
    );

    SemanticConcept findConceptByToken(
            String token
    );

    SemanticConcept findConceptById(
            String conceptId
    );

    List<SemanticConcept> getAllConcepts();
}
