package com.bliss.aimemorysearch.ai;

import android.content.Context;

public final class LanguageDetectionEngine {

    private static volatile LanguageDetectionEngine instance;

    private final Context context;

    private LanguageDetectionEngine(
            Context context
    ) {
        this.context =
                context.getApplicationContext();
    }

    public static synchronized LanguageDetectionEngine getInstance(
            Context context
    ) {
        if (instance == null) {
            instance =
                    new LanguageDetectionEngine(
                            context.getApplicationContext()
                    );
        }

        return instance;
    }

    public TranslationModelId detect(
            String text
    ) {
        return TranslationModelId.ROMANCE;
    }
}
