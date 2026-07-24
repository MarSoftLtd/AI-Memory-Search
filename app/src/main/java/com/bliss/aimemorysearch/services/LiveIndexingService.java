package com.bliss.aimemorysearch.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Data;
import androidx.work.WorkManager;

import com.bliss.aimemorysearch.workers.IndexWorker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LiveIndexingService extends Service {
    private static final String CHANNEL_ID = "live_index_channel";
    private static final int NOTIFICATION_ID = 1001;

    private final List<FileObserver> observers =
            new ArrayList<>();

    private final Handler pollingHandler =
            new Handler();

    private long latestImageTimestamp = 0;
    private androidx.lifecycle.LiveData<List<androidx.work.WorkInfo>> workInfoLiveData;
    private final androidx.lifecycle.Observer<List<androidx.work.WorkInfo>> workObserver =
            this::updateIndexNotification;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onTimeout(int startId, int fgsType) {
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf(startId);
    }

    @Override
    public void onCreate() {

        super.onCreate();

        startForegroundServiceInternal();
        observeIndexWork();

        initializeLatestTimestamp();

        startWatching();

        startMediaStoreMonitoring();
    }

    private void initializeLatestTimestamp() {

        try {

            String[] projection = {
                    MediaStore.Images.Media.DATE_ADDED
            };

            Cursor cursor =
                    getContentResolver()
                            .query(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    projection,
                                    null,
                                    null,
                                    MediaStore.Images.Media.DATE_ADDED + " DESC"
                            );

            if (cursor == null) {
                return;
            }

            if (cursor.moveToFirst()) {

                latestImageTimestamp =
                        cursor.getLong(0);
            }

            cursor.close();

        } catch (Exception ignored) {
        }
    }

    private void startMediaStoreMonitoring() {

        pollingHandler.postDelayed(
                new Runnable() {

                    @Override
                    public void run() {

                        try {

                            checkLatestMedia();

                        } catch (Exception ignored) {
                        }

                        pollingHandler.postDelayed(
                                this,
                                3000
                        );
                    }

                },
                2000
        );
    }

    private void checkLatestMedia() {

        String[] projection = {
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATA
        };

        Cursor cursor =
                getContentResolver()
                        .query(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                projection,
                                null,
                                null,
                                MediaStore.Images.Media.DATE_ADDED + " DESC"
                        );

        if (cursor == null) {
            return;
        }

        if (cursor.moveToFirst()) {

            long latest =
                    cursor.getLong(0);

            if (latest > latestImageTimestamp) {

                latestImageTimestamp = latest;
                String path = cursor.getString(1);

                triggerIncrementalIndex(
                        path == null ? null : new File(path)
                );
            }
        }

        cursor.close();
    }

    private void startForegroundServiceInternal() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Live Indexing",
                            NotificationManager.IMPORTANCE_LOW
                    );

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );

            if (manager != null) {

                manager.createNotificationChannel(
                        channel
                );
            }
        }

        Notification notification =
                new NotificationCompat.Builder(
                        this,
                        CHANNEL_ID
                )
                        .setContentTitle(
                                "AI Memory Search"
                        )
                        .setContentText(
                                "Live indexing active"
                        )
                        .setSmallIcon(
                                android.R.drawable.ic_menu_search
                        )
                        .setOngoing(true)
                        .build();

        startForeground(
                NOTIFICATION_ID,
                notification
        );
    }

    private void observeIndexWork() {
        workInfoLiveData = WorkManager.getInstance(this)
                .getWorkInfosForUniqueWorkLiveData("ai_memory_index_worker_debug");
        workInfoLiveData.observeForever(workObserver);
    }

    private void updateIndexNotification(List<androidx.work.WorkInfo> workInfos) {
        androidx.work.WorkInfo active = null;
        if (workInfos != null) {
            for (androidx.work.WorkInfo info : workInfos) {
                if (info.getState() == androidx.work.WorkInfo.State.RUNNING) {
                    active = info;
                    break;
                }
            }
        }
        String title = "AI Memory Search";
        String content = "Watching for new files";
        int progress = 0;
        boolean showProgress = false;
        if (active != null) {
            androidx.work.Data data = active.getProgress();
            int processed = data.getInt("processed", 0);
            int total = data.getInt("total", 0);
            String phase = data.getString("stage");
            boolean reconciliation = active.getTags().contains(
                    com.bliss.aimemorysearch.workers.CanonicalReindexWorker
                            .RECONCILIATION_TAG);
            title = reconciliation
                    ? "Preparing multilingual search"
                    : "Indexing your library";
            content = phase == null || phase.isEmpty()
                    ? (processed + " of " + total)
                    : phase + (total > 0 ? " · " + processed + " of " + total : "");
            if (total > 0) {
                progress = Math.max(0, Math.min(100, processed * 100 / total));
                showProgress = true;
            }
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_menu_search)
                .setOnlyAlertOnce(true)
                .setOngoing(true);
        if (showProgress) builder.setProgress(100, progress, false);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.notify(NOTIFICATION_ID, builder.build());
    }

    private void startWatching() {

        watchFolder(
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DCIM
                )
        );

        watchFolder(
                new File(
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DCIM
                        ),
                        "Camera"
                )
        );

        watchFolder(
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES
                )
        );

        watchFolder(
                new File(
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES
                        ),
                        "Screenshots"
                )
        );

        watchFolder(
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                )
        );
    }

    private void watchFolder(
            File folder
    ) {

        if (
                folder == null
                        || !folder.exists()
        ) {
            return;
        }

        FileObserver observer =
                new FileObserver(
                        folder.getAbsolutePath(),
                        FileObserver.CREATE
                                | FileObserver.MOVED_TO
                                | FileObserver.CLOSE_WRITE
                ) {

                    @Override
                    public void onEvent(
                            int event,
                            @Nullable String path
                    ) {

                        if (path == null) {
                            return;
                        }

                        triggerIncrementalIndex(
                                new File(folder, path)
                        );
                    }
                };

        observer.startWatching();

        observers.add(
                observer
        );
    }

    private void triggerIncrementalIndex(
            File file
    ) {

        if (file == null || !file.isFile()) {
            return;
        }

        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(
                        IndexWorker.class
                )
                        .setInputData(
                                new Data.Builder()
                                        .putString(
                                                IndexWorker.KEY_FILE_PATH,
                                                file.getAbsolutePath()
                                        )
                                        .build()
                        )
                        .setInitialDelay(
                                1200,
                                TimeUnit.MILLISECONDS
                        )
                        .build();

        WorkManager
                .getInstance(this)
                .enqueueUniqueWork(
                        "ai_memory_index_worker_debug",
                        androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE,
                        request
                );
    }

    @Override
    public void onDestroy() {

        if (workInfoLiveData != null) {
            workInfoLiveData.removeObserver(workObserver);
        }

        super.onDestroy();

        for (FileObserver observer : observers) {

            observer.stopWatching();
        }

        pollingHandler.removeCallbacksAndMessages(
                null
        );

        observers.clear();
    }

    @Nullable
    @Override
    public IBinder onBind(
            Intent intent
    ) {
        return null;
    }
}
