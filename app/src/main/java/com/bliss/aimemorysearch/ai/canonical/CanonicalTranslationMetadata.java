package com.bliss.aimemorysearch.ai.canonical;

import java.util.Locale;

public final class CanonicalTranslationMetadata {

    private final String language;
    private final String family;
    private final float confidence;
    private final String normalizationVersion;
    private final String translationModelVersion;

    public CanonicalTranslationMetadata(
            String language,
            String family,
            float confidence,
            String normalizationVersion,
            String translationModelVersion
    ) {
        this.language = requireScope(language, "language");
        this.family = requireScope(family, "family");
        if (Float.isNaN(confidence)
                || Float.isInfinite(confidence)
                || confidence < 0f
                || confidence > 1f) {
            throw new IllegalArgumentException(
                    "confidence must be between 0 and 1"
            );
        }
        this.confidence = confidence;
        this.normalizationVersion = requireVersion(
                normalizationVersion,
                "normalizationVersion"
        );
        this.translationModelVersion = requireVersion(
                translationModelVersion,
                "translationModelVersion"
        );
    }

    public String getLanguage() {
        return language;
    }

    public String getFamily() {
        return family;
    }

    public float getConfidence() {
        return confidence;
    }

    public String getNormalizationVersion() {
        return normalizationVersion;
    }

    public String getTranslationModelVersion() {
        return translationModelVersion;
    }

    private static String requireScope(String value, String name) {
        String normalized = requireVersion(value, name)
                .toLowerCase(Locale.ROOT);
        if (normalized.indexOf('\u0000') >= 0) {
            throw new IllegalArgumentException(name + " contains NUL");
        }
        return normalized;
    }

    private static String requireVersion(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return value.trim();
    }
}
