package com.bliss.aimemorysearch.ai;

import android.content.Context;

public final class ImageRuntimeLoader {

    private final AiRuntimeManager runtimeManager;
    private final AiPackageManager packageManager;

    public ImageRuntimeLoader(
            AiRuntimeManager runtimeManager,
            AiPackageManager packageManager
    ) {
        this.runtimeManager =
                runtimeManager;
        this.packageManager =
                packageManager;
    }

    public static ImageRuntimeLoader createDefault() {
        return new ImageRuntimeLoader(
                AiPlatform.getRuntimeManager(),
                AiPlatform.getPackageManager()
        );
    }

    public MobileClipTextEmbeddingEngine loadTextEmbeddingRuntime(
            Context context
    ) {
        MobileClipTextEmbeddingEngine engine =
                MobileClipTextEmbeddingEngine.getInstance();

        engine.initialize(
                context
        );

        registerRuntime();

        return engine;
    }

    public ImageEmbeddingEngine loadImageEmbeddingRuntime(
            Context context
    ) {
        ImageEmbeddingEngine engine =
                ImageEmbeddingEngine.getInstance();

        engine.initialize(
                context
        );

        registerRuntime();

        return engine;
    }

    public MobileClipTextEmbeddingEngine getTextEmbeddingRuntime() {
        registerRuntime();

        return MobileClipTextEmbeddingEngine.getInstance();
    }

    public ImageEmbeddingEngine getImageEmbeddingRuntime() {
        registerRuntime();

        return ImageEmbeddingEngine.getInstance();
    }

    private void registerRuntime() {
        AiPackageInfo packageInfo =
                packageManager.getActivePackage(
                        AiCapability.IMAGE_SEARCH
                );

        if (packageInfo != null) {
            runtimeManager.loadRuntime(
                    AiCapability.IMAGE_SEARCH,
                    packageInfo
            );
        }
    }
}
