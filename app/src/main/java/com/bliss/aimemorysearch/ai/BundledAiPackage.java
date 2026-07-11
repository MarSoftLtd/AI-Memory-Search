package com.bliss.aimemorysearch.ai;

public final class BundledAiPackage {

    private final AiCapability capability;
    private final AiPackageInfo packageInfo;
    private final String assetDirectory;

    public BundledAiPackage(
            AiCapability capability,
            AiPackageInfo packageInfo,
            String assetDirectory
    ) {
        this.capability = capability;
        this.packageInfo = packageInfo;
        this.assetDirectory = assetDirectory;
    }

    public AiCapability getCapability() {
        return capability;
    }

    public AiPackageInfo getPackageInfo() {
        return packageInfo;
    }

    public String getAssetDirectory() {
        return assetDirectory;
    }
}
