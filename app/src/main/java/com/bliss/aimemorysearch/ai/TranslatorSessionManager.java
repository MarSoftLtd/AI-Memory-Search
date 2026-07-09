package com.bliss.aimemorysearch.ai;

public final class TranslatorSessionManager {

    private static volatile TranslatorSessionManager instance;

    private TranslationModelId activeModelId;
    private RomanceTranslator activeTranslator;

    private TranslatorSessionManager() {
    }

    public static synchronized TranslatorSessionManager getInstance() {
        if (instance == null) {
            instance =
                    new TranslatorSessionManager();
        }

        return instance;
    }

    public synchronized RomanceTranslator getTranslator(
            TranslationModelId modelId,
            TranslationPackage translationPackage
    ) {
        if (
                activeTranslator != null
                        &&
                        activeModelId == modelId
        ) {
            return activeTranslator;
        }

        if (activeTranslator != null) {
            activeTranslator.close();
            activeTranslator =
                    null;
            activeModelId =
                    null;
        }

        activeTranslator =
                RomanceTranslator.getInstance(
                        translationPackage.getDirectory()
                );
        activeModelId =
                modelId;

        return activeTranslator;
    }
}
