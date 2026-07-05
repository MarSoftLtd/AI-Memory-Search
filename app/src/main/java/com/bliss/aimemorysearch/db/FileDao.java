package com.bliss.aimemorysearch.db;
import androidx.lifecycle.LiveData;
import androidx.paging.PagingSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(FileEntity file);

    @Update
    void updateFile(FileEntity file);

    @Query(
            "SELECT * FROM files_index " +
                    "ORDER BY indexedAt DESC"
    )
    PagingSource<Integer, FileEntity> pagingFiles();

    @Query("SELECT * FROM files_index ORDER BY indexedAt DESC")
    LiveData<List<FileEntity>> getRecentFiles();

    @Query(
            "SELECT * FROM files_index " +
                    "WHERE " +
                    "name LIKE '%' || :query || '%' " +
                    "OR path LIKE '%' || :query || '%' " +
                    "OR ocrText LIKE '%' || :query || '%' " +
                    "ORDER BY indexedAt DESC"
    )
    List<FileEntity> searchFiles(String query);

    @Query(
            "SELECT COUNT(*) FROM files_index"
    )
    int countFiles();

    @Query(
            "SELECT * FROM files_index " +
                    "WHERE path = :path LIMIT 1"
    )
    FileEntity getFileByPath(String path);

    @Query(
            "DELETE FROM files_index " +
                    "WHERE path = :path"
    )
    void deleteByPath(String path);

    @Query(
            "SELECT * FROM files_index " +
                    "WHERE indexStatus = 'INDEXING'"
    )
    List<FileEntity> getStuckFiles();

    @Query(
            "UPDATE files_index " +
                    "SET indexStatus = 'PENDING' " +
                    "WHERE indexStatus = 'INDEXING'"
    )
    void resetInterruptedFiles();

    @Query(
            "SELECT COUNT(*) FROM files_index " +
                    "WHERE indexStatus = 'DONE'"
    )
    int countIndexed();

    @Query(
            "SELECT COUNT(*) FROM files_index " +
                    "WHERE indexStatus = 'FAILED'"
    )
    int countFailed();

    @Query(
            "SELECT COUNT(*) FROM files_index " +
                    "WHERE indexStatus = 'PENDING'"
    )
    int countPending();
    @Query(
            "SELECT COUNT(*) FROM files_index"
    )
    LiveData<Integer> getIndexedCountLive();

    @Query(
            "SELECT COUNT(*) FROM files_index " +
                    "WHERE indexStatus = 'FAILED'"
    )
    LiveData<Integer> getFailedCountLive();

    @Query(
            "SELECT COUNT(*) FROM files_index " +
                    "WHERE indexStatus = 'PENDING'"
    )
    LiveData<Integer> getPendingCountLive();
    
    @Query("SELECT * FROM files_index")
    List<FileEntity> getAllFilesSync();

    @Query(
            "SELECT COUNT(*) FROM files_index WHERE type = 'PDF'"
    )
    LiveData<Integer> getPdfCountLive();

    @Query(
            "SELECT COUNT(*) FROM files_index WHERE type = 'IMAGE'"
    )
    LiveData<Integer> getImageCountLive();

    @Query(
            "SELECT COUNT(*) FROM files_index WHERE ocrText IS NOT NULL AND ocrText != ''"
    )
    LiveData<Integer> getOcrCountLive();

    @Query(
            "SELECT COUNT(*) FROM files_index WHERE embedding IS NOT NULL"
    )
    LiveData<Integer> getEmbeddingCountLive();
    
    @Query("SELECT * FROM files_index")
    List<FileEntity> getAllFiles();
    @Query(
            "SELECT * FROM files_index " +
                    "WHERE type = 'IMAGE' " +
                    "AND imageEmbedding IS NOT NULL"
    )
    List<FileEntity> getAllIndexedImages();
}