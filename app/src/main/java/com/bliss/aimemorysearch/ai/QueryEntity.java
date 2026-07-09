package com.bliss.aimemorysearch.ai;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class QueryEntity {

    private final QueryEntityType type;
    private final String text;
    private final String normalizedText;
    private final String canonicalValue;
    private final QueryEntityRole role;
    private final int startTokenIndex;
    private final int endTokenIndex;
    private final float confidence;
    private final QueryEntitySource source;
    private final Map<String, Object> attributes;

    public QueryEntity(
            QueryEntityType type,
            String text,
            String normalizedText,
            String canonicalValue,
            QueryEntityRole role,
            int startTokenIndex,
            int endTokenIndex,
            float confidence,
            QueryEntitySource source,
        Map<String, Object> attributes
    ) {
        this.type =
                Objects.requireNonNull(
                        type,
                        "type"
                );
        this.text =
                nonNullString(
                        text
                );
        this.normalizedText =
                nonNullString(
                        normalizedText
                );
        this.canonicalValue =
                nonNullString(
                        canonicalValue
                );
        this.role =
                Objects.requireNonNull(
                        role,
                        "role"
                );
        this.startTokenIndex =
                startTokenIndex;
        this.endTokenIndex =
                endTokenIndex;
        this.confidence =
                clampConfidence(
                        confidence
                );
        this.source =
                Objects.requireNonNull(
                        source,
                        "source"
                );
        this.attributes =
                immutableAttributeMap(
                        attributes
                );
    }

    public QueryEntityType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public String getNormalizedText() {
        return normalizedText;
    }

    public String getCanonicalValue() {
        return canonicalValue;
    }

    public QueryEntityRole getRole() {
        return role;
    }

    public int getStartTokenIndex() {
        return startTokenIndex;
    }

    public int getEndTokenIndex() {
        return endTokenIndex;
    }

    public float getConfidence() {
        return confidence;
    }

    public QueryEntitySource getSource() {
        return source;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    private static String nonNullString(
            String value
    ) {
        return value == null
                ? ""
                : value;
    }

    private static float clampConfidence(
            float value
    ) {
        if (value < 0f) {
            return 0f;
        }

        if (value > 1f) {
            return 1f;
        }

        return value;
    }

    private static Map<String, Object> immutableAttributeMap(
            Map<String, Object> values
    ) {
        if (values == null) {
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(
                new LinkedHashMap<>(
                        values
                )
        );
    }
}
