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
        android.util.Log.d("MULTILINGUAL_PIPELINE", "Language detector input: " + text);
        android.util.Log.d("MULTILINGUAL_PIPELINE", "Detected language family/model: " + TranslationModelId.ROMANCE + " (hard-coded fallback)");
        return TranslationModelId.ROMANCE;
    }
}
