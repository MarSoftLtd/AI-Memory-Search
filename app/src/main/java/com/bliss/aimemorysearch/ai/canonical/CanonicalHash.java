package com.bliss.aimemorysearch.ai.canonical;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Locale;

public final class CanonicalHash implements Comparable<CanonicalHash> {

    public static final int BYTE_COUNT = 16;
    private static final String ALGORITHM = "SHA-256";
    private static final String DOMAIN = "AI_MEMORY_SEARCH_CANONICAL_HASH";

    private final long high;
    private final long low;

    public CanonicalHash(long high, long low) {
        this.high = high;
        this.low = low;
    }

    public static CanonicalHash fromCanonicalText(
            String canonicalText,
            String normalizationVersion
    ) {
        if (normalizationVersion == null
                || normalizationVersion.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "normalizationVersion must not be empty"
            );
        }

        String normalized = normalize(canonicalText);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "canonicalText must contain searchable text"
            );
        }

        MessageDigest digest = newDigest();
        updateField(digest, DOMAIN);
        updateField(digest, normalizationVersion.trim());
        updateField(digest, normalized);
        return fromBytes(digest.digest(), 0);
    }

    public static CanonicalHash fromBytes(byte[] bytes, int offset) {
        if (bytes == null
                || offset < 0
                || bytes.length - offset < BYTE_COUNT) {
            throw new IllegalArgumentException(
                    "A canonical hash requires 16 bytes"
            );
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes, offset, BYTE_COUNT);
        return new CanonicalHash(buffer.getLong(), buffer.getLong());
    }

    public long getHigh() {
        return high;
    }

    public long getLow() {
        return low;
    }

    public byte[] toBytes() {
        return ByteBuffer.allocate(BYTE_COUNT)
                .putLong(high)
                .putLong(low)
                .array();
    }

    @Override
    public int compareTo(CanonicalHash other) {
        int highComparison = Long.compareUnsigned(high, other.high);
        return highComparison != 0
                ? highComparison
                : Long.compareUnsigned(low, other.low);
    }

    @Override
    public boolean equals(Object value) {
        if (this == value) {
            return true;
        }
        if (!(value instanceof CanonicalHash)) {
            return false;
        }
        CanonicalHash other = (CanonicalHash) value;
        return high == other.high && low == other.low;
    }

    @Override
    public int hashCode() {
        return 31 * Long.hashCode(high) + Long.hashCode(low);
    }

    @Override
    public String toString() {
        return String.format(
                Locale.ROOT,
                "%016x%016x",
                high,
                low
        );
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static void updateField(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES)
                .putInt(bytes.length)
                .array());
        digest.update(bytes);
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 is required by the Android runtime",
                    e
            );
        }
    }
}
