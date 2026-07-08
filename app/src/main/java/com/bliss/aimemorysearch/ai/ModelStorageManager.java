package com.bliss.aimemorysearch.ai;

import android.content.Context;

import java.io.File;

public final class ModelStorageManager {

    private static final String MODELS_ROOT_DIRECTORY =
            "translation_models";

    private static volatile ModelStorageManager instance;

    private final Context context;

    private ModelStorageManager(
            Context context
    ) {
        this.context =
                context.getApplicationContext();
    }

    public static synchronized ModelStorageManager getInstance(
            Context context
    ) {
        if (instance == null) {
            instance =
                    new ModelStorageManager(
                            context.getApplicationContext()
                    );
        }

        return instance;
    }

    public File getModelsRootDirectory() {
        return new File(
                context.getFilesDir(),
                MODELS_ROOT_DIRECTORY
        );
    }

    public File getModelDirectory(
            TranslationModelId modelId
    ) {
        return new File(
                getModelsRootDirectory(),
                getModelDirectoryName(
                        modelId
                )
        );
    }

    private static String getModelDirectoryName(
            TranslationModelId modelId
    ) {
        switch (modelId) {
            case ROMANCE:
                return "romance-en";
            case GERMANIC:
                return "germanic-en";
            case SLAVIC:
                return "slavic-en";
            case GREEK:
                return "greek-en";
            case TURKISH:
                return "turkish-en";
            default:
                throw new IllegalArgumentException(
                        "Unknown translation model: " + modelId
                );
        }
    }
}
