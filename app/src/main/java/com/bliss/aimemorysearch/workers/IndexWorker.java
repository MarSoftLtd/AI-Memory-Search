package com.bliss.aimemorysearch.workers;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.os.Environment;

import com.bliss.aimemorysearch.ai.EmbeddingEngine;
import com.bliss.aimemorysearch.ai.EmbeddingUtils;
import com.bliss.aimemorysearch.ai.DocumentRuntimeLoader;
import com.bliss.aimemorysearch.ai.EntityExtractionEngine;
import com.bliss.aimemorysearch.ai.ImageEmbeddingEngine;
import com.bliss.aimemorysearch.ai.ImageRuntimeLoader;
import com.bliss.aimemorysearch.ai.MobileClipTextEmbeddingEngine;
import com.bliss.aimemorysearch.ai.OcrRuntimeLoader;
import com.bliss.aimemorysearch.ai.PrefixVocabularyCache;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import java.io.File;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.bliss.aimemorysearch.db.AppDatabase;
import com.bliss.aimemorysearch.db.FileEntity;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import com.bliss.aimemorysearch.ai.EmbeddingEngine;
import com.bliss.aimemorysearch.ai.EmbeddingUtils;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;

import java.io.FileOutputStream;
import com.bliss.aimemorysearch.ai.TextChunker;
import com.bliss.aimemorysearch.db.ChunkEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import com.bliss.aimemorysearch.ai.DocumentTextExtractor;
import com.bliss.aimemorysearch.ai.EmbeddingEngine;
import com.bliss.aimemorysearch.ai.EmbeddingUtils;
import com.bliss.aimemorysearch.ai.MiniLMTokenizer;
import com.bliss.aimemorysearch.db.TokenIndexEntity;
import java.util.HashSet;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.bliss.aimemorysearch.ai.ImageEmbeddingEngine;
import com.bliss.aimemorysearch.ai.canonical.CanonicalIndexingPipeline;
import com.bliss.aimemorysearch.ai.canonical.CanonicalIndexStore;
import com.bliss.aimemorysearch.indexing.FileIndexingPolicy;
public class IndexWorker extends Worker {
    public static final String UNIQUE_WORK_NAME = "ai_memory_index_worker_debug";
    public static final String KEY_FILE_PATH = "file_path";
    public static final String KEY_MAINTENANCE = "maintenance";
    public static final String KEY_RECONCILIATION_FAMILY = "reconciliation_family";
    public static final String KEY_POSTPONED_RETRY = "postponed_retry";
    public static final String USER_SKIP_PREFS = "index_user_skips";
    private static final String POSTPONED_FILE_PREFS = "index_postponed_files";
    private static final String POSTPONED_PATHS_KEY = "paths";
    private static final String CHECKPOINT_PREFIX = "resume_checkpoint.";
    public static final String KEY_DECISION_TOKEN = "decision_token";
    public static final String KEY_AWAITING_DECISION = "awaiting_decision";
    public static final String KEY_CURRENT_OPERATION = "current_operation";
    public static final String KEY_OPERATION_ELAPSED_MS = "operation_elapsed_ms";
    public static final String KEY_PROGRESS_RUN_ATTEMPT = "progress_run_attempt";
    private static final String PDF_TIMING_TAG = "PDF_PIPELINE_TIMING";
    private static final int CANONICAL_RECONCILIATION_BATCH_SIZE = 64;
    private static final String AI_PACKAGE_STAGE_DIRECTORY =
            "aimemory-package-stage";
    private static final FileIndexingPolicy FILE_INDEXING_POLICY =
            new FileIndexingPolicy();

    private AppDatabase database;
    private SharedPreferences prefs;
    private int newFilesIndexed = 0;
    private String sessionId;
    private final Set<String> scannedPaths =
            new HashSet<>();
    private final ExecutorService ocrExecutor =
            Executors.newFixedThreadPool(4);
    private int pdfCount = 0;
    private int jpgCount = 0;
    private int pngCount = 0;
    private int webpCount = 0;
    private int jpegCount = 0;
    private int ocrCount = 0;
    private int embeddingCount = 0;
    private int documentTotal = 0;
    private int imageTotal = 0;
    private int ocrEstimatedTotal = 0;
    private int embeddingEstimatedTotal = 0;
    private int documentProcessed = 0;
    private int imageProcessed = 0;
    private int libraryDocumentTotal = 0;
    private int totalFilesToIndex = 0;
    private int processedFiles = 0;
    private int failedFiles = 0;
    private int userSkippedFiles = 0;
    private long workerStartedAt;
    private String progressFileName = "";
    private long progressFileSize = 0L;
    private long currentFileStartedAt;
    private String approvedLongFileToken = "";
    private String currentStage = "Preparing...";
    private String currentFileTerminalState = "";
    private String currentFileTerminalReason = "";
    private String lastPolicySkipReason = "";
    private boolean filePostponedThisAttempt;
    private boolean fullScanCheckpointEnabled;
    private boolean resumeCheckpointPending;
    private String resumeCheckpointPath = "";
    private boolean restoredFullScanCheckpoint;
    private boolean firstResumedFileLogged;
    private long workerStartedElapsedRealtime;
    private long lastDiscoveryProgressAt;
    private CanonicalIndexingPipeline canonicalIndexingPipeline;
    private static final class UserSkippedFileException extends Exception {
        UserSkippedFileException() {
            super("Current file skipped by user");
        }
    }
    private static final class PostponedFileException extends Exception {
        PostponedFileException(File file, String operation) {
            super("Postponed long file during " + operation + ": "
                    + (file == null ? "" : file.getAbsolutePath()));
        }
    }
    private static final class StartupOperationException
            extends RuntimeException {
        StartupOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    public IndexWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams
    ) {
        super(context, workerParams);
    }
    @NonNull
    @Override
    public Result doWork() {

        workerStartedAt = System.currentTimeMillis();
        workerStartedElapsedRealtime = android.os.SystemClock.elapsedRealtime();
        setProgressAsync(new androidx.work.Data.Builder()
                .putInt(KEY_PROGRESS_RUN_ATTEMPT, getRunAttemptCount())
                .build());
        sendIndexProgress("Starting index worker...", "");
        try {
            return runIndexWork();
        } catch (Exception error) {
            if (cancellationRequested()) {
                return finishCancelledIndexing();
            }
            currentFileTerminalState = "failed";
            currentFileTerminalReason = failureReason(error);
            sendIndexProgress("Index worker failed", progressFileName);
            android.util.Log.e(
                    "INDEX_WORKER",
                    "Unhandled index worker failure",
                    error
            );
            if (error instanceof StartupOperationException
                    && getRunAttemptCount() < 2) {
                return Result.retry();
            }
            return Result.failure(new androidx.work.Data.Builder()
                    .putString("reason", currentFileTerminalReason)
                    .build());
        }
    }

