package com.bliss.aimemorysearch.ai;

import android.content.Context;

public final class TranslationEngine {

    private static volatile TranslationEngine instance;

    private final RomanceTranslator translator;

    private TranslationEngine(
            Context context
    ) {
        translator =
                RomanceTranslator.getInstance(
                        context.getApplicationContext()
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
        return translator.translate(
                text
        );
    }
}
