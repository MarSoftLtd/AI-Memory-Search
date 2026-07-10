package com.bliss.aimemorysearch.ai;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public final class AiPackageDownloader {

    private final Context context;

    public AiPackageDownloader(
            Context context
    ) {
        this.context =
                context.getApplicationContext();
    }

    public File download(
            AiPackageInfo packageInfo
    ) throws Exception {

        if (
                packageInfo == null
                        ||
                        packageInfo.getDownloadUrl() == null
                        ||
                        packageInfo.getDownloadUrl().trim().isEmpty()
        ) {
            throw new IllegalArgumentException(
                    "Invalid AI package download URL"
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
            AiPackageInfo packageInfo
    ) {
        String packageId =
                packageInfo.getPackageId();

        if (
                packageId == null
                        ||
                        packageId.trim().isEmpty()
        ) {
            packageId =
                    "ai-package";
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
