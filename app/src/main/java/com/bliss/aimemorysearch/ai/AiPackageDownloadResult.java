package com.bliss.aimemorysearch.ai;

import java.io.File;

public final class AiPackageDownloadResult {

    private final boolean success;
    private final boolean cancelled;
    private final AiPackageInfo packageInfo;
    private final File downloadedFile;
    private final long downloadedBytes;
    private final String message;
    private final Throwable error;

    public AiPackageDownloadResult(
            boolean success,
            boolean cancelled,
            AiPackageInfo packageInfo,
            File downloadedFile,
            long downloadedBytes,
            String message,
            Throwable error
    ) {
        this.success =
                success;
        this.cancelled =
                cancelled;
        this.packageInfo =
                packageInfo;
        this.downloadedFile =
                downloadedFile;
        this.downloadedBytes =
                downloadedBytes;
        this.message =
                message;
        this.error =
                error;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public AiPackageInfo getPackageInfo() {
        return packageInfo;
    }

    public File getDownloadedFile() {
        return downloadedFile;
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getError() {
        return error;
    }
}
