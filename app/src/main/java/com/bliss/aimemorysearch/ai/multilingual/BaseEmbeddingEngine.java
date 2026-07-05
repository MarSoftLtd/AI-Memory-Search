package com.bliss.aimemorysearch.ai.multilingual;

public abstract class BaseEmbeddingEngine implements MultilingualTextEmbeddingEngine {

    protected boolean initialized;

    @Override
    public void initialize() throws Exception {
        initialized = true;
    }

    @Override
    public void close() {
        initialized = false;
    }

    protected void checkInitialized() {

        if (!initialized) {
            throw new IllegalStateException("Embedding engine is not initialized.");
        }
    }
}