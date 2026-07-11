package com.bliss.aimemorysearch.ai;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class BundledAiPackageCatalog {

    public static final String ROMANCE_TRANSLATION_PACKAGE_ID =
            "core-translation-romance-en";

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

        AiPackageInfo romanceTranslation =
                new AiPackageInfo(
                        ROMANCE_TRANSLATION_PACKAGE_ID,
                        AiPackageType.TRANSLATION,
                        "romance",
                        "Core Romance to English Translation",
                        "Bundled offline Romance-language translation runtime",
                        "1.0.0",
                        0L,
                        0L,
                        "",
                        "",
                        "1.0"
                );

        packages.put(
                AiCapability.TRANSLATION,
                new BundledAiPackage(
                        AiCapability.TRANSLATION,
                        romanceTranslation,
                        "models/translator/romance-en"
                )
        );

        return Collections.unmodifiableMap(packages);
    }
}
