package com.bliss.aimemorysearch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.work.WorkInfo;
import org.junit.Test;

public class IndexingUiVisibilityTest {
    @Test public void showsOnlyRealActiveIndexing() {
        assertTrue(IndexingUiVisibility.isActive(
                WorkInfo.State.ENQUEUED, false, false, 0));
        assertTrue(IndexingUiVisibility.isActive(
                WorkInfo.State.RUNNING, true, false, 3));
        assertTrue(IndexingUiVisibility.isActive(
                WorkInfo.State.RUNNING, true, true, 0));
        assertFalse(IndexingUiVisibility.isActive(
                WorkInfo.State.RUNNING, true, false, 0));
        assertFalse(IndexingUiVisibility.isActive(
                WorkInfo.State.SUCCEEDED, false, false, 10));
        assertFalse(IndexingUiVisibility.isActive(
                WorkInfo.State.FAILED, false, true, 10));
    }

    @Test public void firstIndexHeaderNeverReturnsAfterPersistentCompletion() {
        assertTrue(IndexingUiVisibility.showFirstIndexHeader(
                WorkInfo.State.RUNNING, false, false, 0));
        assertFalse(IndexingUiVisibility.showFirstIndexHeader(
                WorkInfo.State.RUNNING, true, false, 12));
        assertFalse(IndexingUiVisibility.showFirstIndexHeader(
                WorkInfo.State.RUNNING, false, true, 12));
        assertFalse(IndexingUiVisibility.showFirstIndexHeader(
                WorkInfo.State.SUCCEEDED, false, false, 12));
    }
}
