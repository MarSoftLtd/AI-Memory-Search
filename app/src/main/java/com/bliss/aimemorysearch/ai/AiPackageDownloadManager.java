package com.bliss.aimemorysearch.ai;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.bliss.aimemorysearch.ai.model.AIPackageInfo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AiPackageDownloadManager {
    public enum Event {
        STARTED,
        PROGRESS,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    public enum FailureReason {
        NOT_AVAILABLE,
        NETWORK,
        HTTP,
        STORAGE
    }

    public interface Listener {
        void onDownloadEvent(DownloadEvent event);
    }

    public static final class DownloadEvent {
        private final Event event;
        private final long downloadedBytes;
        private final long totalBytes;
        private final File temporaryFile;
        private final FailureReason failureReason;

        private DownloadEvent(
                Event event,
                long downloadedBytes,
                long totalBytes,
                File temporaryFile,
                FailureReason failureReason
        ) {
            this.event = event;
            this.downloadedBytes = downloadedBytes;
            this.totalBytes = totalBytes;
            this.temporaryFile = temporaryFile;
            this.failureReason = failureReason;
        }

        public Event getEvent() { return event; }
        public long getDownloadedBytes() { return downloadedBytes; }
        public long getTotalBytes() { return totalBytes; }
        public File getTemporaryFile() { return temporaryFile; }
        public FailureReason getFailureReason() { return failureReason; }
    }

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int BUFFER_SIZE = 64 * 1024;

    private final Context context;
    private final AiPackageDownloadLocator locator = new AiPackageDownloadLocator();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Object lock = new Object();

    private volatile boolean cancelled;
    private volatile HttpURLConnection activeConnection;
    private boolean downloading;

    public AiPackageDownloadManager(Context context) {
        this.context = Objects.requireNonNull(context, "context")
                .getApplicationContext();
    }

    public boolean start(AIPackageInfo packageInfo, Listener listener) {
        Objects.requireNonNull(packageInfo, "packageInfo");
        Objects.requireNonNull(listener, "listener");
        synchronized (lock) {
            if (downloading) {
                return false;
            }
            downloading = true;
            cancelled = false;
        }
        executor.execute(() -> download(packageInfo, listener));
        return true;
    }

    public void cancel() {
        cancelled = true;
        HttpURLConnection connection = activeConnection;
        if (connection != null) {
            connection.disconnect();
        }
    }

    public boolean isDownloading() {
        synchronized (lock) {
            return downloading;
        }
    }

    public void close() {
        cancel();
        executor.shutdownNow();
    }

    private void download(AIPackageInfo packageInfo, Listener listener) {
        File temporaryFile = temporaryFile(packageInfo);
        long downloadedBytes = 0L;
        long totalBytes = Math.max(0L, packageInfo.getDownloadSizeBytes());
        post(listener, event(Event.STARTED, 0L, totalBytes, temporaryFile, null));

        try {
            deleteTemporaryFile(temporaryFile);
            File parent = temporaryFile.getParentFile();
            if (parent == null || (!parent.exists() && !parent.mkdirs())) {
                finishFailed(listener, temporaryFile, FailureReason.STORAGE);
                return;
            }

            HttpURLConnection connection = (HttpURLConnection) new URL(
                    locator.locate(packageInfo)
            ).openConnection();
            activeConnection = connection;
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                finishFailed(listener, temporaryFile, FailureReason.NOT_AVAILABLE);
                return;
            }
            if (responseCode < 200 || responseCode >= 300) {
                finishFailed(listener, temporaryFile, FailureReason.HTTP);
                return;
            }

            long contentLength = connection.getContentLengthLong();
            if (contentLength > 0L) {
                totalBytes = contentLength;
            }

            try (InputStream input = new BufferedInputStream(connection.getInputStream());
                 BufferedOutputStream output = new BufferedOutputStream(
                         new FileOutputStream(temporaryFile)
                 )) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (cancelled) {
                        finishCancelled(listener, temporaryFile);
                        return;
                    }
                    output.write(buffer, 0, count);
                    downloadedBytes += count;
                    post(listener, event(
                            Event.PROGRESS,
                            downloadedBytes,
                            totalBytes,
                            temporaryFile,
                            null
                    ));
                }
                output.flush();
            }

            if (cancelled) {
                finishCancelled(listener, temporaryFile);
            } else {
                finish(listener);
                post(listener, event(
                        Event.COMPLETED,
                        downloadedBytes,
                        totalBytes,
                        temporaryFile,
                        null
                ));
            }
        } catch (Exception failure) {
            if (cancelled) {
                finishCancelled(listener, temporaryFile);
            } else {
                finishFailed(listener, temporaryFile, FailureReason.NETWORK);
            }
        } finally {
            HttpURLConnection connection = activeConnection;
            activeConnection = null;
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private File temporaryFile(AIPackageInfo packageInfo) {
        String packageFileName = packageInfo.getPackageId()
                + "-"
                + packageInfo.getVersion()
                + ".aipackage.download";
        return new File(
                new File(context.getFilesDir(), "ai_packages/downloads"),
                packageFileName
        );
    }

    private void finishCancelled(Listener listener, File temporaryFile) {
        deleteTemporaryFile(temporaryFile);
        finish(listener);
        post(listener, event(Event.CANCELLED, 0L, 0L, null, null));
    }

    private void finishFailed(
            Listener listener,
            File temporaryFile,
            FailureReason failureReason
    ) {
        deleteTemporaryFile(temporaryFile);
        finish(listener);
        post(listener, event(Event.FAILED, 0L, 0L, null, failureReason));
    }

    private void finish(Listener listener) {
        synchronized (lock) {
            downloading = false;
        }
    }

    private static void deleteTemporaryFile(File temporaryFile) {
        if (temporaryFile.exists() && !temporaryFile.delete()) {
            temporaryFile.deleteOnExit();
        }
    }

    private void post(Listener listener, DownloadEvent event) {
        mainHandler.post(() -> listener.onDownloadEvent(event));
    }

    private static DownloadEvent event(
            Event event,
            long downloadedBytes,
            long totalBytes,
            File temporaryFile,
            FailureReason failureReason
    ) {
        return new DownloadEvent(
                event,
                downloadedBytes,
                totalBytes,
                temporaryFile,
                failureReason
        );
    }
}
