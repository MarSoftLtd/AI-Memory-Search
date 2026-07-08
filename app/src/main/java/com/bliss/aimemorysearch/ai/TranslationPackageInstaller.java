package com.bliss.aimemorysearch.ai;

import android.content.Context;

import java.io.File;

public final class TranslationPackageInstaller {

    private static volatile TranslationPackageInstaller instance;

    private final ModelStorageManager storageManager;
    private final TranslationPackageValidator packageValidator;

    private TranslationPackageInstaller(
            Context context
    ) {
        Context applicationContext =
                context.getApplicationContext();

        storageManager =
                ModelStorageManager.getInstance(
                        applicationContext
                );
        packageValidator =
                new TranslationPackageValidator();
    }

    public static synchronized TranslationPackageInstaller getInstance(
            Context context
    ) {
        if (instance == null) {
            instance =
                    new TranslationPackageInstaller(
                            context.getApplicationContext()
                    );
        }

        return instance;
    }

    public boolean install(
            TranslationModelId modelId,
            File zipFile
    ) {
        if (
                modelId == null
                        ||
                        zipFile == null
                        ||
                        !zipFile.isFile()
                        ||
                        zipFile.length() == 0
        ) {
            return false;
        }

        File modelDirectory =
                storageManager.getModelDirectory(
                        modelId
                );

        if (
                !modelDirectory.exists()
                        &&
                        !modelDirectory.mkdirs()
        ) {
            return false;
        }

        TranslationPackageLayout layout =
                new TranslationPackageLayout(
                        modelDirectory
                );

        // TODO: Extract zipFile into layout.getRootDirectory().

        return packageValidator.isValid(
                layout
        );
    }
}
