package com.bliss.aimemorysearch.ai;

import java.io.File;

public final class TranslationPackage {

    private final TranslationModelId modelId;
    private final File directory;
    private final File manifestFile;
    private final boolean installed;

    public TranslationPackage(
            TranslationModelId modelId,
            File directory,
            File manifestFile,
            boolean installed
    ) {
        this.modelId =
                modelId;
        this.directory =
                directory;
        this.manifestFile =
                manifestFile;
        this.installed =
                installed;
    }

    public TranslationModelId getModelId() {
        return modelId;
    }

    public File getDirectory() {
        return directory;
    }

    public File getManifestFile() {
        return manifestFile;
    }

    public boolean isInstalled() {
        return installed;
    }
}
