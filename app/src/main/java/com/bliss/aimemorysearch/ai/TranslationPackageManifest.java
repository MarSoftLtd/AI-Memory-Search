package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TranslationPackageManifest {

    private final AiPackageManifest packageManifest;
    private final String family;
    private final String translatorEngine;
    private final String modelDirectory;
    private final String tokenizer;
    private final List<String> supportedLanguages;

    public TranslationPackageManifest(
            String packageId,
            String displayName,
            String family,
            String version,
            String minimumAppVersion,
            String translatorEngine,
            String modelDirectory,
            String tokenizer,
            List<String> supportedLanguages
    ) {
        this.packageManifest =
                new AiPackageManifest(
                        packageId,
                        AiPackageType.TRANSLATION,
                        displayName,
                        version,
                        minimumAppVersion
                );
        this.family =
                family;
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

    public AiPackageManifest getAiPackageManifest() {
        return packageManifest;
    }

    public String getPackageId() {
        return packageManifest.getPackageId();
    }

    public String getDisplayName() {
        return packageManifest.getDisplayName();
    }

    public String getFamily() {
        return family;
    }

    public String getVersion() {
        return packageManifest.getVersion();
    }

    public String getMinimumAppVersion() {
        return packageManifest.getMinimumAppVersion();
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
