package com.bliss.aimemorysearch.ai;

import java.io.File;

public final class RomanceTranslator {

    private static volatile RomanceTranslator instance;

    private final CTranslate2Native nativeTranslator;

    private RomanceTranslator(
            File modelDirectory
    ) {

        nativeTranslator =
                new CTranslate2Native();

        try {
            nativeTranslator.init(
                    modelDirectory
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to initialize offline translation engine",
                    e
            );
        }
    }

    public static synchronized RomanceTranslator getInstance(
            File modelDirectory
    ) {

        if (instance == null) {
            instance =
                    new RomanceTranslator(
                            modelDirectory
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

        String translated = nativeTranslator.translate(
                text
        );
        return translated;
    }

    public synchronized void close() {
        nativeTranslator.close();
        instance =
                null;
    }
}
