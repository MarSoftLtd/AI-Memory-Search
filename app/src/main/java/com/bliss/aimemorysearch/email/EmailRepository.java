package com.bliss.aimemorysearch.email;

import com.bliss.aimemorysearch.ai.TextEmbeddingEngine;
import com.bliss.aimemorysearch.db.AppDatabase;
import com.bliss.aimemorysearch.db.ChunkEntity;

import java.util.List;

/** Transactional persistence boundary for email rows and their shared text index. */
public final class EmailRepository {
    private final Storage storage;
    private final TextEmbeddingEngine embeddingEngine;
    private final Clock clock;

    public EmailRepository(AppDatabase database, TextEmbeddingEngine embeddingEngine) {
        if (database == null) throw new IllegalArgumentException("database == null");
        this.storage = new RoomStorage(database);
        this.embeddingEngine = embeddingEngine;
        this.clock = System::currentTimeMillis;
    }

    EmailRepository(Storage storage, TextEmbeddingEngine embeddingEngine, Clock clock) {
        if (storage == null) throw new IllegalArgumentException("storage == null");
        this.storage = storage;
        this.embeddingEngine = embeddingEngine;
        this.clock = clock == null ? System::currentTimeMillis : clock;
    }

    public String upsert(String provider, String account, EmailMessage message) {
        EmailIndexDocumentBuilder.Result result = EmailIndexDocumentBuilder.build(
                provider, account, message, embeddingEngine, clock.now());
        String id = result.getEmail().id;
        storage.replace(result);
        return id;
    }

    public void delete(String provider, String account, String messageId) {
        deleteById(EmailIdentity.stableId(provider, account, messageId));
    }

    public void deleteById(String id) {
        if (id == null || id.trim().isEmpty()) return;
        storage.delete(id);
    }

    interface Storage {
        void replace(EmailIndexDocumentBuilder.Result result);
        void delete(String id);
    }

    interface Clock { long now(); }

    private static final class RoomStorage implements Storage {
        private final AppDatabase database;
        RoomStorage(AppDatabase database) { this.database = database; }

        @Override public void replace(EmailIndexDocumentBuilder.Result result) {
            String id = result.getEmail().id;
            database.runInTransaction(() -> {
                database.tokenIndexDao().deleteByFilePath(id);
                database.chunkDao().deleteByFilePath(id);
                database.fileDao().deleteByPath(id);
                database.emailDao().insertOrUpdate(result.getEmail());
                List<ChunkEntity> chunks = result.getChunks();
                if (!chunks.isEmpty()) database.chunkDao().insertChunks(chunks);
                database.fileDao().insertOrUpdate(result.getFile());
            });
        }

        @Override public void delete(String id) {
            database.runInTransaction(() -> {
                database.tokenIndexDao().deleteByFilePath(id);
                database.chunkDao().deleteByFilePath(id);
                database.fileDao().deleteByPath(id);
                database.emailDao().deleteById(id);
            });
        }
    }
}
