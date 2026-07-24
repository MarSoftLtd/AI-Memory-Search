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
import java.util.UUID;
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
    public static final String USER_SKIP_PREFS = "index_user_skips";
    public static final String KEY_DECISION_TOKEN = "decision_token";
    public static final String KEY_AWAITING_DECISION = "awaiting_decision";
    public static final String KEY_CURRENT_OPERATION = "current_operation";
    public static final String KEY_OPERATION_ELAPSED_MS = "operation_elapsed_ms";
    private static final long LONG_OPERATION_THRESHOLD_MS = 30_000L;
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
    private String approvedLongFileToken = "";
    private String currentStage = "Preparing...";
    private CanonicalIndexingPipeline canonicalIndexingPipeline;
    private static final class UserSkippedFileException extends Exception {
        UserSkippedFileException() {
            super("Current file skipped by user");
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

        PDFBoxResourceLoader.init(
                getApplicationContext()
        );
        DocumentRuntimeLoader
                .createDefault()
                .loadEmbeddingRuntime(
                        getApplicationContext()
                );
        ImageRuntimeLoader
                .createDefault()
                .loadImageEmbeddingRuntime(
                        getApplicationContext()
                );
        ImageRuntimeLoader
                .createDefault()
                .loadTextEmbeddingRuntime(
                        getApplicationContext()
                );
        MiniLMTokenizer
                .getInstance()
                .initialize(
                        getApplicationContext()
                );
        if (cancellationRequested()) {
            return finishCancelledIndexing();
        }
        sessionId =
                UUID.randomUUID().toString();

        String incrementalFilePath =
                getInputData().getString(KEY_FILE_PATH);

        if (
                incrementalFilePath != null
                        &&
                        !incrementalFilePath.trim().isEmpty()
        ) {
            File incrementalFile =
                    new File(incrementalFilePath);

            totalFilesToIndex = 1;
            processedFiles = 0;
            processSingleFile(incrementalFile);

            if (cancellationRequested()) {
                return finishCancelledIndexing();
            }

            if (!closeCanonicalIndexingPipeline()
                    || failedFiles != 0) {
                return Result.failure();
            }

            return Result.success();
        }

        database.fileDao()
                .resetInterruptedFiles();
        File rootFolder =
                Environment
                        .getExternalStorageDirectory();

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

        sendIndexProgress(
                "Filesystem scan completed",
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
        libraryDocumentTotal = documentTotal;
        prefs.edit().putInt("last_discovered_document_total", documentTotal).apply();
        if (cancellationRequested()) {
            return finishCancelledIndexing();
        }
        boolean completedSuccessfully = failedFiles == 0;
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
                        completedSuccessfully
                )
                .apply();

        setProgressAsync(
                new androidx.work.Data.Builder()
                        .putString(
                                "status",
                                newFilesIndexed == 0
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
                        .build()
        );

        completedSuccessfully =
                closeCanonicalIndexingPipeline()
                        && completedSuccessfully;

        return completedSuccessfully
                ? Result.success()
                : Result.failure(
                        new androidx.work.Data.Builder()
                                .putInt("failed", failedFiles)
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

                    scanFolderRecursive(file);

                    continue;
                }

                String lower =
                        file.getName()
                                .toLowerCase();

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
                        sendIndexProgress("Image processing completed", file.getName());
                    }

                    continue;
                }

                if (lower.endsWith(".pdf")) {

                    processPdf(file);
                    if (!cancellationRequested()) {
                        documentProcessed++;
                        sendIndexProgress("Document processing completed", file.getName());
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
                        sendIndexProgress("Document processing completed", file.getName());
                    }
                }

            } catch (Exception e) {

                e.printStackTrace();
            }
        }
    }

    private void processSingleFile(
            File file
    ) {
        if (cancellationRequested() || file == null) {
            return;
        }
        if (isExcludedAiPackagePath(file)) {
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
            }
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
        }
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
            return;
        }
        try {

            String path = file.getAbsolutePath();
            String name = file.getName();
            long modified = file.lastModified();

            FileEntity old =
                    database.fileDao()
                            .getFileByPath(path);

            if (old != null) {

                boolean unchanged =
                        old.lastModified == modified
                                &&
                                old.fileSize == file.length();

                if (unchanged) {

                    ensureCanonicalIndexed(path);

                    processedFiles++;

                    sendIndexProgress(
                            "Skipping unchanged document...",
                            name
                    );

                    return;
                }
            }
            String processingAttemptToken = processingAttemptToken(file);
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

            float[] embeddingVector = runLongOperation(
                    file,
                    "Embedding generation",
                    processingAttemptToken,
                    () -> DocumentRuntimeLoader
                        .createDefault()
                        .getEmbeddingRuntime()
                        .generateEmbedding(combinedText)
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
                        float[] chunkEmbedding = runLongOperation(
                                file,
                                "Chunk embedding generation",
                                processingAttemptToken,
                                () -> DocumentRuntimeLoader.createDefault()
                                        .getEmbeddingRuntime()
                                        .generateEmbedding(chunkText)
                        );
                        if (chunkEmbedding != null) {
                            chunkEntity.embedding =
                                    EmbeddingUtils.floatArrayToBytes(chunkEmbedding);
                        }
                    }
                    documentChunks.add(chunkEntity);
                    documentChunkIndex++;
                }

            String canonicalGeneration = beginCanonicalGeneration(path);
            replaceFileIndex(path, entity, documentChunks, false);
            CanonicalEnrichmentWorker.enqueue(
                    getApplicationContext(), path, canonicalGeneration);
            if (cancellationRequested()) return;

            newFilesIndexed++;
            processedFiles++;
            embeddingCount++;

            android.util.Log.d(
                    "UNIVERSAL_SCAN",
                    type + " INDEXED: " + path
            );

            sendIndexProgress(
                    "Building document vectors...",
                    name
            );

        } catch (UserSkippedFileException skipped) {
            return;
        } catch (Exception e) {

            if (cancellationRequested()) {
                return;
            }

            processedFiles++;
            failedFiles++;

            sendIndexProgress(
                    "Document processing failed",
                    file == null ? "" : file.getName()
            );

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
            return;
        }
        try {

            String path = imageFile.getAbsolutePath();
            String name = imageFile.getName();
            long modified = imageFile.lastModified();

            FileEntity old =
                    database.fileDao()
                            .getFileByPath(path);

            if (old != null) {

                boolean unchanged =
                        old.lastModified == modified
                                &&
                                old.fileSize == imageFile.length();

                if (unchanged) {

                    ensureCanonicalIndexed(path);

                    processedFiles++;

                    sendIndexProgress(
                            "Skipping unchanged image...",
                            name
                    );

                    return;
                }
            }

            String processingAttemptToken = processingAttemptToken(imageFile);
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

            float[] embeddingVector = runLongOperation(
                    imageFile,
                    "Embedding generation",
                    processingAttemptToken,
                    () -> DocumentRuntimeLoader
                        .createDefault()
                        .getEmbeddingRuntime()
                        .generateEmbedding(combinedText)
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
                        Bitmap.Config.RGB_565;

                options.inSampleSize = 8;

                Bitmap bitmap =
                        BitmapFactory.decodeFile(
                                path,
                                options
                        );

                if (bitmap != null) {

                    long imageSizeMb =
                            imageFile.length() / (1024 * 1024);

                    if (imageSizeMb <= 20) {

                        float[] imageEmbeddingVector =
                                ImageRuntimeLoader
                                        .createDefault()
                                        .getImageEmbeddingRuntime()
                                        .generateEmbedding(
                                                bitmap
                                        );
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

            String canonicalGeneration = beginCanonicalGeneration(path);
            replaceFileIndex(
                    path,
                    entity,
                    imageChunkEntities,
                    false
            );
            CanonicalEnrichmentWorker.enqueue(
                    getApplicationContext(), path, canonicalGeneration);
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

            sendIndexProgress(
                    "Indexing image...",
                    name
            );

        } catch (UserSkippedFileException skipped) {
            return;
        } catch (Exception e) {

            if (cancellationRequested()) {
                return;
            }

            processedFiles++;
            failedFiles++;

            sendIndexProgress(
                    "Image processing failed",
                    imageFile == null ? "" : imageFile.getName()
            );

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

            FileEntity old =
                    database.fileDao()
                            .getFileByPath(path);

            if (old != null) {

                boolean unchanged =
                        old.lastModified == modified
                                &&
                                old.fileSize == file.length();

                if (unchanged) {

                    ensureCanonicalIndexed(path);

                    scannedPaths.add(path);

                    processedFiles++;

                    sendIndexProgress(
                            "Skipping unchanged PDF...",
                            name
                    );

                    return;
                }
            }
            String processingAttemptToken = processingAttemptToken(file);
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

            float[] embeddingVector = runLongOperation(
                    file,
                    "Embedding generation",
                    processingAttemptToken,
                    () -> {
                        long startedAt = android.os.SystemClock.elapsedRealtime();
                        float[] result = DocumentRuntimeLoader
                                .createDefault()
                                .getEmbeddingRuntime()
                                .generateEmbedding(combinedText);
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
                        float[] chunkEmbedding = runLongOperation(
                                file,
                                "Chunk embedding generation",
                                processingAttemptToken,
                                () -> {
                                    long startedAt =
                                            android.os.SystemClock.elapsedRealtime();
                                    float[] result = DocumentRuntimeLoader
                                            .createDefault()
                                            .getEmbeddingRuntime()
                                            .generateEmbedding(chunkText);
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

            String pdfThumbnail =
                    generatePdfThumbnail(path);

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
            String canonicalGeneration = beginCanonicalGeneration(path);
            replaceFileIndex(
                    path,
                    entity,
                    chunkEntities,
                    true
            );
            logPdfStage(file, "database_writes", databaseStartedAt);
            CanonicalEnrichmentWorker.enqueue(
                    getApplicationContext(), path, canonicalGeneration);
            if (cancellationRequested()) return;

            newFilesIndexed++;
            processedFiles++;
            pdfCount++;
            embeddingCount++;

            android.util.Log.d(
                    "PDF_INDEX",
                    "PDF INDEXED: " + path
            );

            sendIndexProgress(
                    "Analyzing PDF semantic chunks...",
                    name
            );
            logPdfStage(file, "total_pdf_pipeline", pdfPipelineStartedAt);

        } catch (UserSkippedFileException skipped) {
            return;
        } catch (Exception e) {

            if (cancellationRequested()) {
                return;
            }

            processedFiles++;
            failedFiles++;

            sendIndexProgress(
                    "PDF processing failed",
                    file == null ? "" : file.getName()
            );

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
        android.util.Log.i(
                "FILE_INDEX_POLICY",
                "Skipping path=" + file.getAbsolutePath()
                        + " | reason=" + evaluation.skipReason()
                        + " | signals=" + evaluation.signals()
        );
        sendIndexProgress("Skipping low-value generated file...", file.getName());
        return true;
    }

    private boolean skipByUserPolicy(File file) {
        if (file == null) {
            return false;
        }
        progressFileName = file.getName();
        progressFileSize = file.length();
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
        sendIndexProgress("Skipped by user", file.getName());
        return true;
    }

    private <T> T runLongOperation(
            File file,
            String operation,
            String processingAttemptToken,
            Callable<T> operationCall
    ) throws Exception {
        long operationStartedAt = android.os.SystemClock.elapsedRealtime();
        ExecutorService operationExecutor = Executors.newSingleThreadExecutor();
        Future<T> operationFuture = operationExecutor.submit(operationCall);
        try {
            if (processingAttemptToken.equals(approvedLongFileToken)) {
                return operationFuture.get();
            }
            try {
                return operationFuture.get(
                        LONG_OPERATION_THRESHOLD_MS,
                        TimeUnit.MILLISECONDS
                );
            } catch (TimeoutException timeout) {
                // The operation is still running. Give the UI control over it.
            } catch (ExecutionException failed) {
                throw operationFailure(failed);
            } catch (InterruptedException interrupted) {
                operationFuture.cancel(true);
                Thread.currentThread().interrupt();
                throw interrupted;
            }

            android.content.SharedPreferences decisions = getApplicationContext()
                    .getSharedPreferences(USER_SKIP_PREFS, Context.MODE_PRIVATE);
            String decisionKey = "decision." + processingAttemptToken;
            while (!cancellationRequested()) {
                String decision = decisions.getString(decisionKey, "");
                if ("continue".equals(decision)) {
                    decisions.edit().remove(decisionKey).apply();
                    approvedLongFileToken = processingAttemptToken;
                    sendIndexProgress("Continuing current file", file.getName());
                    try {
                        return operationFuture.get();
                    } catch (ExecutionException failed) {
                        throw operationFailure(failed);
                    } catch (InterruptedException interrupted) {
                        operationFuture.cancel(true);
                        Thread.currentThread().interrupt();
                        throw interrupted;
                    }
                }
                if ("skip".equals(decision)) {
                    operationFuture.cancel(true);
                    decisions.edit()
                            .putString("skip." + decisionToken(file), fileIdentity(file))
                            .remove(decisionKey)
                            .apply();
                    processedFiles++;
                    userSkippedFiles++;
                    sendIndexProgress("Skipped by user", file.getName());
                    throw new UserSkippedFileException();
                }
                publishLongOperationDecision(
                        file, operation, operationStartedAt, processingAttemptToken);
                try {
                    Thread.sleep(250L);
                } catch (InterruptedException interrupted) {
                    operationFuture.cancel(true);
                    decisions.edit().remove(decisionKey).apply();
                    Thread.currentThread().interrupt();
                    throw interrupted;
                }
            }
            operationFuture.cancel(true);
            decisions.edit().remove(decisionKey).apply();
            throw new InterruptedException("Index worker cancelled");
        } finally {
            if (!operationFuture.isDone()) {
                operationFuture.cancel(true);
            }
            operationExecutor.shutdownNow();
        }
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
