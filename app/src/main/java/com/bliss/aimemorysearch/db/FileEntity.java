package com.bliss.aimemorysearch.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "files_index")
public class FileEntity {

    @PrimaryKey
    @NonNull
    public String path;
    public String name;
    public String type;
    public String imagePath;
    public String ocrText;
    public long indexedAt;
    public long lastModified;
    public long fileSize;
    public String indexStatus;
    public String errorMessage;
    public String scanSessionId;
    public byte[] embedding;
    public byte[] imageEmbedding;
    public String detectedPersons;
    public String detectedOrganizations;
    public String detectedDocumentType;
    public String detectedNumbers;
    public FileEntity(
            @NonNull String path,
            String name,
            String type,
            String imagePath,
            String ocrText,
            long indexedAt,
            long lastModified,
            long fileSize,
            String indexStatus,
            String errorMessage,
            String scanSessionId,
            byte[] embedding
    ) {

        this.path = path;
        this.name = name;
        this.type = type;
        this.imagePath = imagePath;
        this.ocrText = ocrText;
        this.indexedAt = indexedAt;
        this.lastModified = lastModified;
        this.fileSize = fileSize;
        this.indexStatus = indexStatus;
        this.errorMessage = errorMessage;
        this.scanSessionId = scanSessionId;
        this.embedding = embedding;
    }
}