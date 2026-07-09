package com.bliss.aimemorysearch.ai;

import android.content.Context;

import java.io.File;

public final class TranslationModelManager {

    private static volatile TranslationModelManager instance;

    private final TranslationPackageManager packageManager;
    private final TranslationPackageDownloadManager downloadManager;

    private TranslationModelManager(
            TranslationPackageManager packageManager,
            TranslationPackageDownloadManager downloadManager
    ) {
        this.packageManager =
                packageManager;
        this.downloadManager =
                downloadManager;
    }

    public static synchronized TranslationModelManager getInstance(
            Context context
    ) {
        if (instance == null) {
            Context applicationContext =
                    context.getApplicationContext();

            instance =
                    new TranslationModelManager(
                            TranslationPackageManager.getInstance(
                                    applicationContext
                            ),
                            TranslationPackageDownloadManager.getInstance(
                                    applicationContext
                            )
                    );
        }

        return instance;
    }

    public TranslationPackage getOrInstall(
            TranslationModelId modelId
    ) throws Exception {
        TranslationPackage translationPackage =
                packageManager.getPackage(
                        modelId
                );

        if (translationPackage.isInstalled()) {
            return translationPackage;
        }

        return downloadManager.ensureInstalled(
                modelId
        );
    }

    public File getModelDirectory(
            TranslationModelId modelId
    ) throws Exception {
        return getOrInstall(
                modelId
        ).getDirectory();
    }
}
