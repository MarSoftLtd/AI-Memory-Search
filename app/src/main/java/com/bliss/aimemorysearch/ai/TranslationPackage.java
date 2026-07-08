package com.bliss.aimemorysearch.ai;

import java.io.File;

public final class TranslationPackage {

    private final TranslationModelId modelId;
    private final File directory;
    private final TranslationPackageManifest manifest;
    private final boolean installed;

    public TranslationPackage(
            TranslationModelId modelId,
            File directory,
            TranslationPackageManifest manifest,
            boolean installed
    ) {
        this.modelId =
                modelId;
        this.directory =
                directory;
        this.manifest =
                manifest;
        this.installed =
                installed;
    }

    public TranslationModelId getModelId() {
        return modelId;
    }

    public File getDirectory() {
        return directory;
    }

    public TranslationPackageManifest getManifest() {
        return manifest;
    }

    public boolean isInstalled() {
        return installed;
    }
}
