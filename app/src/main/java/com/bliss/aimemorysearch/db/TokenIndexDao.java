package com.bliss.aimemorysearch.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TokenIndexDao {

    @Insert
    void insertTokenIndexes(
            List<TokenIndexEntity> indexes
    );

    @Query(
            "SELECT chunkId FROM token_index " +
                    "WHERE token LIKE :prefix || '%' " +
                    "LIMIT :limit"
    )
    List<Integer> searchChunkIdsByPrefix(
            String prefix,
            int limit
    );

    @Query("DELETE FROM token_index")
    void deleteAllTokenIndexes();
}