package com.bliss.aimemorysearch.ai.canonical;

import android.content.Context;
import android.util.Log;

import com.bliss.aimemorysearch.ai.AiPackageInfo;
import com.bliss.aimemorysearch.ai.AiPackageType;
import com.bliss.aimemorysearch.ai.AiPlatform;
import com.bliss.aimemorysearch.ai.TextChunker;
import com.bliss.aimemorysearch.db.ChunkEntity;
import com.bliss.aimemorysearch.ai.TranslationEngine;
import com.bliss.aimemorysearch.ai.TranslationModelInfo;
import com.bliss.aimemorysearch.ai.TranslationModelRegistry;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.nl.languageid.IdentifiedLanguage;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions;
import com.google.mlkit.nl.languageid.LanguageIdentifier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** Index-worker-only bridge from original contextual units to canonical evidence. */
public final class CanonicalIndexingPipeline implements AutoCloseable {

    public static final String STATUS_TRANSLATED = "TRANSLATED";
    public static final String STATUS_PACKAGE_MISSING = "PACKAGE_MISSING";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_IDENTITY_ENGLISH = "IDENTITY_ENGLISH";
    public static final String STATUS_UNSUPPORTED = "UNSUPPORTED";
    public static final String STATUS_UNDETERMINED = "UNDETERMINED";

    private static final String TAG = "CANONICAL_INDEX";
    private static final Object DATABASE_LOCK = new Object();
    private static final int DETECTION_SAMPLE_CHARS = 4000;
    private static final float DETECTION_THRESHOLD = 0.20f;

    private final Context context;
    private final TranslationEngine translationEngine;
    private final LanguageIdentifier languageIdentifier;
    private final CanonicalVocabularyExtractor extractor;
    private final CanonicalVocabularyCache vocabularyCache;
    private final CanonicalIndexStore indexStore;
    private final File cacheFile;

