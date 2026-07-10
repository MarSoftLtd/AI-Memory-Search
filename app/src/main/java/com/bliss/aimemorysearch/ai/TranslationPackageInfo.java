package com.bliss.aimemorysearch.ai;

public final class TranslationPackageInfo {

    private final AiPackageInfo packageInfo;
    private final String translationFamily;

    public TranslationPackageInfo(
            String packageId,
            String translationFamily,
            String displayName,
            String description,
            String version,
            long sizeBytes,
            long requiredSpaceBytes,
            String downloadUrl,
            String checksumSha256,
            String minAppVersion
    ) {
        this.translationFamily =
                translationFamily;
        this.packageInfo =
                new AiPackageInfo(
                        packageId,
                        AiPackageType.TRANSLATION,
                        translationFamily,
                        displayName,
                        description,
                        version,
                        sizeBytes,
                        requiredSpaceBytes,
                        downloadUrl,
                        checksumSha256,
                        minAppVersion
                );
    }

    public AiPackageInfo getAiPackageInfo() {
        return packageInfo;
    }

    public String getPackageId() {
        return packageInfo.getPackageId();
    }

    public String getTranslationFamily() {
        return translationFamily;
    }

    public String getDisplayName() {
        return packageInfo.getDisplayName();
    }

    public String getDescription() {
        return packageInfo.getDescription();
    }

    public String getVersion() {
        return packageInfo.getVersion();
    }

    public long getSizeBytes() {
        return packageInfo.getSizeBytes();
    }

    public long getRequiredSpaceBytes() {
        return packageInfo.getRequiredSpaceBytes();
    }

    public String getDownloadUrl() {
        return packageInfo.getDownloadUrl();
    }

    public String getChecksumSha256() {
        return packageInfo.getChecksumSha256();
    }

    public String getMinAppVersion() {
        return packageInfo.getMinAppVersion();
    }
}
