package com.bliss.aimemorysearch.ai.explanation;

/** Immutable metadata for the explanation projection itself. */
public final class GenerationMetadata {
    private final long generatedAtEpochMillis;
    private final String engineVersion;
    private final String rendererVersion;

    public GenerationMetadata(
            long generatedAtEpochMillis,
            String engineVersion,
            String rendererVersion
    ) {
        this.generatedAtEpochMillis = generatedAtEpochMillis;
        this.engineVersion = engineVersion;
        this.rendererVersion = rendererVersion;
    }

    public long getGeneratedAtEpochMillis() { return generatedAtEpochMillis; }
    public String getEngineVersion() { return engineVersion; }
    public String getRendererVersion() { return rendererVersion; }
}
