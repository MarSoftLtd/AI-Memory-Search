package com.bliss.aimemorysearch.ai.model;

import com.bliss.aimemorysearch.ai.AiPackageLifecycleState;
import com.bliss.aimemorysearch.ai.AiPackageType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AIPackageInfo {
    private final String packageId;
    private final String displayNameKey;
    private final String descriptionKey;
    private final List<String> supportedLanguages;
    private final String version;
    private final String modelVersion;
    private final long downloadSizeBytes;
    private final long installedSizeBytes;
    private final AiPackageType packageType;
    private final AiPackageLifecycleState state;
    private final String sha256;

    public AIPackageInfo(
            String packageId,
            String displayNameKey,
            String descriptionKey,
            List<String> supportedLanguages,
            String version,
            String modelVersion,
            long downloadSizeBytes,
            long installedSizeBytes,
            AiPackageType packageType,
            AiPackageLifecycleState state,
            String sha256
    ) {
        this.packageId = packageId;
        this.displayNameKey = displayNameKey;
        this.descriptionKey = descriptionKey;
        this.supportedLanguages = supportedLanguages == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(supportedLanguages));
        this.version = version;
        this.modelVersion = modelVersion;
        this.downloadSizeBytes = downloadSizeBytes;
        this.installedSizeBytes = installedSizeBytes;
        this.packageType = packageType;
        this.state = state;
        this.sha256 = sha256;
    }

    public String getPackageId() { return packageId; }
    public String getDisplayNameKey() { return displayNameKey; }
    public String getDescriptionKey() { return descriptionKey; }
    public List<String> getSupportedLanguages() { return supportedLanguages; }
    public String getVersion() { return version; }
    public String getModelVersion() { return modelVersion; }
    public long getDownloadSizeBytes() { return downloadSizeBytes; }
    public long getInstalledSizeBytes() { return installedSizeBytes; }
    public AiPackageType getPackageType() { return packageType; }
    public AiPackageLifecycleState getState() { return state; }
    public String getSha256() { return sha256; }
}
