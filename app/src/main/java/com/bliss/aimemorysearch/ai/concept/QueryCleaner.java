package com.bliss.aimemorysearch.ai.concept;

import java.text.Normalizer;
import java.util.Locale;

public class QueryCleaner {

    public QueryTokens clean(String query) {

        QueryTokens result =
                new QueryTokens();

        if (query == null) {
            return result;
        }

        query =
                Normalizer.normalize(
                        query,
                        Normalizer.Form.NFD
                );

        query =
                query.replaceAll(
                        "\\p{InCombiningDiacriticalMarks}+",
                        ""
                );

        query =
                query.toLowerCase(Locale.ROOT);

        query =
                query.replaceAll(
                        "[^a-z0-9 ]",
                        " "
                );

        query =
                query.replaceAll(
                        "\\s+",
                        " "
                );

        query =
                query.trim();

        if (query.isEmpty()) {
            return result;
        }

        String[] split =
                query.split(" ");

        for (String word : split) {

            result.add(word);
        }

        return result;
    }
}