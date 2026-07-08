package com.bliss.aimemorysearch.ai;

import android.content.Context;

public final class RomanceTranslator {

    private static volatile RomanceTranslator instance;

    private final CTranslate2Native nativeTranslator;

    private RomanceTranslator(
            Context context
    ) {

        nativeTranslator =
                new CTranslate2Native();

        try {
            nativeTranslator.init(
                    context.getApplicationContext()
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to initialize offline translation engine",
                    e
            );
        }
    }

    public static synchronized RomanceTranslator getInstance(
            Context context
    ) {

        if (instance == null) {
            instance =
                    new RomanceTranslator(
                            context.getApplicationContext()
                    );
        }

        return instance;
    }

    public String translate(
            String text
    ) {

        if (text == null) {
            return "";
        }

        text =
                text.trim();

        if (text.isEmpty()) {
            return "";
        }

        return nativeTranslator.translate(
                text
        );
    }
}
