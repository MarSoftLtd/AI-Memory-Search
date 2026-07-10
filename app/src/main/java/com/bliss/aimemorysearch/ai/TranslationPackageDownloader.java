package com.bliss.aimemorysearch.ai;

import android.content.Context;

import java.io.File;

public final class TranslationPackageDownloader {

    private static volatile TranslationPackageDownloader instance;

    private final AiPackageDownloader downloader;

    private TranslationPackageDownloader(
            Context context
    ) {
        this.downloader =
                new AiPackageDownloader(
                        context.getApplicationContext()
                );
    }

    public static synchronized TranslationPackageDownloader getInstance(
            Context context
    ) {
        if (instance == null) {
            instance =
                    new TranslationPackageDownloader(
                            context.getApplicationContext()
                    );
        }

        return instance;
    }

    public File download(
            TranslationPackageInfo packageInfo
    ) throws Exception {

        if (
                packageInfo == null
        ) {
            throw new IllegalArgumentException(
                    "Invalid translation package"
            );
        }

        return downloader.download(
                packageInfo.getAiPackageInfo()
        );
    }
}
