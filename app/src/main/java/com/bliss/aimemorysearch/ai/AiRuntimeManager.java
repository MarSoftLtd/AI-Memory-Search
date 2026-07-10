package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class AiRuntimeManager {

    private final Map<AiCapability, AiRuntimeState> loadedRuntimes;

    public AiRuntimeManager() {
        loadedRuntimes =
                new EnumMap<>(
                        AiCapability.class
                );
    }

    public synchronized AiRuntimeState loadRuntime(
            AiCapability capability,
            AiPackageInfo packageInfo
    ) {
        if (
                capability == null
                        ||
                        packageInfo == null
        ) {
            return null;
        }

        AiRuntimeState runtimeState =
                new AiRuntimeState(
                        capability,
                        packageInfo,
                        System.currentTimeMillis()
                );

        loadedRuntimes.put(
                capability,
                runtimeState
        );

        return runtimeState;
    }

    public synchronized void unloadRuntime(
            AiCapability capability
    ) {
        if (capability == null) {
            return;
        }

        loadedRuntimes.remove(
                capability
        );
    }

    public synchronized void unloadAll() {
        loadedRuntimes.clear();
    }

    public synchronized boolean isLoaded(
            AiCapability capability
    ) {
        if (capability == null) {
            return false;
        }

        return loadedRuntimes.containsKey(
                capability
        );
    }

    public synchronized boolean isLoaded(
            AiCapability capability,
            AiPackageInfo packageInfo
    ) {
        if (
                capability == null
                        ||
                        packageInfo == null
        ) {
            return false;
        }

        AiRuntimeState runtimeState =
                loadedRuntimes.get(
                        capability
                );

        if (
                runtimeState == null
                        ||
                        runtimeState.getPackageInfo() == null
        ) {
            return false;
        }

        return samePackage(
                runtimeState.getPackageInfo(),
                packageInfo
        );
    }

    public synchronized AiRuntimeState getLoadedRuntime(
            AiCapability capability
    ) {
        if (capability == null) {
            return null;
        }

        return loadedRuntimes.get(
                capability
        );
    }

    public synchronized List<AiRuntimeState> getLoadedRuntimes() {
        return Collections.unmodifiableList(
                new ArrayList<>(
                        loadedRuntimes.values()
                )
        );
    }

    private static boolean samePackage(
            AiPackageInfo first,
            AiPackageInfo second
    ) {
        String firstPackageId =
                first.getPackageId();
        String secondPackageId =
                second.getPackageId();

        return firstPackageId != null
                &&
                firstPackageId.equals(
                        secondPackageId
                );
    }
}
