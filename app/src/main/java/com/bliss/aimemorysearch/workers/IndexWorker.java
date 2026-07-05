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
import com.bliss.aimemorysearch.ai.EntityExtractionEngine;
import com.bliss.aimemorysearch.ai.ImageEmbeddingEngine;
import com.bliss.aimemorysearch.ai.MobileClipTextEmbeddingEngine;
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
import com.bliss.aimemorysearch.ai.DocumentTextExtractor;
import com.bliss.aimemorysearch.ai.EmbeddingEngine;
import com.bliss.aimemorysearch.ai.EmbeddingUtils;
import com.bliss.aimemorysearch.ai.MiniLMTokenizer;
import com.bliss.aimemorysearch.db.TokenIndexEntity;
import java.util.HashSet;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.bliss.aimemorysearch.ai.ImageEmbeddingEngine;
public class IndexWorker extends Worker {
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
    private int totalFilesToIndex = 0;
    private int processedFiles = 0;
    private String currentStage = "Preparing...";
    public IndexWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams
    ) {
        super(context, workerParams);
    }
    @NonNull
    @Override
    public Result doWork() {

        database = AppDatabase.getInstance(
                getApplicationContext()
        );
        prefs = getApplicationContext()
                .getSharedPreferences(
                        "index_state",
                        Context.MODE_PRIVATE
                );
        PDFBoxResourceLoader.init(
                getApplicationContext()
        );
        EmbeddingEngine
                .getInstance()
                .initialize(
                        getApplicationContext()
                );
        ImageEmbeddingEngine
                .getInstance()
                .initialize(
                        getApplicationContext()
                );
        MobileClipTextEmbeddingEngine
                .getInstance()
                .initialize(
                        getApplicationContext()
                );
        MiniLMTokenizer
                .getInstance()
                .initialize(
                        getApplicationContext()
                );
        sessionId =
                UUID.randomUUID().toString();

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
        prefs.edit()
                .putLong(
                        "last_index_time",
                        System.currentTimeMillis()
                )
                .putInt(
                        "last_indexed_count",
                        newFilesIndexed
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

        return Result.success();
        
    }
    private void scanPdfFiles() {

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

            TextRecognition.getClient(
                            TextRecognizerOptions.DEFAULT_OPTIONS
                    )
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

            TextRecognition.getClient(
                            TextRecognizerOptions.DEFAULT_OPTIONS
                    )
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
                folder == null
                        ||
                        !folder.exists()
        ) {
            return;
        }

        File[] files =
                folder.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {

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

                    continue;
                }

                if (lower.endsWith(".pdf")) {

                    processPdf(file);

                    continue;
                }

                if (
                        lower.endsWith(".txt")
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
                }

            } catch (Exception e) {

                e.printStackTrace();
            }
        }
    }

    private void countFilesRecursive(
            File folder
    ) {

        if (
                folder == null
                        ||
                        !folder.exists()
        ) {
            return;
        }

        File[] files =
                folder.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {

            try {

                if (file.isDirectory()) {

                    countFilesRecursive(file);

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
                                lower.endsWith(".pdf")
                                ||
                                lower.endsWith(".txt")
                                ||
                                lower.endsWith(".csv")
                                ||
                                lower.endsWith(".docx")
                                ||
                                lower.endsWith(".xlsx")
                ) {

                    totalFilesToIndex++;
                }

            } catch (Exception e) {

                e.printStackTrace();
            }
        }
    }
    private void processDocumentFile(
            File file
    ) {

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

                    processedFiles++;

                    sendIndexProgress(
                            "Skipping unchanged document...",
                            name
                    );

                    return;
                }
            }

            String text =
                    DocumentTextExtractor.extractText(file);

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

            float[] embeddingVector =
                    EmbeddingEngine
                            .getInstance()
                            .generateEmbedding(combinedText);

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

            database.fileDao()
                    .insertOrUpdate(entity);

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

        } catch (Exception e) {

            processedFiles++;

            sendIndexProgress(
                    "Document processing failed",
                    file == null ? "" : file.getName()
            );

            e.printStackTrace();
        }
    }
    private void processImageFile(
            File imageFile
    ) {

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

                    processedFiles++;

                    sendIndexProgress(
                            "Skipping unchanged image...",
                            name
                    );

                    return;
                }
            }

            String ocrText =
                    runOcrBlocking(path);

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

            float[] embeddingVector =
                    EmbeddingEngine
                            .getInstance()
                            .generateEmbedding(combinedText);

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
                                ImageEmbeddingEngine
                                        .getInstance()
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

            database.fileDao()
                    .insertOrUpdate(entity);

            List<String> imageChunks =
                    TextChunker.chunkText(
                            combinedText
                    );

            List<ChunkEntity> imageChunkEntities =
                    new ArrayList<>();

            int imageChunkIndex = 0;

            for (String chunk : imageChunks) {

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

            database.chunkDao()
                    .insertChunks(
                            imageChunkEntities
                    );

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

        } catch (Exception e) {

            processedFiles++;

            sendIndexProgress(
                    "Image processing failed",
                    imageFile == null ? "" : imageFile.getName()
            );

            e.printStackTrace();
        }
    }
    private void processPdf(
            File file
    ) {

        try {

            String path =
                    file.getAbsolutePath();

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

                    scannedPaths.add(path);

                    processedFiles++;

                    sendIndexProgress(
                            "Skipping unchanged PDF...",
                            name
                    );

                    return;
                }
            }

            PDDocument document =
                    PDDocument.load(file);

            PDFTextStripper stripper =
                    new PDFTextStripper();

            String text =
                    stripper.getText(document);

            document.close();

            if (
                    text == null
                            ||
                            text.trim().length() < 10
            ) {

                text =
                        runPdfOcr(file);
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

            float[] embeddingVector =
                    EmbeddingEngine
                            .getInstance()
                            .generateEmbedding(combinedText);

            byte[] embeddingBytes =
                    EmbeddingUtils
                            .floatArrayToBytes(embeddingVector);

            List<String> chunks =
                    TextChunker.chunkText(combinedText);

            List<ChunkEntity> chunkEntities =
                    new ArrayList<>();

            int chunkIndex = 0;

            for (String chunk : chunks) {

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

                    float[] chunkEmbedding =
                            EmbeddingEngine
                                    .getInstance()
                                    .generateEmbedding(chunk);

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

            database.chunkDao()
                    .insertChunks(chunkEntities);
            List<TokenIndexEntity> tokenIndexes =
                    new ArrayList<>();

            for (ChunkEntity chunk : chunkEntities) {

                if (
                        chunk == null
                                ||
                                chunk.normalizedText == null
                ) {

                    continue;
                }

                String[] words =
                        chunk.normalizedText.split("\\s+");

                HashSet<String> uniqueWords =
                        new HashSet<>();

                for (String word : words) {

                    if (
                            word == null
                                    ||
                                    word.length() < 2
                    ) {

                        continue;
                    }

                    uniqueWords.add(word);
                }

                for (String word : uniqueWords) {

                    TokenIndexEntity tokenEntity =
                            new TokenIndexEntity();

                    tokenEntity.token =
                            word;

                    tokenEntity.chunkId =
                            chunk.id;

                    tokenEntity.filePath =
                            chunk.filePath;

                    tokenEntity.chunkIndex =
                            chunk.chunkIndex;

                    tokenIndexes.add(
                            tokenEntity
                    );
                }
            }

            database
                    .tokenIndexDao()
                    .insertTokenIndexes(
                            tokenIndexes
                    );
            android.util.Log.d(
                    "TOKEN_INDEX",
                    "INSERTED = "
                            + tokenIndexes.size()
            );
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

            database.fileDao()
                    .insertOrUpdate(entity);

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

        } catch (Exception e) {

            processedFiles++;

            sendIndexProgress(
                    "PDF processing failed",
                    file == null ? "" : file.getName()
            );

            e.printStackTrace();
        }
    }
    private void sendIndexProgress(
            String status,
            String currentFile
    ) {

        currentStage = status;

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
                                processedFiles
                        )
                        .putInt(
                                "total",
                                totalFilesToIndex
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