package com.bliss.aimemorysearch.ai.canonical;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts compact canonical term and adjacent-phrase evidence. */
public final class CanonicalVocabularyExtractor {

    public static final String NORMALIZATION_VERSION = "canonical-nfkc-v1";
    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}]{2,}");
    private static final int MAX_TERMS_PER_UNIT = 4096;

    public Map<CanonicalHash, Integer> extract(String canonicalText) {
        String normalized = normalize(canonicalText);
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(normalized);
        while (matcher.find() && tokens.size() < MAX_TERMS_PER_UNIT) {
            tokens.add(matcher.group());
        }

        Map<CanonicalHash, Integer> frequencies = new LinkedHashMap<>();
        for (int index = 0; index < tokens.size(); index++) {
            add(frequencies, tokens.get(index));
            if (index + 1 < tokens.size()) {
                add(frequencies,
                        tokens.get(index) + " " + tokens.get(index + 1));
            }
        }
        return frequencies;
    }

    private static void add(
            Map<CanonicalHash, Integer> frequencies,
            String expression
    ) {
        CanonicalHash hash = CanonicalHash.fromCanonicalText(
                expression,
                NORMALIZATION_VERSION
        );
        frequencies.putIfAbsent(hash, 1);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
    }
}
