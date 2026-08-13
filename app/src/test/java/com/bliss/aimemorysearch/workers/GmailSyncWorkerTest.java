package com.bliss.aimemorysearch.workers;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GmailSyncWorkerTest {
    @Test public void completedInitialSyncWithHistoryUsesIncrementalPath() {
        assertTrue(GmailSyncWorker.shouldUseIncremental(true, "123456"));
    }

    @Test public void missingCompletionOrCheckpointRequiresFullSync() {
        assertFalse(GmailSyncWorker.shouldUseIncremental(false, "123456"));
        assertFalse(GmailSyncWorker.shouldUseIncremental(true, ""));
        assertFalse(GmailSyncWorker.shouldUseIncremental(true, null));
    }
}
