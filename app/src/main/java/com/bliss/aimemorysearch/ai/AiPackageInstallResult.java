package com.bliss.aimemorysearch.ai;

import java.io.File;

public final class AiPackageInstallResult {

    private final boolean success;
    private final AiPackageInfo packageInfo;
    private final File installedDirectory;
    private final PackageVerificationResult verificationResult;
    private final String message;
    private final Throwable error;

    public AiPackageInstallResult(
            boolean success,
            AiPackageInfo packageInfo,
            File installedDirectory,
            PackageVerificationResult verificationResult,
            String message,
            Throwable error
    ) {
        this.success =
                success;
        this.packageInfo =
                packageInfo;
        this.installedDirectory =
                installedDirectory;
        this.verificationResult =
                verificationResult;
        this.message =
                message;
        this.error =
                error;
    }

    public boolean isSuccess() {
        return success;
    }

    public AiPackageInfo getPackageInfo() {
        return packageInfo;
    }

    public File getInstalledDirectory() {
        return installedDirectory;
    }

    public PackageVerificationResult getVerificationResult() {
        return verificationResult;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getError() {
        return error;
    }
}
