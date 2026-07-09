package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;
import java.util.List;

public final class DefaultQueryTokenizer implements QueryTokenizer {

    @Override
    public void tokenize(
            SearchRequest searchRequest
    ) {
        if (searchRequest == null) {
            return;
        }

        String normalizedQuery =
                searchRequest.getNormalizedQuery();

        List<String> tokens =
                new ArrayList<>();

        if (
                normalizedQuery == null
                        ||
                        normalizedQuery.isEmpty()
        ) {
            searchRequest.setTokens(
                    tokens
            );
            return;
        }

        String[] parts =
                normalizedQuery.split(
                        "\\s+"
                );

        for (String part : parts) {
            if (
                    part != null
                            &&
                            !part.isEmpty()
            ) {
                tokens.add(
                        part
                );
            }
        }

        searchRequest.setTokens(
                tokens
        );
    }
}
