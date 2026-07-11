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
        android.util.Log.d("MULTILINGUAL_PIPELINE", "TranslationEngine input: " + text);
        String translated = getTranslator(
                text
        ).translate(
                text
        );
        android.util.Log.d("MULTILINGUAL_PIPELINE", "TranslationEngine output: " + translated);
        return translated;
    }

    private synchronized RomanceTranslator getTranslator(
            String text
    ) {

        try {
            TranslationModelId modelId =
                    LanguageDetectionEngine
                            .getInstance(context)
                            .detect(text);

            TranslationPackage translationPackage =
                    TranslationModelManager
                            .getInstance(context)
                            .getOrInstall(
                                    modelId
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
