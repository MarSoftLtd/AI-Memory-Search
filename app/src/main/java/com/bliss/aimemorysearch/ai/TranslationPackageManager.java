package com.bliss.aimemorysearch.ai;

import android.content.Context;

import java.io.File;
import java.util.HashSet;
import java.util.List;

public final class TranslationPackageManager {

    private static volatile TranslationPackageManager instance;

    private final ModelStorageManager storageManager;
    private final ModelInstallationManager installationManager;
    private final TranslationPackageManifestReader manifestReader;
    private final TranslationPackageValidator packageValidator;
    private final AssetsTranslationPackageRepository metadataRepository;
    private final AiPackageLifecycleManager lifecycleManager;

    private TranslationPackageManager(
            Context context
    ) {
        Context applicationContext =
                context.getApplicationContext();

        storageManager =
                ModelStorageManager.getInstance(
                        applicationContext
                );
        installationManager =
                ModelInstallationManager.getInstance(
                        applicationContext
                );
        manifestReader =
                new TranslationPackageManifestReader();
        packageValidator =
                new TranslationPackageValidator();
        metadataRepository =
                new AssetsTranslationPackageRepository(applicationContext);
        lifecycleManager = AiPlatform.createPackageLifecycleManager(
                applicationContext,
                null
        );
    }

    public static synchronized TranslationPackageManager getInstance(
            Context context
    ) {
        if (instance == null) {
            instance =
                    new TranslationPackageManager(
                            context.getApplicationContext()
                    );
        }

        return instance;
    }

    public TranslationPackage getPackage(
            TranslationModelId modelId
    ) {
        File directory =
                getPackageDirectory(modelId);

        TranslationPackageLayout layout =
                new TranslationPackageLayout(
                        directory
                );

        boolean installed =
                packageValidator.isValid(
                        layout
                );

        TranslationPackageManifest manifest =
                readManifest(
                        layout
                );

        return new TranslationPackage(
                modelId,
                layout.getRootDirectory(),
                manifest,
                installed
        );
    }

    public com.bliss.aimemorysearch.ai.model.AIPackageInfo getMetadata(
            String packageId
    ) {
        return metadataRepository.findByPackageId(packageId);
    }

    public java.util.List<com.bliss.aimemorysearch.ai.model.AIPackageInfo>
    getAvailablePackageMetadata() {
        return metadataRepository.getAIPackages();
    }

    public com.bliss.aimemorysearch.ai.model.AIPackageInfo getMetadata(
            TranslationModelId modelId
    ) {
        TranslationModelInfo modelInfo = TranslationModelRegistry.getModel(modelId);
        HashSet<String> requiredLanguages =
                new HashSet<>(modelInfo.getSupportedLanguages());
        for (com.bliss.aimemorysearch.ai.model.AIPackageInfo packageInfo
                : metadataRepository.getAIPackages()) {
            if (
                    packageInfo.getPackageType() == AiPackageType.TRANSLATION
                            &&
                            requiredLanguages.equals(
                                    new HashSet<>(packageInfo.getSupportedLanguages())
                            )
            ) {
                return packageInfo;
            }
        }
        return null;
    }

    public boolean isInstalled(String packageId) {
        com.bliss.aimemorysearch.ai.model.AIPackageInfo metadata =
                metadataRepository.findByPackageId(packageId);
        if (metadata == null || metadata.getPackageType() != AiPackageType.TRANSLATION) {
            return false;
        }
        HashSet<String> packageLanguages =
                new HashSet<>(metadata.getSupportedLanguages());
        for (TranslationModelId modelId : TranslationModelId.values()) {
            TranslationModelInfo modelInfo = TranslationModelRegistry.getModel(modelId);
            if (
                    packageLanguages.equals(
                            new HashSet<>(modelInfo.getSupportedLanguages())
                    )
            ) {
                return getPackage(modelId).isInstalled();
            }
        }
        return false;
    }

    public AiPackageLifecycleResult activateInstalledPackage(
            com.bliss.aimemorysearch.ai.model.AIPackageInfo metadata,
            File installedDirectory
    ) {
        if (metadata == null
                || metadata.getPackageType() != AiPackageType.TRANSLATION
                || installedDirectory == null) {
            return activationFailure("Installed package metadata is invalid", null);
        }

        TranslationPackageLayout layout = new TranslationPackageLayout(installedDirectory);
        if (!packageValidator.isValid(layout)) {
            return activationFailure("Installed package contents are invalid", null);
        }

        try {
            TranslationPackageManifest manifest = manifestReader.read(layout);
            if (!metadata.getPackageId().equals(manifest.getPackageId())
                    || !metadata.getVersion().equals(manifest.getVersion())) {
                return activationFailure("Installed package manifest does not match metadata", null);
            }

            TranslationModelId modelId = findModelId(metadata);
            if (modelId == null) {
                return activationFailure("No translation runtime matches the package", null);
            }

            AiPackageInfo runtimePackageInfo = new AiPackageInfo(
                    manifest.getPackageId(),
                    AiPackageType.TRANSLATION,
                    manifest.getFamily(),
                    manifest.getDisplayName(),
                    "",
                    manifest.getVersion(),
                    0L,
                    0L,
                    "",
                    metadata.getSha256(),
                    manifest.getMinimumAppVersion()
            );
            TranslationPackage translationPackage = new TranslationPackage(
                    modelId,
                    installedDirectory,
                    manifest,
                    true
            );
            return lifecycleManager.activateInstalledTranslationPackage(
                    modelId,
                    translationPackage,
                    runtimePackageInfo
            );
        } catch (Exception failure) {
            return activationFailure("Installed package activation failed", failure);
        }
    }

    private static TranslationModelId findModelId(
            com.bliss.aimemorysearch.ai.model.AIPackageInfo metadata
    ) {
        HashSet<String> packageLanguages = new HashSet<>(metadata.getSupportedLanguages());
        for (TranslationModelId modelId : TranslationModelId.values()) {
            if (packageLanguages.equals(new HashSet<>(
                    TranslationModelRegistry.getModel(modelId).getSupportedLanguages()
            ))) {
                return modelId;
            }
        }
        return null;
    }

    private static AiPackageLifecycleResult activationFailure(
            String message,
            Throwable error
    ) {
        return AiPackageLifecycleResult.failure(
                AiPackageLifecycleState.FAILED,
                AiCapability.TRANSLATION,
                null,
                "ACTIVATION_FAILED",
                message,
                error
        );
    }

    private File getPackageDirectory(
            TranslationModelId modelId
    ) {
        return storageManager.getModelDirectory(modelId);
    }

    private TranslationPackageManifest readManifest(
            TranslationPackageLayout layout
    ) {
        try {
            return manifestReader.read(
                    layout
            );
        } catch (Exception e) {
            return null;
        }
    }
}
