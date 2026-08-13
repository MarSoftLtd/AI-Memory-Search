package com.bliss.aimemorysearch.email;

import com.bliss.aimemorysearch.ai.TextChunker;
import com.bliss.aimemorysearch.db.ChunkEntity;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EmailIndexDocumentBuilderTest {
    @Test public void createsSearchableEmailResultWithExactProvenance() {
        EmailIndexDocumentBuilder.Result result = build(message("m-1", "first body"));
        assertEquals("EMAIL", result.getFile().type);
        assertTrue(result.getFile().path.startsWith("email://"));
        assertEquals(result.getEmail().id, result.getFile().path);
        assertEquals("provider", result.getEmail().provider);
        assertEquals("account@example.com", result.getEmail().account);
        assertFalse(result.getChunks().isEmpty());
        for (ChunkEntity chunk : result.getChunks()) {
            assertEquals("EMAIL", chunk.sourceType);
            assertEquals(result.getEmail().id, chunk.sourceId);
            assertEquals(result.getFile().path, chunk.filePath);
        }
        List<String> queryChunks = TextChunker.chunkText(result.getIndexText());
        assertFalse(queryChunks.isEmpty());
        assertTrue(result.getChunks().get(0).normalizedText.contains("first body"));
        assertNotNull(result.getFile().embedding);
    }

    @Test public void sameSourceMessageProducesSameIdentityForReplacement() {
        EmailIndexDocumentBuilder.Result first = build(message("m-1", "old body"));
        EmailIndexDocumentBuilder.Result update = build(message("m-1", "new body"));
        assertEquals(first.getEmail().id, update.getEmail().id);
        assertEquals(first.getFile().path, update.getFile().path);
        assertFalse(update.getChunks().get(0).normalizedText.contains("old body"));
        assertTrue(update.getChunks().get(0).normalizedText.contains("new body"));
    }

    @Test public void differentAccountsCannotCollide() {
        String first = EmailIdentity.stableId("provider", "a@example.com", "same-id");
        String second = EmailIdentity.stableId("provider", "b@example.com", "same-id");
        assertFalse(first.equals(second));
    }

    private static EmailIndexDocumentBuilder.Result build(EmailMessage message) {
        return EmailIndexDocumentBuilder.build(
                "Provider", "account@example.com", message,
                text -> new float[]{1f, 2f}, 1234L);
    }

    private static EmailMessage message(String id, String body) {
        return new EmailMessage(id, "thread-1", "Subject", "sender@example.com",
                Arrays.asList("to@example.com"), Collections.singletonList("cc@example.com"),
                body, Collections.emptyList(), 1000L);
    }
}
