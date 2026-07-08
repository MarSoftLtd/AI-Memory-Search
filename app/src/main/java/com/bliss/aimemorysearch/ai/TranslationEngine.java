package com.bliss.aimemorysearch.ai;

import android.content.Context;

import java.io.File;

public final class TranslationEngine {

    private static volatile TranslationEngine instance;

    private final RomanceTranslator translator;

    private TranslationEngine(
            Context context
    ) {
        try {
            File modelDirectory =
                    TranslationModelManager
                            .getInstance(context)
                            .getRomanceModelDirectory();

            translator =
                    RomanceTranslator.getInstance(
                            modelDirectory
                    );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to initialize offline translation engine",
                    e
            );
        }
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
