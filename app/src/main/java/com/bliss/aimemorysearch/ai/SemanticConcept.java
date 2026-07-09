package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SemanticConcept {

    private final String id;
    private final String label;
    private final String language;
    private final List<String> tokens;

    public SemanticConcept(
            String id,
            String label,
            String language,
            List<String> tokens
    ) {
        this.id =
                nonNullString(
                        id
                );
        this.label =
                nonNullString(
                        label
                );
        this.language =
                nonNullString(
                        language
                );
        this.tokens =
                Collections.unmodifiableList(
                        nonNullList(
                                tokens
                        )
                );
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getLanguage() {
        return language;
    }

    public List<String> getTokens() {
        return tokens;
    }

    private static String nonNullString(
            String value
    ) {
        return value != null
                ? value
                : "";
    }

    private static List<String> nonNullList(
            List<String> values
    ) {
        if (values == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(
                values
        );
    }
}
