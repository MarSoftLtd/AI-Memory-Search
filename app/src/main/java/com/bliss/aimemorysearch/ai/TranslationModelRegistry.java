package com.bliss.aimemorysearch.ai;

public final class TranslationModelRegistry {

    private static final TranslationModelInfo ROMANCE =
            new TranslationModelInfo(
                    TranslationModelId.ROMANCE,
                    "models/translator/romance-en",
                    "models/translator/romance-en"
            );

    private static final TranslationModelInfo GERMANIC =
            new TranslationModelInfo(
                    TranslationModelId.GERMANIC,
                    "models/translator/germanic-en",
                    "models/translator/germanic-en"
            );

    private static final TranslationModelInfo SLAVIC =
            new TranslationModelInfo(
                    TranslationModelId.SLAVIC,
                    "models/translator/slavic-en",
                    "models/translator/slavic-en"
            );

    private static final TranslationModelInfo GREEK =
            new TranslationModelInfo(
                    TranslationModelId.GREEK,
                    "models/translator/greek-en",
                    "models/translator/greek-en"
            );

    private static final TranslationModelInfo TURKISH =
            new TranslationModelInfo(
                    TranslationModelId.TURKISH,
                    "models/translator/turkish-en",
                    "models/translator/turkish-en"
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
}
