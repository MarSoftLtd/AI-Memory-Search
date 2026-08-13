package com.bliss.aimemorysearch.email;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EmailSyncProgressTest {
    @Test public void fullThenRestartKeepsPersistentTotalForIncremental() {
        EmailSyncProgress full = new EmailSyncProgress(0);
        full.reconciledTotal(100);
        full.reconciledTotal(200);
        EmailSyncProgress restartedIncremental = new EmailSyncProgress(full.total());
        assertEquals(200, restartedIncremental.total());
        assertEquals(0, restartedIncremental.delta());
    }

    @Test public void backgroundResumeDoesNotResetTotal() {
        EmailSyncProgress running = new EmailSyncProgress(8000);
        running.reconciledTotal(8050);
        EmailSyncProgress resumed = new EmailSyncProgress(running.total());
        assertEquals(8050, resumed.total());
    }

    @Test public void incrementalWithoutChangesKeepsTotalAndZeroDelta() {
        EmailSyncProgress progress = new EmailSyncProgress(8430);
        assertEquals(8430, progress.total());
        assertEquals(0, progress.delta());
    }

    @Test public void incrementalChangesUseActualLocalCount() {
        EmailSyncProgress progress = new EmailSyncProgress(8430);
        progress.reconciledTotal(8431);
        progress.reconciledTotal(8430);
        assertEquals(8430, progress.total());
        assertEquals(2, progress.delta());
    }
}
