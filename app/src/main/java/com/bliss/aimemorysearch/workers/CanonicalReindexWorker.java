package com.bliss.aimemorysearch.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.UUID;

/** Coordinates targeted canonical reconciliation through the index-worker ownership chain. */
public final class CanonicalReindexWorker extends Worker {
    private static final String LEGACY_KEY_FAMILY = "translation_family";

    public CanonicalReindexWorker(
            @NonNull Context context,
            @NonNull WorkerParameters parameters
    ) {
        super(context, parameters);
    }

    public static UUID enqueue(Context context, String family) {
        if (family == null || family.trim().isEmpty()) {
            throw new IllegalArgumentException("translation family is required");
        }
        Data input = new Data.Builder()
                .putString(IndexWorker.KEY_RECONCILIATION_FAMILY, family)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(IndexWorker.class)
                .setInputData(input)
                .build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
                IndexWorker.UNIQUE_WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request);
        return request.getId();
    }

    /** Migrates already-persisted requests created before reconciliation was delegated. */
    @NonNull
    @Override
    public Result doWork() {
        String family = getInputData().getString(LEGACY_KEY_FAMILY);
        if (family == null || family.trim().isEmpty()) {
            return Result.failure();
        }
        enqueue(getApplicationContext(), family);
        return Result.success();
    }
}
