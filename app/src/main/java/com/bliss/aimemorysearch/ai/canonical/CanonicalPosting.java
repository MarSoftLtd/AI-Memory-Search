package com.bliss.aimemorysearch.ai.canonical;

public final class CanonicalPosting {

    private final String filePath;
    private final int chunkIndex;
    private final int termFrequency;
    private final int fieldFlags;

    public CanonicalPosting(
            String filePath,
            int chunkIndex,
            int termFrequency,
            int fieldFlags
    ) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "filePath must not be empty"
            );
        }
        if (chunkIndex < 0) {
            throw new IllegalArgumentException("chunkIndex must not be negative");
        }
        if (termFrequency <= 0) {
            throw new IllegalArgumentException(
                    "termFrequency must be positive"
            );
        }
        if ((fieldFlags & ~0xFF) != 0) {
            throw new IllegalArgumentException(
                    "fieldFlags must fit in one unsigned byte"
            );
        }
        this.filePath = filePath;
        this.chunkIndex = chunkIndex;
        this.termFrequency = termFrequency;
        this.fieldFlags = fieldFlags;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public int getTermFrequency() {
        return termFrequency;
    }

    public int getFieldFlags() {
        return fieldFlags;
    }
}