    public CanonicalIndexingPipeline(Context context) throws IOException {
        this.context = context.getApplicationContext();
        translationEngine = TranslationEngine.getInstance(this.context);
        languageIdentifier = LanguageIdentification.getClient(
                new LanguageIdentificationOptions.Builder()
                        .setConfidenceThreshold(DETECTION_THRESHOLD)
                        .build()
        );
        extractor = new CanonicalVocabularyExtractor();
        File directory = new File(this.context.getFilesDir(), "canonical");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Unable to create canonical directory");
        }
        cacheFile = new File(directory, "vocabulary.bin");
        vocabularyCache = new CanonicalVocabularyCache(cacheFile);
        indexStore = new CanonicalIndexStore(this.context);
    }

    public IndexingStats index(String filePath, List<ChunkEntity> chunks)
            throws Exception {
        return index(filePath, chunks, () -> false);
    }

    public IndexingStats index(
            String filePath,
            List<ChunkEntity> chunks,
            BooleanSupplier cancellationRequested
    ) throws Exception {
        throwIfCancelled(cancellationRequested);
        synchronized (DATABASE_LOCK) {
            throwIfCancelled(cancellationRequested);
            return indexLocked(filePath, chunks, cancellationRequested);
        }
    }

    private IndexingStats indexLocked(
            String filePath,
            List<ChunkEntity> chunks,
            BooleanSupplier cancellationRequested
    ) throws Exception {
        throwIfCancelled(cancellationRequested);
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("filePath must not be empty");
        }
        if (chunks == null || chunks.isEmpty()) {
            CanonicalIndexStore.StoreStats storeStats = indexStore.replaceFile(
                    filePath, new LinkedHashMap<>(), java.util.Collections.singletonList(
                            new CanonicalIndexStore.LanguageMetadata("und", "none",
                                    contentKind(filePath), 0, 0, 0f, STATUS_UNDETERMINED,
                                    0, 0, "none")));
            return logStats(filePath, "und", "none", "none", 0f,
                    0, 0, 0, storeStats);
        }
        Map<Integer, Map<CanonicalHash, Integer>> chunkEvidence =
                new LinkedHashMap<>();
        int cacheHits = 0;
        int cacheMisses = 0;
        int translatedUnits = 0;
        Map<String, MutableLanguageMetadata> languages = new LinkedHashMap<>();
        Detection lastDetection = new Detection("und", 0f);
        TranslationScope lastScope = new TranslationScope(
                "none", "none", "none", false, STATUS_UNDETERMINED);

        for (ChunkEntity chunk : chunks) {
            throwIfCancelled(cancellationRequested);
            if (chunk == null || chunk.chunkIndex < 0 || chunk.chunkText == null
                    || chunk.chunkText.trim().isEmpty()) {
                continue;
            }
            String contextualUnit = chunk.chunkText.trim();
            Detection detection = detect(contextualUnit);
            throwIfCancelled(cancellationRequested);
            TranslationScope scope = resolveScope(detection);
            lastDetection = detection;
            lastScope = scope;
            CanonicalTranslationMetadata metadata =
                    new CanonicalTranslationMetadata(
                            detection.language,
                            scope.family,
                            detection.confidence,
                            CanonicalVocabularyExtractor.NORMALIZATION_VERSION,
                            scope.modelVersion
                    );
            CanonicalVocabularyEntry cached = vocabularyCache.lookup(
                    contextualUnit,
                    metadata
            );
            throwIfCancelled(cancellationRequested);
            Map<CanonicalHash, Integer> frequencies = new LinkedHashMap<>();
            if (cached != null) {
                for (CanonicalHash hash : cached.getCanonicalHashes()) {
                    frequencies.put(hash, 1);
                }
                cacheHits++;
            } else {
                String canonicalText;
                try {
                    canonicalText = scope.translate
                            ? translationEngine.translate(contextualUnit, scope.family)
                            : contextualUnit;
                    throwIfCancelled(cancellationRequested);
                } catch (Exception translationFailure) {
                    throwIfCancelled(cancellationRequested);
                    Log.e(TAG, "Canonical translation failed for " + filePath
                            + " chunk=" + chunk.chunkIndex, translationFailure);
                    scope = scope.failed();
                    canonicalText = "";
                }
                frequencies.putAll(extractor.extract(canonicalText));
                throwIfCancelled(cancellationRequested);
                if (!frequencies.isEmpty()) {
                    vocabularyCache.insert(contextualUnit,
                            new ArrayList<>(frequencies.keySet()), metadata);
                    throwIfCancelled(cancellationRequested);
                }
                cacheMisses++;
                if (scope.translate) {
                    translatedUnits++;
                }
            }
            if (!frequencies.isEmpty()) chunkEvidence.put(chunk.chunkIndex, frequencies);
            MutableLanguageMetadata language = languages.get(detection.language);
            if (language == null) {
                language = new MutableLanguageMetadata(detection.language, scope.family,
                        contentKind(filePath), scope.status, scope.modelVersion);
                languages.put(detection.language, language);
            }
            language.add(contextualUnit.length(), detection.confidence,
                    frequencies.size(), scope.translate && !frequencies.isEmpty());
            Log.d(TAG, "Original chunk=" + contextualUnit
                    + " | detectedLanguage=" + detection.language
                    + " | confidence=" + detection.confidence
                    + " | translationPackage=" + scope.packageLabel
                    + " | canonicalVocabularyEntries=" + frequencies.size());
        }

        List<CanonicalIndexStore.LanguageMetadata> metadataRows = new ArrayList<>();
        for (MutableLanguageMetadata language : languages.values()) {
            throwIfCancelled(cancellationRequested);
            metadataRows.add(language.toImmutable());
        }
        throwIfCancelled(cancellationRequested);
        CanonicalIndexStore.StoreStats storeStats = indexStore.replaceFile(
                filePath, chunkEvidence, metadataRows);
        throwIfCancelled(cancellationRequested);
        return logStats(filePath, lastDetection.language, lastScope.family,
                lastScope.packageLabel, lastDetection.confidence,
                cacheHits, cacheMisses, translatedUnits, storeStats);
    }

    private static void throwIfCancelled(BooleanSupplier cancellationRequested) {
        if (cancellationRequested.getAsBoolean()) {
            throw new CancellationException("Canonical indexing cancelled");
        }
    }

    public void deleteFile(String filePath) throws IOException {
        synchronized (DATABASE_LOCK) {
            indexStore.deleteFile(filePath);
        }
    }

    public boolean hasFile(String filePath) {
        synchronized (DATABASE_LOCK) {
            return indexStore.hasFile(filePath) && indexStore.hasLanguageMetadata(filePath);
        }
    }

    public List<String> findPackageMissingFiles(String family, int limit) {
        synchronized (DATABASE_LOCK) {
            return indexStore.findFiles(family, STATUS_PACKAGE_MISSING, limit);
        }
    }

    private Detection detect(String source) throws Exception {
        String sample = source.substring(0,
                Math.min(source.length(), DETECTION_SAMPLE_CHARS));
        List<IdentifiedLanguage> candidates = Tasks.await(
                languageIdentifier.identifyPossibleLanguages(sample),
                15,
                TimeUnit.SECONDS
        );
        return candidates.stream()
                .filter(candidate -> !"und".equals(candidate.getLanguageTag()))
                .max(Comparator.comparingDouble(
                        IdentifiedLanguage::getConfidence))
                .map(candidate -> new Detection(
                        candidate.getLanguageTag(),
                        candidate.getConfidence()))
                .orElse(new Detection("und", 0f));
    }

    private TranslationScope resolveScope(Detection detection) {
        if ("en".equalsIgnoreCase(detection.language)) {
            return new TranslationScope(
                    "english", "identity-v1", "identity/en", false,
                    STATUS_IDENTITY_ENGLISH);
        }
        if ("und".equalsIgnoreCase(detection.language)) {
            return new TranslationScope("none", "none", "identity/und", false,
                    STATUS_UNDETERMINED);
        }
        TranslationModelInfo model =
                TranslationModelRegistry.getModelForLanguage(
                        detection.language);
        if (model == null) {
            return new TranslationScope("unsupported", "identity-v1",
                    "identity/" + detection.language, false, STATUS_UNSUPPORTED);
        }
        AiPackageInfo installed = AiPlatform.getPackageManager()
                .findInstalledPackage(
                        AiPackageType.TRANSLATION,
                        model.getTranslationFamily());
        if (installed == null) {
            return new TranslationScope(model.getTranslationFamily(),
                    "unavailable", "identity/" + detection.language, false,
                    STATUS_PACKAGE_MISSING);
        }
        return new TranslationScope(
                model.getTranslationFamily(),
                installed.getVersion(),
                installed.getPackageId() + "/" + installed.getVersion(),
                true, STATUS_TRANSLATED
        );
    }

    private static String contentKind(String filePath) {
        String lower = filePath == null ? "" : filePath.toLowerCase(java.util.Locale.ROOT);
        return lower.matches(".*\\.(jpg|jpeg|png|gif|webp|bmp|heic|heif)$")
                ? "IMAGE" : "DOCUMENT";
    }

    private IndexingStats logStats(
            String filePath,
            String language,
            String family,
            String packageLabel,
            float confidence,
            int cacheHits,
            int cacheMisses,
            int translatedUnits,
            CanonicalIndexStore.StoreStats storeStats
    ) {
        long cacheBytes = cacheFile.isFile() ? cacheFile.length() : 0L;
        long indexBytes = indexStore.databaseBytes(context);
        long originalDatabaseBytes = databaseFamilyBytes(
                context.getDatabasePath("ai_memory_db"));
        IndexingStats stats = new IndexingStats(
                language, family, packageLabel, confidence,
                storeStats.hashCount, storeStats.chunkEvidenceBytes,
                storeStats.postingBytes, cacheHits, cacheMisses,
                translatedUnits, cacheBytes, indexBytes,
                originalDatabaseBytes
        );
        Log.d(TAG, "file=" + filePath
                + " | detectedLanguage=" + language
                + " | translationPackage=" + packageLabel
                + " | canonicalVocabularyEntries=" + stats.vocabularyEntries
                + " | canonicalHashes=" + stats.hashCount
                + " | postingBytes=" + stats.postingBytes
                + " | documentEvidenceBytes=" + stats.documentEvidenceBytes
                + " | cacheHits=" + cacheHits
                + " | cacheMisses=" + cacheMisses
                + " | translatedUnits=" + translatedUnits
                + " | originalDatabaseBytes=" + originalDatabaseBytes
                + " | canonicalIndexBytes=" + indexBytes
                + " | vocabularyCacheBytes=" + cacheBytes
                + " | totalCanonicalBytes=" + (indexBytes + cacheBytes));
        return stats;
    }

    private static long databaseFamilyBytes(File database) {
        return fileBytes(database)
                + fileBytes(new File(database.getPath() + "-wal"))
                + fileBytes(new File(database.getPath() + "-shm"));
    }

    private static long fileBytes(File file) {
        return file.isFile() ? file.length() : 0L;
    }

    @Override
    public void close() throws IOException {
        synchronized (DATABASE_LOCK) {
            vocabularyCache.close();
            long cacheBytes = cacheFile.isFile() ? cacheFile.length() : 0L;
            long indexBytes = indexStore.databaseBytes(context);
            Log.d(TAG, "CANONICAL_STORAGE_FINAL"
                    + " | canonicalIndexBytes=" + indexBytes
                    + " | vocabularyCacheBytes=" + cacheBytes
                    + " | totalCanonicalBytes=" + (indexBytes + cacheBytes)
                    + " | vocabularyCacheEntries=" + vocabularyCache.size());
            indexStore.close();
            languageIdentifier.close();
        }
    }

    private static final class Detection {
        private final String language;
        private final float confidence;

        private Detection(String language, float confidence) {
            this.language = language;
            this.confidence = confidence;
        }
    }

    private static final class TranslationScope {
        private final String family;
        private final String modelVersion;
        private final String packageLabel;
        private final boolean translate;
        private final String status;

        private TranslationScope(String family, String modelVersion,
                String packageLabel, boolean translate, String status) {
            this.family = family;
            this.modelVersion = modelVersion;
            this.packageLabel = packageLabel;
            this.translate = translate;
            this.status = status;
        }

        private TranslationScope failed() {
            return new TranslationScope(family, modelVersion, packageLabel, false, STATUS_FAILED);
        }
    }

    private static final class MutableLanguageMetadata {
        final String language, family, contentKind, status, packageVersion;
        int chunks, characters, hashes, translated;
        float confidenceSum;
        MutableLanguageMetadata(String language, String family, String contentKind,
                String status, String packageVersion) {
            this.language = language; this.family = family; this.contentKind = contentKind;
            this.status = status; this.packageVersion = packageVersion;
        }
        void add(int characterCount, float confidence, int hashCount, boolean wasTranslated) {
            chunks++; characters += characterCount; confidenceSum += confidence;
            hashes += hashCount; if (wasTranslated) translated++;
        }
        CanonicalIndexStore.LanguageMetadata toImmutable() {
            return new CanonicalIndexStore.LanguageMetadata(language, family, contentKind,
                    chunks, characters, confidenceSum, status, hashes, translated, packageVersion);
        }
    }

    public static final class IndexingStats {
        public final String language;
        public final String family;
        public final String packageLabel;
        public final float confidence;
        public final int vocabularyEntries;
        public final int hashCount;
        public final long documentEvidenceBytes;
        public final long postingBytes;
        public final int cacheHits;
        public final int cacheMisses;
        public final int translatedUnits;
        public final long vocabularyCacheBytes;
        public final long canonicalIndexBytes;
        public final long originalDatabaseBytes;

        private IndexingStats(String language, String family,
                String packageLabel, float confidence, int hashCount,
                long documentEvidenceBytes, long postingBytes,
                int cacheHits, int cacheMisses, int translatedUnits,
                long vocabularyCacheBytes, long canonicalIndexBytes,
                long originalDatabaseBytes) {
            this.language = language;
            this.family = family;
            this.packageLabel = packageLabel;
            this.confidence = confidence;
            this.vocabularyEntries = hashCount;
            this.hashCount = hashCount;
            this.documentEvidenceBytes = documentEvidenceBytes;
            this.postingBytes = postingBytes;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.translatedUnits = translatedUnits;
            this.vocabularyCacheBytes = vocabularyCacheBytes;
            this.canonicalIndexBytes = canonicalIndexBytes;
            this.originalDatabaseBytes = originalDatabaseBytes;
        }
    }
}
