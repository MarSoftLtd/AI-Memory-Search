package com.bliss.aimemorysearch.ai;

import android.content.Context;

import java.io.File;

public final class TranslationPackageManager {

    private static final String MANIFEST_FILE_NAME =
            "manifest.json";

    private static volatile TranslationPackageManager instance;

    private final ModelStorageManager storageManager;
    private final ModelInstallationManager installationManager;

    private TranslationPackageManager(
            Context context
    ) {
        Context applicationContext =
                context.getApplicationContext();

        storageManager =
                ModelStorageManager.getInstance(
                        applicationContext
                );
        installationManager =
                ModelInstallationManager.getInstance(
                        applicationContext
                );
    }

    public static synchronized TranslationPackageManager getInstance(
            Context context
    ) {
        if (instance == null) {
            instance =
                    new TranslationPackageManager(
                            context.getApplicationContext()
                    );
        }

        return instance;
    }

    public TranslationPackage getPackage(
            TranslationModelId modelId
    ) {
        File directory =
                storageManager.getModelDirectory(
                        modelId
                );

        File manifestFile =
                new File(
                        directory,
                        MANIFEST_FILE_NAME
                );

        boolean installed =
                installationManager.isInstalled(
                        modelId
                );

        return new TranslationPackage(
                modelId,
                directory,
                manifestFile,
                installed
        );
    }
}
