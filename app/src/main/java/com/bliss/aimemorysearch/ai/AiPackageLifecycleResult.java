package com.bliss.aimemorysearch.ai;

public final class AiPackageLifecycleResult {

    private final boolean success;
    private final AiPackageLifecycleState state;
    private final AiCapability capability;
    private final String packageId;
    private final AiPackageInfo packageInfo;
    private final String reason;
    private final String message;
    private final Throwable error;

    public AiPackageLifecycleResult(
            boolean success,
            AiPackageLifecycleState state,
            AiCapability capability,
            String packageId,
            AiPackageInfo packageInfo,
            String reason,
            String message,
            Throwable error
    ) {
        this.success =
                success;
        this.state =
                state;
        this.capability =
                capability;
        this.packageId =
                packageId;
        this.packageInfo =
                packageInfo;
        this.reason =
                reason;
        this.message =
                message;
        this.error =
                error;
    }

    public boolean isSuccess() {
        return success;
    }

    public AiPackageLifecycleState getState() {
        return state;
    }

    public AiCapability getCapability() {
        return capability;
    }

    public String getPackageId() {
        return packageId;
    }

    public AiPackageInfo getPackageInfo() {
        return packageInfo;
    }

    public String getReason() {
        return reason;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getError() {
        return error;
    }

    public static AiPackageLifecycleResult success(
            AiPackageLifecycleState state,
            AiCapability capability,
            AiPackageInfo packageInfo,
            String message
    ) {
        return new AiPackageLifecycleResult(
                true,
                state,
                capability,
                packageInfo == null
                        ? ""
                        : packageInfo.getPackageId(),
                packageInfo,
                "",
                message,
                null
        );
    }

    public static AiPackageLifecycleResult failure(
            AiPackageLifecycleState state,
            AiCapability capability,
            AiPackageInfo packageInfo,
            String reason,
            String message,
            Throwable error
    ) {
        return new AiPackageLifecycleResult(
                false,
                state,
                capability,
                packageInfo == null
                        ? ""
                        : packageInfo.getPackageId(),
                packageInfo,
                reason,
                message,
                error
        );
    }
}
