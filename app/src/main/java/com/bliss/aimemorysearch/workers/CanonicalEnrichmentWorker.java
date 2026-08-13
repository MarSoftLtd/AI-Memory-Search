package com.bliss.aimemorysearch.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.bliss.aimemorysearch.ai.canonical.CanonicalIndexStore;
import com.bliss.aimemorysearch.ai.canonical.CanonicalIndexingPipeline;
import com.bliss.aimemorysearch.db.AppDatabase;
import com.bliss.aimemorysearch.db.FileEntity;

import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

/** Durable, generation-bound secondary indexing that never owns the primary index chain. */
public final class CanonicalEnrichmentWorker extends Worker {
    private static final String TAG = "CANONICAL_INDEX";
    private static final String KEY_FILE_PATH = "canonical_file_path";
    private static final String KEY_GENERATION = "canonical_generation";
    private static final String WORK_PREFIX = "canonical_enrichment:";

    public CanonicalEnrichmentWorker(
            @NonNull Context context,
            @NonNull WorkerParameters parameters
    ) {
        super(context, parameters);
    }

    public static String newGeneration() {
        return UUID.randomUUID().toString();
    }

    public static void enqueue(Context context, String filePath, String generation) {
        enqueue(context, filePath, generation,
                CanonicalEnrichmentLifecycle.shouldPauseForForeground(context));
    }

    private static void enqueue(
            Context context,
            String filePath,
            String generation,
            boolean delayedForForeground
    ) {
        Data input = new Data.Builder()
                .putString(KEY_FILE_PATH, filePath)
                .putString(KEY_GENERATION, generation)
                .build();
        OneTimeWorkRequest.Builder builder =
                new OneTimeWorkRequest.Builder(CanonicalEnrichmentWorker.class)
                        .setInputData(input)
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,
                                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                                TimeUnit.MILLISECONDS)
                        .addTag(WORK_PREFIX + Integer.toHexString(filePath.hashCode()));
        if (delayedForForeground) {
            builder.setInitialDelay(
                    OneTimeWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS);
        }
        OneTimeWorkRequest request = builder.build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
                workName(filePath), ExistingWorkPolicy.REPLACE, request);
    }

    public static void cancel(Context context, String filePath) {
        WorkManager.getInstance(context.getApplicationContext())
                .cancelUniqueWork(workName(filePath));
    }

    static void pauseForForeground(Context context) {
        /*
         * REPLACE is the stop signal for every currently pending generation and also leaves
         * behind a durable delayed request if the process dies while the app is foreground.
         */
        schedulePending(context, true);
    }

    static void resumePending(Context context) {
        schedulePending(context, false);
    }

    private static void schedulePending(Context context, boolean delayedForForeground) {
        try (CanonicalIndexStore store =
                     new CanonicalIndexStore(context.getApplicationContext())) {
            for (java.util.Map.Entry<String, String> pending
                    : store.pendingGenerations().entrySet()) {
                enqueue(context, pending.getKey(), pending.getValue(),
                        delayedForForeground
                                || CanonicalEnrichmentLifecycle
                                .shouldPauseForForeground(context));
            }
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        String filePath = getInputData().getString(KEY_FILE_PATH);
        String generation = getInputData().getString(KEY_GENERATION);
        if (filePath == null || generation == null) {
            return Result.failure();
        }
        if (CanonicalEnrichmentLifecycle.shouldPauseForForeground(
                getApplicationContext())) {
            return Result.retry();
        }
        try (CanonicalIndexStore store = new CanonicalIndexStore(getApplicationContext())) {
            if (!store.isCurrentGeneration(filePath, generation)) {
                return Result.success();
            }
            AppDatabase database = AppDatabase.getInstance(getApplicationContext());
            FileEntity file = database.fileDao().getFileByPath(filePath);
            if (file == null) {
                store.deleteFileIfCurrent(filePath, generation);
                return Result.success();
            }
            try (CanonicalIndexingPipeline pipeline =
                         new CanonicalIndexingPipeline(getApplicationContext())) {
                pipeline.indexGeneration(filePath, generation,
                        database.chunkDao().getCanonicalChunksByFilePath(filePath),
                        () -> isStopped()
                                || CanonicalEnrichmentLifecycle
                                .shouldPauseForForeground(
                                        getApplicationContext()));
            }
            CanonicalBootstrapState.evaluateCompletion(getApplicationContext());
            return Result.success();
        } catch (CancellationException cancelled) {
            return Result.retry();
        } catch (Exception failure) {
            Log.e(TAG, "Canonical background enrichment failed for " + filePath
                    + " generation=" + generation, failure);
            return Result.retry();
        }
    }

    private static String workName(String filePath) {
        return WORK_PREFIX + UUID.nameUUIDFromBytes(
                filePath.getBytes(StandardCharsets.UTF_8));
    }
}
