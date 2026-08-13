package com.bliss.aimemorysearch.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface EmailDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(EmailEntity email);

    @Query("SELECT * FROM emails WHERE id = :id LIMIT 1")
    EmailEntity getById(String id);

    @Query("SELECT * FROM emails WHERE provider = :provider "
            + "AND account = :account AND messageId = :messageId LIMIT 1")
    EmailEntity getBySource(String provider, String account, String messageId);

    @Query("SELECT * FROM emails ORDER BY timestamp DESC")
    List<EmailEntity> getAll();

    @Query("DELETE FROM emails WHERE id = :id")
    void deleteById(String id);

    @Query("SELECT COUNT(*) FROM emails")
    int count();
}
