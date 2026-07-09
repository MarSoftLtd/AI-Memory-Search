package com.bliss.aimemorysearch.ai;

public interface SemanticConceptRepository {

    SemanticConcept findConcept(
            String token
    );
}
