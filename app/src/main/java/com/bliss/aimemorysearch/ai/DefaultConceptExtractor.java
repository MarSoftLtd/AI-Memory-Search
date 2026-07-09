package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;

public final class DefaultConceptExtractor implements ConceptExtractor {

    @Override
    public void extract(
            SearchRequest searchRequest
    ) {
        if (searchRequest == null) {
            return;
        }

        searchRequest.setConcepts(
                new ArrayList<>(
                        searchRequest.getTokens()
                )
        );
    }
}
