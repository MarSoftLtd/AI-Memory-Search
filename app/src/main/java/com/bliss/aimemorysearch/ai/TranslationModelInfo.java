package com.bliss.aimemorysearch.ai;

public final class TranslationModelInfo {

    private final TranslationModelId id;
    private final String translationFamily;
    private final String assetDirectory;
    private final String internalDirectory;

    public TranslationModelInfo(
            TranslationModelId id,
            String translationFamily,
            String assetDirectory,
            String internalDirectory
    ) {
        this.id =
                id;
        this.translationFamily =
                translationFamily;
        this.assetDirectory =
                assetDirectory;
        this.internalDirectory =
                internalDirectory;
    }

    public TranslationModelId getId() {
        return id;
    }

    public String getTranslationFamily() {
        return translationFamily;
    }

    public String getAssetDirectory() {
        return assetDirectory;
    }

    public String getInternalDirectory() {
        return internalDirectory;
    }
}
