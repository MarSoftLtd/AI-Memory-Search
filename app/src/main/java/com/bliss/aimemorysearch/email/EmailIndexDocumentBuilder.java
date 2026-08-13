package com.bliss.aimemorysearch.email;

import com.bliss.aimemorysearch.ai.EmbeddingUtils;
import com.bliss.aimemorysearch.ai.TextChunker;
import com.bliss.aimemorysearch.ai.TextEmbeddingEngine;
import com.bliss.aimemorysearch.db.ChunkEntity;
import com.bliss.aimemorysearch.db.EmailEntity;
import com.bliss.aimemorysearch.db.FileEntity;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Converts provider-neutral email data into the existing textual index representation. */
public final class EmailIndexDocumentBuilder {
    private EmailIndexDocumentBuilder() {}

    public static Result build(String provider, String account, EmailMessage message,
                               TextEmbeddingEngine embeddingEngine, long indexedAt) {
        if (message == null) throw new IllegalArgumentException("message == null");
        String id = EmailIdentity.stableId(provider, account, message.getSourceMessageId());
        String indexText = EmailIndexTextBuilder.build(message);
        byte[] fileEmbedding = embedding(embeddingEngine, indexText);

        FileEntity file = new FileEntity(
                id,
                message.getSubject().isEmpty() ? "Email" : message.getSubject(),
                "EMAIL",
                null,
                indexText.substring(0, Math.min(2000, indexText.length())),
                indexedAt,
                message.getTimestamp(),
                indexText.getBytes(StandardCharsets.UTF_8).length,
                "DONE",
                "",
                "email",
                fileEmbedding);

        EmailEntity email = new EmailEntity(
                id, provider.trim().toLowerCase(Locale.ROOT), account.trim(),
                message.getSourceMessageId().trim(), message.getThreadId(),
                message.getSubject(), message.getFrom(), join(message.getTo()),
                join(message.getCc()), message.getBody(), message.getTimestamp(),
                message.getAttachments().size(), indexedAt);

        List<ChunkEntity> chunks = new ArrayList<>();
        int chunkIndex = 0;
        for (String text : TextChunker.chunkText(indexText)) {
            if (text == null || text.trim().length() < 5) continue;
            ChunkEntity chunk = new ChunkEntity();
            chunk.parentFileId = 0L;
            chunk.filePath = id;
            chunk.fileName = file.name;
            chunk.chunkText = text.trim();
            chunk.normalizedText = normalize(chunk.chunkText);
            if (chunkIndex % 3 == 0) {
                chunk.embedding = embedding(embeddingEngine, chunk.chunkText);
            }
            chunk.chunkIndex = chunkIndex++;
            chunk.indexedAt = indexedAt;
            chunk.sourceType = "EMAIL";
            chunk.sourceId = id;
            chunks.add(chunk);
        }
        return new Result(email, file, chunks, indexText);
    }

    private static byte[] embedding(TextEmbeddingEngine engine, String text) {
        if (engine == null || text == null || text.trim().isEmpty()) return null;
        float[] vector = engine.generateEmbedding(text);
        return vector == null || vector.length == 0
                ? null : EmbeddingUtils.floatArrayToBytes(vector);
    }

    private static String normalize(String text) {
        String value = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]", " ")
                .replaceAll("\\s+", " ");
        return value.trim();
    }

    private static String join(List<String> values) {
        if (values == null || values.isEmpty()) return "";
        return String.join("\n", values);
    }

    public static final class Result {
        private final EmailEntity email;
        private final FileEntity file;
        private final List<ChunkEntity> chunks;
        private final String indexText;

        Result(EmailEntity email, FileEntity file, List<ChunkEntity> chunks,
               String indexText) {
            this.email = email;
            this.file = file;
            this.chunks = Collections.unmodifiableList(new ArrayList<>(chunks));
            this.indexText = indexText;
        }

        public EmailEntity getEmail() { return email; }
        public FileEntity getFile() { return file; }
        public List<ChunkEntity> getChunks() { return chunks; }
        public String getIndexText() { return indexText; }
    }
}
