package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TranslationPackageManifest {

    private final TranslationModelId id;
    private final String displayName;
    private final String family;
    private final String version;
    private final String minimumAppVersion;
    private final String translatorEngine;
    private final String modelDirectory;
    private final String tokenizer;
    private final List<String> supportedLanguages;

    public TranslationPackageManifest(
            TranslationModelId id,
            String displayName,
            String family,
            String version,
            String minimumAppVersion,
            String translatorEngine,
            String modelDirectory,
            String tokenizer,
            List<String> supportedLanguages
    ) {
        this.id =
                id;
        this.displayName =
                displayName;
        this.family =
                family;
        this.version =
                version;
        this.minimumAppVersion =
                minimumAppVersion;
        this.translatorEngine =
                translatorEngine;
        this.modelDirectory =
                modelDirectory;
        this.tokenizer =
                tokenizer;
        this.supportedLanguages =
                Collections.unmodifiableList(
                        new ArrayList<>(
                                supportedLanguages
                        )
                );
    }

    public TranslationModelId getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFamily() {
        return family;
    }

    public String getVersion() {
        return version;
    }

    public String getMinimumAppVersion() {
        return minimumAppVersion;
    }

    public String getTranslatorEngine() {
        return translatorEngine;
    }

    public String getModelDirectory() {
        return modelDirectory;
    }

    public String getTokenizer() {
        return tokenizer;
    }

    public List<String> getSupportedLanguages() {
        return supportedLanguages;
    }
}
