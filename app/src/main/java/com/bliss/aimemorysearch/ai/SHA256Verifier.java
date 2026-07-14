package com.bliss.aimemorysearch.ai;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public final class SHA256Verifier {
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final int SHA256_BYTES = 32;

    private SHA256Verifier() {
    }

    public static boolean verify(File file, String expectedSha) throws IOException {
        Objects.requireNonNull(file, "file");
        byte[] expectedDigest = decodeHex(expectedSha);
        if (expectedDigest == null) {
            return false;
        }

        MessageDigest digest = newDigest();
        try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            while ((count = input.read(buffer)) != -1) {
                digest.update(buffer, 0, count);
            }
        }
        return MessageDigest.isEqual(expectedDigest, digest.digest());
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static byte[] decodeHex(String value) {
        if (value == null || value.length() != SHA256_BYTES * 2) {
            return null;
        }
        byte[] decoded = new byte[SHA256_BYTES];
        for (int i = 0; i < decoded.length; i++) {
            int high = Character.digit(value.charAt(i * 2), 16);
            int low = Character.digit(value.charAt(i * 2 + 1), 16);
            if (high < 0 || low < 0) {
                return null;
            }
            decoded[i] = (byte) ((high << 4) | low);
        }
        return decoded;
    }
}
