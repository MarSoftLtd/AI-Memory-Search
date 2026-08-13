package com.bliss.aimemorysearch.email;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EmailRepositoryTest {
    @Test public void updateReplacesPreviousIndexWithoutDuplicate() {
        MemoryStorage storage = new MemoryStorage();
        EmailRepository repository = repository(storage);
        String firstId = repository.upsert("provider", "account", message("id-1", "old text"));
        String updatedId = repository.upsert("provider", "account", message("id-1", "new text"));

        assertEquals(firstId, updatedId);
        assertEquals(1, storage.records.size());
        assertEquals(2, storage.replaceCalls);
        assertTrue(storage.records.get(firstId).getIndexText().contains("new text"));
        assertFalse(storage.records.get(firstId).getIndexText().contains("old text"));
    }

    @Test public void deleteRemovesEmailAndAssociatedIndexRecord() {
        MemoryStorage storage = new MemoryStorage();
        EmailRepository repository = repository(storage);
        String id = repository.upsert("provider", "account", message("id-1", "body"));
        assertTrue(storage.records.containsKey(id));

        repository.delete("provider", "account", "id-1");

        assertFalse(storage.records.containsKey(id));
        assertEquals(id, storage.deletedId);
    }

    private static EmailRepository repository(MemoryStorage storage) {
        return new EmailRepository(storage, text -> new float[]{1f}, () -> 2000L);
    }

    private static EmailMessage message(String id, String body) {
        return new EmailMessage(id, "thread", "subject", "from@example.com",
                Arrays.asList("to@example.com"), Collections.emptyList(), body,
                Collections.emptyList(), 1000L);
    }

    private static final class MemoryStorage implements EmailRepository.Storage {
        final Map<String, EmailIndexDocumentBuilder.Result> records = new LinkedHashMap<>();
        int replaceCalls;
        String deletedId;

        @Override public void replace(EmailIndexDocumentBuilder.Result result) {
            replaceCalls++;
            records.put(result.getEmail().id, result);
        }

        @Override public void delete(String id) {
            deletedId = id;
            records.remove(id);
        }
    }
}
