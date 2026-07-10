package com.bliss.aimemorysearch.ai;

import android.content.Context;

public final class DocumentRuntimeLoader {

    private final AiRuntimeManager runtimeManager;
    private final AiPackageManager packageManager;

    public DocumentRuntimeLoader(
            AiRuntimeManager runtimeManager,
            AiPackageManager packageManager
    ) {
        this.runtimeManager =
                runtimeManager;
        this.packageManager =
                packageManager;
    }

    public static DocumentRuntimeLoader createDefault() {
        return new DocumentRuntimeLoader(
                AiPlatform.getRuntimeManager(),
                AiPlatform.getPackageManager()
        );
    }

    public EmbeddingEngine loadEmbeddingRuntime(
            Context context
    ) {
        EmbeddingEngine engine =
                EmbeddingEngine.getInstance();

        engine.initialize(
                context
        );

        registerRuntime();

        return engine;
    }

    public EmbeddingEngine getEmbeddingRuntime() {
        registerRuntime();

        return EmbeddingEngine.getInstance();
    }

    private void registerRuntime() {
        AiPackageInfo packageInfo =
                packageManager.getActivePackage(
                        AiCapability.DOCUMENT_SEARCH
                );

        if (packageInfo != null) {
            runtimeManager.loadRuntime(
                    AiCapability.DOCUMENT_SEARCH,
                    packageInfo
            );
        }
    }
}
