package com.bliss.aimemorysearch.ai;

public final class DefaultQueryNormalizer implements QueryNormalizer {

    @Override
    public void normalize(
            SearchRequest searchRequest
    ) {
        if (searchRequest == null) {
            return;
        }

        searchRequest.setNormalizedQuery(
                searchRequest.getOriginalQuery()
        );
    }
}
