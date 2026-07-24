package com.bliss.aimemorysearch.ai;

import java.io.File;
import java.util.Collections;
import java.util.List;

public final class AiPackageLifecycleManager {

    private final AiCapabilityManager capabilityManager;
    private final AiCapabilityResolver capabilityResolver;
    private final AiPackageManager packageManager;
    private final AiPackageInstaller packageInstaller;
    private final AiPackageDownloader packageDownloader;
    private final AiRuntimeManager runtimeManager;
    private final AiPackageRepository packageRepository;

    public AiPackageLifecycleManager(
            AiCapabilityManager capabilityManager,
            AiCapabilityResolver capabilityResolver,
            AiPackageManager packageManager,
            AiPackageInstaller packageInstaller,
            AiPackageDownloader packageDownloader,
            AiRuntimeManager runtimeManager,
            AiPackageRepository packageRepository
    ) {
        this.capabilityManager =
                capabilityManager;
        this.capabilityResolver =
                capabilityResolver;
        this.packageManager =
                packageManager;
        this.packageInstaller =
                packageInstaller;
        this.packageDownloader =
                packageDownloader;
        this.runtimeManager =
                runtimeManager;
        this.packageRepository =
                packageRepository;
    }

    public AiPackageLifecycleResult ensureCapabilityAvailable(
            AiCapability capability
    ) {
        CapabilityPlan plan =
                capabilityManager.evaluate(
                        Collections.singletonList(
                                capability
                        )
                );

        if (plan.canContinue()) {
            AiPackageInfo activePackage =
                    packageManager.getActivePackage(
                            capability
                    );

            if (activePackage != null) {
                loadRuntimeState(
                        capability,
                        activePackage
                );
            }

            return AiPackageLifecycleResult.success(
                    activePackage == null
                            ? AiPackageLifecycleState.INSTALLED
                            : AiPackageLifecycleState.ACTIVE,
                    capability,
                    activePackage,
                    "Capability is available"
            );
        }

        CapabilityRequirement requirement =
                capabilityResolver.resolve(
                        capability
                );

        AiPackageInfo selectedPackage =
                requirement.getSelectedPackage();

        if (selectedPackage == null) {
            return AiPackageLifecycleResult.failure(
                    AiPackageLifecycleState.NOT_INSTALLED,
                    capability,
                    null,
                    "NO_PACKAGE",
                    requirement.getReason(),
                    null
            );
        }

        return installCapability(
                capability,
                selectedPackage
        );
    }

    public AiPackageLifecycleResult installCapability(
            AiCapability capability
    ) {
        CapabilityRequirement requirement =
                capabilityResolver.resolve(
                        capability
                );

        AiPackageInfo selectedPackage =
                requirement.getSelectedPackage();

        if (selectedPackage == null) {
            return AiPackageLifecycleResult.failure(
                    AiPackageLifecycleState.NOT_INSTALLED,
                    capability,
                    null,
                    "NO_PACKAGE",
                    requirement.getReason(),
                    null
            );
        }

        return installCapability(
                capability,
                selectedPackage
        );
    }

    public AiPackageLifecycleResult installCapability(
            AiCapability capability,
            AiPackageInfo packageInfo
    ) {
        if (packageInfo == null) {
            return AiPackageLifecycleResult.failure(
                    AiPackageLifecycleState.NOT_INSTALLED,
                    capability,
                    null,
                    "NO_PACKAGE",
                    "Package metadata is missing",
                    null
            );
        }

        if (packageManager.isInstalled(packageInfo)) {
            packageManager.activatePackage(
                    capability,
                    packageInfo
            );
            loadRuntimeState(
                    capability,
                    packageInfo
            );

            return AiPackageLifecycleResult.success(
                    AiPackageLifecycleState.ACTIVE,
                    capability,
                    packageInfo,
                    "Package already installed and activated"
            );
        }

        AiPackageDownloadResult downloadResult =
                packageDownloader.downloadPackage(
                        packageInfo
                );

        if (!downloadResult.isSuccess()) {
            return AiPackageLifecycleResult.failure(
                    downloadResult.isCancelled()
                            ? AiPackageLifecycleState.INACTIVE
                            : AiPackageLifecycleState.FAILED,
                    capability,
                    packageInfo,
                    downloadResult.isCancelled()
                            ? "DOWNLOAD_CANCELLED"
                            : "DOWNLOAD_FAILED",
                    downloadResult.getMessage(),
                    downloadResult.getError()
            );
        }

        AiPackageInstallResult installResult =
                packageInstaller.installPackage(
                        packageInfo,
                        downloadResult.getDownloadedFile()
                );

        if (!installResult.isSuccess()) {
            return AiPackageLifecycleResult.failure(
                    AiPackageLifecycleState.FAILED,
                    capability,
                    packageInfo,
                    "INSTALL_FAILED",
                    installResult.getMessage(),
                    installResult.getError()
            );
        }

        packageManager.activatePackage(
                capability,
                packageInfo
        );
        loadRuntimeState(
                capability,
                packageInfo
        );

        return AiPackageLifecycleResult.success(
                AiPackageLifecycleState.ACTIVE,
                capability,
                packageInfo,
                "Capability installed and activated"
        );
    }

