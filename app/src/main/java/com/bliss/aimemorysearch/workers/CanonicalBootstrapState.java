package com.bliss.aimemorysearch.workers;

import android.content.Context;
import android.content.SharedPreferences;

import com.bliss.aimemorysearch.R;
import com.bliss.aimemorysearch.ai.canonical.CanonicalIndexStore;

/** Persisted, one-time readiness gate for the initial canonical backlog. */
public final class CanonicalBootstrapState {
    private static final String PREFERENCES = "index_state";
    private static final String KEY_INITIAL_INDEX_DONE = "first_index_done";
    private static final String KEY_BOOTSTRAP_DONE =
            "initial_canonical_enrichment_done";

    private CanonicalBootstrapState() {}

    public static boolean isComplete(Context context) {
        return preferences(context).getBoolean(KEY_BOOTSTRAP_DONE, false);
    }

    public static boolean shouldRunWhileForeground(Context context) {
        return !isComplete(context);
    }

    public static boolean evaluateCompletion(Context context) {
        SharedPreferences preferences = preferences(context);
        if (preferences.getBoolean(KEY_BOOTSTRAP_DONE, false)) {
            return true;
        }
        if (!preferences.getBoolean(KEY_INITIAL_INDEX_DONE, false)) {
            return false;
        }
        int pending;
        try (CanonicalIndexStore store = new CanonicalIndexStore(context)) {
            pending = store.countPendingFiles();
        }
        int target = Math.max(
                0,
                context.getResources().getInteger(
                        R.integer.initial_canonical_pending_target));
        if (pending > target) {
            return false;
        }
        preferences.edit().putBoolean(KEY_BOOTSTRAP_DONE, true).apply();
        return true;
    }

    public static int pendingCount(Context context) {
        try (CanonicalIndexStore store = new CanonicalIndexStore(context)) {
            return store.countPendingFiles();
        }
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(
                PREFERENCES, Context.MODE_PRIVATE);
    }
}
