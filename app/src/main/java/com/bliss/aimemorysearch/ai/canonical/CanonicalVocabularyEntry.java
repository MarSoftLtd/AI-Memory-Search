package com.bliss.aimemorysearch.ai.canonical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public final class CanonicalVocabularyEntry {

    private final CanonicalHash sourceHash;
    private final List<CanonicalHash> canonicalHashes;
    private final CanonicalTranslationMetadata metadata;

    public CanonicalVocabularyEntry(
            CanonicalHash sourceHash,
            List<CanonicalHash> canonicalHashes,
            CanonicalTranslationMetadata metadata
    ) {
        if (sourceHash == null) {
            throw new IllegalArgumentException("sourceHash must not be null");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("metadata must not be null");
        }
        if (canonicalHashes == null || canonicalHashes.isEmpty()) {
            throw new IllegalArgumentException(
                    "canonicalHashes must not be empty"
            );
        }
        LinkedHashSet<CanonicalHash> unique = new LinkedHashSet<>();
        for (CanonicalHash hash : canonicalHashes) {
            if (hash == null) {
                throw new IllegalArgumentException(
                        "canonicalHashes must not contain null"
                );
            }
            unique.add(hash);
        }
        this.sourceHash = sourceHash;
        this.canonicalHashes = Collections.unmodifiableList(
                new ArrayList<>(unique)
        );
        this.metadata = metadata;
    }

    public CanonicalHash getSourceHash() {
        return sourceHash;
    }

    public List<CanonicalHash> getCanonicalHashes() {
        return canonicalHashes;
    }

    public CanonicalTranslationMetadata getMetadata() {
        return metadata;
    }
}
