package com.bliss.aimemorysearch.ai;

public final class AiPlatform {

    private static final AiRuntimeManager RUNTIME_MANAGER =
            new AiRuntimeManager();
    private static final AiPackageManager PACKAGE_MANAGER =
            createPackageManager();

    private AiPlatform() {
    }

    public static AiRuntimeManager getRuntimeManager() {
        return RUNTIME_MANAGER;
    }

    public static AiPackageManager getPackageManager() {
        return PACKAGE_MANAGER;
    }

    private static AiPackageManager createPackageManager() {
        AiPackageManager packageManager =
                new AiPackageManager();

        registerCorePackage(
                packageManager,
                AiCapability.DOCUMENT_SEARCH,
                new AiPackageInfo(
                        "core-document-runtime",
                        AiPackageType.DOCUMENT,
                        "document-search",
                        "Core Document Runtime",
                        "",
                        "",
                        0L,
                        0L,
                        "",
                        "",
                        ""
                )
        );
        registerCorePackage(
                packageManager,
                AiCapability.IMAGE_SEARCH,
                new AiPackageInfo(
                        "core-image-runtime",
                        AiPackageType.IMAGE,
                        "image-search",
                        "Core Image Runtime",
                        "",
                        "",
                        0L,
                        0L,
                        "",
                        "",
                        ""
                )
        );
        registerCorePackage(
                packageManager,
                AiCapability.OCR,
                new AiPackageInfo(
                        "core-ocr-runtime",
                        AiPackageType.OCR,
                        "ocr",
                        "Core OCR Runtime",
                        "",
                        "",
                        0L,
                        0L,
                        "",
                        "",
                        ""
                )
        );

        return packageManager;
    }

    private static void registerCorePackage(
            AiPackageManager packageManager,
            AiCapability capability,
            AiPackageInfo packageInfo
    ) {
        packageManager.registerInstalledPackage(
                packageInfo
        );
        packageManager.activatePackage(
                capability,
                packageInfo
        );
    }
}
