package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

public final class AiPackageManager {

    private final Map<String, AiPackageInfo> installedPackages;
    private final Map<AiCapability, String> activePackageIds;
    private final Set<String> activeModelPackageIds;

    public AiPackageManager() {
        installedPackages =
                new LinkedHashMap<>();
        activePackageIds =
                new EnumMap<>(
                        AiCapability.class
                );
        activeModelPackageIds = new LinkedHashSet<>();
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

    public synchronized AiPackageInfo findInstalledPackage(
            AiPackageType packageType,
            String capabilityKey
    ) {
        if (packageType == null || isBlank(capabilityKey)) {
            return null;
        }

        for (AiPackageInfo packageInfo : installedPackages.values()) {
            if (
                    packageType == packageInfo.getPackageType()
                            &&
                            capabilityKey.equals(
                                    packageInfo.getCapabilityKey()
                            )
            ) {
                return packageInfo;
            }
        }

        return null;
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
        activeModelPackageIds.remove(packageId);
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

    public synchronized void activateModelPackage(AiPackageInfo packageInfo) {
        if (packageInfo == null
                || packageInfo.getPackageType() != AiPackageType.MODEL
                || !isInstalled(packageInfo)) {
            return;
        }
        activeModelPackageIds.add(packageInfo.getPackageId());
    }

    public synchronized void deactivateModelPackage(String packageId) {
        if (!isBlank(packageId)) {
            activeModelPackageIds.remove(packageId);
        }
    }

    public synchronized boolean isModelPackageActive(String packageId) {
        return !isBlank(packageId) && activeModelPackageIds.contains(packageId);
    }

    public synchronized List<AiPackageInfo> getActiveModelPackages() {
        List<AiPackageInfo> result = new ArrayList<>();
        for (String packageId : activeModelPackageIds) {
            AiPackageInfo packageInfo = installedPackages.get(packageId);
            if (packageInfo != null) {
                result.add(packageInfo);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public synchronized void deactivateAllModelPackages() {
        activeModelPackageIds.clear();
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
