package com.bliss.aimemorysearch.ai;

import android.content.Context;

import com.bliss.aimemorysearch.ai.model.AIPackageInfo;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class AtomicAiPackageInstaller {
    public enum FailureReason {
        INVALID_PACKAGE,
        INVALID_MANIFEST,
        UNSUPPORTED_FORMAT,
        INVALID_CONTENTS,
        INSTALLATION
    }

    public static final class Result {
        private final boolean success;
        private final File installedDirectory;
        private final FailureReason failureReason;

        private Result(
                boolean success,
                File installedDirectory,
                FailureReason failureReason
        ) {
            this.success = success;
            this.installedDirectory = installedDirectory;
            this.failureReason = failureReason;
        }

        public boolean isSuccess() { return success; }
        public File getInstalledDirectory() { return installedDirectory; }
        public FailureReason getFailureReason() { return failureReason; }
    }

    private static final int SUPPORTED_PACKAGE_FORMAT_VERSION = 1;
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final String[] REQUIRED_FILES = {
            ManifestFileNames.MANIFEST,
            ManifestFileNames.CONFIG,
            ManifestFileNames.MODEL,
            ManifestFileNames.SOURCE_TOKENIZER,
            ManifestFileNames.TARGET_TOKENIZER,
            ManifestFileNames.SHARED_VOCABULARY
    };

    private final File packageRootDirectory;

    public AtomicAiPackageInstaller(Context context) {
        Context applicationContext = Objects.requireNonNull(context, "context")
                .getApplicationContext();
        packageRootDirectory = new File(applicationContext.getFilesDir(), "ai_packages");
    }

    public Result install(AIPackageInfo packageInfo, File packageFile) {
        Objects.requireNonNull(packageInfo, "packageInfo");
        if (!isSafePackageId(packageInfo.getPackageId())
                || packageFile == null
                || !packageFile.isFile()
                || !packageFile.getName().endsWith(".aipackage")) {
            return failure(FailureReason.INVALID_PACKAGE);
        }

        File temporaryDirectory = new File(
                packageRootDirectory,
                packageInfo.getPackageId() + ".tmp"
        );
        File installedDirectory = new File(
                packageRootDirectory,
                packageInfo.getPackageId()
        );

        deleteRecursively(temporaryDirectory);
        try (ZipFile archive = new ZipFile(packageFile, StandardCharsets.UTF_8)) {
            FailureReason archiveFailure = validateArchive(archive, packageInfo);
            if (archiveFailure != null) {
                return failure(archiveFailure);
            }
            if ((!packageRootDirectory.exists() && !packageRootDirectory.mkdirs())
                    || !temporaryDirectory.mkdir()) {
                return failure(FailureReason.INSTALLATION);
            }

            extract(archive, temporaryDirectory);
            if (!hasRequiredFiles(temporaryDirectory)) {
                return failure(FailureReason.INVALID_CONTENTS);
            }
            FailureReason extractedManifestFailure = validateManifest(
                    new JSONObject(Files.readString(
                            new File(temporaryDirectory, ManifestFileNames.MANIFEST).toPath(),
                            StandardCharsets.UTF_8
                    )),
                    packageInfo
            );
            if (extractedManifestFailure != null) {
                return failure(extractedManifestFailure);
            }
            if (installedDirectory.exists()) {
                return failure(FailureReason.INSTALLATION);
            }

            atomicMove(temporaryDirectory, installedDirectory);
            if (!packageFile.delete()) {
                deleteRecursively(installedDirectory);
                return failure(FailureReason.INSTALLATION);
            }
            return new Result(true, installedDirectory, null);
        } catch (Exception failure) {
            return failure(FailureReason.INSTALLATION);
        } finally {
            deleteRecursively(temporaryDirectory);
        }
    }

    private static FailureReason validateArchive(
            ZipFile archive,
            AIPackageInfo packageInfo
    ) throws Exception {
        for (String requiredFile : REQUIRED_FILES) {
            ZipEntry entry = archive.getEntry(requiredFile);
            if (entry == null || entry.isDirectory()) {
                return FailureReason.INVALID_CONTENTS;
            }
        }
        ZipEntry manifestEntry = archive.getEntry(ManifestFileNames.MANIFEST);
        try (InputStream input = archive.getInputStream(manifestEntry)) {
            String manifestJson = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return validateManifest(new JSONObject(manifestJson), packageInfo);
        }
    }

    private static FailureReason validateManifest(
            JSONObject manifest,
            AIPackageInfo packageInfo
    ) throws Exception {
        if (!packageInfo.getPackageId().equals(manifest.optString("packageId"))
                || !packageInfo.getVersion().equals(manifest.optString("version"))
                || packageInfo.getPackageType() == null
                || !packageInfo.getPackageType().name().equals(manifest.optString("packageType"))) {
            return FailureReason.INVALID_MANIFEST;
        }
        if (!manifest.has("packageFormatVersion")
                || manifest.getInt("packageFormatVersion")
                != SUPPORTED_PACKAGE_FORMAT_VERSION) {
            return FailureReason.UNSUPPORTED_FORMAT;
        }
        return null;
    }

    private static void extract(ZipFile archive, File targetDirectory) throws Exception {
        String canonicalRoot = targetDirectory.getCanonicalPath() + File.separator;
        java.util.Enumeration<? extends ZipEntry> entries = archive.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            File outputFile = new File(targetDirectory, entry.getName());
            if (!outputFile.getCanonicalPath().startsWith(canonicalRoot)) {
                throw new IllegalStateException("Archive entry escapes installation directory");
            }
            if (entry.isDirectory()) {
                if (!outputFile.exists() && !outputFile.mkdirs()) {
                    throw new IllegalStateException("Cannot create package directory");
                }
                continue;
            }
            File parent = outputFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("Cannot create package directory");
            }
            try (InputStream input = archive.getInputStream(entry);
                 BufferedOutputStream output = new BufferedOutputStream(
                         new FileOutputStream(outputFile)
                 )) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
            }
        }
    }

    private static boolean hasRequiredFiles(File directory) {
        for (String requiredFile : REQUIRED_FILES) {
            File file = new File(directory, requiredFile);
            if (!file.isFile()) {
                return false;
            }
        }
        return true;
    }

    private static void atomicMove(File source, File target) throws Exception {
        try {
            Files.move(
                    source.toPath(),
                    target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException unsupported) {
            throw new IllegalStateException("Atomic package installation is unavailable", unsupported);
        }
    }

    private static boolean isSafePackageId(String packageId) {
        return packageId != null && packageId.matches("[A-Za-z0-9][A-Za-z0-9._-]*");
    }

    private static Result failure(FailureReason reason) {
        return new Result(false, null, reason);
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}

