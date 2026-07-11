package com.bliss.aimemorysearch.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

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

    @Query("DELETE FROM token_index WHERE filePath = :filePath")
    void deleteByFilePath(String filePath);

    @Query("SELECT * FROM token_index")
    List<TokenIndexEntity> getAllTokenIndexes();

    @Update
    void updateTokenIndexes(List<TokenIndexEntity> indexes);

    @Query(
            "DELETE FROM token_index WHERE id NOT IN " +
                    "(SELECT MIN(id) FROM token_index " +
                    "GROUP BY filePath, chunkIndex, token)"
    )
    void deleteDuplicateTokenIndexes();
}
