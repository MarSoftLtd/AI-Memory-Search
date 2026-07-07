package com.bliss.aimemorysearch.ai;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CTranslate2Native {

    static {
        System.loadLibrary("ctranslate2");
        System.loadLibrary("e5_sentencepiece_jni");
        System.loadLibrary("ctranslate2_jni");
    }

    private static final String ASSET_MODEL_DIR =
            "models/translator/romance-en";

    private static final String INTERNAL_MODEL_DIR =
            "models/translator/romance-en";

    private boolean initialized = false;

    private native void nativeInit(String modelPath);

    private native String nativeTranslate(String text);

    private native void nativeClose();

    public void init(
            Context context
    ) throws IOException {

        File modelDir =
                new File(
                        context.getFilesDir(),
                        INTERNAL_MODEL_DIR
                );

        copyAssetDirectory(
                context.getAssets(),
                ASSET_MODEL_DIR,
                modelDir
        );

        nativeInit(
                modelDir.getAbsolutePath()
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

    private static void copyAssetDirectory(
            AssetManager assetManager,
            String assetPath,
            File outputDir
    ) throws IOException {

        String[] children =
                assetManager.list(assetPath);

        if (
                children == null
                        || children.length == 0
        ) {
            copyAssetFile(
                    assetManager,
                    assetPath,
                    outputDir
            );
            return;
        }

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        for (String child : children) {
            copyAssetDirectory(
                    assetManager,
                    assetPath + "/" + child,
                    new File(
                            outputDir,
                            child
                    )
            );
        }
    }

    private static void copyAssetFile(
            AssetManager assetManager,
            String assetPath,
            File outputFile
    ) throws IOException {

        if (
                outputFile.exists()
                        && outputFile.length() > 0
        ) {
            return;
        }

        File parent =
                outputFile.getParentFile();

        if (
                parent != null
                        && !parent.exists()
        ) {
            parent.mkdirs();
        }

        try (
                InputStream inputStream =
                        assetManager.open(assetPath);
                FileOutputStream outputStream =
                        new FileOutputStream(outputFile)
        ) {

            byte[] buffer =
                    new byte[8192];
            int read;

            while (
                    (read = inputStream.read(buffer)) != -1
            ) {
                outputStream.write(
                        buffer,
                        0,
                        read
                );
            }
        }
    }
}
