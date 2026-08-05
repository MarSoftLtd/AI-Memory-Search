package com.bliss.aimemorysearch.ai;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** Adds deterministic, model-independent multilingual query aliases. */
public final class QueryAliasRegistry {

    private static final List<Alias> ALIASES = Collections.unmodifiableList(
            Arrays.asList(
                    new Alias("invoice", "factura")
            )
    );

    private QueryAliasRegistry() {
    }

    public static List<String> expand(String query) {
        List<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> expansions = new LinkedHashSet<>();
        for (Alias alias : ALIASES) {
            addReplacement(expansions, queryTokens, alias.left, alias.right);
            addReplacement(expansions, queryTokens, alias.right, alias.left);
        }
        return new ArrayList<>(expansions);
    }

    private static void addReplacement(
            LinkedHashSet<String> expansions,
            List<String> queryTokens,
            List<String> source,
            List<String> replacement
    ) {
        if (source.isEmpty() || queryTokens.size() < source.size()) {
            return;
        }
        for (int start = 0;
             start <= queryTokens.size() - source.size();
             start++) {
            if (!matchesAt(queryTokens, source, start)) {
                continue;
            }
            List<String> expanded = new ArrayList<>(queryTokens);
            for (int index = 0; index < source.size(); index++) {
                expanded.remove(start);
            }
            expanded.addAll(start, replacement);
            expansions.add(join(expanded));
        }
    }

    private static boolean matchesAt(
            List<String> queryTokens,
            List<String> aliasTokens,
            int start
    ) {
        for (int index = 0; index < aliasTokens.size(); index++) {
            if (!queryTokens.get(start + index).equals(aliasTokens.get(index))) {
                return false;
            }
        }
        return true;
    }

    private static List<String> tokenize(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(normalized.split(" "));
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static String join(List<String> tokens) {
        StringBuilder value = new StringBuilder();
        for (String token : tokens) {
            if (value.length() > 0) {
                value.append(' ');
            }
            value.append(token);
        }
        return value.toString();
    }

    private static final class Alias {
        final List<String> left;
        final List<String> right;

        Alias(String left, String right) {
            this.left = tokenize(left);
            this.right = tokenize(right);
        }
    }
}
