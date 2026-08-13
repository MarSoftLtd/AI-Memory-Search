package com.bliss.aimemorysearch.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chunks")
public class ChunkEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public long parentFileId;

    public String filePath;

    public String fileName;

    public String chunkText;

    // TEXT NORMALIZAT PRECOMPUTAT
    public String normalizedText;

    public byte[] embedding;

    public int chunkIndex;

    public long indexedAt;

    /** Nullable for legacy document/image rows; EMAIL rows point to emails.id. */
    public String sourceType;

    /** Stable source identifier used for exact provenance and future citations. */
    public String sourceId;
}
