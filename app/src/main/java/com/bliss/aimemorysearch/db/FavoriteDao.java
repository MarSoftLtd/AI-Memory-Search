package com.bliss.aimemorysearch.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface FavoriteDao {

    @Insert(
            onConflict =
                    OnConflictStrategy.REPLACE
    )
    void insert(
            FavoriteEntity favorite
    );

    @Query(
            "SELECT * FROM favorites ORDER BY addedAt DESC"
    )
    List<FavoriteEntity> getAll();

    @Query(
            "DELETE FROM favorites WHERE path = :path"
    )
    void delete(
            String path
    );

    @Query(
            "SELECT EXISTS(" +
                    "SELECT 1 FROM favorites " +
                    "WHERE path = :path)"
    )
    boolean exists(
            String path
    );
    @Query("SELECT * FROM files_index")
    List<FileEntity> getAllFilesSync();
}