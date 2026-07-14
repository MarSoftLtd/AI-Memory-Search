package com.bliss.aimemorysearch.ai;

import com.bliss.aimemorysearch.ai.model.AIPackageInfo;

import java.util.Objects;

public final class AiPackageDownloadLocator {
    public String locate(AIPackageInfo packageInfo) {
        Objects.requireNonNull(packageInfo, "packageInfo");
        return locate(packageInfo.getPackageId(), packageInfo.getVersion());
    }

    public String locate(String packageId, String version) {
        String fileName = requireFileNamePart(packageId, "packageId")
                + "-"
                + requireFileNamePart(version, "version")
                + ".aipackage";

        return AiPackageRepositoryConfig.BASE_DOWNLOAD_URL
                + "/"
                + AiPackageRepositoryConfig.REPOSITORY_OWNER
                + "/"
                + AiPackageRepositoryConfig.REPOSITORY_NAME
                + "/releases/download/"
                + AiPackageRepositoryConfig.RELEASE_TAG
                + "/"
                + fileName;
    }

    private static String requireFileNamePart(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        if (normalized.contains("/") || normalized.contains("\\")) {
            throw new IllegalArgumentException(name + " must not contain path separators");
        }
        return normalized;
    }
}
