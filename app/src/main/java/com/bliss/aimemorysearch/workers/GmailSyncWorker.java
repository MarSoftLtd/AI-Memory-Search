package com.bliss.aimemorysearch.workers;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.bliss.aimemorysearch.ai.DocumentRuntimeLoader;
import com.bliss.aimemorysearch.db.AppDatabase;
import com.bliss.aimemorysearch.email.EmailMessage;
import com.bliss.aimemorysearch.email.EmailRepository;
import com.bliss.aimemorysearch.email.EmailSource;
import com.bliss.aimemorysearch.email.EmailSyncState;
import com.bliss.aimemorysearch.email.gmail.GmailAuthorization;
import com.bliss.aimemorysearch.email.gmail.GmailEmailSource;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.tasks.Tasks;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class GmailSyncWorker extends Worker {
    private static final java.util.concurrent.atomic.AtomicBoolean SCHEDULING =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    public static final String UNIQUE_WORK_NAME = "gmail_email_sync";
    public static final String PREFS = "gmail_sync_state";
    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_STATUS = "gmail_status";
    public static final String KEY_PROCESSED = "gmail_processed";
    public static final String KEY_TOTAL = "email_total";
    public static final String KEY_REQUIRES_AUTH = "gmail_requires_auth";
    private static final String KEY_ACCOUNT = "account";
    private static final String KEY_HISTORY = "history_id";
    static final String KEY_INITIAL_SYNC_COMPLETE = "initial_sync_complete";
    private static final String KEY_FULL_PAGE = "full_page";
    private static final String KEY_FULL_HISTORY = "full_history";

    public GmailSyncWorker(@NonNull Context context,
                           @NonNull WorkerParameters parameters) {
        super(context, parameters);
    }

    public static void enqueue(Context context) {
        if (!SCHEDULING.compareAndSet(false, true)) return;
        Context applicationContext = context.getApplicationContext();
        WorkManager workManager = WorkManager.getInstance(applicationContext);
        com.google.common.util.concurrent.ListenableFuture<java.util.List<androidx.work.WorkInfo>>
                existing = workManager.getWorkInfosForUniqueWork(UNIQUE_WORK_NAME);
        existing.addListener(() -> {
            try {
                for (androidx.work.WorkInfo workInfo : existing.get()) {
                    if (workInfo.getState() == androidx.work.WorkInfo.State.ENQUEUED
                            || workInfo.getState() == androidx.work.WorkInfo.State.RUNNING
                            || workInfo.getState() == androidx.work.WorkInfo.State.BLOCKED) {
                        SCHEDULING.set(false);
                        return;
                    }
                }
                enqueueNew(workManager);
                SCHEDULING.set(false);
            } catch (Exception ignored) {
                // A later app start can safely retry scheduling.
                SCHEDULING.set(false);
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(applicationContext));
    }

    private static void enqueueNew(WorkManager workManager) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED).build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(GmailSyncWorker.class)
                .setConstraints(constraints).build();
        workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request);
    }

    @NonNull @Override public Result doWork() {
        AppDatabase database = AppDatabase.getInstance(getApplicationContext());
        SharedPreferences preferences = getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        EmailSyncState persistentState = new EmailSyncState(preferences);
        int startTotal = database.emailDao().count();
        persistentState.progress(startTotal, 0);
        publish("Checking Email", startTotal, 0);
        try {
            publish("Authorizing Email", startTotal, 0);
            AuthorizationResult authorization = Tasks.await(
                    GmailAuthorization.authorize(
                            GmailAuthorization.client(getApplicationContext())),
                    30, TimeUnit.SECONDS);
            if (authorization.hasResolution() || authorization.getAccessToken() == null) {
                return Result.failure(new Data.Builder()
                        .putBoolean(KEY_REQUIRES_AUTH, true)
                        .putString(KEY_STATUS, "Email authorization required").build());
            }

            GmailEmailSource source = new GmailEmailSource(authorization.getAccessToken());
            GmailEmailSource.Profile profile = source.getProfile();
            if (profile.emailAddress == null || profile.emailAddress.isEmpty()) {
                return Result.failure(status("Email account unavailable", startTotal, 0));
            }
            String storedAccount = preferences.getString(KEY_ACCOUNT, "");
            if (!storedAccount.isEmpty() && !storedAccount.equals(profile.emailAddress)) {
                preferences.edit().remove(KEY_FULL_PAGE).remove(KEY_FULL_HISTORY)
                        .remove(KEY_HISTORY).remove(KEY_INITIAL_SYNC_COMPLETE).apply();
            }
            String storedHistory = storedAccount.equals(profile.emailAddress)
                    ? preferences.getString(KEY_HISTORY, "") : "";
            boolean initialSyncComplete = storedAccount.equals(profile.emailAddress)
                    && preferences.getBoolean(KEY_INITIAL_SYNC_COMPLETE, false);

            DocumentRuntimeLoader loader = DocumentRuntimeLoader.createDefault();
            loader.loadEmbeddingRuntime(getApplicationContext());
            EmailRepository repository = new EmailRepository(database, loader.getEmbeddingRuntime());

            SyncResult sync;
            String mode;
            if (!shouldUseIncremental(initialSyncComplete, storedHistory)) {
                mode = EmailSyncState.MODE_FULL;
                persistentState.syncStarted(mode, startTotal);
                sync = fullSync(source, repository, profile.emailAddress,
                        profile.historyId, preferences, database, persistentState, startTotal);
            } else {
                mode = EmailSyncState.MODE_INCREMENTAL;
                persistentState.syncStarted(mode, startTotal);
                try {
                    sync = incrementalSync(source, repository, profile.emailAddress, storedHistory,
                            database, persistentState, startTotal);
                } catch (GmailEmailSource.GmailApiException expired) {
                    if (expired.statusCode != 404) throw expired;
                    mode = EmailSyncState.MODE_FULL;
                    persistentState.syncStarted(mode, startTotal);
                    sync = fullSync(source, repository, profile.emailAddress,
                            profile.historyId, preferences, database, persistentState, startTotal);
                }
            }
            int finalTotal = database.emailDao().count();
            long successfulAt = System.currentTimeMillis();
            preferences.edit().putString(KEY_ACCOUNT, profile.emailAddress)
                    .putString(KEY_HISTORY, sync.historyId)
                    .putBoolean(KEY_INITIAL_SYNC_COMPLETE, true)
                    .remove(KEY_FULL_PAGE).remove(KEY_FULL_HISTORY)
                    .putLong(EmailSyncState.KEY_LAST_SUCCESS, successfulAt).apply();
            persistentState.succeeded(finalTotal, successfulAt);
            android.util.Log.i("EMAIL_SYNC", "mode=" + mode + " startTotal=" + startTotal
                    + " processedDelta=" + sync.processed + " finalTotal=" + finalTotal
                    + " reason=" + sync.reason);
            return Result.success(status("Email synchronization complete", finalTotal,
                    sync.processed));
        } catch (GmailEmailSource.GmailApiException apiError) {
            if (apiError.statusCode == 401 || apiError.statusCode == 403) {
                return Result.failure(new Data.Builder()
                        .putBoolean(KEY_REQUIRES_AUTH, true)
                        .putString(KEY_STATUS, "Email authorization expired").build());
            }
            return Result.retry();
        } catch (java.util.concurrent.ExecutionException authorizationError) {
            return Result.failure(new Data.Builder()
                    .putBoolean(KEY_REQUIRES_AUTH, true)
                    .putString(KEY_STATUS, "Email authorization required").build());
        } catch (Exception error) {
            // Existing local email rows are intentionally retained on auth/network failures.
            return Result.retry();
        }
    }

    private SyncResult fullSync(GmailEmailSource source, EmailRepository repository,
                                String account, String historyId,
                                SharedPreferences preferences, AppDatabase database,
                                EmailSyncState state, int startTotal) throws Exception {
        int processed = 0;
        String pageToken = preferences.getString(KEY_FULL_PAGE, null);
        String baselineHistory = preferences.getString(KEY_FULL_HISTORY, "");
        if (baselineHistory == null || baselineHistory.isEmpty()) {
            baselineHistory = historyId;
            preferences.edit().putString(KEY_FULL_HISTORY, baselineHistory).apply();
        }
        do {
            EmailSource.Page page = source.fetchMessages(pageToken, 100);
            for (EmailMessage message : page.getMessages()) {
                if (isStopped()) throw new InterruptedException("Email synchronization stopped");
                repository.upsert("gmail", account, message);
                processed++;
                int currentTotal = database.emailDao().count();
                state.progress(currentTotal, processed);
                publish("Indexing Email", currentTotal, processed);
            }
            pageToken = page.getContinuationToken();
            SharedPreferences.Editor checkpoint = preferences.edit();
            if (pageToken == null) {
                checkpoint.remove(KEY_FULL_PAGE).remove(KEY_FULL_HISTORY)
                        .putString(KEY_ACCOUNT, account)
                        .putString(KEY_HISTORY, baselineHistory)
                        .putBoolean(KEY_INITIAL_SYNC_COMPLETE, true);
            } else {
                checkpoint.putString(KEY_FULL_PAGE, pageToken);
            }
            checkpoint.apply();
        } while (pageToken != null);
        return new SyncResult(processed, baselineHistory, "initial_or_invalid_checkpoint");
    }

    private SyncResult incrementalSync(GmailEmailSource source, EmailRepository repository,
                                       String account, String startHistoryId,
                                       AppDatabase database, EmailSyncState state,
                                       int startTotal) throws Exception {
        int processed = 0;
        String pageToken = null;
        String latestHistoryId = startHistoryId;
        Set<String> added = new LinkedHashSet<>();
        Set<String> deleted = new LinkedHashSet<>();
        do {
            GmailEmailSource.HistoryPage page = source.fetchHistory(startHistoryId, pageToken);
            added.addAll(page.addedMessageIds);
            deleted.addAll(page.deletedMessageIds);
            latestHistoryId = page.historyId;
            pageToken = page.nextPageToken;
        } while (pageToken != null);
        added.removeAll(deleted);
        for (String messageId : deleted) {
            repository.delete("gmail", account, messageId);
            processed++;
            int currentTotal = database.emailDao().count();
            state.progress(currentTotal, processed);
            publish("Removing deleted Email messages", currentTotal, processed);
        }
        for (String messageId : added) {
            repository.upsert("gmail", account, source.fetchMessage(messageId));
            processed++;
            int currentTotal = database.emailDao().count();
            state.progress(currentTotal, processed);
            publish("Indexing Email changes", currentTotal, processed);
        }
        int finalTotal = database == null ? startTotal : database.emailDao().count();
        if (state != null) {
            state.progress(finalTotal, processed);
            publish(processed == 0 ? "Email is up to date" : "Email changes indexed",
                    finalTotal, processed);
        }
        return new SyncResult(processed, latestHistoryId,
                processed == 0 ? "no_changes" : "history_changes");
    }

    private void publish(String status, int total, int delta) {
        setProgressAsync(status(status, total, delta));
    }

    static boolean shouldUseIncremental(boolean initialSyncComplete, String historyId) {
        return initialSyncComplete && historyId != null && !historyId.trim().isEmpty();
    }

    private static Data status(String status, int total, int delta) {
        return new Data.Builder().putString(KEY_STATUS, status)
                .putInt(KEY_TOTAL, total).putInt(KEY_PROCESSED, delta).build();
    }

    private static final class SyncResult {
        final int processed;
        final String historyId;
        final String reason;
        SyncResult(int processed, String historyId, String reason) {
            this.processed = processed;
            this.historyId = historyId == null ? "" : historyId;
            this.reason = reason;
        }
    }
}
