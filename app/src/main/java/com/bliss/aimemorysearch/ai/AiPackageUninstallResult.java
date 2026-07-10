package com.bliss.aimemorysearch.ai;

public final class AiPackageUninstallResult {

    private final boolean success;
    private final AiPackageInfo packageInfo;
    private final String packageId;
    private final String message;
    private final Throwable error;

    public AiPackageUninstallResult(
            boolean success,
            AiPackageInfo packageInfo,
            String packageId,
            String message,
            Throwable error
    ) {
        this.success =
                success;
        this.packageInfo =
                packageInfo;
        this.packageId =
                packageId;
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

    public String getPackageId() {
        return packageId;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getError() {
        return error;
    }
}
