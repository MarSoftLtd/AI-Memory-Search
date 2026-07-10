package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AiPackageManager {

    private final Map<String, AiPackageInfo> installedPackages;
    private final Map<AiCapability, String> activePackageIds;

    public AiPackageManager() {
        installedPackages =
                new LinkedHashMap<>();
        activePackageIds =
                new EnumMap<>(
                        AiCapability.class
                );
    }

    public synchronized List<AiPackageInfo> getInstalledPackages() {
        return Collections.unmodifiableList(
                new ArrayList<>(
                        installedPackages.values()
                )
        );
    }

    public synchronized AiPackageInfo getInstalledPackage(
            String packageId
    ) {
        if (isBlank(packageId)) {
            return null;
        }

        return installedPackages.get(
                packageId
        );
    }

    public synchronized boolean isInstalled(
            AiPackageInfo packageInfo
    ) {
        if (packageInfo == null) {
            return false;
        }

        return isInstalled(
                packageInfo.getPackageId()
        );
    }

    public synchronized boolean isInstalled(
            String packageId
    ) {
        if (isBlank(packageId)) {
            return false;
        }

        return installedPackages.containsKey(
                packageId
        );
    }

    public synchronized void registerInstalledPackage(
            AiPackageInfo packageInfo
    ) {
        if (
                packageInfo == null
                        ||
                        isBlank(
                                packageInfo.getPackageId()
                        )
        ) {
            return;
        }

        installedPackages.put(
                packageInfo.getPackageId(),
                packageInfo
        );
    }

    public synchronized void unregisterInstalledPackage(
            String packageId
    ) {
        if (isBlank(packageId)) {
            return;
        }

        installedPackages.remove(
                packageId
        );

        removeActiveReferences(
                packageId
        );
    }

    public synchronized void unregisterInstalledPackage(
            AiPackageInfo packageInfo
    ) {
        if (packageInfo == null) {
            return;
        }

        unregisterInstalledPackage(
                packageInfo.getPackageId()
        );
    }

    public synchronized void activatePackage(
            AiCapability capability,
            AiPackageInfo packageInfo
    ) {
        if (packageInfo == null) {
            return;
        }

        activatePackage(
                capability,
                packageInfo.getPackageId()
        );
    }

    public synchronized void activatePackage(
            AiCapability capability,
            String packageId
    ) {
        if (
                capability == null
                        ||
                        !isInstalled(
                                packageId
                        )
        ) {
            return;
        }

        activePackageIds.put(
                capability,
                packageId
        );
    }

    public synchronized void deactivatePackage(
            AiCapability capability
    ) {
        if (capability == null) {
            return;
        }

        activePackageIds.remove(
                capability
        );
    }

    public synchronized AiPackageInfo getActivePackage(
            AiCapability capability
    ) {
        if (capability == null) {
            return null;
        }

        String packageId =
                activePackageIds.get(
                        capability
                );

        return getInstalledPackage(
                packageId
        );
    }

    private void removeActiveReferences(
            String packageId
    ) {
        List<AiCapability> capabilitiesToDeactivate =
                new ArrayList<>();

        for (
                Map.Entry<AiCapability, String> entry
                : activePackageIds.entrySet()
        ) {
            if (
                    packageId.equals(
                            entry.getValue()
                    )
            ) {
                capabilitiesToDeactivate.add(
                        entry.getKey()
                );
            }
        }

        for (AiCapability capability : capabilitiesToDeactivate) {
            activePackageIds.remove(
                    capability
            );
        }
    }

    private static boolean isBlank(
            String value
    ) {
        return value == null
                ||
                value.trim().isEmpty();
    }
}
