package com.bliss.aimemorysearch.ai.multilingual;

public interface MultilingualTextEmbeddingEngine {

    void initialize() throws Exception;

    EmbeddingResult generateEmbedding(String text) throws Exception;

    int getEmbeddingDimension();

    void close();
}