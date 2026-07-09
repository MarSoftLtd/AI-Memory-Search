package com.bliss.aimemorysearch.ai;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public final class TranslationPackageDownloader {

    private static volatile TranslationPackageDownloader instance;

    private final Context context;

    private TranslationPackageDownloader(
            Context context
    ) {
        this.context =
                context.getApplicationContext();
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
                        ||
                        packageInfo.getDownloadUrl() == null
                        ||
                        packageInfo.getDownloadUrl().trim().isEmpty()
        ) {
            throw new IllegalArgumentException(
                    "Invalid translation package download URL"
            );
        }

        File outputFile =
                new File(
                        context.getCacheDir(),
                        buildDownloadFileName(
                                packageInfo
                        )
                );

        downloadToFile(
                packageInfo.getDownloadUrl(),
                outputFile
        );

        return outputFile;
    }

    private static String buildDownloadFileName(
            TranslationPackageInfo packageInfo
    ) {
        String packageId =
                packageInfo.getPackageId();

        if (
                packageId == null
                        ||
                        packageId.trim().isEmpty()
        ) {
            packageId =
                    "translation-package";
        }

        return packageId.trim()
                + "-"
                + packageInfo.getVersion()
                + ".zip";
    }

    private static void downloadToFile(
            String downloadUrl,
            File outputFile
    ) throws Exception {

        URLConnection connection =
                new URL(
                        downloadUrl
                ).openConnection();

        connection.connect();

        try (
                InputStream inputStream =
                        connection.getInputStream();
                FileOutputStream outputStream =
                        new FileOutputStream(
                                outputFile
                        )
        ) {

            byte[] buffer =
                    new byte[8192];
            int read;

            while (
                    (read = inputStream.read(buffer)) != -1
            ) {
                outputStream.write(
                        buffer,
                        0,
                        read
                );
            }
        }
    }
}
