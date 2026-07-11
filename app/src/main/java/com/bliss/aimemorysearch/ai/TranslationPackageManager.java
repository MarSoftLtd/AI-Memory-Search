package com.bliss.aimemorysearch.ai;

import android.content.Context;

import java.io.File;

public final class TranslationPackageManager {

    private static volatile TranslationPackageManager instance;

    private final ModelStorageManager storageManager;
    private final AiStorageManager aiStorageManager;
    private final ModelInstallationManager installationManager;
    private final TranslationPackageManifestReader manifestReader;
    private final BundledAiPackageInstaller bundledPackageInstaller;
    private final TranslationPackageValidator packageValidator;

    private TranslationPackageManager(
            Context context
    ) {
        Context applicationContext =
                context.getApplicationContext();

        storageManager =
                ModelStorageManager.getInstance(
                        applicationContext
                );
        aiStorageManager =
                new AiStorageManager(
                        applicationContext
                );
        installationManager =
                ModelInstallationManager.getInstance(
                        applicationContext
                );
        manifestReader =
                new TranslationPackageManifestReader();
        bundledPackageInstaller =
                new BundledAiPackageInstaller(
                        applicationContext,
                        AiPlatform.getPackageManager()
                );
        packageValidator =
                new TranslationPackageValidator();
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
        if (modelId == TranslationModelId.ROMANCE) {
            try {
                bundledPackageInstaller.ensureInstalled(
                        BundledAiPackageCatalog.get(
                                AiCapability.TRANSLATION
                        )
                );
            } catch (Exception e) {
                android.util.Log.e(
                        "AI_PACKAGE",
                        "Failed to install bundled Romance translation package",
                        e
                );
            }
        }

        File directory =
                getPackageDirectory(modelId);

        TranslationPackageLayout layout =
                new TranslationPackageLayout(
                        directory
                );

        boolean installed =
                packageValidator.isValid(
                        layout
                );

        TranslationPackageManifest manifest =
                readManifest(
                        layout
                );

        return new TranslationPackage(
                modelId,
                layout.getRootDirectory(),
                manifest,
                installed
        );
    }

    private File getPackageDirectory(
            TranslationModelId modelId
    ) {
        if (modelId == TranslationModelId.ROMANCE) {
            return aiStorageManager.getInstalledPackageDirectory(
                    BundledAiPackageCatalog.ROMANCE_TRANSLATION_PACKAGE_ID
            );
        }

        return storageManager.getModelDirectory(modelId);
    }

    private TranslationPackageManifest readManifest(
            TranslationPackageLayout layout
    ) {
        try {
            return manifestReader.read(
                    layout
            );
        } catch (Exception e) {
            return null;
        }
    }
}
