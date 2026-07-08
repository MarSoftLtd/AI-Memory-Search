package com.bliss.aimemorysearch.ai;

import android.content.Context;

public final class ModelInstallationManager {

    private static volatile ModelInstallationManager instance;

    private final ModelStorageManager storageManager;

    private ModelInstallationManager(
            Context context
    ) {
        storageManager =
                ModelStorageManager.getInstance(
                        context.getApplicationContext()
                );
    }

    public static synchronized ModelInstallationManager getInstance(
            Context context
    ) {
        if (instance == null) {
            instance =
                    new ModelInstallationManager(
                            context.getApplicationContext()
                    );
        }

        return instance;
    }

    public boolean isInstalled(
            TranslationModelId modelId
    ) {
        return storageManager
                .getModelDirectory(
                        modelId
                )
                .isDirectory();
    }
}