    private Result runIndexWork() {

        sendIndexProgress("Opening index database...", "");
        database = AppDatabase.getInstance(
                getApplicationContext()
        );
        prefs = getApplicationContext()
                .getSharedPreferences(
                        "index_state",
                        Context.MODE_PRIVATE
                );

        if (getInputData().getBoolean(KEY_MAINTENANCE, false)) {
            repairLegacyIndexRows();
            return Result.success();
        }

        String reconciliationFamily =
                getInputData().getString(KEY_RECONCILIATION_FAMILY);
        if (reconciliationFamily != null
                && !reconciliationFamily.trim().isEmpty()) {
            return reconcileCanonicalFamily(reconciliationFamily);
        }

        if (cancellationRequested()) {
            return finishCancelledIndexing();
        }

        sessionId = getId().toString();
        String incrementalFilePath =
                getInputData().getString(KEY_FILE_PATH);
        fullScanCheckpointEnabled = incrementalFilePath == null
                || incrementalFilePath.trim().isEmpty();
        restoredFullScanCheckpoint =
                fullScanCheckpointEnabled && restoreFullScanCheckpoint();

        runStartupOperation("Loading PDF runtime...", () -> {
            PDFBoxResourceLoader.init(getApplicationContext());
            return null;
        });
        runStartupOperation("Loading document AI model...", () -> {
            if (!EmbeddingEngine.getInstance().isInitialized()) {
                DocumentRuntimeLoader.createDefault()
                        .loadEmbeddingRuntime(getApplicationContext());
            }
            return null;
        });
        runStartupOperation("Loading image AI model...", () -> {
            if (!ImageEmbeddingEngine.getInstance().isInitialized()) {
                ImageRuntimeLoader.createDefault()
                        .loadImageEmbeddingRuntime(getApplicationContext());
            }
            return null;
        });
        runStartupOperation("Loading image text model...", () -> {
            if (!MobileClipTextEmbeddingEngine.getInstance().isInitialized()) {
                ImageRuntimeLoader.createDefault()
                        .loadTextEmbeddingRuntime(getApplicationContext());
            }
            return null;
        });
        runStartupOperation("Loading tokenizer...", () -> {
            if (!MiniLMTokenizer.getInstance().isInitialized()) {
                MiniLMTokenizer.getInstance().initialize(getApplicationContext());
            }
            return null;
        });
        if (cancellationRequested()) {
            return finishCancelledIndexing();
        }

        if (
                incrementalFilePath != null
                        &&
                        !incrementalFilePath.trim().isEmpty()
        ) {
            boolean postponedRetry =
                    getInputData().getBoolean(KEY_POSTPONED_RETRY, false);
            File incrementalFile =
                    new File(incrementalFilePath);

            totalFilesToIndex = 1;
            processedFiles = 0;
            processSingleFile(incrementalFile);

            if (cancellationRequested()) {
                return finishCancelledIndexing();
            }
            if (postponedRetry && filePostponedThisAttempt) {
                closeCanonicalIndexingPipeline();
                if (getRunAttemptCount() >= 2) {
                    failedFiles++;
                    currentFileTerminalState = "failed";
                    currentFileTerminalReason =
                            "Per-file time limit exceeded on postponed retry";
                    sendIndexProgress(
                            "Postponed file failed after retry limit",
                            incrementalFile.getName()
                    );
                    clearPostponedFile(incrementalFile);
                    return Result.success(new androidx.work.Data.Builder()
                            .putString("reason", currentFileTerminalReason)
                            .putInt("failed", failedFiles)
                            .build());
                }
                return Result.retry();
            }

            if (!closeCanonicalIndexingPipeline()
                    || failedFiles != 0) {
                if (postponedRetry) {
                    clearPostponedFile(incrementalFile);
                }
                return Result.success(new androidx.work.Data.Builder()
                        .putString("reason", currentFileTerminalReason)
                        .putInt("failed", Math.max(1, failedFiles))
                        .build());
            }

            if (postponedRetry) {
                clearPostponedFile(incrementalFile);
            }
            return Result.success();
        }

        database.fileDao()
                .resetInterruptedFiles();
        File rootFolder =
                Environment
                        .getExternalStorageDirectory();

        boolean restoredCheckpoint = restoredFullScanCheckpoint;
        if (!restoredCheckpoint) {
            totalFilesToIndex = 0;
            processedFiles = 0;

            sendIndexProgress(
                    "Scanning filesystem...",
                    "Preparing AI indexing..."
            );

            countFilesRecursive(
                    rootFolder
            );

            if (cancellationRequested()) {
                return finishCancelledIndexing();
            }
        }

        sendIndexProgress(
                restoredCheckpoint
                        ? "Checkpoint restored"
                        : "Filesystem scan completed",
                totalFilesToIndex
                        + " files discovered"
        );

        android.util.Log.d(
                "INDEX_COUNT",
                "TOTAL FILES TO INDEX = "
                        + totalFilesToIndex
        );

        scanFolderRecursive(
                rootFolder
        );
        if (resumeCheckpointPending && !cancellationRequested()) {
            android.util.Log.i(
                    "INDEX_RESUME",
                    "Checkpoint was the final traversable file"
                            + " | workId=" + getId()
                            + " | path=" + resumeCheckpointPath
            );
            resumeCheckpointPending = false;
        }
        libraryDocumentTotal = documentTotal;
        prefs.edit().putInt("last_discovered_document_total", documentTotal).apply();
        if (cancellationRequested()) {
            return finishCancelledIndexing();
        }
        boolean completedSuccessfully = true;
        prefs.edit()
                .putLong(
                        "last_index_time",
                        System.currentTimeMillis()
                )
                .putInt(
                        "last_indexed_count",
                        database.fileDao().countIndexed()
                )
                .putBoolean(
                        "first_index_done",
                        true
                )
                .apply();

        setProgressAsync(
                new androidx.work.Data.Builder()
                        .putInt(KEY_PROGRESS_RUN_ATTEMPT, getRunAttemptCount())
                        .putString(
                                "status",
                                failedFiles > 0
                                        ? "Index completed with "
                                        + failedFiles + " failed files"
                                        : newFilesIndexed == 0
                                        ? "No new files found"
                                        : "Index completed successfully"
                        )
                        .putInt(
                                "processed",
                                newFilesIndexed
                        )
                        .putInt(
                                "total",
                                newFilesIndexed
                        )
                        .putInt("failed", failedFiles)
                        .build()
        );

        completedSuccessfully =
                closeCanonicalIndexingPipeline()
                        && completedSuccessfully;

        if (completedSuccessfully) {
            clearFullScanCheckpoint();
        }

        return Result.success(
                new androidx.work.Data.Builder()
                        .putInt(
                                "failed",
                                failedFiles + (completedSuccessfully ? 0 : 1)
                        )
                        .build()
        );
        
    }
    private void scanPdfFiles() {

        if (cancellationRequested()) {
            return;
        }

        String[] projection = {
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.MIME_TYPE
        };

        String selection =
                MediaStore.Files.FileColumns.DISPLAY_NAME
                        + " LIKE ?";

        String[] selectionArgs = {
                "%.pdf"
        };

        Cursor cursor =
                getApplicationContext()
                        .getContentResolver()
                        .query(
                                MediaStore.Files.getContentUri("external"),
                                projection,
                                selection,
                                selectionArgs,
                                MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
                        );

        if (cursor == null) {
            return;
        }

        int pathColumn =
                cursor.getColumnIndexOrThrow(
                        MediaStore.Files.FileColumns.DATA
                );

        int nameColumn =
                cursor.getColumnIndexOrThrow(
                        MediaStore.Files.FileColumns.DISPLAY_NAME
                );

        while (cursor.moveToNext()) {

            if (cancellationRequested()) {
                break;
            }

            String path =
                    cursor.getString(pathColumn);

            String name =
                    cursor.getString(nameColumn);

            if (path == null || name == null) {
                continue;
            }

            File file = new File(path);

            if (!file.exists()) {
                continue;
            }

            if (!name.toLowerCase().endsWith(".pdf")) {
                continue;
            }

            processPdf(file);
        }

        cursor.close();
    }
    private String generatePdfThumbnail(
            String pdfPath
    ) {

        try {

            File pdfFile =
                    new File(pdfPath);

            if (!pdfFile.exists()) {
                return null;
            }

            ParcelFileDescriptor fd =
                    ParcelFileDescriptor.open(
                            pdfFile,
                            ParcelFileDescriptor.MODE_READ_ONLY
                    );

            PdfRenderer renderer =
                    new PdfRenderer(fd);

            if (renderer.getPageCount() <= 0) {

                renderer.close();
                fd.close();

                return null;
            }

            PdfRenderer.Page page =
                    renderer.openPage(0);

            Bitmap bitmap =
                    Bitmap.createBitmap(
                            page.getWidth(),
                            page.getHeight(),
                            Bitmap.Config.ARGB_8888
                    );

            page.render(
                    bitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            );

            File thumbsDir =
                    new File(
                            getApplicationContext()
                                    .getFilesDir(),
                            "pdf_thumbs"
                    );

            if (!thumbsDir.exists()) {

                thumbsDir.mkdirs();
            }

            String thumbName =
                    System.currentTimeMillis()
                            + ".jpg";

            File thumbFile =
                    new File(
                            thumbsDir,
                            thumbName
                    );

            FileOutputStream out =
                    new FileOutputStream(
                            thumbFile
                    );

            bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    82,
                    out
            );

            out.flush();
            out.close();

            page.close();
            renderer.close();
            fd.close();

            bitmap.recycle();

            android.util.Log.d(
                    "PDF_THUMB",
                    "THUMB CREATED = "
                            + thumbFile.getAbsolutePath()
            );

            return thumbFile.getAbsolutePath();

        } catch (Exception e) {

            android.util.Log.e(
                    "PDF_THUMB",
                    "THUMB FAILED",
                    e
            );

            return null;
        }
    }
    private String runPdfOcr(File pdfFile) {

        try {

            ParcelFileDescriptor fd =
                    ParcelFileDescriptor.open(
                            pdfFile,
                            ParcelFileDescriptor.MODE_READ_ONLY
                    );

            PdfRenderer renderer =
                    new PdfRenderer(fd);

            if (renderer.getPageCount() == 0) {

                renderer.close();
                fd.close();

                return "";
            }

            PdfRenderer.Page page =
                    renderer.openPage(0);

            Bitmap bitmap =
                    Bitmap.createBitmap(
                            page.getWidth() * 2,
                            page.getHeight() * 2,
                            Bitmap.Config.ARGB_8888
                    );

            page.render(
                    bitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            );

            InputImage image =
                    InputImage.fromBitmap(
                            bitmap,
                            0
                    );

            final String[] result = {""};

            CountDownLatch latch =
                    new CountDownLatch(1);

            OcrRuntimeLoader
                    .createDefault()
                    .loadRuntime()
                    .process(image)
                    .addOnSuccessListener(text -> {

                        result[0] = text.getText();

                        latch.countDown();
                    })
                    .addOnFailureListener(e -> {

                        latch.countDown();
                    });

            latch.await(
                    20,
                    TimeUnit.SECONDS
            );

            page.close();
            renderer.close();
            fd.close();

            bitmap.recycle();

            return result[0];

        } catch (Exception e) {

            e.printStackTrace();
        }

        return "";
    }
    private String runOcrBlocking(String imagePath) {

        final String[] resultText = {""};

        try {

            BitmapFactory.Options options =
                    new BitmapFactory.Options();

            options.inSampleSize = 4;

            Bitmap bitmap =
                    BitmapFactory.decodeFile(
                            imagePath,
                            options
                    );

            if (bitmap == null) {
                return "";
            }

            InputImage image =
                    InputImage.fromBitmap(
                            bitmap,
                            0
                    );

            CountDownLatch latch =
                    new CountDownLatch(1);

            OcrRuntimeLoader
                    .createDefault()
                    .loadRuntime()
                    .process(image)
                    .addOnSuccessListener(text -> {

                        resultText[0] =
                                text.getText();

                        bitmap.recycle();

                        latch.countDown();
                    })
                    .addOnFailureListener(e -> {

                        bitmap.recycle();

                        latch.countDown();
                    });

            latch.await(
                    15,
                    TimeUnit.SECONDS
            );

        } catch (Exception e) {

            e.printStackTrace();
        }

        return resultText[0];
    }
    private String normalizeOcrText(
            String text
    ) {

        if (text == null) {
            return "";
        }

        text =
                text.replace("\n", " ");

        text =
                text.replace("\r", " ");

        text =
                text.replaceAll(
                        "[^a-zA-Z0-9ăâîșțĂÂÎȘȚ ]",
                        " "
                );

        text =
                text.replaceAll(
                        "\\s+",
                        " "
                );

        String[] tokens =
                text.split("\\s+");

        StringBuilder builder =
                new StringBuilder();

        for (String token : tokens) {

            if (token == null) {
                continue;
            }

            token = token.trim();

            if (token.length() < 2) {
                continue;
            }

            builder.append(token)
                    .append(" ");
        }

        return builder.toString()
                .toLowerCase()
                .trim();
    }
    private void scanFolderRecursive(
            File folder
    ) {

        if (
                cancellationRequested()
                        || folder == null
                        ||
                        !folder.exists()
                        ||
                        isExcludedAiPackagePath(folder)
        ) {
            return;
        }

        File[] files =
                folder.listFiles();

        if (files == null) {
            return;
        }

        Arrays.sort(
                files,
                Comparator.comparing(File::getAbsolutePath)
        );

        for (File file : files) {

            if (cancellationRequested()) {
                return;
            }

            try {

                String fullPath =
                        file.getAbsolutePath()
                                .toLowerCase();

                if (
                        fullPath.contains("/android/data/")
                                ||
                                fullPath.contains("/android/obb/")
                                ||
                                fullPath.contains("/cache/")
                                ||
                                fullPath.contains("/.thumbnails/")
                ) {
                    continue;
                }

                if (file.isDirectory()) {

                    if (skipCompletedCheckpointDirectory(file)) {
                        continue;
                    }
                    scanFolderRecursive(file);

                    continue;
                }

                String lower =
                        file.getName()
                                .toLowerCase();

                if (skipCompletedCheckpointFile(file, lower)) {
                    continue;
                }
                logFirstResumedFile(file, lower);

                if (
                        lower.endsWith(".jpg")
                                ||
                                lower.endsWith(".jpeg")
                                ||
                                lower.endsWith(".png")
                                ||
                                lower.endsWith(".webp")
                                ||
                                lower.endsWith(".heic")
                                ||
                                lower.endsWith(".heif")
                                ||
                                lower.endsWith(".bmp")
                                ||
                                lower.endsWith(".gif")
                ) {

                    processImageFile(file);
                    if (!cancellationRequested()) {
                        imageProcessed++;
                        sendIndexProgress(currentStage, file.getName());
                        saveFullScanCheckpoint(file);
                    }

                    continue;
                }

                if (lower.endsWith(".pdf")) {

                    processPdf(file);
                    if (!cancellationRequested()) {
                        documentProcessed++;
                        sendIndexProgress(currentStage, file.getName());
                        saveFullScanCheckpoint(file);
                    }

                    continue;
                }

                if (
                        lower.endsWith(".txt")
                                || lower.endsWith(".html")
                                || lower.endsWith(".htm")
                                ||
                                lower.endsWith(".csv")
                                ||
                                lower.endsWith(".json")
                                ||
                                lower.endsWith(".xml")
                                ||
                                lower.endsWith(".md")
                                ||
                                lower.endsWith(".rtf")
                                ||
                                lower.endsWith(".doc")
                                ||
                                lower.endsWith(".docx")
                                ||
                                lower.endsWith(".xls")
                                ||
                                lower.endsWith(".xlsx")
                                ||
                                lower.endsWith(".ppt")
                                ||
                                lower.endsWith(".pptx")
                ) {

                    processDocumentFile(file);
                    if (!cancellationRequested()) {
                        documentProcessed++;
                        sendIndexProgress(currentStage, file.getName());
                        saveFullScanCheckpoint(file);
                    }
                }

            } catch (Exception e) {
                if (!cancellationRequested()) {
                    processedFiles++;
                    failedFiles++;
                    String failedName = file == null
                            ? ""
                            : file.getName().toLowerCase();
                    if (isSupportedImage(failedName)) {
                        imageProcessed++;
                    } else if (isSupportedDocument(failedName)) {
                        documentProcessed++;
                    }
                    publishFileTerminalState(
                            file,
                            "failed",
                            failureReason(e),
                            "File processing failed"
                    );
                    saveFullScanCheckpoint(file);
                    android.util.Log.e(
                            "INDEX_WORKER",
                            "Uncaught file processing failure",
                            e
                    );
                }
            }
        }
    }

    private boolean skipCompletedCheckpointFile(File file, String lowerName) {
        if (!resumeCheckpointPending
                || (!isSupportedImage(lowerName)
                && !isSupportedDocument(lowerName))) {
            return false;
        }
        int checkpointOrder =
                file.getAbsolutePath().compareTo(resumeCheckpointPath);
        if (checkpointOrder >= 0) {
            resumeCheckpointPending = false;
            android.util.Log.i(
                    "INDEX_RESUME",
                    (checkpointOrder == 0
                            ? "Reached checkpoint; resuming with next file"
                            : "Checkpoint cursor passed; resuming with next file")
                            + " | workId=" + getId()
                            + " | path=" + resumeCheckpointPath
            );
        }
        return checkpointOrder <= 0;
    }

    private boolean skipCompletedCheckpointDirectory(File directory) {
        if (!resumeCheckpointPending || directory == null) {
            return false;
        }
        String directoryPath = directory.getAbsolutePath();
        String descendantPrefix = directoryPath.endsWith(File.separator)
                ? directoryPath
                : directoryPath + File.separator;
        if (resumeCheckpointPath.startsWith(descendantPrefix)) {
            return false;
        }
        return directoryPath.compareTo(resumeCheckpointPath) < 0;
    }

    private void logFirstResumedFile(File file, String lowerName) {
        if (!restoredFullScanCheckpoint
                || firstResumedFileLogged
                || resumeCheckpointPending
                || (!isSupportedImage(lowerName)
                && !isSupportedDocument(lowerName))) {
            return;
        }
        firstResumedFileLogged = true;
        android.util.Log.i(
                "INDEX_FAST_RESUME",
                "First resumed file"
                        + " | workId=" + getId()
                        + " | attempt=" + getRunAttemptCount()
                        + " | elapsedMs="
                        + Math.max(0L,
                        android.os.SystemClock.elapsedRealtime()
                                - workerStartedElapsedRealtime)
                        + " | path=" + file.getAbsolutePath()
        );
    }

    private boolean restoreFullScanCheckpoint() {
        String prefix = checkpointPrefix();
        String checkpointPath = prefs.getString(prefix + "last_path", "");
        if (checkpointPath == null
                || checkpointPath.isEmpty()
                || !new File(checkpointPath).exists()) {
            return false;
        }

        resumeCheckpointPath = checkpointPath;
        resumeCheckpointPending = true;
        totalFilesToIndex = prefs.getInt(prefix + "total_files", 0);
        documentTotal = prefs.getInt(prefix + "document_total", 0);
        imageTotal = prefs.getInt(prefix + "image_total", 0);
        ocrEstimatedTotal = prefs.getInt(prefix + "ocr_estimated_total", 0);
        embeddingEstimatedTotal =
                prefs.getInt(prefix + "embedding_estimated_total", 0);
        if (totalFilesToIndex <= 0
                || documentTotal + imageTotal <= 0) {
            resumeCheckpointPath = "";
            resumeCheckpointPending = false;
            return false;
        }
        workerStartedAt = prefs.getLong(
                prefix + "worker_started_at",
                workerStartedAt
        );
        newFilesIndexed = prefs.getInt(prefix + "new_files_indexed", 0);
        processedFiles = prefs.getInt(prefix + "processed_files", 0);
        failedFiles = prefs.getInt(prefix + "failed_files", 0);
        userSkippedFiles = prefs.getInt(prefix + "user_skipped_files", 0);
        documentProcessed = prefs.getInt(prefix + "document_processed", 0);
        imageProcessed = prefs.getInt(prefix + "image_processed", 0);
        pdfCount = prefs.getInt(prefix + "pdf_count", 0);
        jpgCount = prefs.getInt(prefix + "jpg_count", 0);
        jpegCount = prefs.getInt(prefix + "jpeg_count", 0);
        pngCount = prefs.getInt(prefix + "png_count", 0);
        webpCount = prefs.getInt(prefix + "webp_count", 0);
        ocrCount = prefs.getInt(prefix + "ocr_count", 0);
        embeddingCount = prefs.getInt(prefix + "embedding_count", 0);

        android.util.Log.i(
                "INDEX_RESUME",
                "Restored checkpoint"
                        + " | workId=" + getId()
                        + " | attempt=" + getRunAttemptCount()
                        + " | path=" + resumeCheckpointPath
                        + " | processed=" + processedFiles
                        + " | documents=" + documentProcessed
                        + " | images=" + imageProcessed
        );
        return true;
    }

    private void saveFullScanCheckpoint(File file) {
        if (!fullScanCheckpointEnabled
                || cancellationRequested()
                || file == null
                || currentFileTerminalState == null
                || currentFileTerminalState.isEmpty()) {
            return;
        }

        String prefix = checkpointPrefix();
        boolean saved = prefs.edit()
                .putString(prefix + "last_path", file.getAbsolutePath())
                .putLong(prefix + "worker_started_at", workerStartedAt)
                .putInt(prefix + "new_files_indexed", newFilesIndexed)
                .putInt(prefix + "processed_files", processedFiles)
                .putInt(prefix + "total_files", totalFilesToIndex)
                .putInt(prefix + "document_total", documentTotal)
                .putInt(prefix + "image_total", imageTotal)
                .putInt(prefix + "ocr_estimated_total", ocrEstimatedTotal)
                .putInt(prefix + "embedding_estimated_total",
                        embeddingEstimatedTotal)
                .putInt(prefix + "failed_files", failedFiles)
                .putInt(prefix + "user_skipped_files", userSkippedFiles)
                .putInt(prefix + "document_processed", documentProcessed)
                .putInt(prefix + "image_processed", imageProcessed)
                .putInt(prefix + "pdf_count", pdfCount)
                .putInt(prefix + "jpg_count", jpgCount)
                .putInt(prefix + "jpeg_count", jpegCount)
                .putInt(prefix + "png_count", pngCount)
                .putInt(prefix + "webp_count", webpCount)
                .putInt(prefix + "ocr_count", ocrCount)
                .putInt(prefix + "embedding_count", embeddingCount)
                .commit();
        if (!saved) {
            android.util.Log.e(
                    "INDEX_RESUME",
                    "Unable to persist checkpoint"
                            + " | workId=" + getId()
                            + " | path=" + file.getAbsolutePath()
            );
        }
    }

    private void clearFullScanCheckpoint() {
        String prefix = checkpointPrefix();
        prefs.edit()
                .remove(prefix + "last_path")
                .remove(prefix + "worker_started_at")
                .remove(prefix + "new_files_indexed")
                .remove(prefix + "processed_files")
                .remove(prefix + "total_files")
                .remove(prefix + "document_total")
                .remove(prefix + "image_total")
                .remove(prefix + "ocr_estimated_total")
                .remove(prefix + "embedding_estimated_total")
                .remove(prefix + "failed_files")
                .remove(prefix + "user_skipped_files")
                .remove(prefix + "document_processed")
                .remove(prefix + "image_processed")
                .remove(prefix + "pdf_count")
                .remove(prefix + "jpg_count")
                .remove(prefix + "jpeg_count")
                .remove(prefix + "png_count")
                .remove(prefix + "webp_count")
                .remove(prefix + "ocr_count")
                .remove(prefix + "embedding_count")
                .commit();
    }

    private String checkpointPrefix() {
        return CHECKPOINT_PREFIX + getId() + ".";
    }

    private void processSingleFile(
            File file
    ) {
        try {
            processSingleFileInternal(file);
        } catch (Exception error) {
            if (!cancellationRequested()) {
                processedFiles++;
                failedFiles++;
                publishFileTerminalState(
                        file,
                        "failed",
                        failureReason(error),
                        "File processing failed"
                );
                android.util.Log.e(
                        "INDEX_WORKER",
                        "Uncaught incremental file failure",
                        error
                );
            }
        }
    }

    private void processSingleFileInternal(
            File file
    ) {
        if (cancellationRequested() || file == null) {
            return;
        }
        if (isExcludedAiPackagePath(file)) {
            processedFiles++;
            publishFileTerminalState(
                    file, "skipped", "AI package path", "File skipped");
            return;
        }
        if (!file.isFile()) {
            String deletedPath = file.getAbsolutePath();
            database.runInTransaction(() -> {
                database.tokenIndexDao().deleteByFilePath(deletedPath);
                database.chunkDao().deleteByFilePath(deletedPath);
                database.fileDao().deleteByPath(deletedPath);
            });
            CanonicalEnrichmentWorker.cancel(getApplicationContext(), deletedPath);
            try (CanonicalIndexStore store =
                         new CanonicalIndexStore(getApplicationContext())) {
                store.deleteFile(deletedPath);
            } catch (Exception e) {
                failedFiles++;
                android.util.Log.e("CANONICAL_INDEX",
                        "Unable to remove deleted file evidence", e);
                processedFiles++;
                publishFileTerminalState(
                        file, "failed", failureReason(e),
                        "Unable to remove stale index entry");
                return;
            }
            processedFiles++;
            publishFileTerminalState(
                    file, "skipped", "File no longer exists", "Removed stale index entry");
            return;
        }

        String lower = file.getName().toLowerCase();

        if (
                lower.endsWith(".jpg")
                        || lower.endsWith(".jpeg")
                        || lower.endsWith(".png")
                        || lower.endsWith(".webp")
                        || lower.endsWith(".heic")
                        || lower.endsWith(".heif")
                        || lower.endsWith(".bmp")
                        || lower.endsWith(".gif")
        ) {
            imageTotal = 1;
            ocrEstimatedTotal = 1;
            embeddingEstimatedTotal = 1;
            processImageFile(file);
            if (!cancellationRequested()) imageProcessed = 1;
            return;
        }

        if (lower.endsWith(".pdf")) {
            documentTotal = 1;
            ocrEstimatedTotal = 1;
            embeddingEstimatedTotal = 1;
            processPdf(file);
            if (!cancellationRequested()) documentProcessed = 1;
            return;
        }

        if (
                lower.endsWith(".txt")
                        || lower.endsWith(".html")
                        || lower.endsWith(".htm")
                        || lower.endsWith(".csv")
                        || lower.endsWith(".json")
                        || lower.endsWith(".xml")
                        || lower.endsWith(".md")
                        || lower.endsWith(".rtf")
                        || lower.endsWith(".doc")
                        || lower.endsWith(".docx")
                        || lower.endsWith(".xls")
                        || lower.endsWith(".xlsx")
                        || lower.endsWith(".ppt")
                        || lower.endsWith(".pptx")
        ) {
            documentTotal = 1;
            embeddingEstimatedTotal = 1;
            processDocumentFile(file);
            if (!cancellationRequested()) documentProcessed = 1;
            return;
        }
        processedFiles++;
        publishFileTerminalState(
                file, "skipped", "Unsupported file type", "File skipped");
    }

    private void countFilesRecursive(
            File folder
    ) {

        if (
                cancellationRequested()
                        || folder == null
                        ||
                        !folder.exists()
                        ||
                        isExcludedAiPackagePath(folder)
        ) {
            return;
        }

        File[] files =
                folder.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {

            if (cancellationRequested()) {
                return;
            }

            try {

                if (file.isDirectory()) {

                    countFilesRecursive(file);

                    continue;
                }

                String lower =
                        file.getName()
                                .toLowerCase();

                boolean image = isSupportedImage(lower);
                boolean document = isSupportedDocument(lower);
                if (image || document) {
                    totalFilesToIndex++;
                    embeddingEstimatedTotal++;
                    if (image) {
                        imageTotal++;
                        ocrEstimatedTotal++;
                    } else {
                        documentTotal++;
                        if (lower.endsWith(".pdf")) ocrEstimatedTotal++;
                    }
                    long now = android.os.SystemClock.elapsedRealtime();
                    if (now - lastDiscoveryProgressAt >= 1000L) {
                        lastDiscoveryProgressAt = now;
                        progressFileName = file.getName();
                        progressFileSize = file.length();
                        currentFileTerminalState = "";
                        currentFileTerminalReason = "";
                        sendIndexProgress("Discovering work...", file.getName());
                    }
                }

            } catch (Exception e) {

                e.printStackTrace();
            }
        }
    }
    private void processDocumentFile(
            File file
    ) {

        if (cancellationRequested()) {
            return;
        }
        if (skipByUserPolicy(file)) {
            return;
        }
        if (skipByFileIndexingPolicy(file)) {
            processedFiles++;
            publishFileTerminalState(
                    file, "skipped", lastPolicySkipReason, "File skipped");
            return;
        }
        try {

            String path = file.getAbsolutePath();
            String name = file.getName();
            long modified = file.lastModified();
            String processingAttemptToken = processingAttemptToken(file);

            FileEntity old =
                    runLongOperation(
                            file,
                            "Index lookup",
                            processingAttemptToken,
                            () -> database.fileDao().getFileByPath(path)
                    );

            if (old != null) {

                boolean unchanged =
                        old.lastModified == modified
                                &&
                                old.fileSize == file.length();

                if (unchanged) {

                    runLongOperation(
                            file,
                            "Canonical index verification",
                            processingAttemptToken,
                            () -> {
                                ensureCanonicalIndexed(path);
                                return null;
                            }
                    );

                    processedFiles++;

                    publishFileTerminalState(
                            file, "skipped", "Unchanged",
                            "Skipping unchanged document...");

                    return;
                }
            }
            String text = runLongOperation(
                    file,
                    "Text extraction",
                    processingAttemptToken,
                    () -> DocumentTextExtractor.extractText(file)
            );

            if (cancellationRequested()) {
                return;
            }

            if (text == null) {
                text = "";
            }

            String combinedText =
                    (
                            name
                                    + " "
                                    + text
                                    + " "
                                    + path
                    ).trim();

            throwIfCancellationRequested();
            float[] embeddingVector = runLongOperation(
                    file,
                    "Embedding generation",
                    processingAttemptToken,
                    () -> runEmbeddingIfActive(
                            this::cancellationRequested,
                            () -> DocumentRuntimeLoader
                                    .createDefault()
                                    .getEmbeddingRuntime()
                                    .generateEmbedding(combinedText))
            );

            if (cancellationRequested()) {
                return;
            }

            byte[] embeddingBytes =
                    EmbeddingUtils
                            .floatArrayToBytes(embeddingVector);

            String lower =
                    name.toLowerCase();

            String type =
                    "DOCUMENT";

            if (lower.endsWith(".txt")) {
                type = "TXT";
            } else if (lower.endsWith(".csv")) {
                type = "CSV";
            } else if (lower.endsWith(".docx")) {
                type = "DOCX";
            } else if (lower.endsWith(".xlsx")) {
                type = "XLSX";
            }

            FileEntity entity =
                    new FileEntity(
                            path,
                            name,
                            type,
                            null,
                            text.substring(
                                    0,
                                    Math.min(
                                            text.length(),
                                            2000
                                    )
                            ),
                            System.currentTimeMillis(),
                            modified,
                            file.length(),
                            "DONE",
                            "",
                            sessionId,
                            embeddingBytes
                    );

            List<ChunkEntity> documentChunks = new ArrayList<>();
            int documentChunkIndex = 0;
            final String extractedText = text;
            List<String> documentChunkTexts = runLongOperation(
                    file,
                    "Chunk generation",
                    processingAttemptToken,
                    () -> TextChunker.chunkText(extractedText)
            );
            for (String chunk : documentChunkTexts) {
                    if (cancellationRequested()) return;
                    if (chunk == null || chunk.trim().length() < 5) continue;
                    ChunkEntity chunkEntity = new ChunkEntity();
                    chunkEntity.parentFileId = 0;
                    chunkEntity.filePath = path;
                    chunkEntity.fileName = name;
                    chunkEntity.chunkText = chunk.trim();
                    chunkEntity.normalizedText = normalizeForSearch(chunkEntity.chunkText);
                    chunkEntity.chunkIndex = documentChunkIndex;
                    chunkEntity.indexedAt = System.currentTimeMillis();
                    if (documentChunkIndex % 3 == 0) {
                        final String chunkText = chunkEntity.chunkText;
                        throwIfCancellationRequested();
                        float[] chunkEmbedding = runLongOperation(
                                file,
                                "Chunk embedding generation",
                                processingAttemptToken,
                                () -> runEmbeddingIfActive(
                                        this::cancellationRequested,
                                        () -> DocumentRuntimeLoader.createDefault()
                                                .getEmbeddingRuntime()
                                                .generateEmbedding(chunkText))
                        );
                        if (chunkEmbedding != null) {
                            chunkEntity.embedding =
                                    EmbeddingUtils.floatArrayToBytes(chunkEmbedding);
                        }
                    }
                    documentChunks.add(chunkEntity);
                    documentChunkIndex++;
                }

            runLongOperation(
                    file,
                    "Index persistence",
                    processingAttemptToken,
                    () -> {
                        String canonicalGeneration = beginCanonicalGeneration(path);
                        replaceFileIndex(path, entity, documentChunks, false);
                        CanonicalEnrichmentWorker.enqueue(
                                getApplicationContext(), path, canonicalGeneration);
                        return null;
                    }
            );
            if (cancellationRequested()) return;

            newFilesIndexed++;
            processedFiles++;
            embeddingCount++;

            android.util.Log.d(
                    "UNIVERSAL_SCAN",
                    type + " INDEXED: " + path
            );

            publishFileTerminalState(
                    file, "indexed", "", "Building document vectors...");

        } catch (UserSkippedFileException skipped) {
            return;
        } catch (PostponedFileException postponed) {
            finishPostponedFile(file, postponed);
            return;
        } catch (Exception e) {

            if (cancellationRequested()) {
                return;
            }

            processedFiles++;
            failedFiles++;

            publishFileTerminalState(
                    file, "failed", failureReason(e),
                    "Document processing failed");

            android.util.Log.e(
                    "INDEX_WORKER",
                    "Document processing failed: "
                            + (file == null ? "" : file.getAbsolutePath()),
                    e
            );
        }
    }
    private void processImageFile(
            File imageFile
    ) {

        if (cancellationRequested()) {
            return;
        }
        if (skipByUserPolicy(imageFile)) {
            return;
        }
        if (skipByFileIndexingPolicy(imageFile)) {
            processedFiles++;
            publishFileTerminalState(
                    imageFile, "skipped", lastPolicySkipReason, "File skipped");
            return;
        }
        try {

            String path = imageFile.getAbsolutePath();
            String name = imageFile.getName();
            long modified = imageFile.lastModified();
            String processingAttemptToken = processingAttemptToken(imageFile);

            FileEntity old =
                    runLongOperation(
                            imageFile,
                            "Index lookup",
                            processingAttemptToken,
                            () -> database.fileDao().getFileByPath(path)
                    );

            if (old != null) {

                boolean unchanged =
                        old.lastModified == modified
                                &&
                                old.fileSize == imageFile.length();

                if (unchanged) {

                    runLongOperation(
                            imageFile,
                            "Canonical index verification",
                            processingAttemptToken,
                            () -> {
                                ensureCanonicalIndexed(path);
                                return null;
                            }
                    );

                    processedFiles++;

                    publishFileTerminalState(
                            imageFile, "skipped", "Unchanged",
                            "Skipping unchanged image...");

                    return;
                }
            }

            String ocrText = runLongOperation(
                    imageFile,
                    "OCR",
                    processingAttemptToken,
                    () -> runOcrBlocking(path)
            );

            if (cancellationRequested()) {
                return;
            }

            if (
                    ocrText != null
                            &&
                            !ocrText.trim().isEmpty()
            ) {

                String lowerOcr =
                        ocrText.toLowerCase();

                if (
                        lowerOcr.contains("ai search results")
                                ||
                                lowerOcr.contains("ai search explanation")
                                ||
                                lowerOcr.contains("memories found")
                                ||
                                lowerOcr.contains("ai detected the searched terms")
                ) {

                    android.util.Log.d(
                            "INDEX_SKIP",
                            "SELF SCREENSHOT SKIPPED = "
                                    + imageFile.getAbsolutePath()
                    );

                    processedFiles++;
                    publishFileTerminalState(
                            imageFile, "skipped", "Application screenshot",
                            "Application screenshot skipped");
                    return;
                }

                ocrCount++;
            }

            String combinedText =
                    (
                            name
                                    + " "
                                    + ocrText
                                    + " "
                                    + path
                    ).trim();

            throwIfCancellationRequested();
            float[] embeddingVector = runLongOperation(
                    imageFile,
                    "Embedding generation",
                    processingAttemptToken,
                    () -> runEmbeddingIfActive(
                            this::cancellationRequested,
                            () -> DocumentRuntimeLoader
                                    .createDefault()
                                    .getEmbeddingRuntime()
                                    .generateEmbedding(combinedText))
            );

            if (cancellationRequested()) {
                return;
            }

            byte[] embeddingBytes =
                    EmbeddingUtils
                            .floatArrayToBytes(embeddingVector);

            byte[] imageEmbeddingBytes = null;

            try {

                BitmapFactory.Options options =
                        new BitmapFactory.Options();

                options.inPreferredConfig =
                        Bitmap.Config.ARGB_8888;

                Bitmap bitmap =
                        runLongOperation(
                                imageFile,
                                "Image decoding",
                                processingAttemptToken,
                                () -> BitmapFactory.decodeFile(
                                        path,
                                        options
                                )
                        );

                if (bitmap != null) {

                    long imageSizeMb =
                            imageFile.length() / (1024 * 1024);

                    if (imageSizeMb <= 20) {

                        throwIfCancellationRequested();
                        float[] imageEmbeddingVector =
                                runLongOperation(
                                        imageFile,
                                        "Image embedding generation",
                                        processingAttemptToken,
                                        () -> runEmbeddingIfActive(
                                                this::cancellationRequested,
                                                () -> ImageRuntimeLoader
                                                        .createDefault()
                                                        .getImageEmbeddingRuntime()
                                                        .generateEmbedding(bitmap)));
                        if (imageEmbeddingVector == null) {

                            android.util.Log.e(
                                    "IMAGE_EMBEDDING",
                                    "IMAGE VECTOR IS NULL"
                            );

                        } else {

                        }
                        if (
                                imageEmbeddingVector != null
                                        &&
                                        imageEmbeddingVector.length == 512
                                        &&
                                        !Float.isNaN(imageEmbeddingVector[0])
                                        &&
                                        !Float.isInfinite(imageEmbeddingVector[0])
                        ) {

                            imageEmbeddingBytes =
                                    EmbeddingUtils
                                            .floatArrayToBytes(
                                                    imageEmbeddingVector
                                            );

                        } else {

                            imageEmbeddingBytes = null;

                            android.util.Log.e(
                                    "IMAGE_EMBEDDING",
                                    "INVALID IMAGE VECTOR - NOT SAVED: "
                                            + path
                            );
                        }
                    }

                    bitmap.recycle();
                }

            } catch (PostponedFileException postponed) {
                throw postponed;
            } catch (Exception imageEmbeddingError) {

                android.util.Log.e(
                        "IMAGE_EMBEDDING",
                        "Image embedding failed for: " + path,
                        imageEmbeddingError
                );
            }

            FileEntity entity =
                    new FileEntity(
                            path,
                            name,
                            "IMAGE",
                            path,
                            ocrText,
                            System.currentTimeMillis(),
                            modified,
                            imageFile.length(),
                            "DONE",
                            "",
                            sessionId,
                            embeddingBytes
                    );

            entity.imageEmbedding =
                    imageEmbeddingBytes;

            List<String> imageChunks = runLongOperation(
                    imageFile,
                    "Chunk generation",
                    processingAttemptToken,
                    () -> TextChunker.chunkText(combinedText)
            );

            List<ChunkEntity> imageChunkEntities =
                    new ArrayList<>();

            int imageChunkIndex = 0;

            for (String chunk : imageChunks) {

                if (cancellationRequested()) {
                    return;
                }

                if (
                        chunk == null
                                ||
                                chunk.trim().length() < 5
                ) {

                    continue;
                }

                ChunkEntity chunkEntity =
                        new ChunkEntity();

                chunkEntity.parentFileId = 0;
                chunkEntity.filePath = path;
                chunkEntity.fileName = name;
                chunkEntity.chunkText = chunk;
                chunkEntity.normalizedText =
                        normalizeForSearch(chunk);
                chunkEntity.chunkIndex =
                        imageChunkIndex;
                chunkEntity.indexedAt =
                        System.currentTimeMillis();

                imageChunkEntities.add(
                        chunkEntity
                );

                imageChunkIndex++;
            }

            runLongOperation(
                    imageFile,
                    "Index persistence",
                    processingAttemptToken,
                    () -> {
                        String canonicalGeneration = beginCanonicalGeneration(path);
                        replaceFileIndex(
                                path,
                                entity,
                                imageChunkEntities,
                                false
                        );
                        CanonicalEnrichmentWorker.enqueue(
                                getApplicationContext(), path, canonicalGeneration);
                        return null;
                    }
            );
            if (cancellationRequested()) return;

            newFilesIndexed++;
            processedFiles++;
            jpgCount++;
            embeddingCount++;

            android.util.Log.d(
                    "UNIVERSAL_SCAN",
                    "IMAGE INDEXED: " + path
            );

            android.util.Log.d(
                    "IMAGE_EMBEDDING",
                    "IMAGE EMBEDDING SAVED: "
                            + path
                            + " | bytes="
                            + (
                            imageEmbeddingBytes == null
                                    ? 0
                                    : imageEmbeddingBytes.length
                    )
            );

            publishFileTerminalState(
                    imageFile, "indexed", "", "Indexing image...");

        } catch (UserSkippedFileException skipped) {
            return;
        } catch (PostponedFileException postponed) {
            finishPostponedFile(imageFile, postponed);
            return;
        } catch (Exception e) {

            if (cancellationRequested()) {
                return;
            }

            processedFiles++;
            failedFiles++;

            publishFileTerminalState(
                    imageFile, "failed", failureReason(e),
                    "Image processing failed");

            android.util.Log.e(
                    "INDEX_WORKER",
                    "Image processing failed: "
                            + (imageFile == null ? "" : imageFile.getAbsolutePath()),
                    e
            );
        }
    }
    private void processPdf(
            File file
    ) {

        if (cancellationRequested()) {
            return;
        }
        if (skipByUserPolicy(file)) {
            return;
        }
        if (skipByFileIndexingPolicy(file)) {
            processedFiles++;
            publishFileTerminalState(
                    file, "skipped", lastPolicySkipReason, "File skipped");
            return;
        }
        try {

            String path =
                    file.getAbsolutePath();
            long pdfPipelineStartedAt = android.os.SystemClock.elapsedRealtime();
            AtomicLong embeddingElapsedMs = new AtomicLong();
            AtomicLong embeddingCalls = new AtomicLong();

            String name =
                    file.getName();

            long modified =
                    file.lastModified();
            String processingAttemptToken = processingAttemptToken(file);

            FileEntity old =
                    runLongOperation(
                            file,
                            "Index lookup",
                            processingAttemptToken,
                            () -> database.fileDao().getFileByPath(path)
                    );

            if (old != null) {

                boolean unchanged =
                        old.lastModified == modified
                                &&
                                old.fileSize == file.length();

                if (unchanged) {

                    runLongOperation(
                            file,
                            "Canonical index verification",
                            processingAttemptToken,
                            () -> {
                                ensureCanonicalIndexed(path);
                                return null;
                            }
                    );

                    scannedPaths.add(path);

                    processedFiles++;

                    publishFileTerminalState(
                            file, "skipped", "Unchanged",
                            "Skipping unchanged PDF...");

                    return;
                }
            }
            String extractionOperation = "PDF loading and text extraction";

            String text = runLongOperation(
                    file,
                    extractionOperation,
                    processingAttemptToken,
                    () -> {
                        long loadStartedAt = android.os.SystemClock.elapsedRealtime();
                        try (PDDocument document = PDDocument.load(file)) {
                            logPdfStage(file, "pdf_loading", loadStartedAt);
                            long extractionStartedAt =
                                    android.os.SystemClock.elapsedRealtime();
                            String extracted =
                                    new PDFTextStripper().getText(document);
                            logPdfStage(file, "pdf_text_extraction",
                                    extractionStartedAt);
                            return extracted;
                        }
                    }
            );

            if (
                    text == null
                            ||
                            text.trim().length() < 10
            ) {

                extractionOperation = "PDF OCR";
                text = runLongOperation(
                        file,
                        extractionOperation,
                        processingAttemptToken,
                        () -> {
                            long startedAt =
                                    android.os.SystemClock.elapsedRealtime();
                            String result = runPdfOcr(file);
                            logPdfStage(file, "ocr", startedAt);
                            return result;
                        }
                );
            } else {
                android.util.Log.i(PDF_TIMING_TAG,
                        "stage=ocr elapsedMs=0 used=false file="
                                + file.getAbsolutePath());
            }

            if (text == null) {
                text = "";
            }

            if (!text.trim().isEmpty()) {
                ocrCount++;
            }

            String combinedText =
                    (
                            name
                                    + " "
                                    + text
                                    + " "
                                    + path
                    ).trim();

            throwIfCancellationRequested();
            float[] embeddingVector = runLongOperation(
                    file,
                    "Embedding generation",
                    processingAttemptToken,
                    () -> {
                        long startedAt = android.os.SystemClock.elapsedRealtime();
                        float[] result = runEmbeddingIfActive(
                                this::cancellationRequested,
                                () -> DocumentRuntimeLoader
                                        .createDefault()
                                        .getEmbeddingRuntime()
                                        .generateEmbedding(combinedText));
                        embeddingElapsedMs.addAndGet(elapsedSince(startedAt));
                        embeddingCalls.incrementAndGet();
                        return result;
                    }
            );

            if (cancellationRequested()) {
                return;
            }

            byte[] embeddingBytes =
                    EmbeddingUtils
                            .floatArrayToBytes(embeddingVector);

            List<String> chunks = runLongOperation(
                    file,
                    "Chunk generation",
                    processingAttemptToken,
                    () -> {
                        long startedAt = android.os.SystemClock.elapsedRealtime();
                        List<String> result = TextChunker.chunkText(combinedText);
                        logPdfStage(file, "chunk_generation", startedAt);
                        return result;
                    }
            );

            List<ChunkEntity> chunkEntities =
                    new ArrayList<>();

            int chunkIndex = 0;

            for (String chunk : chunks) {

                    if (cancellationRequested()) {
                        return;
                    }

                if (chunk == null) {
                    continue;
                }

                chunk =
                        chunk.trim();

                if (chunk.length() < 15) {
                    continue;
                }

                if (chunk.length() > 1200) {

                    chunk =
                            chunk.substring(
                                    0,
                                    1200
                            );
                }

                ChunkEntity chunkEntity =
                        new ChunkEntity();

                chunkEntity.parentFileId = 0;
                chunkEntity.filePath = path;
                chunkEntity.fileName = name;
                chunkEntity.chunkText = chunk;
                chunkEntity.normalizedText = normalizeForSearch(chunk);
                PrefixVocabularyCache.addText( chunkEntity.normalizedText );
                chunkEntity.chunkIndex = chunkIndex;
                chunkEntity.indexedAt = System.currentTimeMillis();

                    if (chunkIndex % 3 == 0) {
                        final String chunkText = chunk;
                        throwIfCancellationRequested();
                        float[] chunkEmbedding = runLongOperation(
                                file,
                                "Chunk embedding generation",
                                processingAttemptToken,
                                () -> {
                                    long startedAt =
                                            android.os.SystemClock.elapsedRealtime();
                                    float[] result = runEmbeddingIfActive(
                                            this::cancellationRequested,
                                            () -> DocumentRuntimeLoader
                                                    .createDefault()
                                                    .getEmbeddingRuntime()
                                                    .generateEmbedding(chunkText));
                                    embeddingElapsedMs.addAndGet(
                                            elapsedSince(startedAt));
                                    embeddingCalls.incrementAndGet();
                                    return result;
                                }
                        );

                        if (chunkEmbedding != null) {

                            chunkEntity.embedding =
                                    EmbeddingUtils.floatArrayToBytes(
                                            chunkEmbedding
                                    );
                        }
                    }

                    chunkEntities.add(chunkEntity);

                    chunkIndex++;
                }

            String pdfThumbnail = runLongOperation(
                    file,
                    "PDF thumbnail generation",
                    processingAttemptToken,
                    () -> generatePdfThumbnail(path)
            );

            FileEntity entity =
                    new FileEntity(
                            path,
                            name,
                            "PDF",
                            pdfThumbnail,
                            text.substring(
                                    0,
                                    Math.min(
                                            text.length(),
                                            2000
                                    )
                            ),
                            System.currentTimeMillis(),
                            modified,
                            file.length(),
                            "DONE",
                            "",
                            sessionId,
                            embeddingBytes
                    );

            android.util.Log.i(PDF_TIMING_TAG,
                    "stage=embedding_generation elapsedMs="
                            + embeddingElapsedMs.get()
                            + " calls=" + embeddingCalls.get()
                            + " file=" + file.getAbsolutePath());

            long databaseStartedAt = android.os.SystemClock.elapsedRealtime();
            runLongOperation(
                    file,
                    "Index persistence",
                    processingAttemptToken,
                    () -> {
                        String canonicalGeneration = beginCanonicalGeneration(path);
                        replaceFileIndex(
                                path,
                                entity,
                                chunkEntities,
                                true
                        );
                        CanonicalEnrichmentWorker.enqueue(
                                getApplicationContext(), path, canonicalGeneration);
                        return null;
                    }
            );
            logPdfStage(file, "database_writes", databaseStartedAt);
            if (cancellationRequested()) return;

            newFilesIndexed++;
            processedFiles++;
            pdfCount++;
            embeddingCount++;

            android.util.Log.d(
                    "PDF_INDEX",
                    "PDF INDEXED: " + path
            );

            publishFileTerminalState(
                    file, "indexed", "",
                    "Analyzing PDF semantic chunks...");
            logPdfStage(file, "total_pdf_pipeline", pdfPipelineStartedAt);

        } catch (UserSkippedFileException skipped) {
            return;
        } catch (PostponedFileException postponed) {
            finishPostponedFile(file, postponed);
            return;
        } catch (Exception e) {

            if (cancellationRequested()) {
                return;
            }

            processedFiles++;
            failedFiles++;

            publishFileTerminalState(
                    file, "failed", failureReason(e),
                    "PDF processing failed");

            android.util.Log.e(
                    "INDEX_WORKER",
                    "PDF processing failed: "
                            + (file == null ? "" : file.getAbsolutePath()),
                    e
            );
        }
    }

    private void replaceFileIndex(
            String filePath,
            FileEntity entity,
            List<ChunkEntity> chunks,
            boolean createTokenIndex
    ) {
        database.runInTransaction(() -> {
            throwIfCancellationRequested();
            database.tokenIndexDao()
                    .deleteByFilePath(filePath);
            database.chunkDao()
                    .deleteByFilePath(filePath);

            if (chunks != null && !chunks.isEmpty()) {
                long[] chunkIds =
                        database.chunkDao()
                                .insertChunks(chunks);

                for (
                        int index = 0;
                        index < chunks.size()
                                && index < chunkIds.length;
                        index++
                ) {
                    throwIfCancellationRequested();
                    chunks.get(index).id =
                            (int) chunkIds[index];
                }
            }

            if (createTokenIndex && chunks != null) {
                List<TokenIndexEntity> tokenIndexes =
                        new ArrayList<>();

                for (ChunkEntity chunk : chunks) {
                    throwIfCancellationRequested();
                    if (chunk == null || chunk.normalizedText == null) {
                        continue;
                    }

                    HashSet<String> uniqueWords =
                            new HashSet<>();

                    for (String word
                            : chunk.normalizedText.split("\\s+")) {
                        throwIfCancellationRequested();
                        if (word != null && word.length() >= 2) {
                            uniqueWords.add(word);
                        }
                    }

                    for (String word : uniqueWords) {
                        throwIfCancellationRequested();
                        TokenIndexEntity tokenEntity =
                                new TokenIndexEntity();
                        tokenEntity.token = word;
                        tokenEntity.chunkId = chunk.id;
                        tokenEntity.filePath = chunk.filePath;
                        tokenEntity.chunkIndex = chunk.chunkIndex;
                        tokenIndexes.add(tokenEntity);
                    }
                }

                if (!tokenIndexes.isEmpty()) {
                    database.tokenIndexDao()
                            .insertTokenIndexes(tokenIndexes);
                }
            }

            database.fileDao()
                    .insertOrUpdate(entity);
        });
    }

    private static boolean isSupportedImage(String lower) {
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".webp")
                || lower.endsWith(".heic") || lower.endsWith(".heif")
                || lower.endsWith(".bmp") || lower.endsWith(".gif");
    }

    private static boolean isSupportedDocument(String lower) {
        return lower.endsWith(".pdf") || lower.endsWith(".txt")
                || lower.endsWith(".html") || lower.endsWith(".htm")
                || lower.endsWith(".csv") || lower.endsWith(".json")
                || lower.endsWith(".xml") || lower.endsWith(".md")
                || lower.endsWith(".rtf") || lower.endsWith(".doc")
                || lower.endsWith(".docx") || lower.endsWith(".xls")
                || lower.endsWith(".xlsx") || lower.endsWith(".ppt")
                || lower.endsWith(".pptx");
    }

    private boolean closeCanonicalIndexingPipeline() {
        if (canonicalIndexingPipeline == null) {
            return true;
        }
        try {
            canonicalIndexingPipeline.close();
            canonicalIndexingPipeline = null;
            return true;
        } catch (Exception e) {
            android.util.Log.e(
                    "CANONICAL_INDEX",
                    "Canonical indexing persistence failed",
                    e
            );
            canonicalIndexingPipeline = null;
            return false;
        }
    }

    private boolean cancellationRequested() {
        return isStopped() || Thread.currentThread().isInterrupted();
    }

    private boolean skipByFileIndexingPolicy(File file) {
        FileIndexingPolicy.Evaluation evaluation =
                FILE_INDEXING_POLICY.evaluate(file);
        if (evaluation.shouldIndex()) {
            return false;
        }
        lastPolicySkipReason = evaluation.skipReason();
        android.util.Log.i(
                "FILE_INDEX_POLICY",
                "Skipping path=" + file.getAbsolutePath()
                        + " | reason=" + evaluation.skipReason()
                        + " | signals=" + evaluation.signals()
        );
        return true;
    }

    private boolean skipByUserPolicy(File file) {
        if (file == null) {
            return false;
        }
        progressFileName = file.getName();
        progressFileSize = file.length();
        currentFileStartedAt = android.os.SystemClock.elapsedRealtime();
        currentFileTerminalState = "";
        currentFileTerminalReason = "";
        filePostponedThisAttempt = false;
        sendIndexProgress("Preparing file...", file.getName());
        android.content.SharedPreferences decisions = getApplicationContext()
                .getSharedPreferences(USER_SKIP_PREFS, Context.MODE_PRIVATE);
        String token = decisionToken(file);
        String storedIdentity = decisions.getString("skip." + token, "");
        if (storedIdentity == null || storedIdentity.isEmpty()) {
            return false;
        }
        if (!storedIdentity.equals(fileIdentity(file))) {
            decisions.edit().remove("skip." + token).apply();
            return false;
        }
        processedFiles++;
        userSkippedFiles++;
        publishFileTerminalState(
                file,
                "skipped",
                "Skipped by user",
                "Skipped by user"
        );
        return true;
    }

    private <T> T runStartupOperation(
            String stage,
            Callable<T> operationCall
    ) {
        sendIndexProgress(stage, "");
        ExecutorService operationExecutor = Executors.newSingleThreadExecutor();
        Future<T> operationFuture = operationExecutor.submit(operationCall);
        try {
            return operationFuture.get(
                    configuredFileProcessingLimitMs(),
                    TimeUnit.MILLISECONDS
            );
        } catch (TimeoutException timeout) {
            operationFuture.cancel(true);
            throw new StartupOperationException(
                    stage + " exceeded the startup time limit",
                    timeout
            );
        } catch (ExecutionException failed) {
            throw new StartupOperationException(
                    stage + " failed",
                    operationFailure(failed)
            );
        } catch (InterruptedException interrupted) {
            operationFuture.cancel(true);
            Thread.currentThread().interrupt();
            throw new StartupOperationException(
                    stage + " was interrupted",
                    interrupted
            );
        } finally {
            if (!operationFuture.isDone()) {
                operationFuture.cancel(true);
            }
            operationExecutor.shutdownNow();
        }
    }

    private <T> T runLongOperation(
            File file,
            String operation,
            String processingAttemptToken,
            Callable<T> operationCall
    ) throws Exception {
        sendIndexProgress(
                operation,
                file == null ? "" : file.getName()
        );
        boolean unrestrictedRetry =
                processingAttemptToken.equals(approvedLongFileToken);
        long remainingForFile = Long.MAX_VALUE;
        if (!unrestrictedRetry) {
            long elapsedForFile = currentFileStartedAt <= 0L
                    ? 0L
                    : Math.max(
                            0L,
                            android.os.SystemClock.elapsedRealtime()
                                    - currentFileStartedAt);
            remainingForFile =
                    configuredFileProcessingLimitMs() - elapsedForFile;
            if (remainingForFile <= 0L) {
                recordPostponedFile(file);
                throw new PostponedFileException(file, operation);
            }
        }
        ExecutorService operationExecutor = Executors.newSingleThreadExecutor();
        Future<T> operationFuture = operationExecutor.submit(operationCall);
        try {
            if (unrestrictedRetry) {
                return operationFuture.get();
            }
            try {
                return operationFuture.get(
                        remainingForFile,
                        TimeUnit.MILLISECONDS
                );
            } catch (TimeoutException timeout) {
                operationFuture.cancel(true);
                recordPostponedFile(file);
                throw new PostponedFileException(file, operation);
            } catch (ExecutionException failed) {
                throw operationFailure(failed);
            } catch (InterruptedException interrupted) {
                operationFuture.cancel(true);
                Thread.currentThread().interrupt();
                throw interrupted;
            }

        } finally {
            if (!operationFuture.isDone()) {
                operationFuture.cancel(true);
            }
            operationExecutor.shutdownNow();
        }
    }

    private long configuredFileProcessingLimitMs() {
        int seconds = getApplicationContext().getResources().getInteger(
                com.bliss.aimemorysearch.R.integer.index_file_processing_limit_seconds);
        return Math.max(1L, seconds) * 1000L;
    }

    private void recordPostponedFile(File file) {
        if (file == null) {
            return;
        }
        android.content.SharedPreferences postponed =
                getApplicationContext().getSharedPreferences(
                        POSTPONED_FILE_PREFS, Context.MODE_PRIVATE);
        Set<String> paths = new HashSet<>(
                postponed.getStringSet(
                        POSTPONED_PATHS_KEY,
                        java.util.Collections.emptySet()));
        if (!paths.add(file.getAbsolutePath())) {
            return;
        }
        postponed.edit().putStringSet(POSTPONED_PATHS_KEY, paths).apply();

        androidx.work.OneTimeWorkRequest retry =
                new androidx.work.OneTimeWorkRequest.Builder(IndexWorker.class)
                        .setInputData(new androidx.work.Data.Builder()
                                .putString(KEY_FILE_PATH, file.getAbsolutePath())
                                .putBoolean(KEY_POSTPONED_RETRY, true)
                                .build())
                        .setInitialDelay(5L, TimeUnit.MINUTES)
                        .setBackoffCriteria(
                                androidx.work.BackoffPolicy.EXPONENTIAL,
                                10L,
                                TimeUnit.MINUTES)
                        .build();
        androidx.work.WorkManager.getInstance(getApplicationContext())
                .enqueueUniqueWork(
                        UNIQUE_WORK_NAME,
                        androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE,
                        retry);
    }

    private void clearPostponedFile(File file) {
        if (file == null) {
            return;
        }
        android.content.SharedPreferences postponed =
                getApplicationContext().getSharedPreferences(
                        POSTPONED_FILE_PREFS, Context.MODE_PRIVATE);
        Set<String> paths = new HashSet<>(
                postponed.getStringSet(
                        POSTPONED_PATHS_KEY,
                        java.util.Collections.emptySet()));
        if (paths.remove(file.getAbsolutePath())) {
            postponed.edit().putStringSet(POSTPONED_PATHS_KEY, paths).apply();
        }
    }

    private void finishPostponedFile(
            File file,
            PostponedFileException postponed
    ) {
        filePostponedThisAttempt = true;
        processedFiles++;
        publishFileTerminalState(
                file,
                "postponed",
                postponed.getMessage(),
                "Long file postponed; continuing indexing"
        );
        android.util.Log.w(
                "INDEX_POSTPONED",
                postponed.getMessage());
    }

    private void publishFileTerminalState(
            File file,
            String state,
            String reason,
            String status
    ) {
        currentFileTerminalState = state == null ? "" : state;
        currentFileTerminalReason = reason == null ? "" : reason;
        sendIndexProgress(
                status,
                file == null ? "" : file.getName()
        );
    }

    private static String failureReason(Throwable error) {
        if (error == null) {
            return "Unknown failure";
        }
        String message = error.getMessage();
        return error.getClass().getSimpleName()
                + (message == null || message.trim().isEmpty()
                ? ""
                : ": " + message);
    }

    private static Exception operationFailure(ExecutionException failed) {
        Throwable cause = failed.getCause();
        if (cause instanceof Exception) {
            return (Exception) cause;
        }
        return new RuntimeException(cause);
    }

    private static long elapsedSince(long startedAt) {
        return Math.max(0L,
                android.os.SystemClock.elapsedRealtime() - startedAt);
    }

    private static void logPdfStage(
            File file,
            String stage,
            long startedAt
    ) {
        android.util.Log.i(
                PDF_TIMING_TAG,
                "stage=" + stage
                        + " elapsedMs=" + elapsedSince(startedAt)
                        + " file=" + (file == null
                        ? "" : file.getAbsolutePath())
        );
    }

    private void publishLongOperationDecision(
            File file,
            String operation,
            long operationStartedAt,
            String processingAttemptToken
    ) {
        progressFileName = file.getName();
        progressFileSize = file.length();
        long operationElapsedMs = Math.max(0L,
                android.os.SystemClock.elapsedRealtime() - operationStartedAt);
        setProgressAsync(new androidx.work.Data.Builder()
                .putInt(KEY_PROGRESS_RUN_ATTEMPT, getRunAttemptCount())
                .putString("status", "Long operation needs confirmation")
                .putString("stage", operation)
                .putString("currentFile", file.getName())
                .putString(KEY_CURRENT_OPERATION, operation)
                .putLong(KEY_OPERATION_ELAPSED_MS, operationElapsedMs)
                .putLong("elapsedMs", Math.max(0L,
                        System.currentTimeMillis() - workerStartedAt))
                .putLong("workerStartedAt", workerStartedAt)
                .putInt("processed", processedFiles)
                .putInt("total", totalFilesToIndex)
                .putInt("indexed", newFilesIndexed)
                .putInt("skipped", skippedFiles())
                .putInt("skippedByUser", userSkippedFiles)
                .putBoolean(KEY_AWAITING_DECISION, true)
                .putString(KEY_DECISION_TOKEN, processingAttemptToken)
                .build());
    }

    private static String processingAttemptToken(File file) {
        return decisionToken(file) + "." + Long.toHexString(
                android.os.SystemClock.elapsedRealtimeNanos());
    }

    private static String decisionToken(File file) {
        return android.util.Base64.encodeToString(
                file.getAbsolutePath().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                android.util.Base64.NO_WRAP | android.util.Base64.URL_SAFE);
    }

    private static String fileIdentity(File file) {
        return file.length() + ":" + file.lastModified();
    }

    private int skippedFiles() {
        return Math.max(0, processedFiles - newFilesIndexed - failedFiles);
    }

    private void throwIfCancellationRequested() {
        if (cancellationRequested()) {
            throw new java.util.concurrent.CancellationException(
                    "Index worker cancelled"
            );
        }
    }

    static <T> T runEmbeddingIfActive(
            java.util.function.BooleanSupplier cancellationRequested,
            java.util.concurrent.Callable<T> embeddingCall
    ) throws Exception {
        if (cancellationRequested.getAsBoolean()) {
            throw new java.util.concurrent.CancellationException(
                    "Index worker cancelled"
            );
        }
        return embeddingCall.call();
    }

    private Result finishCancelledIndexing() {
        android.util.Log.i(
                "INDEX_WORKER",
                "Cancellation honored during normal indexing"
        );
        ocrExecutor.shutdownNow();
        closeCanonicalIndexingPipeline();
        return Result.retry();
    }

    private Result reconcileCanonicalFamily(String family) {
        int indexed = 0;
        try (CanonicalIndexStore store =
                     new CanonicalIndexStore(getApplicationContext())) {
            libraryDocumentTotal = prefs.getInt("last_discovered_document_total", 0);
            totalFilesToIndex =
                    store.countFiles(
                            family, CanonicalIndexingPipeline.STATUS_PACKAGE_MISSING);
            processedFiles = 0;
            newFilesIndexed = 0;
            progressFileName = family;
            sendIndexProgress("Updating multilingual search", family);
            while (true) {
                if (cancellationRequested()) {
                    closeCanonicalIndexingPipeline();
                    return Result.retry();
                }
                java.util.List<String> paths =
                        store.findFiles(
                                family,
                                CanonicalIndexingPipeline.STATUS_PACKAGE_MISSING,
                                CANONICAL_RECONCILIATION_BATCH_SIZE
                        );
                if (paths.isEmpty()) {
                    android.util.Log.i(
                            "CANONICAL_REINDEX",
                            "family=" + family + " canonicalOnlyFiles=" + indexed
                    );
                    long elapsedMs = Math.max(
                            0L,
                            System.currentTimeMillis() - workerStartedAt
                    );
                    androidx.work.Data completion =
                            new androidx.work.Data.Builder()
                                    .putString("status", "Multilingual search updated")
                                    .putString("stage", "Completed")
                                    .putString("currentFile", family)
                                    .putString("fileName", family)
                                    .putInt("processed", processedFiles)
                                    .putInt("total", totalFilesToIndex)
                                    .putInt("indexed", newFilesIndexed)
                                    .putInt("libraryDocumentTotal", libraryDocumentTotal)
                                    .putLong("elapsedMs", elapsedMs)
                                    .putLong("workerStartedAt", workerStartedAt)
                                    .build();
                    sendIndexProgress("Multilingual search updated", family);
                    return closeCanonicalIndexingPipeline()
                            ? Result.success(completion)
                            : Result.retry();
                }
                for (String path : paths) {
                    if (cancellationRequested()) {
                        closeCanonicalIndexingPipeline();
                        return Result.retry();
                    }
                    progressFileName = new java.io.File(path).getName();
                    sendIndexProgress(
                            "Updating multilingual search",
                            progressFileName
                    );
                    String generation = CanonicalEnrichmentWorker.newGeneration();
                    store.markPending(path, generation);
                    CanonicalEnrichmentWorker.enqueue(
                            getApplicationContext(), path, generation);
                    indexed++;
                    processedFiles = indexed;
                    newFilesIndexed = indexed;
                    sendIndexProgress(
                            "Updating multilingual search",
                            progressFileName
                    );
                }
            }
        } catch (Exception error) {
            if (cancellationRequested()) {
                android.util.Log.i(
                        "CANONICAL_REINDEX",
                        "Cancellation honored family=" + family
                );
            } else {
                android.util.Log.e(
                        "CANONICAL_REINDEX",
                        "Targeted canonical reindex failed family=" + family,
                        error
                );
            }
            closeCanonicalIndexingPipeline();
            return Result.retry();
        }
    }

    private boolean isExcludedAiPackagePath(File file) {
        String path = normalizedPath(file);
        if (hasPathSegment(path, AI_PACKAGE_STAGE_DIRECTORY)) {
            android.util.Log.d(
                    "INDEX_DISCOVERY",
                    "AI Package path excluded: " + file.getAbsolutePath()
            );
            return true;
        }

        File filesDirectory = getApplicationContext().getFilesDir();
        File cacheDirectory = getApplicationContext().getCacheDir();
        File installedPackagesDirectory =
                new File(filesDirectory, "ai_packages");

        boolean excluded = isPathAtOrBelow(path, normalizedPath(installedPackagesDirectory))
                || isPathAtOrBelow(path, normalizedPath(cacheDirectory));
        if (excluded) {
            android.util.Log.d(
                    "INDEX_DISCOVERY",
                    "AI Package path excluded: " + file.getAbsolutePath()
            );
        }
        return excluded;
    }

    private static boolean isPathAtOrBelow(String path, String directory) {
        return path.equals(directory)
                || path.startsWith(directory + "/");
    }

    private static boolean hasPathSegment(String path, String segment) {
        return path.equals(segment)
                || path.startsWith(segment + "/")
                || path.endsWith("/" + segment)
                || path.contains("/" + segment + "/");
    }

    private static String normalizedPath(File file) {
        return file.getAbsolutePath()
                .replace('\\', '/')
                .toLowerCase(java.util.Locale.ROOT);
    }

    private void ensureCanonicalIndexed(String filePath) throws Exception {
        throwIfCancellationRequested();
        String generation;
        try (CanonicalIndexStore store =
                     new CanonicalIndexStore(getApplicationContext())) {
            if (store.hasFile(filePath) && store.hasLanguageMetadata(filePath)) {
                return;
            }
            generation = store.pendingGeneration(filePath);
            if (generation == null) {
                generation = CanonicalEnrichmentWorker.newGeneration();
                store.markPending(filePath, generation);
            }
        }
        CanonicalEnrichmentWorker.enqueue(getApplicationContext(), filePath, generation);
    }

    private String beginCanonicalGeneration(String filePath) {
        String generation = CanonicalEnrichmentWorker.newGeneration();
        try (CanonicalIndexStore store =
                     new CanonicalIndexStore(getApplicationContext())) {
            store.markPending(filePath, generation);
        }
        return generation;
    }

    private void repairLegacyIndexRows() {
        database.runInTransaction(() -> {
            database.chunkDao()
                    .deleteDuplicateChunks();

            Map<String, Integer> chunkIds =
                    new HashMap<>();

            for (ChunkEntity chunk
                    : database.chunkDao().getAllChunks()) {
                chunkIds.put(
                        chunk.filePath + "\n" + chunk.chunkIndex,
                        chunk.id
                );
            }

            List<TokenIndexEntity> tokenIndexes =
                    database.tokenIndexDao()
                            .getAllTokenIndexes();

            for (TokenIndexEntity tokenIndex : tokenIndexes) {
                Integer chunkId =
                        chunkIds.get(
                                tokenIndex.filePath
                                        + "\n"
                                        + tokenIndex.chunkIndex
                        );

                if (chunkId != null) {
                    tokenIndex.chunkId = chunkId;
                }
            }

            database.tokenIndexDao()
                    .updateTokenIndexes(tokenIndexes);
            database.tokenIndexDao()
                    .deleteDuplicateTokenIndexes();
        });

        prefs.edit()
                .putBoolean("index_idempotency_repaired", true)
                .apply();
    }
    private void sendIndexProgress(
            String status,
            String currentFile
    ) {

        currentStage = status;
        long elapsedMs = Math.max(0L, System.currentTimeMillis() - workerStartedAt);
        boolean reconciliation = getInputData().getString(KEY_RECONCILIATION_FAMILY) != null;
        int reportedProcessed = reconciliation
                ? processedFiles : documentProcessed + imageProcessed;
        int reportedTotal = reconciliation
                ? totalFilesToIndex : documentTotal + imageTotal;
        double speed = elapsedMs > 0L ? reportedProcessed * 1000d / elapsedMs : 0d;
        long etaSeconds = speed > 0d
                ? (long) Math.max(0d, (reportedTotal - reportedProcessed) / speed)
                : 0L;

        setProgressAsync(
                new androidx.work.Data.Builder()
                        .putInt(KEY_PROGRESS_RUN_ATTEMPT, getRunAttemptCount())
                        .putString(
                                "status",
                                status
                        )
                        .putString(
                                "currentFile",
                                currentFile
                        )
                        .putString(
                                "stage",
                                currentStage
                        )
                        .putInt(
                                "processed",
                                reportedProcessed
                        )
                        .putInt(
                                "total",
                                reportedTotal
                        )
                        .putInt(
                                "pdfCount",
                                pdfCount
                        )
                        .putInt(
                                "jpgCount",
                                jpgCount
                        )
                        .putInt(
                                "ocrCount",
                                ocrCount
                        )
                        .putInt(
                                "embeddingCount",
                                embeddingCount
                        )
                        .putInt("documentTotal", documentTotal)
                        .putInt("imageTotal", imageTotal)
                        .putInt("ocrEstimatedTotal", ocrEstimatedTotal)
                        .putInt("embeddingEstimatedTotal", embeddingEstimatedTotal)
                        .putInt("documentProcessed", documentProcessed)
                        .putInt("imageProcessed", imageProcessed)
                        .putInt("libraryDocumentTotal", libraryDocumentTotal)
                        .putString("fileName", progressFileName)
                        .putLong("fileSize", progressFileSize)
                        .putLong("elapsedMs", elapsedMs)
                        .putLong("workerStartedAt", workerStartedAt)
                        .putLong("etaSeconds", etaSeconds)
                        .putDouble("speed", speed)
                        .putInt("indexed", newFilesIndexed)
                        .putInt("skipped", skippedFiles())
                        .putInt("skippedByUser", userSkippedFiles)
                        .putString("fileTerminalState", currentFileTerminalState)
                        .putString("fileTerminalReason", currentFileTerminalReason)
                        .putBoolean(KEY_AWAITING_DECISION, false)
                        .build()
        );
    }
    private String normalizeForSearch(
            String text
    ) {

        if (text == null) {
            return "";
        }

        text =
                java.text.Normalizer.normalize(
                        text,
                        java.text.Normalizer.Form.NFD
                );

        text =
                text.replaceAll(
                        "\\p{InCombiningDiacriticalMarks}+",
                        ""
                );

        text =
                text.toLowerCase(
                        java.util.Locale.ROOT
                );

        text =
                text.replaceAll(
                        "[^\\p{L}\\p{N}]",
                        " "
                );

        text =
                text.replaceAll(
                        "\\s+",
                        " "
                );

        return text.trim();
    }
}
