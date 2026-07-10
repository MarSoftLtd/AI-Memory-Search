package com.bliss.aimemorysearch.ai;

import android.content.Context;

import java.util.Collections;
import java.util.List;

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

    public static AiStorageManager createStorageManager(
            Context context
    ) {
        return new AiStorageManager(
                context.getApplicationContext()
        );
    }

    public static AiPackageDownloader createPackageDownloader(
            Context context
    ) {
        return new AiPackageDownloader(
                context.getApplicationContext()
        );
    }

    public static AiPackageInstaller createPackageInstaller(
            Context context
    ) {
        return new AiPackageInstaller(
                createStorageManager(
                        context.getApplicationContext()
                ),
                getPackageManager(),
                new PackageVerification()
        );
    }

    public static AiCapabilityResolver createCapabilityResolver(
            AiPackageRepository packageRepository
    ) {
        return new AiCapabilityResolver(
                new AiCapabilityRegistry(),
                packageRepository == null
                        ? new EmptyAiPackageRepository()
                        : packageRepository,
                getPackageManager()
        );
    }

    public static AiCapabilityManager createCapabilityManager(
            AiPackageRepository packageRepository
    ) {
        return new AiCapabilityManager(
                createCapabilityResolver(
                        packageRepository
                )
        );
    }

    public static AiPackageLifecycleManager createPackageLifecycleManager(
            Context context,
            AiPackageRepository packageRepository
    ) {
        AiPackageRepository safeRepository =
                packageRepository == null
                        ? new EmptyAiPackageRepository()
                        : packageRepository;

        AiCapabilityResolver resolver =
                createCapabilityResolver(
                        safeRepository
                );

        return new AiPackageLifecycleManager(
                new AiCapabilityManager(
                        resolver
                ),
                resolver,
                getPackageManager(),
                createPackageInstaller(
                        context.getApplicationContext()
                ),
                createPackageDownloader(
                        context.getApplicationContext()
                ),
                getRuntimeManager(),
                safeRepository
        );
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

    private static final class EmptyAiPackageRepository
            implements AiPackageRepository {

        @Override
        public List<AiPackageInfo> getPackages() {
            return Collections.emptyList();
        }
    }
}
