package com.bliss.aimemorysearch.email;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.LiveData;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.bliss.aimemorysearch.email.gmail.GmailAuthorization;
import com.bliss.aimemorysearch.workers.GmailSyncWorker;
import com.google.android.gms.auth.api.identity.AuthorizationClient;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.util.List;

/** Provider-neutral entry point used by the main UI. Gmail is the first internal provider. */
public final class EmailSyncCoordinator {
    private EmailSyncCoordinator() {}

    public static AuthorizationClient authorizationClient(Activity activity) {
        return GmailAuthorization.client(activity);
    }

    public static Task<AuthorizationResult> authorize(AuthorizationClient client) {
        return GmailAuthorization.authorize(client);
    }

    public static AuthorizationResult authorizationResult(
            AuthorizationClient client, Intent data) throws ApiException {
        return client.getAuthorizationResultFromIntent(data);
    }

    public static boolean isEnabled(Context context) {
        return context.getSharedPreferences(GmailSyncWorker.PREFS, Context.MODE_PRIVATE)
                .getBoolean(GmailSyncWorker.KEY_ENABLED, false);
    }

    public static void enableAndSync(Context context) {
        context.getSharedPreferences(GmailSyncWorker.PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(GmailSyncWorker.KEY_ENABLED, true).apply();
        GmailSyncWorker.enqueue(context);
    }

    public static void syncIfEnabled(Context context) {
        if (isEnabled(context)) GmailSyncWorker.enqueue(context);
    }

    public static LiveData<List<WorkInfo>> observe(Context context) {
        return WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkLiveData(GmailSyncWorker.UNIQUE_WORK_NAME);
    }

    public static String status(WorkInfo info) {
        return info.getProgress().getString(GmailSyncWorker.KEY_STATUS);
    }

    public static int processed(WorkInfo info) {
        return info.getProgress().getInt(GmailSyncWorker.KEY_TOTAL,
                snapshotTotal(info));
    }

    private static int snapshotTotal(WorkInfo info) {
        return info.getOutputData().getInt(GmailSyncWorker.KEY_TOTAL, 0);
    }

    public static EmailSyncState.Snapshot snapshot(Context context) {
        return EmailSyncState.from(context, GmailSyncWorker.PREFS).read();
    }

    public static WorkInfo currentWork(List<WorkInfo> workInfos) {
        if (workInfos == null || workInfos.isEmpty()) return null;
        for (WorkInfo workInfo : workInfos) {
            if (workInfo.getState() == WorkInfo.State.RUNNING
                    || workInfo.getState() == WorkInfo.State.ENQUEUED
                    || workInfo.getState() == WorkInfo.State.BLOCKED) return workInfo;
        }
        return workInfos.get(workInfos.size() - 1);
    }
}
