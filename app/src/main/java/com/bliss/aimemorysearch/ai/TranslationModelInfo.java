package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TranslationModelInfo {

    private final TranslationModelId id;
    private final String translationFamily;
    private final String assetDirectory;
    private final String internalDirectory;
    private final List<String> supportedLanguages;

    public TranslationModelInfo(
            TranslationModelId id,
            String translationFamily,
            String assetDirectory,
            String internalDirectory,
            List<String> supportedLanguages
    ) {
        this.id =
                id;
        this.translationFamily =
                translationFamily;
        this.assetDirectory =
                assetDirectory;
        this.internalDirectory =
                internalDirectory;
        this.supportedLanguages =
                Collections.unmodifiableList(
                        new ArrayList<>(supportedLanguages)
                );
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

    public List<String> getSupportedLanguages() {
        return supportedLanguages;
    }
}
