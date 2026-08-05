package com.bliss.aimemorysearch.ai.concepts;

import java.util.List;

/** Emits semantic assertions without changing structural concept extraction. */
public interface SemanticAssertionProvider {

    List<SemanticAssertion> provideAssertions(ConceptGraph conceptGraph);
}
