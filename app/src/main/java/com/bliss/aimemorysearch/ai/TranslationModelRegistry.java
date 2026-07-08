package com.bliss.aimemorysearch.ai;

public final class TranslationModelRegistry {

    private static final TranslationModelInfo ROMANCE =
            new TranslationModelInfo(
                    TranslationModelId.ROMANCE,
                    "models/translator/romance-en",
                    "models/translator/romance-en"
            );

    private TranslationModelRegistry() {
    }

    public static TranslationModelInfo getModel(
            TranslationModelId id
    ) {
        if (id == TranslationModelId.ROMANCE) {
            return ROMANCE;
        }

        throw new IllegalArgumentException(
                "Unknown translation model: " + id
        );
    }
}
