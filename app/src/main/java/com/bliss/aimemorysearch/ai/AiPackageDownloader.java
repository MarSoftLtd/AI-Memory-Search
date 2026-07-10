package com.bliss.aimemorysearch.ai;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public final class AiPackageDownloader {

    private final Context context;
    private volatile boolean cancelled;

    public AiPackageDownloader(
            Context context
    ) {
        this.context =
                context.getApplicationContext();
    }

    public File download(
            AiPackageInfo packageInfo
    ) throws Exception {
        AiPackageDownloadResult result =
                downloadPackage(
                        packageInfo,
                        null
                );

        if (!result.isSuccess()) {
            if (result.getError() instanceof Exception) {
                throw (Exception) result.getError();
            }

            throw new IllegalStateException(
                    result.getMessage()
            );
        }

        return result.getDownloadedFile();
    }

    public AiPackageDownloadResult downloadPackage(
            AiPackageInfo packageInfo
    ) {
        return downloadPackage(
                packageInfo,
                null
        );
    }

    public AiPackageDownloadResult downloadPackage(
            AiPackageInfo packageInfo,
            AiPackageDownloadProgressCallback progressCallback
    ) {
        cancelled =
                false;

        if (
                packageInfo == null
                        ||
                        packageInfo.getDownloadUrl() == null
                        ||
                        packageInfo.getDownloadUrl().trim().isEmpty()
        ) {
            return failure(
                    packageInfo,
                    null,
                    0L,
                    "Invalid AI package download URL",
                    null
            );
        }

        File outputFile =
                new File(
                        context.getCacheDir(),
                        buildDownloadFileName(
                                packageInfo
                        )
                );

        try {
            long downloadedBytes =
                    downloadToFile(
                            packageInfo.getDownloadUrl(),
                            outputFile,
                            progressCallback
                    );

            if (cancelled) {
                if (outputFile.exists()) {
                    outputFile.delete();
                }

                return new AiPackageDownloadResult(
                        false,
                        true,
                        packageInfo,
                        null,
                        downloadedBytes,
                        "Download cancelled",
                        null
                );
            }

            return new AiPackageDownloadResult(
                    true,
                    false,
                    packageInfo,
                    outputFile,
                    downloadedBytes,
                    "Package downloaded",
                    null
            );
        } catch (Exception e) {
            return failure(
                    packageInfo,
                    outputFile,
                    0L,
                    "Package download failed",
                    e
            );
        }
    }

    public void cancel() {
        cancelled =
                true;
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

    private long downloadToFile(
            String downloadUrl,
            File outputFile,
            AiPackageDownloadProgressCallback progressCallback
    ) throws Exception {

        URLConnection connection =
                new URL(
                        downloadUrl
                ).openConnection();

        connection.connect();

        long totalBytes =
                connection.getContentLengthLong();
        long downloadedBytes =
                0L;

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
                if (cancelled) {
                    break;
                }

                outputStream.write(
                        buffer,
                        0,
                        read
                );

                downloadedBytes +=
                        read;

                if (progressCallback != null) {
                    progressCallback.onProgress(
                            downloadedBytes,
                            totalBytes
                    );
                }
            }
        }

        return downloadedBytes;
    }

    private static AiPackageDownloadResult failure(
            AiPackageInfo packageInfo,
            File downloadedFile,
            long downloadedBytes,
            String message,
            Throwable error
    ) {
        return new AiPackageDownloadResult(
                false,
                false,
                packageInfo,
                downloadedFile,
                downloadedBytes,
                message,
                error
        );
    }
}
