package com.bliss.aimemorysearch.ai;

import android.content.Context;

import java.io.File;
import java.util.Locale;

public final class LanguagePackageRouter {

    private final AiPackageManager packageManager;
    private final AiCapabilityResolver capabilityResolver;
    private final AiStorageManager storageManager;
    private final TranslationPackageManifestReader manifestReader;
    private final TranslationPackageValidator packageValidator;
    private final LanguagePackageBootstrapper bootstrapper;
    private final AiPackageLifecycleManager lifecycleManager;
    private final TranslationPackageManager translationPackageManager;

    public LanguagePackageRouter(
            Context context
    ) {
        this(
                context,
                null
        );
    }

    public LanguagePackageRouter(
            Context context,
            AiPackageRepository packageRepository
    ) {
        Context applicationContext = context.getApplicationContext();
        packageManager = AiPlatform.getPackageManager();
        capabilityResolver =
                AiPlatform.createCapabilityResolver(packageRepository);
        storageManager =
                new AiStorageManager(applicationContext);
        manifestReader = new TranslationPackageManifestReader();
        packageValidator = new TranslationPackageValidator();
        bootstrapper =
                new LanguagePackageBootstrapper(
                        applicationContext,
                        packageManager
                );
        lifecycleManager =
                AiPlatform.createPackageLifecycleManager(
                        applicationContext,
                        packageRepository
                );
        translationPackageManager =
                TranslationPackageManager.getInstance(applicationContext);
    }

    public int restoreInstalledPackages() {
        return bootstrapper.restoreInstalledPackages();
    }

    public String getRecommendedFamily(
            Locale locale
    ) {
        if (locale == null) {
            return "";
        }

        TranslationModelInfo modelInfo =
                TranslationModelRegistry.getModelForLanguage(
                        locale.getLanguage()
                );

        return modelInfo == null
                ? ""
                : modelInfo.getTranslationFamily();
    }

    public CapabilityRequirement resolve(
            String family
    ) {
        return capabilityResolver.resolve(
                AiCapability.TRANSLATION,
                family
        );
    }

    public AiPackageInfo getInstalledPackage(
            String family
    ) {
        if (family == null || family.trim().isEmpty()) {
            return null;
        }

        return packageManager.findInstalledPackage(
                AiPackageType.TRANSLATION,
                family.trim().toLowerCase(Locale.ROOT)
        );
    }

    public boolean isInstalled(
            String family
    ) {
        return getInstalledPackage(family) != null;
    }

    public String getRequiredPackageId(String family) {
        TranslationModelInfo modelInfo =
                TranslationModelRegistry.getModelByFamily(family);
        if (modelInfo == null) {
            return null;
        }
        com.bliss.aimemorysearch.ai.model.AIPackageInfo metadata =
                translationPackageManager.getMetadata(modelInfo.getId());
        return metadata == null ? null : metadata.getPackageId();
    }

    public boolean activateInstalledPackage(
            String family
    ) {
        AiPackageInfo packageInfo =
                getInstalledPackage(family);

        if (packageInfo == null || !validate(packageInfo)) {
            return false;
        }

        packageManager.activatePackage(
                AiCapability.TRANSLATION,
                packageInfo
        );
        return true;
    }

    public boolean registerInstalledPackage(
            AiPackageInfo packageInfo
    ) {
        if (
                packageInfo == null
                        ||
                        packageInfo.getPackageType() != AiPackageType.TRANSLATION
                        ||
                        !validate(packageInfo)
        ) {
            return false;
        }

        packageManager.registerInstalledPackage(packageInfo);
        packageManager.activatePackage(
                AiCapability.TRANSLATION,
                packageInfo
        );
        return true;
    }

    public AiPackageLifecycleResult installSelectedPackage(
            String family
    ) {
        CapabilityRequirement requirement = resolve(family);
        AiPackageInfo selectedPackage =
                requirement.getSelectedPackage();

        if (selectedPackage == null) {
            return AiPackageLifecycleResult.failure(
                    AiPackageLifecycleState.NOT_INSTALLED,
                    AiCapability.TRANSLATION,
                    null,
                    "NO_PACKAGE_METADATA",
                    "No package metadata is available for family: " + family,
                    null
            );
        }

        return lifecycleManager.installCapability(
                AiCapability.TRANSLATION,
                selectedPackage
        );
    }

    private boolean validate(
            AiPackageInfo packageInfo
    ) {
        File packageDirectory =
                storageManager.getInstalledPackageDirectory(packageInfo);
        TranslationPackageLayout layout =
                new TranslationPackageLayout(packageDirectory);

        if (!packageValidator.isValid(layout)) {
            return false;
        }

        try {
            TranslationPackageManifest manifest =
                    manifestReader.read(layout);
            return packageInfo.getPackageId().equals(manifest.getPackageId())
                    &&
                    packageInfo.getCapabilityKey().equals(manifest.getFamily());
        } catch (Exception e) {
            return false;
        }
    }
}