    public AiPackageLifecycleResult installCapabilityFromFile(
            AiCapability capability,
            AiPackageInfo packageInfo,
            File source
    ) {
        AiPackageInstallResult installResult =
                packageInstaller.installPackage(
                        packageInfo,
                        source
                );

        if (!installResult.isSuccess()) {
            return AiPackageLifecycleResult.failure(
                    AiPackageLifecycleState.FAILED,
                    capability,
                    packageInfo,
                    "INSTALL_FAILED",
                    installResult.getMessage(),
                    installResult.getError()
            );
        }

        packageManager.activatePackage(
                capability,
                packageInfo
        );
        loadRuntimeState(
                capability,
                packageInfo
        );

        return AiPackageLifecycleResult.success(
                AiPackageLifecycleState.ACTIVE,
                capability,
                packageInfo,
                "Capability installed and activated"
        );
    }

    public AiPackageLifecycleResult uninstallCapability(
            AiCapability capability
    ) {
        AiPackageInfo activePackage =
                packageManager.getActivePackage(
                        capability
                );

        if (activePackage == null) {
            return AiPackageLifecycleResult.failure(
                    AiPackageLifecycleState.NOT_INSTALLED,
                    capability,
                    null,
                    "NO_ACTIVE_PACKAGE",
                    "Capability has no active package",
                    null
            );
        }

        unloadRuntimeState(
                capability
        );
        packageManager.deactivatePackage(
                capability
        );

        AiPackageUninstallResult uninstallResult =
                packageInstaller.uninstallPackage(
                        activePackage
                );

        if (!uninstallResult.isSuccess()) {
            return AiPackageLifecycleResult.failure(
                    AiPackageLifecycleState.FAILED,
                    capability,
                    activePackage,
                    "UNINSTALL_FAILED",
                    uninstallResult.getMessage(),
                    uninstallResult.getError()
            );
        }

        return AiPackageLifecycleResult.success(
                AiPackageLifecycleState.NOT_INSTALLED,
                capability,
                activePackage,
                "Capability uninstalled"
        );
    }

    public AiPackageLifecycleResult activateCapability(
            AiCapability capability
    ) {
        CapabilityRequirement requirement =
                capabilityResolver.resolve(
                        capability
                );

        AiPackageInfo packageInfo =
                requirement.getSelectedPackage();

        if (packageInfo == null) {
            return AiPackageLifecycleResult.failure(
                    AiPackageLifecycleState.NOT_INSTALLED,
                    capability,
                    null,
                    "NO_PACKAGE",
                    requirement.getReason(),
                    null
            );
        }

        return activateCapability(
                capability,
                packageInfo
        );
    }

    public AiPackageLifecycleResult activateInstalledTranslationPackage(
            TranslationModelId modelId,
            TranslationPackage translationPackage,
            AiPackageInfo packageInfo
    ) {
        if (modelId == null
                || translationPackage == null
                || !translationPackage.isInstalled()
                || packageInfo == null
                || packageInfo.getPackageType() != AiPackageType.TRANSLATION) {
            return AiPackageLifecycleResult.failure(
                    AiPackageLifecycleState.FAILED,
                    AiCapability.TRANSLATION,
                    packageInfo,
                    "ACTIVATION_INVALID",
                    "Installed translation package is invalid",
                    null
            );
        }

        try {
            packageManager.registerInstalledPackage(packageInfo);
            packageManager.activatePackage(AiCapability.TRANSLATION, packageInfo);
            new TranslationRuntimeLoader(
                    runtimeManager,
                    TranslatorSessionManager.getInstance()
            ).loadRuntime(modelId, translationPackage);
            return AiPackageLifecycleResult.success(
                    AiPackageLifecycleState.ACTIVE,
                    AiCapability.TRANSLATION,
                    packageInfo,
                    "Translation package activated"
            );
        } catch (Exception failure) {
            runtimeManager.unloadRuntime(AiCapability.TRANSLATION);
            packageManager.unregisterInstalledPackage(packageInfo);
            return AiPackageLifecycleResult.failure(
                    AiPackageLifecycleState.FAILED,
                    AiCapability.TRANSLATION,
                    packageInfo,
                    "ACTIVATION_FAILED",
                    "Translation package activation failed",
                    failure
            );
        }
    }

