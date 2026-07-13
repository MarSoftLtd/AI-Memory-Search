package com.bliss.aimemorysearch.ai;

import android.content.Context;

import java.io.File;

public final class LanguagePackageBootstrapper {

    private final AiStorageManager storageManager;
    private final AiPackageManager packageManager;
    private final TranslationPackageManifestReader manifestReader;
    private final TranslationPackageValidator packageValidator;

    public LanguagePackageBootstrapper(
            Context context,
            AiPackageManager packageManager
    ) {
        storageManager =
                new AiStorageManager(
                        context.getApplicationContext()
                );
        this.packageManager = packageManager;
        manifestReader = new TranslationPackageManifestReader();
        packageValidator = new TranslationPackageValidator();
    }

    public int restoreInstalledPackages() {
        File rootDirectory =
                storageManager.getPackageRootDirectory();
        File[] packageDirectories =
                rootDirectory.listFiles(File::isDirectory);

        if (packageDirectories == null) {
            return 0;
        }

        int restored = 0;

        for (File packageDirectory : packageDirectories) {
            TranslationPackageLayout layout =
                    new TranslationPackageLayout(packageDirectory);

            if (!packageValidator.isValid(layout)) {
                continue;
            }

            try {
                TranslationPackageManifest manifest =
                        manifestReader.read(layout);
                AiPackageInfo packageInfo =
                        new AiPackageInfo(
                                manifest.getPackageId(),
                                AiPackageType.TRANSLATION,
                                manifest.getFamily(),
                                manifest.getDisplayName(),
                                "",
                                manifest.getVersion(),
                                packageDirectory.length(),
                                0L,
                                "",
                                "",
                                manifest.getMinimumAppVersion()
                        );

                packageManager.registerInstalledPackage(packageInfo);
                restored++;
            } catch (Exception ignored) {
                // Invalid package metadata is not registered.
            }
        }

        return restored;
    }
}
