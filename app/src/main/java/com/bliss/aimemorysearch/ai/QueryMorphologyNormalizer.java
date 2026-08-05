package com.bliss.aimemorysearch.ai;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Lightweight exact-form normalization used only for search queries. */
public final class QueryMorphologyNormalizer {

    private static final Map<String, String> LEMMAS = buildLemmas();

    private QueryMorphologyNormalizer() {
    }

    public static String normalize(String normalizedQuery) {
        if (normalizedQuery == null || normalizedQuery.trim().isEmpty()) {
            return "";
        }
        String[] tokens = normalizedQuery.trim().split("\\s+");
        StringBuilder result = new StringBuilder(normalizedQuery.length());
        for (String token : tokens) {
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(LEMMAS.getOrDefault(token, token));
        }
        return result.toString();
    }

    private static Map<String, String> buildLemmas() {
        Map<String, String> lemmas = new HashMap<>();
        addFamily(lemmas, "masina", "masini");
        addFamily(lemmas, "floare", "flori");
        addFamily(lemmas, "trandafir", "trandafiri");
        addFamily(lemmas, "copil", "copii");
        return Collections.unmodifiableMap(lemmas);
    }

    private static void addFamily(
            Map<String, String> lemmas,
            String lemma,
            String... forms
    ) {
        lemmas.put(lemma, lemma);
        for (String form : forms) {
            lemmas.put(form, lemma);
        }
    }
}
