package com.bliss.aimemorysearch.email;

/** Small deterministic model for persistent totals across full/incremental executions. */
public final class EmailSyncProgress {
    private int total;
    private int delta;

    public EmailSyncProgress(int persistentTotal) {
        total = Math.max(0, persistentTotal);
    }

    public void reconciledTotal(int actualLocalTotal) {
        total = Math.max(0, actualLocalTotal);
        delta++;
    }

    public int total() { return total; }
    public int delta() { return delta; }
}
