package com.bliss.aimemorysearch.ai;

public final class AiRuntimeState {

    private final AiCapability capability;
    private final AiPackageInfo packageInfo;
    private final long loadedAtMillis;

    public AiRuntimeState(
            AiCapability capability,
            AiPackageInfo packageInfo,
            long loadedAtMillis
    ) {
        this.capability =
                capability;
        this.packageInfo =
                packageInfo;
        this.loadedAtMillis =
                loadedAtMillis;
    }

    public AiCapability getCapability() {
        return capability;
    }

    public AiPackageInfo getPackageInfo() {
        return packageInfo;
    }

    public long getLoadedAtMillis() {
        return loadedAtMillis;
    }
}
