package com.bliss.aimemorysearch.ai;

import java.util.Arrays;
import java.util.Locale;

public final class TranslationModelRegistry {

    private static final TranslationModelInfo ROMANCE =
            new TranslationModelInfo(
                    TranslationModelId.ROMANCE,
                    "romance",
                    "",
                    "romance-en",
                    Arrays.asList("ro", "fr", "es", "it", "pt")
            );

    private static final TranslationModelInfo GERMANIC =
            new TranslationModelInfo(
                    TranslationModelId.GERMANIC,
                    "germanic",
                    "models/translator/germanic-en",
                    "models/translator/germanic-en",
                    Arrays.asList("de", "nl", "sv", "da", "no", "is")
            );

    private static final TranslationModelInfo SLAVIC =
            new TranslationModelInfo(
                    TranslationModelId.SLAVIC,
                    "slavic",
                    "models/translator/slavic-en",
                    "models/translator/slavic-en",
                    Arrays.asList("ru", "uk", "pl", "cs", "sk", "bg", "sr", "hr", "sl")
            );

    private static final TranslationModelInfo GREEK =
            new TranslationModelInfo(
                    TranslationModelId.GREEK,
                    "greek",
                    "models/translator/greek-en",
                    "models/translator/greek-en",
                    Arrays.asList("el")
            );

    private static final TranslationModelInfo TURKISH =
            new TranslationModelInfo(
                    TranslationModelId.TURKISH,
                    "turkish",
                    "models/translator/turkish-en",
                    "models/translator/turkish-en",
                    Arrays.asList("tr")
            );

    private TranslationModelRegistry() {
    }

    public static TranslationModelInfo getModel(
            TranslationModelId id
    ) {
        if (id == TranslationModelId.ROMANCE) {
            return ROMANCE;
        }

        if (id == TranslationModelId.GERMANIC) {
            return GERMANIC;
        }

        if (id == TranslationModelId.SLAVIC) {
            return SLAVIC;
        }

        if (id == TranslationModelId.GREEK) {
            return GREEK;
        }

        if (id == TranslationModelId.TURKISH) {
            return TURKISH;
        }

        throw new IllegalArgumentException(
                "Unknown translation model: " + id
        );
    }

    public static TranslationModelInfo getModelByFamily(
            String family
    ) {
        if (family == null || family.trim().isEmpty()) {
            return null;
        }

        String normalized =
                family.trim().toLowerCase(Locale.ROOT);

        for (TranslationModelId id : TranslationModelId.values()) {
            TranslationModelInfo modelInfo = getModel(id);
            if (modelInfo.getTranslationFamily().equals(normalized)) {
                return modelInfo;
            }
        }

        return null;
    }

    public static TranslationModelInfo getModelForLanguage(
            String languageCode
    ) {
        if (languageCode == null || languageCode.trim().isEmpty()) {
            return null;
        }

        String normalized =
                languageCode.trim().toLowerCase(Locale.ROOT);

        for (TranslationModelId id : TranslationModelId.values()) {
            TranslationModelInfo modelInfo = getModel(id);
            if (modelInfo.getSupportedLanguages().contains(normalized)) {
                return modelInfo;
            }
        }

        return null;
    }
}
