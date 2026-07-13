package com.bliss.aimemorysearch.ai;

import android.content.Context;

public final class TranslationEngine {

    private static volatile TranslationEngine instance;

    private final Context context;
    private final TranslationRuntimeLoader runtimeLoader;

    private TranslationEngine(
            Context context
    ) {
        this.context =
                context.getApplicationContext();
        runtimeLoader =
                new TranslationRuntimeLoader(
                        AiPlatform.getRuntimeManager(),
                        TranslatorSessionManager.getInstance()
                );
    }

    public static synchronized TranslationEngine getInstance(
            Context context
    ) {
        if (instance == null) {
            instance =
                    new TranslationEngine(
                            context.getApplicationContext()
                    );
        }

        return instance;
    }

    public String translate(
            String text
    ) {
        return translate(
                text,
                ""
        );
    }

    public String translate(
            String text,
            String selectedLanguageFamily
    ) {
        android.util.Log.d("MULTILINGUAL_PIPELINE", "TranslationEngine input: " + text);
        if (
                selectedLanguageFamily == null
                        ||
                        selectedLanguageFamily.trim().isEmpty()
        ) {
            return text;
        }

        TranslationModelInfo modelInfo =
                TranslationModelRegistry.getModelByFamily(
                        selectedLanguageFamily
                );

        if (modelInfo == null) {
            throw new IllegalArgumentException(
                    "Unknown language family: " + selectedLanguageFamily
            );
        }

        AiPackageInfo packageInfo =
                AiPlatform
                        .getPackageManager()
                        .findInstalledPackage(
                                AiPackageType.TRANSLATION,
                                modelInfo.getTranslationFamily()
                        );

        if (packageInfo == null) {
            throw new IllegalStateException(
                    "Required language package is not installed: "
                            + modelInfo.getTranslationFamily()
            );
        }

        String translated = getTranslator(
                modelInfo.getId(),
                packageInfo
        ).translate(
                text
        );
        android.util.Log.d("MULTILINGUAL_PIPELINE", "TranslationEngine output: " + translated);
        return translated;
    }

    private synchronized RomanceTranslator getTranslator(
            TranslationModelId modelId,
            AiPackageInfo packageInfo
    ) {

        try {
            java.io.File packageDirectory =
                    new AiStorageManager(context)
                            .getInstalledPackageDirectory(packageInfo);
            TranslationPackageLayout layout =
                    new TranslationPackageLayout(packageDirectory);
            TranslationPackageValidator validator =
                    new TranslationPackageValidator();

            if (!validator.isValid(layout)) {
                throw new IllegalStateException(
                        "Installed language package is invalid"
                );
            }

            TranslationPackageManifest manifest =
                    new TranslationPackageManifestReader()
                            .read(layout);
            TranslationPackage translationPackage =
                    new TranslationPackage(
                            modelId,
                            packageDirectory,
                            manifest,
                            true
                    );

            android.util.Log.d("MULTILINGUAL_PIPELINE", "Translation package directory: " + translationPackage.getDirectory()
                    + " | installed=" + translationPackage.isInstalled());

            return runtimeLoader.loadRuntime(
                    modelId,
                    translationPackage
            );
        } catch (Exception e) {
            android.util.Log.e("MULTILINGUAL_PIPELINE", "Translation runtime/package initialization failed", e);
            throw new IllegalStateException(
                    "Failed to initialize offline translation engine",
                    e
            );
        }
    }
}
