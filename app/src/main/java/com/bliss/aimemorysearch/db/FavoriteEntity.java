package com.bliss.aimemorysearch.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorites")
public class FavoriteEntity {

    @PrimaryKey
    @NonNull
    public String path;

    public String name;

    public String type;

    public String imagePath;

    public long addedAt;

    public FavoriteEntity(
            @NonNull String path,
            String name,
            String type,
            String imagePath,
            long addedAt
    ) {

        this.path = path;
        this.name = name;
        this.type = type;
        this.imagePath = imagePath;
        this.addedAt = addedAt;
    }
}