    public AiPackageLifecycleResult activateInstalledModelPackage(
            com.bliss.aimemorysearch.ai.model.AIPackageInfo metadata,
            File installedDirectory
    ) {
        if (metadata == null
                || metadata.getPackageType() != AiPackageType.MODEL
                || installedDirectory == null
                || !installedDirectory.isDirectory()
                || !metadata.getPackageId().equals(installedDirectory.getName())) {
            return AiPackageLifecycleResult.failure(
                    AiPackageLifecycleState.FAILED,
                    null,
                    null,
                    "ACTIVATION_INVALID",
                    "Installed MODEL package is invalid",
                    null
            );
        }

        AiPackageInfo packageInfo = toRuntimePackageInfo(metadata);
        packageManager.registerInstalledPackage(packageInfo);
        packageManager.activateModelPackage(packageInfo);
        if (!packageManager.isModelPackageActive(packageInfo.getPackageId())) {
            packageManager.unregisterInstalledPackage(packageInfo);
            return AiPackageLifecycleResult.failure(
                    AiPackageLifecycleState.FAILED,
                    null,
                    packageInfo,
                    "ACTIVATION_FAILED",
                    "MODEL package activation failed",
                    null
            );
        }
        return AiPackageLifecycleResult.success(
                AiPackageLifecycleState.ACTIVE,
                null,
                packageInfo,
                "MODEL package activated"
        );
    }

    public AiPackageLifecycleResult deactivateModelPackage(String packageId) {
        AiPackageInfo packageInfo = packageManager.getInstalledPackage(packageId);
        if (packageInfo == null || packageInfo.getPackageType() != AiPackageType.MODEL) {
            return AiPackageLifecycleResult.failure(
                    AiPackageLifecycleState.NOT_INSTALLED,
                    null,
                    packageInfo,
                    "NOT_INSTALLED",
                    "MODEL package is not installed",
                    null
            );
        }
        packageManager.deactivateModelPackage(packageId);
        return AiPackageLifecycleResult.success(
                AiPackageLifecycleState.INACTIVE,
                null,
                packageInfo,
                "MODEL package deactivated"
        );
    }

    private static AiPackageInfo toRuntimePackageInfo(
            com.bliss.aimemorysearch.ai.model.AIPackageInfo metadata
    ) {
        return new AiPackageInfo(
                metadata.getPackageId(),
                AiPackageType.MODEL,
                metadata.getPackageId(),
                metadata.getDisplayNameKey(),
                metadata.getDescriptionKey(),
                metadata.getVersion(),
                metadata.getDownloadSizeBytes(),
                metadata.getInstalledSizeBytes(),
                "",
                metadata.getSha256(),
                ""
        );
    }

    public AiPackageLifecycleResult activateCapability(
            AiCapability capability,
            AiPackageInfo packageInfo
    ) {
        if (!packageManager.isInstalled(packageInfo)) {
            return AiPackageLifecycleResult.failure(
                    AiPackageLifecycleState.NOT_INSTALLED,
                    capability,
                    packageInfo,
                    "NOT_INSTALLED",
                    "Package is not installed",
                    null
            );
        }

        packageManager.activatePackage(
                capability,
                packageInfo
        );
        loadRuntimeState(
                capability,
                packageInfo
        );

        return AiPackageLifecycleResult.success(
                AiPackageLifecycleState.ACTIVE,
                capability,
                packageInfo,
                "Capability activated"
        );
    }

    public AiPackageLifecycleResult deactivateCapability(
            AiCapability capability
    ) {
        AiPackageInfo activePackage =
                packageManager.getActivePackage(
                        capability
                );

        unloadRuntimeState(
                capability
        );
        packageManager.deactivatePackage(
                capability
        );

        return AiPackageLifecycleResult.success(
                AiPackageLifecycleState.INACTIVE,
                capability,
                activePackage,
                "Capability deactivated"
        );
    }

    public AiPackageLifecycleResult updateCapability(
            AiCapability capability
    ) {
        AiPackageInfo updatePackage =
                findUpdatePackage(
                        capability
                );

        if (updatePackage == null) {
            return AiPackageLifecycleResult.failure(
                    AiPackageLifecycleState.INSTALLED,
                    capability,
                    packageManager.getActivePackage(
                            capability
                    ),
                    "NO_UPDATE",
                    "No update package is available",
                    null
            );
        }

        return installCapability(
                capability,
                updatePackage
        );
    }

    private AiPackageInfo findUpdatePackage(
            AiCapability capability
    ) {
        if (packageRepository == null) {
            return null;
        }

        List<AiPackageInfo> packages =
                packageRepository.getPackages();

        if (packages == null || packages.isEmpty()) {
            return null;
        }

        CapabilityRequirement requirement =
                capabilityResolver.resolve(
                        capability
                );

        for (AiPackageInfo packageInfo : packages) {
            if (
                    packageInfo != null
                            &&
                            requirement.getCandidatePackages()
                                    .contains(packageInfo)
            ) {
                return packageInfo;
            }
        }

        return null;
    }

    private void loadRuntimeState(
            AiCapability capability,
            AiPackageInfo packageInfo
    ) {
        runtimeManager.loadRuntime(
                capability,
                packageInfo
        );
    }

    private void unloadRuntimeState(
            AiCapability capability
    ) {
        runtimeManager.unloadRuntime(
                capability
        );
    }
}
