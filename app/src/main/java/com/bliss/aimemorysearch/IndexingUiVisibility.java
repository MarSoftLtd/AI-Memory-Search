package com.bliss.aimemorysearch;

import androidx.work.WorkInfo;

/** Pure visibility rule for transient indexing UI. */
public final class IndexingUiVisibility {
    private IndexingUiVisibility() {}

    public static boolean isActive(WorkInfo.State state, boolean initialIndexComplete,
                                   boolean reconciliation, int discoveredTotal) {
        if (state == null || state.isFinished()) return false;
        return reconciliation || !initialIndexComplete || discoveredTotal > 0;
    }

    public static boolean showFirstIndexHeader(WorkInfo.State state,
            boolean initialIndexComplete, boolean reconciliation, int discoveredTotal) {
        return !initialIndexComplete && !reconciliation
                && isActive(state, false, false, discoveredTotal);
    }
}
