package com.bliss.aimemorysearch.ai.canonical;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Compact chunk-level canonical index with bounded term lookup. */
public final class CanonicalIndexStore extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "canonical_index.db";
    private static final int DATABASE_VERSION = 5;
    private static final String STATE_PENDING = "PENDING";
    private static final String STATE_PUBLISHED = "PUBLISHED";
    private static final int EVIDENCE_FORMAT_VERSION = 1;
    private static final int FIELD_BODY = 1;

    public CanonicalIndexStore(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE canonical_files (file_path TEXT PRIMARY KEY,"
                + "generation TEXT NOT NULL,state TEXT NOT NULL) WITHOUT ROWID");
        database.execSQL("CREATE TABLE canonical_chunks ("
                + "file_path TEXT NOT NULL,chunk_index INTEGER NOT NULL,"
                + "term_hashes BLOB NOT NULL,PRIMARY KEY(file_path,chunk_index))");
        database.execSQL("CREATE TABLE canonical_postings ("
                + "hash_high INTEGER NOT NULL,hash_low INTEGER NOT NULL,"
                + "postings BLOB NOT NULL,PRIMARY KEY(hash_high,hash_low)) WITHOUT ROWID");
        createLanguageMetadataTable(database);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion == 1) {
            database.execSQL("DROP TABLE IF EXISTS canonical_documents");
            database.execSQL("DROP TABLE IF EXISTS canonical_postings");
            database.execSQL("DROP TABLE IF EXISTS canonical_chunks");
            database.execSQL("DROP TABLE IF EXISTS canonical_files");
            onCreate(database);
            return;
        }
        if (oldVersion == 2) {
            database.execSQL("CREATE TABLE canonical_files "
                    + "(file_path TEXT PRIMARY KEY) WITHOUT ROWID");
            oldVersion = 3;
        }
        if (oldVersion == 3) {
            createLanguageMetadataTable(database);
            oldVersion = 4;
        }
        if (oldVersion == 4) {
            database.execSQL("ALTER TABLE canonical_files ADD COLUMN generation "
                    + "TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE canonical_files ADD COLUMN state "
                    + "TEXT NOT NULL DEFAULT 'PUBLISHED'");
            oldVersion = 5;
        }
        if (oldVersion == newVersion) {
            return;
        }
        throw new IllegalStateException("Unsupported canonical index migration "
                + oldVersion + " -> " + newVersion);
    }

    public StoreStats replaceFile(
            String filePath,
            Map<Integer, Map<CanonicalHash, Integer>> chunkEvidence
    ) throws IOException {
        return replaceFile(filePath, chunkEvidence, Collections.emptyList());
    }

    public StoreStats replaceFile(
            String filePath,
            Map<Integer, Map<CanonicalHash, Integer>> chunkEvidence,
            List<LanguageMetadata> languageMetadata
    ) throws IOException {
        String generation = UUID.randomUUID().toString();
        markPending(filePath, generation);
        StoreStats stats = replaceFileIfCurrent(
                filePath, generation, chunkEvidence, languageMetadata);
        if (stats == null) {
            throw new IOException("Canonical generation was superseded");
        }
        return stats;
    }

    /**
     * Makes a generation authoritative before the primary Room transaction commits.
     * Existing evidence is retained but hidden until this generation is published.
     */
    public void markPending(String filePath, String generation) {
        requireIdentity(filePath, generation);
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            database.execSQL("INSERT OR REPLACE INTO canonical_files"
                            + "(file_path,generation,state) VALUES(?,?,?)",
                    new Object[]{filePath, generation, STATE_PENDING});
            database.delete("canonical_file_languages", "file_path=?",
                    new String[]{filePath});
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public String pendingGeneration(String filePath) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT generation FROM canonical_files WHERE file_path=? AND state=? LIMIT 1",
                new String[]{filePath, STATE_PENDING})) {
            return cursor.moveToFirst() ? cursor.getString(0) : null;
        }
    }

    public Map<String, String> pendingGenerations() {
        Map<String, String> generations = new LinkedHashMap<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT file_path,generation FROM canonical_files WHERE state=? "
                        + "ORDER BY file_path",
                new String[]{STATE_PENDING})) {
            while (cursor.moveToNext()) {
                generations.put(cursor.getString(0), cursor.getString(1));
            }
        }
        return generations;
    }

    public boolean isCurrentGeneration(String filePath, String generation) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT 1 FROM canonical_files WHERE file_path=? AND generation=? LIMIT 1",
                new String[]{filePath, generation})) {
            return cursor.moveToFirst();
        }
    }

    /**
     * Atomically replaces evidence only if no newer primary index has claimed the file.
     * A null result means this computation is stale and was discarded.
     */
    public StoreStats replaceFileIfCurrent(
            String filePath,
            String generation,
            Map<Integer, Map<CanonicalHash, Integer>> chunkEvidence,
            List<LanguageMetadata> languageMetadata
    ) throws IOException {
        requireIdentity(filePath, generation);
        if (chunkEvidence == null) {
            throw new IllegalArgumentException("file chunk evidence is required");
        }
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            if (!isCurrentGeneration(database, filePath, generation)) {
                return null;
            }
            deleteFile(database, filePath);
            int hashCount = 0;
            long evidenceBytes = 0L;
            Map<CanonicalHash, List<CanonicalPosting>> additions = new LinkedHashMap<>();
            List<Integer> chunkIndexes = new ArrayList<>(chunkEvidence.keySet());
            Collections.sort(chunkIndexes);
            for (Integer chunkIndex : chunkIndexes) {
                if (chunkIndex == null || chunkIndex < 0) {
                    throw new IllegalArgumentException("invalid canonical chunk index");
                }
                Map<CanonicalHash, Integer> frequencies = chunkEvidence.get(chunkIndex);
                if (frequencies == null) {
                    continue;
                }
                List<CanonicalHash> hashes = new ArrayList<>(frequencies.keySet());
                Collections.sort(hashes);
                for (CanonicalHash hash : hashes) {
                    Integer frequency = frequencies.get(hash);
                    if (hash == null || frequency == null || frequency <= 0) {
                        throw new IllegalArgumentException("canonical frequencies must be positive");
                    }
                    additions.computeIfAbsent(hash, ignored -> new ArrayList<>())
                            .add(new CanonicalPosting(
                                    filePath, chunkIndex, frequency, FIELD_BODY));
                }
                byte[] encodedHashes = encodeHashes(hashes);
                database.execSQL("INSERT INTO canonical_chunks "
                                + "(file_path,chunk_index,term_hashes) VALUES(?,?,?)",
                        new Object[]{filePath, chunkIndex, encodedHashes});
                hashCount += hashes.size();
                evidenceBytes += encodedHashes.length;
            }
            List<CanonicalHash> additionHashes = new ArrayList<>(additions.keySet());
            Collections.sort(additionHashes);
            for (CanonicalHash hash : additionHashes) {
                List<CanonicalPosting> postings = readPostings(database, hash);
                postings.removeIf(posting -> posting.getFilePath().equals(filePath));
                postings.addAll(additions.get(hash));
                writePostings(database, hash, postings);
            }
            database.execSQL("INSERT OR REPLACE INTO canonical_files"
                            + "(file_path,generation,state) VALUES(?,?,?)",
                    new Object[]{filePath, generation, STATE_PUBLISHED});
            for (LanguageMetadata metadata : languageMetadata) {
                database.execSQL("INSERT INTO canonical_file_languages "
                                + "(file_path,language_tag,translation_family,content_kind,"
                                + "chunk_count,character_count,confidence_sum,canonical_status,"
                                + "canonical_hash_count,translated_chunk_count,package_version) "
                                + "VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                        new Object[]{filePath, metadata.languageTag,
                                metadata.translationFamily, metadata.contentKind,
                                metadata.chunkCount, metadata.characterCount,
                                metadata.confidenceSum, metadata.canonicalStatus,
                                metadata.canonicalHashCount, metadata.translatedChunkCount,
                                metadata.packageVersion});
            }
            database.setTransactionSuccessful();
            return new StoreStats(chunkIndexes.size(), hashCount, evidenceBytes, 0L);
        } finally {
            database.endTransaction();
        }
    }

    public void deleteFile(String filePath) throws IOException {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            deleteFile(database, filePath);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public boolean deleteFileIfCurrent(String filePath, String generation) throws IOException {
        requireIdentity(filePath, generation);
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            if (!isCurrentGeneration(database, filePath, generation)) {
                return false;
            }
            deleteFile(database, filePath);
            database.setTransactionSuccessful();
            return true;
        } finally {
            database.endTransaction();
        }
    }

    public boolean hasFile(String filePath) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT 1 FROM canonical_files WHERE file_path=? AND state=? LIMIT 1",
                new String[]{filePath, STATE_PUBLISHED})) {
            return cursor.moveToFirst();
        }
    }

    public int countPendingFiles() {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM canonical_files WHERE state=?",
                new String[]{STATE_PENDING})) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    /** True only for rows written with the language-discovery schema. */
    public boolean hasLanguageMetadata(String filePath) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT 1 FROM canonical_file_languages WHERE file_path=? LIMIT 1",
                new String[]{filePath})) {
            return cursor.moveToFirst();
        }
    }

    public List<String> findFiles(String family, String status, int limit) {
        if (limit <= 0) return Collections.emptyList();
        List<String> paths = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT DISTINCT file_path FROM canonical_file_languages "
                        + "WHERE translation_family=? AND canonical_status=? "
                        + "ORDER BY file_path LIMIT ?",
                new String[]{family, status, Integer.toString(limit)})) {
            while (cursor.moveToNext()) paths.add(cursor.getString(0));
        }
        return paths;
    }

    public int countFiles(String family, String status) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(DISTINCT file_path) FROM canonical_file_languages "
                        + "WHERE translation_family=? AND canonical_status=?",
                new String[]{family, status})) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public List<LanguageSummary> languageSummaries() {
        List<LanguageSummary> summaries = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT language_tag,translation_family,content_kind,canonical_status,"
                        + "COUNT(DISTINCT file_path),SUM(chunk_count) "
                        + "FROM canonical_file_languages GROUP BY language_tag,"
                        + "translation_family,content_kind,canonical_status "
                        + "ORDER BY language_tag,content_kind,canonical_status", null)) {
            while (cursor.moveToNext()) {
                summaries.add(new LanguageSummary(cursor.getString(0), cursor.getString(1),
                        cursor.getString(2), cursor.getString(3), cursor.getInt(4),
                        cursor.getInt(5)));
            }
        }
        return Collections.unmodifiableList(summaries);
    }

    public List<FileLanguageProfile> fileLanguageProfiles() {
        LinkedHashMap<String, MutableFileProfile> profiles = new LinkedHashMap<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT file_path,language_tag,translation_family,content_kind,"
                        + "canonical_status,character_count FROM canonical_file_languages "
                        + "ORDER BY file_path,character_count DESC,language_tag", null)) {
            while (cursor.moveToNext()) {
                String path = cursor.getString(0);
                MutableFileProfile profile = profiles.get(path);
                if (profile == null) {
                    profile = new MutableFileProfile(path, cursor.getString(3));
                    profiles.put(path, profile);
                }
                profile.languages.add(cursor.getString(1));
                profile.families.add(cursor.getString(2));
                profile.statuses.add(cursor.getString(4));
            }
        }
        List<FileLanguageProfile> results = new ArrayList<>();
        for (MutableFileProfile profile : profiles.values()) results.add(profile.freeze());
        return Collections.unmodifiableList(results);
    }

    public List<Match> lookup(CanonicalHash hash, int limit) throws IOException {
        return lookup(Collections.singletonList(hash), false, limit);
    }

    /** Deterministic OR by default; intersection=true requires every distinct hash. */
    public List<Match> lookup(
            List<CanonicalHash> hashes,
            boolean intersection,
            int limit
    ) throws IOException {
        if (hashes == null || limit <= 0) {
            return Collections.emptyList();
        }
        LinkedHashMap<String, MutableMatch> merged = new LinkedHashMap<>();
        Set<String> publishedFiles = publishedFiles(getReadableDatabase());
        List<CanonicalHash> unique = new ArrayList<>();
        for (CanonicalHash hash : hashes) {
            if (hash != null && !unique.contains(hash)) {
                unique.add(hash);
            }
        }
        for (CanonicalHash hash : unique) {
            for (CanonicalPosting posting : readPostings(getReadableDatabase(), hash)) {
                if (!publishedFiles.contains(posting.getFilePath())) {
                    continue;
                }
                String key = posting.getFilePath() + '\u0000' + posting.getChunkIndex();
                MutableMatch match = merged.get(key);
                if (match == null) {
                    match = new MutableMatch(posting.getFilePath(), posting.getChunkIndex());
                    merged.put(key, match);
                }
                match.matchedTerms++;
                match.totalFrequency += posting.getTermFrequency();
            }
        }
        List<Match> results = new ArrayList<>();
        for (MutableMatch match : merged.values()) {
            if (!intersection || match.matchedTerms == unique.size()) {
                results.add(new Match(match.filePath, match.chunkIndex,
                        match.matchedTerms, match.totalFrequency));
            }
        }
        results.sort(Comparator.comparingInt(Match::getMatchedTerms).reversed()
                .thenComparing(Comparator.comparingInt(Match::getTotalFrequency).reversed())
                .thenComparing(Match::getFilePath)
                .thenComparingInt(Match::getChunkIndex));
        return Collections.unmodifiableList(results.subList(0, Math.min(limit, results.size())));
    }

    public long databaseBytes(Context context) {
        File database = context.getDatabasePath(DATABASE_NAME);
        return fileBytes(database) + fileBytes(new File(database.getPath() + "-wal"))
                + fileBytes(new File(database.getPath() + "-shm"));
    }

    private static void deleteFile(SQLiteDatabase database, String filePath) throws IOException {
        java.util.LinkedHashSet<CanonicalHash> affectedHashes =
                new java.util.LinkedHashSet<>();
        try (Cursor cursor = database.rawQuery(
                "SELECT chunk_index,term_hashes FROM canonical_chunks WHERE file_path=?",
                new String[]{filePath})) {
            while (cursor.moveToNext()) {
                affectedHashes.addAll(decodeHashes(cursor.getBlob(1)));
            }
        }
        List<CanonicalHash> sortedHashes = new ArrayList<>(affectedHashes);
        Collections.sort(sortedHashes);
        for (CanonicalHash hash : sortedHashes) {
            List<CanonicalPosting> postings = readPostings(database, hash);
            postings.removeIf(posting -> posting.getFilePath().equals(filePath));
            if (postings.isEmpty()) {
                database.delete("canonical_postings", "hash_high=? AND hash_low=?",
                        new String[]{Long.toString(hash.getHigh()),
                                Long.toString(hash.getLow())});
            } else {
                writePostings(database, hash, postings);
            }
        }
        database.delete("canonical_chunks", "file_path=?", new String[]{filePath});
        database.delete("canonical_file_languages", "file_path=?", new String[]{filePath});
        database.delete("canonical_files", "file_path=?", new String[]{filePath});
    }

    private static boolean isCurrentGeneration(
            SQLiteDatabase database, String filePath, String generation) {
        try (Cursor cursor = database.rawQuery(
                "SELECT 1 FROM canonical_files WHERE file_path=? AND generation=? LIMIT 1",
                new String[]{filePath, generation})) {
            return cursor.moveToFirst();
        }
    }

    private static Set<String> publishedFiles(SQLiteDatabase database) {
        Set<String> paths = new HashSet<>();
        try (Cursor cursor = database.rawQuery(
                "SELECT file_path FROM canonical_files WHERE state=?",
                new String[]{STATE_PUBLISHED})) {
            while (cursor.moveToNext()) {
                paths.add(cursor.getString(0));
            }
        }
        return paths;
    }

    private static void requireIdentity(String filePath, String generation) {
        if (filePath == null || filePath.trim().isEmpty()
                || generation == null || generation.trim().isEmpty()) {
            throw new IllegalArgumentException("file path and generation are required");
        }
    }

    private static void createLanguageMetadataTable(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE IF NOT EXISTS canonical_file_languages ("
                + "file_path TEXT NOT NULL,language_tag TEXT NOT NULL,"
                + "translation_family TEXT NOT NULL,content_kind TEXT NOT NULL,"
                + "chunk_count INTEGER NOT NULL,character_count INTEGER NOT NULL,"
                + "confidence_sum REAL NOT NULL,canonical_status TEXT NOT NULL,"
                + "canonical_hash_count INTEGER NOT NULL,translated_chunk_count INTEGER NOT NULL,"
                + "package_version TEXT NOT NULL,PRIMARY KEY(file_path,language_tag))");
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_canonical_language_status "
                + "ON canonical_file_languages(translation_family,canonical_status)");
    }

    private static void removePosting(SQLiteDatabase database, CanonicalHash hash,
            String filePath, int chunkIndex) throws IOException {
        List<CanonicalPosting> postings = readPostings(database, hash);
        postings.removeIf(posting -> posting.getFilePath().equals(filePath)
                && posting.getChunkIndex() == chunkIndex);
        if (postings.isEmpty()) {
            database.delete("canonical_postings", "hash_high=? AND hash_low=?",
                    new String[]{Long.toString(hash.getHigh()), Long.toString(hash.getLow())});
        } else {
            writePostings(database, hash, postings);
        }
    }

    private static void addPosting(SQLiteDatabase database, CanonicalHash hash,
            CanonicalPosting newPosting) throws IOException {
        List<CanonicalPosting> postings = readPostings(database, hash);
        postings.removeIf(posting -> posting.getFilePath().equals(newPosting.getFilePath())
                && posting.getChunkIndex() == newPosting.getChunkIndex());
        postings.add(newPosting);
        writePostings(database, hash, postings);
    }

    private static List<CanonicalPosting> readPostings(SQLiteDatabase database,
            CanonicalHash hash) throws IOException {
        try (Cursor cursor = database.rawQuery(
                "SELECT postings FROM canonical_postings WHERE hash_high=? AND hash_low=?",
                new String[]{Long.toString(hash.getHigh()), Long.toString(hash.getLow())})) {
            return cursor.moveToFirst()
                    ? new ArrayList<>(CanonicalPostingCodec.decode(cursor.getBlob(0)))
                    : new ArrayList<>();
        }
    }

    private static void writePostings(SQLiteDatabase database, CanonicalHash hash,
            List<CanonicalPosting> postings) {
        database.execSQL("INSERT OR REPLACE INTO canonical_postings "
                        + "(hash_high,hash_low,postings) VALUES(?,?,?)",
                new Object[]{hash.getHigh(), hash.getLow(),
                        CanonicalPostingCodec.encode(postings)});
    }

    private static byte[] encodeHashes(List<CanonicalHash> hashes) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(8 + hashes.size() * 16);
        output.write(EVIDENCE_FORMAT_VERSION);
        writeVarLong(output, hashes.size());
        for (CanonicalHash hash : hashes) {
            byte[] bytes = hash.toBytes();
            output.write(bytes, 0, bytes.length);
        }
        return output.toByteArray();
    }

    private static List<CanonicalHash> decodeHashes(byte[] encoded) throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(encoded);
        if (input.read() != EVIDENCE_FORMAT_VERSION) throw new IOException("Invalid evidence version");
        long count = readVarLong(input);
        if (count > Integer.MAX_VALUE) throw new IOException("Canonical evidence is too large");
        List<CanonicalHash> hashes = new ArrayList<>((int) count);
        byte[] buffer = new byte[CanonicalHash.BYTE_COUNT];
        for (int index = 0; index < count; index++) {
            int offset = 0;
            while (offset < buffer.length) {
                int read = input.read(buffer, offset, buffer.length - offset);
                if (read < 0) throw new EOFException("Truncated canonical evidence");
                offset += read;
            }
            hashes.add(CanonicalHash.fromBytes(buffer, 0));
        }
        if (input.available() != 0) throw new IOException("Trailing canonical evidence bytes");
        return hashes;
    }

    private static void writeVarLong(ByteArrayOutputStream output, long value) {
        while ((value & ~0x7FL) != 0L) {
            output.write((int) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        output.write((int) value);
    }

    private static long readVarLong(ByteArrayInputStream input) throws IOException {
        long value = 0L;
        for (int shift = 0; shift < 64; shift += 7) {
            int current = input.read();
            if (current < 0) throw new EOFException("Truncated canonical varint");
            value |= (long) (current & 0x7F) << shift;
            if ((current & 0x80) == 0) return value;
        }
        throw new IOException("Canonical varint is too long");
    }

    private static long fileBytes(File file) { return file.isFile() ? file.length() : 0L; }

    private static final class MutableMatch {
        final String filePath;
        final int chunkIndex;
        int matchedTerms;
        int totalFrequency;
        MutableMatch(String filePath, int chunkIndex) {
            this.filePath = filePath;
            this.chunkIndex = chunkIndex;
        }
    }

    private static final class MutableFileProfile {
        final String filePath, contentKind;
        final List<String> languages = new ArrayList<>();
        final List<String> families = new ArrayList<>();
        final List<String> statuses = new ArrayList<>();
        MutableFileProfile(String filePath, String contentKind) {
            this.filePath = filePath; this.contentKind = contentKind;
        }
        FileLanguageProfile freeze() {
            return new FileLanguageProfile(filePath, contentKind, languages.get(0),
                    languages.size() > 1, languages, families, statuses);
        }
    }

    public static final class Match {
        private final String filePath;
        private final int chunkIndex;
        private final int matchedTerms;
        private final int totalFrequency;
        Match(String filePath, int chunkIndex, int matchedTerms, int totalFrequency) {
            this.filePath = filePath;
            this.chunkIndex = chunkIndex;
            this.matchedTerms = matchedTerms;
            this.totalFrequency = totalFrequency;
        }
        public String getFilePath() { return filePath; }
        public int getChunkIndex() { return chunkIndex; }
        public int getMatchedTerms() { return matchedTerms; }
        public int getTotalFrequency() { return totalFrequency; }
    }

    public static final class StoreStats {
        public final int chunkCount;
        public final int hashCount;
        public final long chunkEvidenceBytes;
        public final long postingBytes;
        StoreStats(int chunkCount, int hashCount, long chunkEvidenceBytes, long postingBytes) {
            this.chunkCount = chunkCount;
            this.hashCount = hashCount;
            this.chunkEvidenceBytes = chunkEvidenceBytes;
            this.postingBytes = postingBytes;
        }
    }

    public static final class LanguageMetadata {
        public final String languageTag;
        public final String translationFamily;
        public final String contentKind;
        public final int chunkCount;
        public final int characterCount;
        public final float confidenceSum;
        public final String canonicalStatus;
        public final int canonicalHashCount;
        public final int translatedChunkCount;
        public final String packageVersion;

        public LanguageMetadata(String languageTag, String translationFamily,
                String contentKind, int chunkCount, int characterCount,
                float confidenceSum, String canonicalStatus, int canonicalHashCount,
                int translatedChunkCount, String packageVersion) {
            this.languageTag = languageTag;
            this.translationFamily = translationFamily;
            this.contentKind = contentKind;
            this.chunkCount = chunkCount;
            this.characterCount = characterCount;
            this.confidenceSum = confidenceSum;
            this.canonicalStatus = canonicalStatus;
            this.canonicalHashCount = canonicalHashCount;
            this.translatedChunkCount = translatedChunkCount;
            this.packageVersion = packageVersion;
        }
    }

    public static final class LanguageSummary {
        public final String languageTag;
        public final String translationFamily;
        public final String contentKind;
        public final String canonicalStatus;
        public final int fileCount;
        public final int chunkCount;

        LanguageSummary(String languageTag, String translationFamily, String contentKind,
                String canonicalStatus, int fileCount, int chunkCount) {
            this.languageTag = languageTag;
            this.translationFamily = translationFamily;
            this.contentKind = contentKind;
            this.canonicalStatus = canonicalStatus;
            this.fileCount = fileCount;
            this.chunkCount = chunkCount;
        }
    }

    public static final class FileLanguageProfile {
        public final String filePath;
        public final String contentKind;
        public final String dominantLanguage;
        public final boolean mixedLanguage;
        public final List<String> languages;
        public final List<String> translationFamilies;
        public final List<String> canonicalStatuses;
        FileLanguageProfile(String filePath, String contentKind, String dominantLanguage,
                boolean mixedLanguage, List<String> languages, List<String> families,
                List<String> statuses) {
            this.filePath = filePath; this.contentKind = contentKind;
            this.dominantLanguage = dominantLanguage; this.mixedLanguage = mixedLanguage;
            this.languages = Collections.unmodifiableList(new ArrayList<>(languages));
            this.translationFamilies = Collections.unmodifiableList(new ArrayList<>(families));
            this.canonicalStatuses = Collections.unmodifiableList(new ArrayList<>(statuses));
        }
    }
}
