package com.bliss.aimemorysearch.ai;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class TranslationModelManager {

    private static volatile TranslationModelManager instance;

    private final Context context;

    private TranslationModelManager(
            Context context
    ) {
        this.context =
                context.getApplicationContext();
    }

    public static synchronized TranslationModelManager getInstance(
            Context context
    ) {
        if (instance == null) {
            instance =
                    new TranslationModelManager(
                            context.getApplicationContext()
                    );
        }

        return instance;
    }

    public File getRomanceModelDirectory() throws IOException {

        TranslationModelInfo modelInfo =
                TranslationModelRegistry.getModel(
                        TranslationModelId.ROMANCE
                );

        File modelDirectory =
                new File(
                        context.getFilesDir(),
                        modelInfo.getInternalDirectory()
                );

        copyAssetDirectory(
                context.getAssets(),
                modelInfo.getAssetDirectory(),
                modelDirectory
        );

        validateModelDirectory(
                modelDirectory
        );

        return modelDirectory;
    }

    private static void validateModelDirectory(
            File modelDirectory
    ) throws IOException {

        if (
                modelDirectory == null
                        ||
                        !modelDirectory.exists()
                        ||
                        !modelDirectory.isDirectory()
        ) {
            throw new IOException(
                    "Translation model directory is invalid"
            );
        }

        validateModelFile(
                modelDirectory,
                "source.spm"
        );
        validateModelFile(
                modelDirectory,
                "target.spm"
        );
        validateModelFile(
                modelDirectory,
                "model.bin"
        );
        validateModelFile(
                modelDirectory,
                "config.json"
        );
    }

    private static void validateModelFile(
            File modelDirectory,
            String fileName
    ) throws IOException {

        File file =
                new File(
                        modelDirectory,
                        fileName
                );

        if (
                !file.exists()
                        ||
                        !file.isFile()
                        ||
                        file.length() == 0
        ) {
            throw new IOException(
                    "Translation model file is missing: " + fileName
            );
        }
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
                        ||
                        children.length == 0
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
                        &&
                        outputFile.length() > 0
        ) {
            return;
        }

        File parent =
                outputFile.getParentFile();

        if (
                parent != null
                        &&
                        !parent.exists()
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
