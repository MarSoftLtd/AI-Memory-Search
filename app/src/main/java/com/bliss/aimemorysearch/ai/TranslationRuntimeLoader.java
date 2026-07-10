package com.bliss.aimemorysearch.ai;

public final class TranslationRuntimeLoader {

    private final AiRuntimeManager runtimeManager;
    private final TranslatorSessionManager sessionManager;

    public TranslationRuntimeLoader(
            AiRuntimeManager runtimeManager,
            TranslatorSessionManager sessionManager
    ) {
        this.runtimeManager =
                runtimeManager;
        this.sessionManager =
                sessionManager;
    }

    public synchronized RomanceTranslator loadRuntime(
            TranslationModelId modelId,
            TranslationPackage translationPackage
    ) {
        RomanceTranslator translator =
                sessionManager.getTranslator(
                        modelId,
                        translationPackage
                );

        runtimeManager.loadRuntime(
                AiCapability.TRANSLATION,
                createRuntimePackageInfo(
                        modelId,
                        translationPackage
                )
        );

        return translator;
    }

    public synchronized void unloadRuntime() {
        runtimeManager.unloadRuntime(
                AiCapability.TRANSLATION
        );
    }

    private static AiPackageInfo createRuntimePackageInfo(
            TranslationModelId modelId,
            TranslationPackage translationPackage
    ) {
        TranslationPackageManifest manifest =
                translationPackage.getManifest();

        TranslationModelInfo modelInfo =
                TranslationModelRegistry.getModel(
                        modelId
                );

        String packageId =
                manifest != null
                        ? manifest.getPackageId()
                        : modelInfo.getInternalDirectory();
        String displayName =
                manifest != null
                        ? manifest.getDisplayName()
                        : modelId.name();
        String version =
                manifest != null
                        ? manifest.getVersion()
                        : "";
        String minimumAppVersion =
                manifest != null
                        ? manifest.getMinimumAppVersion()
                        : "";

        return new AiPackageInfo(
                packageId,
                AiPackageType.TRANSLATION,
                modelInfo.getTranslationFamily(),
                displayName,
                "",
                version,
                0L,
                0L,
                "",
                "",
                minimumAppVersion
        );
    }
}
