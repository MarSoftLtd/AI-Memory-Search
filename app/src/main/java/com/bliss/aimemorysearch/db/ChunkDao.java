package com.bliss.aimemorysearch.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ChunkDao {

    @Insert
    void insertChunk(
            ChunkEntity chunk
    );

    @Insert
    void insertChunks(
            List<ChunkEntity> chunks
    );

    @Query("SELECT * FROM chunks")
    List<ChunkEntity> getAllChunks();

    @Query(
            "SELECT * FROM chunks " +
                    "WHERE normalizedText LIKE :token || '%' " +
                    "OR normalizedText LIKE '% ' || :token || '%' " +
                    "LIMIT :limit"
    )
    List<ChunkEntity> searchByToken(
            String token,
            int limit
    );
    @Query(
            "SELECT normalizedText FROM chunks " +
                    "WHERE normalizedText LIKE :prefix || '%' " +
                    "OR normalizedText LIKE '% ' || :prefix || '%' " +
                    "LIMIT 200"
    )
    List<String> findTextsByPrefix(
            String prefix
    );

    @Query(
            "SELECT * FROM chunks " +
                    "LIMIT :limit"
    )
    List<ChunkEntity> getLimitedChunks(
            int limit
    );

    @Query("DELETE FROM chunks")
    void deleteAllChunks();
}