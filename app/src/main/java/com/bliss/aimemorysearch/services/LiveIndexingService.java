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
import androidx.work.WorkManager;

import com.bliss.aimemorysearch.workers.IndexWorker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LiveIndexingService extends Service {

    private final List<FileObserver> observers =
            new ArrayList<>();

    private boolean indexingRunning = false;

    private final Handler pollingHandler =
            new Handler();

    private final Handler mainHandler =
            new Handler();

    private long latestImageTimestamp = 0;

    @Override
    public void onCreate() {

        super.onCreate();

        startForegroundServiceInternal();

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

            long latest =
                    cursor.getLong(0);

            if (latest > latestImageTimestamp) {

                latestImageTimestamp = latest;

                triggerIncrementalIndex();
            }
        }

        cursor.close();
    }

    private void startForegroundServiceInternal() {

        String channelId =
                "live_index_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            channelId,
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
                        channelId
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
                1001,
                notification
        );
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

                        triggerIncrementalIndex();
                    }
                };

        observer.startWatching();

        observers.add(
                observer
        );
    }

    private synchronized void triggerIncrementalIndex() {

        if (indexingRunning) {
            return;
        }

        indexingRunning = true;

        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(
                        IndexWorker.class
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
                        androidx.work.ExistingWorkPolicy.REPLACE,
                        request
                );

        mainHandler.postDelayed(
                () -> indexingRunning = false,
                2000
        );
    }

    @Override
    public void onDestroy() {

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