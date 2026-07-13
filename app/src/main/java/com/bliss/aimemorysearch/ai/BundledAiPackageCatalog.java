package com.bliss.aimemorysearch.ai;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class BundledAiPackageCatalog {

    private static final Map<AiCapability, BundledAiPackage> PACKAGES =
            createPackages();

    private BundledAiPackageCatalog() {
    }

    public static BundledAiPackage get(
            AiCapability capability
    ) {
        return PACKAGES.get(capability);
    }

    public static Map<AiCapability, BundledAiPackage> getPackages() {
        return PACKAGES;
    }

    private static Map<AiCapability, BundledAiPackage> createPackages() {
        EnumMap<AiCapability, BundledAiPackage> packages =
                new EnumMap<>(AiCapability.class);

        return Collections.unmodifiableMap(packages);
    }
}
