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
    long[] insertChunks(
            List<ChunkEntity> chunks
    );

    @Query("SELECT * FROM chunks")
    List<ChunkEntity> getAllChunks();

    @Query(
            "SELECT * FROM chunks " +
                    "WHERE normalizedText = :token " +
                    "OR normalizedText LIKE :token || ' %' " +
                    "OR normalizedText LIKE '% ' || :token || ' %' " +
                    "OR normalizedText LIKE '% ' || :token " +
                    "LIMIT :limit"
    )
    List<ChunkEntity> searchByToken(
            String token,
            int limit
    );

    @Query("SELECT * FROM chunks WHERE filePath IN (:filePaths) "
            + "AND chunkIndex IN (:chunkIndexes)")
    List<ChunkEntity> getChunksByStableIdentities(
            List<String> filePaths,
            List<Integer> chunkIndexes
    );

    @Query("SELECT * FROM chunks WHERE filePath = :filePath ORDER BY chunkIndex")
    List<ChunkEntity> getChunksByFilePath(String filePath);

    @Query("SELECT id,parentFileId,filePath,fileName,chunkText,normalizedText,"
            + "NULL AS embedding,chunkIndex,indexedAt,sourceType,sourceId FROM chunks "
            + "WHERE filePath = :filePath ORDER BY chunkIndex")
    List<ChunkEntity> getCanonicalChunksByFilePath(String filePath);
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

    @Query("DELETE FROM chunks WHERE filePath = :filePath")
    void deleteByFilePath(String filePath);

    @Query(
            "DELETE FROM chunks WHERE id NOT IN " +
                    "(SELECT MIN(id) FROM chunks GROUP BY filePath, chunkIndex)"
    )
    void deleteDuplicateChunks();
}
