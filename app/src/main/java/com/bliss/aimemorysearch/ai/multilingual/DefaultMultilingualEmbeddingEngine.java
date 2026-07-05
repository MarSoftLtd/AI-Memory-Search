package com.bliss.aimemorysearch.ai.multilingual;

import android.content.Context;

import com.bliss.aimemorysearch.ai.EmbeddingEngine;

public class DefaultMultilingualEmbeddingEngine
        implements MultilingualTextEmbeddingEngine {

    private final Context context;

    private EmbeddingEngine embeddingEngine;

    public DefaultMultilingualEmbeddingEngine(Context context) {

        this.context = context.getApplicationContext();
    }

    @Override
    public void initialize() throws Exception {

        embeddingEngine =
                EmbeddingEngine.getInstance();

        embeddingEngine.initialize(context);
    }

    @Override
    public EmbeddingResult generateEmbedding(
            String text
    ) throws Exception {

        float[] embedding =
                embeddingEngine.generateEmbedding(text);

        return new EmbeddingResult(
                embedding,
                embedding.length
        );
    }

    @Override
    public int getEmbeddingDimension() {

        return 384;
    }

    @Override
    public void close() {

    }
}