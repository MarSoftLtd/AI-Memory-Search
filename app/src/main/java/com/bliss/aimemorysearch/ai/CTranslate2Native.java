package com.bliss.aimemorysearch.ai;

import java.io.File;

public class CTranslate2Native {

    static {
        System.loadLibrary("ctranslate2");
        System.loadLibrary("e5_sentencepiece_jni");
        System.loadLibrary("ctranslate2_jni");
    }

    private boolean initialized = false;

    private native void nativeInit(String modelPath);

    private native String nativeTranslate(String text);

    private native void nativeClose();

    public void init(
            File modelDirectory
    ) {

        if (
                modelDirectory == null
                        ||
                        !modelDirectory.exists()
                        ||
                        !modelDirectory.isDirectory()
        ) {
            throw new IllegalArgumentException(
                    "Invalid CTranslate2 model directory"
            );
        }

        nativeInit(
                modelDirectory.getAbsolutePath()
        );

        initialized = true;
    }

    public String translate(
            String text
    ) {

        if (!initialized) {
            throw new IllegalStateException(
                    "CTranslate2Native is not initialized"
            );
        }

        return nativeTranslate(text);
    }

    public void close() {

        nativeClose();
        initialized = false;
    }
}
