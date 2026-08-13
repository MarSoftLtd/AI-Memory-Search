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
    public static final String RECONCILIATION_TAG =
            "canonical_package_reconciliation";

    public CanonicalReindexWorker(
            @NonNull Context context,
            @NonNull WorkerParameters parameters
    ) {
        super(context, parameters);
    }

    public static UUID enqueue(Context context, String family) {
        return enqueue(context, family, UUID.randomUUID());
    }

    public static UUID enqueue(Context context, String family, UUID workId) {
        if (family == null || family.trim().isEmpty()) {
            throw new IllegalArgumentException("translation family is required");
        }
        if (workId == null) {
            throw new IllegalArgumentException("work id is required");
        }
        Data input = new Data.Builder()
                .putString(IndexWorker.KEY_RECONCILIATION_FAMILY, family)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(IndexWorker.class)
                .setId(workId)
                .setInputData(input)
                .addTag(RECONCILIATION_TAG)
                .build();
        WorkManager.getInstance(
                context.getApplicationContext()).enqueueUniqueWork(
                IndexWorker.UNIQUE_WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request);
        return workId;
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
