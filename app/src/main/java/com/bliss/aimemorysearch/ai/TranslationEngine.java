package com.bliss.aimemorysearch.ai;

import android.content.Context;

import java.io.File;

public final class TranslationEngine {

    private static volatile TranslationEngine instance;

    private final Context context;

    private RomanceTranslator translator;

    private TranslationEngine(
            Context context
    ) {
        this.context =
                context.getApplicationContext();
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

        if (translator != null) {
            return translator;
        }

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

            File modelDirectory =
                    translationPackage.getDirectory();

            translator =
                    RomanceTranslator.getInstance(
                            modelDirectory
                    );

            return translator;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to initialize offline translation engine",
                    e
            );
        }
    }
}
