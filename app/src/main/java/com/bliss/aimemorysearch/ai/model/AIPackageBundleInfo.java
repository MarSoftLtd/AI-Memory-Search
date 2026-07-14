package com.bliss.aimemorysearch.ai.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AIPackageBundleInfo {
    private final String bundleId;
    private final String displayNameKey;
    private final String descriptionKey;
    private final List<String> packageIds;

    public AIPackageBundleInfo(String bundleId, String displayNameKey,
            String descriptionKey, List<String> packageIds) {
        this.bundleId = bundleId;
        this.displayNameKey = displayNameKey;
        this.descriptionKey = descriptionKey;
        this.packageIds = Collections.unmodifiableList(new ArrayList<>(packageIds));
    }

    public String getBundleId() { return bundleId; }
    public String getDisplayNameKey() { return displayNameKey; }
    public String getDescriptionKey() { return descriptionKey; }
    public List<String> getPackageIds() { return packageIds; }
}
