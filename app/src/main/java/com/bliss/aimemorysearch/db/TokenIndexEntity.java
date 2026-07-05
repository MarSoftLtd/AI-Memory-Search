package com.bliss.aimemorysearch.db;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "token_index",
        indices = {
                @Index("token"),
                @Index("chunkId")
        }
)
public class TokenIndexEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String token;

    public int chunkId;

    public String filePath;

    public int chunkIndex;
}