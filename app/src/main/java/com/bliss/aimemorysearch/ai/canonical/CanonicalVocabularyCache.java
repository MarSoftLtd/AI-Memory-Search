package com.bliss.aimemorysearch.ai.canonical;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class CanonicalVocabularyCache implements AutoCloseable {

    private static final int MAGIC = 0x43414E56;
    private static final int FORMAT_VERSION = 2;
    private static final int MAX_ENTRY_COUNT = 10_000_000;
    private static final int MAX_STRING_BYTES = 4096;
    private static final int MAX_HASHES_PER_ENTRY = 4096;

    private final File file;
    private final Map<CacheKey, CanonicalVocabularyEntry> entries =
            new HashMap<>();
    private final ReentrantReadWriteLock lock =
            new ReentrantReadWriteLock();

    private long revision;
    private boolean dirty;

    public CanonicalVocabularyCache(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        this.file = file;
        load();
    }

    public CanonicalVocabularyEntry lookup(
            String sourceExpression,
            CanonicalTranslationMetadata metadata
    ) {
        CacheKey key = createKey(sourceExpression, metadata);
        lock.readLock().lock();
        try {
            return entries.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void insert(
            String sourceExpression,
            List<CanonicalHash> canonicalHashes,
            CanonicalTranslationMetadata metadata
    ) {
        CacheKey key = createKey(sourceExpression, metadata);
        CanonicalVocabularyEntry entry = new CanonicalVocabularyEntry(
                key.sourceHash,
                canonicalHashes,
                metadata
        );
        lock.writeLock().lock();
        try {
            if (entries.containsKey(key)) {
                throw new IllegalStateException(
                        "A canonical vocabulary entry already exists"
                );
            }
            entries.put(key, entry);
            revision++;
            dirty = true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void update(
            String sourceExpression,
            List<CanonicalHash> canonicalHashes,
            CanonicalTranslationMetadata metadata
    ) {
        CacheKey key = createKey(sourceExpression, metadata);
        CanonicalVocabularyEntry entry = new CanonicalVocabularyEntry(
                key.sourceHash,
                canonicalHashes,
                metadata
        );
        lock.writeLock().lock();
        try {
            if (!entries.containsKey(key)) {
                throw new IllegalStateException(
                        "Cannot update a missing canonical vocabulary entry"
                );
            }
            entries.put(key, entry);
            revision++;
            dirty = true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int invalidate() {
        lock.writeLock().lock();
        try {
            int removed = entries.size();
            if (removed > 0) {
                entries.clear();
                revision++;
                dirty = true;
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int invalidate(
            String language,
            String family,
            String normalizationVersion,
            String translationModelVersion
    ) {
        CanonicalTranslationMetadata scope =
                new CanonicalTranslationMetadata(
                        language,
                        family,
                        0f,
                        normalizationVersion,
                        translationModelVersion
                );
        lock.writeLock().lock();
        try {
            int before = entries.size();
            entries.entrySet().removeIf(item ->
                    item.getKey().matches(scope));
            int removed = before - entries.size();
            if (removed > 0) {
                revision++;
                dirty = true;
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public long version() {
        lock.readLock().lock();
        try {
            return revision;
        } finally {
            lock.readLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try {
            return entries.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void flush() throws IOException {
        lock.writeLock().lock();
        try {
            if (!dirty) {
                return;
            }
            File parent = file.getAbsoluteFile().getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException(
                        "Unable to create canonical cache directory"
                );
            }
            File temporary = new File(file.getPath() + ".tmp");
            writeSnapshot(temporary);
            replaceAtomically(temporary, file);
            dirty = false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void close() throws IOException {
        flush();
    }

    private CacheKey createKey(
            String sourceExpression,
            CanonicalTranslationMetadata metadata
    ) {
        if (metadata == null) {
            throw new IllegalArgumentException("metadata must not be null");
        }
        CanonicalHash sourceHash = CanonicalHash.fromCanonicalText(
                sourceExpression,
                metadata.getNormalizationVersion()
        );
        return new CacheKey(sourceHash, metadata);
    }

    private void load() throws IOException {
        if (!file.exists()) {
            revision = 0L;
            dirty = false;
            return;
        }
        try (DataInputStream input = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            if (input.readInt() != MAGIC) {
                throw new IOException("Invalid canonical cache magic");
            }
            int formatVersion = input.readInt();
            if (formatVersion != FORMAT_VERSION) {
                throw new IOException(
                        "Unsupported canonical cache format: "
                                + formatVersion
                );
            }
            revision = input.readLong();
            int count = input.readInt();
            if (count < 0 || count > MAX_ENTRY_COUNT) {
                throw new IOException("Invalid canonical cache entry count");
            }
            for (int index = 0; index < count; index++) {
                CanonicalHash sourceHash = readHash(input);
                CanonicalTranslationMetadata metadata =
                        new CanonicalTranslationMetadata(
                                readString(input),
                                readString(input),
                                input.readFloat(),
                                readString(input),
                                readString(input)
                        );
                int hashCount = input.readUnsignedShort();
                if (hashCount == 0 || hashCount > MAX_HASHES_PER_ENTRY) {
                    throw new IOException(
                            "Invalid canonical hash count: " + hashCount
                    );
                }
                List<CanonicalHash> canonicalHashes =
                        new ArrayList<>(hashCount);
                for (int hashIndex = 0;
                        hashIndex < hashCount;
                        hashIndex++) {
                    canonicalHashes.add(readHash(input));
                }
                CacheKey key = new CacheKey(sourceHash, metadata);
                if (entries.put(
                        key,
                        new CanonicalVocabularyEntry(
                                sourceHash,
                                canonicalHashes,
                                metadata
                        )) != null) {
                    throw new IOException(
                            "Duplicate canonical cache key"
                    );
                }
            }
            if (input.read() != -1) {
                throw new IOException(
                        "Trailing bytes in canonical cache"
                );
            }
            dirty = false;
        } catch (EOFException e) {
            throw new IOException("Truncated canonical cache", e);
        }
    }

    private void writeSnapshot(File destination) throws IOException {
        List<Map.Entry<CacheKey, CanonicalVocabularyEntry>> sorted =
                new ArrayList<>(entries.entrySet());
        sorted.sort(Map.Entry.comparingByKey());
        try (FileOutputStream stream = new FileOutputStream(destination);
                DataOutputStream output = new DataOutputStream(
                        new BufferedOutputStream(stream))) {
            output.writeInt(MAGIC);
            output.writeInt(FORMAT_VERSION);
            output.writeLong(revision);
            output.writeInt(sorted.size());
            for (Map.Entry<CacheKey, CanonicalVocabularyEntry> item
                    : sorted) {
                CanonicalVocabularyEntry entry = item.getValue();
                CanonicalTranslationMetadata metadata =
                        entry.getMetadata();
                writeHash(output, entry.getSourceHash());
                writeString(output, metadata.getLanguage());
                writeString(output, metadata.getFamily());
                output.writeFloat(metadata.getConfidence());
                writeString(output, metadata.getNormalizationVersion());
                writeString(output, metadata.getTranslationModelVersion());
                int hashCount = entry.getCanonicalHashes().size();
                if (hashCount > MAX_HASHES_PER_ENTRY) {
                    throw new IOException(
                            "Too many canonical hashes for one entry"
                    );
                }
                output.writeShort(hashCount);
                for (CanonicalHash hash : entry.getCanonicalHashes()) {
                    writeHash(output, hash);
                }
            }
            output.flush();
            stream.getFD().sync();
        }
    }

    private static void replaceAtomically(File temporary, File target)
            throws IOException {
        try {
            Files.move(
                    temporary.toPath(),
                    target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(
                    temporary.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    private static CanonicalHash readHash(DataInputStream input)
            throws IOException {
        return new CanonicalHash(input.readLong(), input.readLong());
    }

    private static void writeHash(
            DataOutputStream output,
            CanonicalHash hash
    ) throws IOException {
        output.writeLong(hash.getHigh());
        output.writeLong(hash.getLow());
    }

    private static String readString(DataInputStream input)
            throws IOException {
        int length = input.readUnsignedShort();
        if (length > MAX_STRING_BYTES) {
            throw new IOException("Canonical cache string is too long");
        }
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void writeString(
            DataOutputStream output,
            String value
    ) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING_BYTES) {
            throw new IOException("Canonical cache string is too long");
        }
        output.writeShort(bytes.length);
        output.write(bytes);
    }

    private static final class CacheKey
            implements Comparable<CacheKey> {

        private final CanonicalHash sourceHash;
        private final String language;
        private final String family;
        private final String normalizationVersion;
        private final String translationModelVersion;

        private CacheKey(
                CanonicalHash sourceHash,
                CanonicalTranslationMetadata metadata
        ) {
            this.sourceHash = sourceHash;
            this.language = metadata.getLanguage();
            this.family = metadata.getFamily();
            this.normalizationVersion =
                    metadata.getNormalizationVersion();
            this.translationModelVersion =
                    metadata.getTranslationModelVersion();
        }

        private boolean matches(CanonicalTranslationMetadata metadata) {
            return language.equals(metadata.getLanguage())
                    && family.equals(metadata.getFamily())
                    && normalizationVersion.equals(
                    metadata.getNormalizationVersion())
                    && translationModelVersion.equals(
                    metadata.getTranslationModelVersion());
        }

        @Override
        public int compareTo(CacheKey other) {
            int comparison = language.compareTo(other.language);
            if (comparison != 0) {
                return comparison;
            }
            comparison = family.compareTo(other.family);
            if (comparison != 0) {
                return comparison;
            }
            comparison = normalizationVersion.compareTo(
                    other.normalizationVersion
            );
            if (comparison != 0) {
                return comparison;
            }
            comparison = translationModelVersion.compareTo(
                    other.translationModelVersion
            );
            return comparison != 0
                    ? comparison
                    : sourceHash.compareTo(other.sourceHash);
        }

        @Override
        public boolean equals(Object value) {
            if (this == value) {
                return true;
            }
            if (!(value instanceof CacheKey)) {
                return false;
            }
            CacheKey other = (CacheKey) value;
            return sourceHash.equals(other.sourceHash)
                    && language.equals(other.language)
                    && family.equals(other.family)
                    && normalizationVersion.equals(
                    other.normalizationVersion)
                    && translationModelVersion.equals(
                    other.translationModelVersion);
        }

        @Override
        public int hashCode() {
            int result = sourceHash.hashCode();
            result = 31 * result + language.hashCode();
            result = 31 * result + family.hashCode();
            result = 31 * result + normalizationVersion.hashCode();
            result = 31 * result + translationModelVersion.hashCode();
            return result;
        }
    }
}
