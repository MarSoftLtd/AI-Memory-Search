package com.bliss.aimemorysearch.ai;

public final class TranslationModelInfo {

    private final TranslationModelId id;
    private final String assetDirectory;
    private final String internalDirectory;

    public TranslationModelInfo(
            TranslationModelId id,
            String assetDirectory,
            String internalDirectory
    ) {
        this.id =
                id;
        this.assetDirectory =
                assetDirectory;
        this.internalDirectory =
                internalDirectory;
    }

    public TranslationModelId getId() {
        return id;
    }

    public String getAssetDirectory() {
        return assetDirectory;
    }

    public String getInternalDirectory() {
        return internalDirectory;
    }
}
