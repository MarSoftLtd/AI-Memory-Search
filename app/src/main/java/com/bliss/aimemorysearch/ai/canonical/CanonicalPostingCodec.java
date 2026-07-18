package com.bliss.aimemorysearch.ai.canonical;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.nio.charset.StandardCharsets;

public final class CanonicalPostingCodec {

    private static final int FORMAT_VERSION = 2;
    private static final int MAX_POSTINGS = 100_000_000;

    private CanonicalPostingCodec() {
    }

    public static byte[] encode(List<CanonicalPosting> postings) {
        if (postings == null) {
            throw new IllegalArgumentException("postings must not be null");
        }
        for (CanonicalPosting posting : postings) {
            if (posting == null) {
                throw new IllegalArgumentException(
                        "postings must not contain null"
                );
            }
        }
        List<CanonicalPosting> sorted = new ArrayList<>(postings);
        sorted.sort(Comparator.comparing(CanonicalPosting::getFilePath)
                .thenComparingInt(CanonicalPosting::getChunkIndex));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(FORMAT_VERSION);
        writeUnsignedVarLong(output, sorted.size());
        String previousPath = null;
        int previousChunkIndex = 0;
        for (CanonicalPosting posting : sorted) {
            String filePath = posting.getFilePath();
            boolean samePath = filePath.equals(previousPath);
            if (samePath && posting.getChunkIndex() <= previousChunkIndex) {
                throw new IllegalArgumentException(
                        "chunk identities must be unique"
                );
            }
            output.write(samePath ? 0 : 1);
            if (!samePath) {
                byte[] pathBytes = filePath.getBytes(StandardCharsets.UTF_8);
                writeUnsignedVarLong(output, pathBytes.length);
                output.write(pathBytes, 0, pathBytes.length);
                previousChunkIndex = 0;
            }
            writeUnsignedVarLong(output, samePath
                    ? posting.getChunkIndex() - previousChunkIndex
                    : posting.getChunkIndex());
            writeUnsignedVarLong(output, posting.getTermFrequency());
            output.write(posting.getFieldFlags());
            previousPath = filePath;
            previousChunkIndex = posting.getChunkIndex();
        }
        return output.toByteArray();
    }

    public static List<CanonicalPosting> decode(byte[] encoded)
            throws IOException {
        if (encoded == null || encoded.length == 0) {
            throw new IOException("Canonical posting block is empty");
        }
        ByteArrayInputStream input = new ByteArrayInputStream(encoded);
        int formatVersion = input.read();
        if (formatVersion != FORMAT_VERSION) {
            throw new IOException(
                    "Unsupported canonical posting format: "
                            + formatVersion
            );
        }
        long countValue = readUnsignedVarLong(input);
        if (countValue > MAX_POSTINGS) {
            throw new IOException("Canonical posting count is too large");
        }
        int count = (int) countValue;
        List<CanonicalPosting> postings = new ArrayList<>(count);
        String previousPath = null;
        int previousChunkIndex = 0;
        for (int index = 0; index < count; index++) {
            int pathMarker = input.read();
            if (pathMarker < 0 || pathMarker > 1) {
                throw new IOException("Invalid canonical path marker");
            }
            if (pathMarker == 1) {
                long pathLength = readUnsignedVarLong(input);
                if (pathLength == 0 || pathLength > 65535 || pathLength > input.available()) {
                    throw new IOException("Invalid canonical path length");
                }
                byte[] pathBytes = new byte[(int) pathLength];
                if (input.read(pathBytes, 0, pathBytes.length) != pathBytes.length) {
                    throw new EOFException("Truncated canonical path");
                }
                previousPath = new String(pathBytes, StandardCharsets.UTF_8);
                previousChunkIndex = 0;
            } else if (previousPath == null) {
                throw new IOException("Missing canonical path");
            }
            long chunkDelta = readUnsignedVarLong(input);
            long chunkIndexValue = pathMarker == 0
                    ? (long) previousChunkIndex + chunkDelta
                    : chunkDelta;
            if (chunkIndexValue > Integer.MAX_VALUE
                    || (pathMarker == 0 && chunkDelta == 0)) {
                throw new IOException("Invalid canonical chunk index");
            }
            long frequency = readUnsignedVarLong(input);
            if (frequency == 0L || frequency > Integer.MAX_VALUE) {
                throw new IOException("Invalid canonical term frequency");
            }
            int fieldFlags = input.read();
            if (fieldFlags < 0) {
                throw new EOFException("Truncated canonical posting block");
            }
            postings.add(new CanonicalPosting(
                    previousPath,
                    (int) chunkIndexValue,
                    (int) frequency,
                    fieldFlags
            ));
            previousChunkIndex = (int) chunkIndexValue;
        }
        if (input.available() != 0) {
            throw new IOException(
                    "Trailing bytes in canonical posting block"
            );
        }
        return Collections.unmodifiableList(postings);
    }

    private static void writeUnsignedVarLong(
            ByteArrayOutputStream output,
            long value
    ) {
        if (value < 0L) {
            throw new IllegalArgumentException(
                    "Variable-length values must not be negative"
            );
        }
        while ((value & ~0x7FL) != 0L) {
            output.write((int) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        output.write((int) value);
    }

    private static long readUnsignedVarLong(ByteArrayInputStream input)
            throws IOException {
        long value = 0L;
        for (int shift = 0; shift < 64; shift += 7) {
            int current = input.read();
            if (current < 0) {
                throw new EOFException(
                        "Truncated variable-length integer"
                );
            }
            if (shift == 63 && (current & 0xFE) != 0) {
                throw new IOException(
                        "Variable-length integer overflow"
                );
            }
            value |= (long) (current & 0x7F) << shift;
            if ((current & 0x80) == 0) {
                return value;
            }
        }
        throw new IOException("Variable-length integer is too long");
    }
}
