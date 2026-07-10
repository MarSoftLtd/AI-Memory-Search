package com.bliss.aimemorysearch.ai;

import android.content.Context;

import java.io.File;

public final class AiStorageManager {

    private static final String PACKAGE_ROOT_DIRECTORY =
            "ai_packages";

    private final Context context;

    public AiStorageManager(
            Context context
    ) {
        this.context =
                context.getApplicationContext();
    }

    public File getPackageRootDirectory() {
        return new File(
                context.getFilesDir(),
                PACKAGE_ROOT_DIRECTORY
        );
    }

    public File getInstalledPackageDirectory(
            AiPackageInfo packageInfo
    ) {
        if (packageInfo == null) {
            return getInstalledPackageDirectory(
                    ""
            );
        }

        return getInstalledPackageDirectory(
                packageInfo.getPackageId()
        );
    }

    public File getInstalledPackageDirectory(
            String packageId
    ) {
        return new File(
                getPackageRootDirectory(),
                sanitizeFileName(
                        packageId
                )
        );
    }

    private static String sanitizeFileName(
            String value
    ) {
        if (
                value == null
                        ||
                        value.trim().isEmpty()
        ) {
            return "unknown-package";
        }

        return value.replaceAll(
                "[^a-zA-Z0-9._-]",
                "_"
        );
    }
}
