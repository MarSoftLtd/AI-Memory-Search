package com.bliss.aimemorysearch.email;

import android.content.Context;
import android.content.SharedPreferences;

/** Persistent UI/sync snapshot. WorkInfo remains only an activity signal. */
public final class EmailSyncState {
    public static final String KEY_TOTAL = "total_emails_indexed";
    public static final String KEY_LAST_SUCCESS = "last_success";
    public static final String KEY_INITIAL_COMPLETE = "initial_sync_complete";
    public static final String KEY_MODE = "sync_mode";
    public static final String KEY_DELTA = "processed_delta";
    public static final String MODE_FULL = "FULL";
    public static final String MODE_INCREMENTAL = "INCREMENTAL";

    private final SharedPreferences preferences;

    public EmailSyncState(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public static EmailSyncState from(Context context, String preferencesName) {
        return new EmailSyncState(context.getSharedPreferences(
                preferencesName, Context.MODE_PRIVATE));
    }

    public Snapshot read() {
        return new Snapshot(
                Math.max(0, preferences.getInt(KEY_TOTAL, 0)),
                Math.max(0L, preferences.getLong(KEY_LAST_SUCCESS, 0L)),
                preferences.getBoolean(KEY_INITIAL_COMPLETE, false),
                preferences.getString(KEY_MODE, ""),
                Math.max(0, preferences.getInt(KEY_DELTA, 0)));
    }

    public void syncStarted(String mode, int total) {
        preferences.edit().putString(KEY_MODE, mode)
                .putInt(KEY_TOTAL, Math.max(0, total)).putInt(KEY_DELTA, 0).apply();
    }

    public void progress(int total, int delta) {
        preferences.edit().putInt(KEY_TOTAL, Math.max(0, total))
                .putInt(KEY_DELTA, Math.max(0, delta)).apply();
    }

    public void succeeded(int total, long timestamp) {
        preferences.edit().putInt(KEY_TOTAL, Math.max(0, total))
                .putInt(KEY_DELTA, 0).putLong(KEY_LAST_SUCCESS, timestamp)
                .putBoolean(KEY_INITIAL_COMPLETE, true).apply();
    }

    public static final class Snapshot {
        public final int totalEmailsIndexed;
        public final long lastSuccessfulSync;
        public final boolean initialSyncComplete;
        public final String mode;
        public final int processedDelta;

        Snapshot(int total, long lastSuccess, boolean complete, String mode, int delta) {
            this.totalEmailsIndexed = total;
            this.lastSuccessfulSync = lastSuccess;
            this.initialSyncComplete = complete;
            this.mode = mode == null ? "" : mode;
            this.processedDelta = delta;
        }
    }
}
