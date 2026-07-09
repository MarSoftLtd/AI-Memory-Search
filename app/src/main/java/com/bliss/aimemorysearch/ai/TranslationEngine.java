package com.bliss.aimemorysearch.ai;

import android.content.Context;

public final class TranslationEngine {

    private static volatile TranslationEngine instance;

    private final Context context;
    private final TranslatorSessionManager sessionManager;

    private TranslationEngine(
            Context context
    ) {
        this.context =
                context.getApplicationContext();
        sessionManager =
                TranslatorSessionManager.getInstance();
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
        return getTranslator(
                text
        ).translate(
                text
        );
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

            return sessionManager.getTranslator(
                    modelId,
                    translationPackage
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to initialize offline translation engine",
                    e
            );
        }
    }
}
