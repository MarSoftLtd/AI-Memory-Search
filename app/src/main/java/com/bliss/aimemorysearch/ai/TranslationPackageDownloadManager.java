package com.bliss.aimemorysearch.ai;

import android.content.Context;

import java.io.File;

public final class TranslationPackageDownloadManager {

    private static volatile TranslationPackageDownloadManager instance;

    private final TranslationPackageResolver resolver;
    private final TranslationPackageDownloader downloader;
    private final TranslationPackageInstaller installer;
    private final TranslationPackageValidator validator;
    private final TranslationPackageManager packageManager;

    public TranslationPackageDownloadManager(
            TranslationPackageResolver resolver,
            TranslationPackageDownloader downloader,
            TranslationPackageInstaller installer,
            TranslationPackageValidator validator,
            TranslationPackageManager packageManager
    ) {
        this.resolver =
                resolver;
        this.downloader =
                downloader;
        this.installer =
                installer;
        this.validator =
                validator;
        this.packageManager =
                packageManager;
    }

    public static synchronized TranslationPackageDownloadManager getInstance(
            Context context
    ) {
        if (instance == null) {
            Context applicationContext =
                    context.getApplicationContext();

            TranslationPackageRepository repository =
                    new AssetsTranslationPackageRepository(
                            applicationContext
                    );

            instance =
                    new TranslationPackageDownloadManager(
                            new TranslationPackageResolver(
                                    repository
                            ),
                            TranslationPackageDownloader.getInstance(
                                    applicationContext
                            ),
                            TranslationPackageInstaller.getInstance(
                                    applicationContext
                            ),
                            new TranslationPackageValidator(),
                            TranslationPackageManager.getInstance(
                                    applicationContext
                            )
                    );
        }

        return instance;
    }

    public TranslationPackage ensureInstalled(
            TranslationModelId modelId
    ) throws Exception {

        TranslationPackage currentPackage =
                packageManager.getPackage(
                        modelId
                );

        if (currentPackage.isInstalled()) {
            return currentPackage;
        }

        TranslationPackageInfo packageInfo =
                resolver.resolve(
                        modelId
                );

        File downloadedPackage =
                downloader.download(
                        packageInfo
                );

        boolean installed =
                installer.install(
                        modelId,
                        downloadedPackage
                );

        if (!installed) {
            throw new IllegalStateException(
                    "Translation package installation failed"
            );
        }

        TranslationPackage installedPackage =
                packageManager.getPackage(
                        modelId
                );

        TranslationPackageLayout layout =
                new TranslationPackageLayout(
                        installedPackage.getDirectory()
                );

        if (!validator.isValid(layout)) {
            throw new IllegalStateException(
                    "Installed translation package is invalid"
            );
        }

        return installedPackage;
    }
}
