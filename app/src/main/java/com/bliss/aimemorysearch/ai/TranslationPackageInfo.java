package com.bliss.aimemorysearch.ai;

public final class TranslationPackageInfo {

    private final String packageId;
    private final String displayName;
    private final String description;
    private final String version;
    private final long sizeBytes;
    private final long requiredSpaceBytes;
    private final String downloadUrl;
    private final String checksumSha256;
    private final String minAppVersion;

    public TranslationPackageInfo(
            String packageId,
            String displayName,
            String description,
            String version,
            long sizeBytes,
            long requiredSpaceBytes,
            String downloadUrl,
            String checksumSha256,
            String minAppVersion
    ) {
        this.packageId =
                packageId;
        this.displayName =
                displayName;
        this.description =
                description;
        this.version =
                version;
        this.sizeBytes =
                sizeBytes;
        this.requiredSpaceBytes =
                requiredSpaceBytes;
        this.downloadUrl =
                downloadUrl;
        this.checksumSha256 =
                checksumSha256;
        this.minAppVersion =
                minAppVersion;
    }

    public String getPackageId() {
        return packageId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public long getRequiredSpaceBytes() {
        return requiredSpaceBytes;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public String getMinAppVersion() {
        return minAppVersion;
    }
}
