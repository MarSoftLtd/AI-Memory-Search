package com.bliss.aimemorysearch.ai;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

public final class PackageVerification {

    public PackageVerificationResult verify(
            File packageFile,
            AiPackageInfo packageInfo
    ) {
        if (packageInfo == null) {
            return failure(
                    "",
                    "",
                    "Package metadata is missing",
                    null
            );
        }

        return verifySha256(
                packageFile,
                packageInfo.getChecksumSha256()
        );
    }

    public PackageVerificationResult verifySha256(
            File file,
            String expectedChecksumSha256
    ) {
        if (
                expectedChecksumSha256 == null
                        ||
                        expectedChecksumSha256.trim().isEmpty()
        ) {
            return new PackageVerificationResult(
                    true,
                    expectedChecksumSha256,
                    "",
                    false,
                    "Checksum not provided",
                    null
            );
        }

        if (
                file == null
                        ||
                        !file.isFile()
        ) {
            return failure(
                    expectedChecksumSha256,
                    "",
                    "Package file is missing",
                    null
            );
        }

        try {
            String actualChecksum =
                    calculateSha256(
                            file
                    );

            boolean success =
                    expectedChecksumSha256.trim()
                            .equalsIgnoreCase(
                                    actualChecksum
                            );

            return new PackageVerificationResult(
                    success,
                    expectedChecksumSha256,
                    actualChecksum,
                    false,
                    success
                            ? "Checksum verified"
                            : "Checksum mismatch",
                    null
            );
        } catch (Exception e) {
            return failure(
                    expectedChecksumSha256,
                    "",
                    "Checksum verification failed",
                    e
            );
        }
    }

    private static PackageVerificationResult failure(
            String expectedChecksum,
            String actualChecksum,
            String message,
            Throwable error
    ) {
        return new PackageVerificationResult(
                false,
                expectedChecksum,
                actualChecksum,
                false,
                message,
                error
        );
    }

    private static String calculateSha256(
            File file
    ) throws Exception {
        MessageDigest digest =
                MessageDigest.getInstance(
                        "SHA-256"
                );

        try (
                FileInputStream inputStream =
                        new FileInputStream(
                                file
                        )
        ) {
            byte[] buffer =
                    new byte[8192];
            int read;

            while (
                    (read = inputStream.read(buffer)) != -1
            ) {
                digest.update(
                        buffer,
                        0,
                        read
                );
            }
        }

        byte[] hash =
                digest.digest();
        StringBuilder builder =
                new StringBuilder();

        for (byte value : hash) {
            builder.append(
                    String.format(
                            "%02x",
                            value
                    )
            );
        }

        return builder.toString();
    }
}
