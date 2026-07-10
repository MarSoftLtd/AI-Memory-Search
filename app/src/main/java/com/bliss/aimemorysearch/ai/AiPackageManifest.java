package com.bliss.aimemorysearch.ai;

public final class AiPackageManifest {

    private final String packageId;
    private final AiPackageType packageType;
    private final String displayName;
    private final String version;
    private final String minimumAppVersion;

    public AiPackageManifest(
            String packageId,
            AiPackageType packageType,
            String displayName,
            String version,
            String minimumAppVersion
    ) {
        this.packageId =
                packageId;
        this.packageType =
                packageType;
        this.displayName =
                displayName;
        this.version =
                version;
        this.minimumAppVersion =
                minimumAppVersion;
    }

    public String getPackageId() {
        return packageId;
    }

    public AiPackageType getPackageType() {
        return packageType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getVersion() {
        return version;
    }

    public String getMinimumAppVersion() {
        return minimumAppVersion;
    }
}
