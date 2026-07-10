package com.bliss.aimemorysearch.ai;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class AiPackageInstaller {

    private static final String PACKAGE_ROOT_DIRECTORY =
            "ai_packages";

    private final Context context;
    private final AiPackageManager packageManager;
    private final PackageVerification packageVerification;

    public AiPackageInstaller(
            Context context,
            AiPackageManager packageManager,
            PackageVerification packageVerification
    ) {
        this.context =
                context.getApplicationContext();
        this.packageManager =
                packageManager;
        this.packageVerification =
                packageVerification;
    }

    public AiPackageInstallResult installPackage(
            AiPackageInfo packageInfo,
            File source
    ) {
        if (packageInfo == null) {
            return installFailure(
                    null,
                    null,
                    null,
                    "Package metadata is missing",
                    null
            );
        }

        if (
                source == null
                        ||
                        !source.exists()
        ) {
            return installFailure(
                    packageInfo,
                    null,
                    null,
                    "Package source is missing",
                    null
            );
        }

        try {
            PackageVerificationResult verificationResult =
                    source.isFile()
                            ? packageVerification.verify(
                                    source,
                                    packageInfo
                            )
                            : null;

            if (
                    verificationResult != null
                            &&
                            !verificationResult.isSuccess()
            ) {
                return installFailure(
                        packageInfo,
                        null,
                        verificationResult,
                        verificationResult.getMessage(),
                        verificationResult.getError()
                );
            }

            File installDirectory =
                    getInstalledPackageDirectory(
                            packageInfo
                    );

            deleteRecursively(
                    installDirectory
            );

            if (
                    !installDirectory.exists()
                            &&
                            !installDirectory.mkdirs()
            ) {
                return installFailure(
                        packageInfo,
                        installDirectory,
                        verificationResult,
                        "Failed to create install directory",
                        null
                );
            }

            if (source.isDirectory()) {
                copyDirectory(
                        source,
                        installDirectory
                );
            } else {
                unzip(
                        source,
                        installDirectory
                );
            }

            if (
                    !validatePackageLayout(
                            installDirectory
                    )
            ) {
                deleteRecursively(
                        installDirectory
                );

                return installFailure(
                        packageInfo,
                        installDirectory,
                        verificationResult,
                        "Installed package layout is invalid",
                        null
                );
            }

            packageManager.registerInstalledPackage(
                    packageInfo
            );

            return new AiPackageInstallResult(
                    true,
                    packageInfo,
                    installDirectory,
                    verificationResult,
                    "Package installed",
                    null
            );
        } catch (Exception e) {
            return installFailure(
                    packageInfo,
                    null,
                    null,
                    "Package installation failed",
                    e
            );
        }
    }

    public AiPackageUninstallResult uninstallPackage(
            AiPackageInfo packageInfo
    ) {
        if (packageInfo == null) {
            return new AiPackageUninstallResult(
                    false,
                    null,
                    "",
                    "Package metadata is missing",
                    null
            );
        }

        return uninstallPackage(
                packageInfo.getPackageId()
        );
    }

    public AiPackageUninstallResult uninstallPackage(
            String packageId
    ) {
        if (
                packageId == null
                        ||
                        packageId.trim().isEmpty()
        ) {
            return new AiPackageUninstallResult(
                    false,
                    null,
                    packageId,
                    "Package id is missing",
                    null
            );
        }

        AiPackageInfo packageInfo =
                packageManager.getInstalledPackage(
                        packageId
                );

        try {
            File packageDirectory =
                    getInstalledPackageDirectory(
                            packageId
                    );

            deleteRecursively(
                    packageDirectory
            );

            packageManager.unregisterInstalledPackage(
                    packageId
            );

            return new AiPackageUninstallResult(
                    true,
                    packageInfo,
                    packageId,
                    "Package uninstalled",
                    null
            );
        } catch (Exception e) {
            return new AiPackageUninstallResult(
                    false,
                    packageInfo,
                    packageId,
                    "Package uninstall failed",
                    e
            );
        }
    }

    public boolean validatePackageLayout(
            File packageDirectory
    ) {
        if (
                packageDirectory == null
                        ||
                        !packageDirectory.isDirectory()
        ) {
            return false;
        }

        File manifestFile =
                new File(
                        packageDirectory,
                        AiPackageFileNames.MANIFEST
                );

        return manifestFile.isFile()
                &&
                manifestFile.length() > 0;
    }

    public File getInstalledPackageDirectory(
            AiPackageInfo packageInfo
    ) {
        return getInstalledPackageDirectory(
                packageInfo.getPackageId()
        );
    }

    public File getInstalledPackageDirectory(
            String packageId
    ) {
        return new File(
                getPackagesRootDirectory(),
                sanitizeFileName(
                        packageId
                )
        );
    }

    private File getPackagesRootDirectory() {
        return new File(
                context.getFilesDir(),
                PACKAGE_ROOT_DIRECTORY
        );
    }

    private static AiPackageInstallResult installFailure(
            AiPackageInfo packageInfo,
            File installedDirectory,
            PackageVerificationResult verificationResult,
            String message,
            Throwable error
    ) {
        return new AiPackageInstallResult(
                false,
                packageInfo,
                installedDirectory,
                verificationResult,
                message,
                error
        );
    }

    private static void unzip(
            File zipFile,
            File targetDirectory
    ) throws Exception {
        String targetPath =
                targetDirectory.getCanonicalPath()
                        + File.separator;

        try (
                ZipInputStream zipInputStream =
                        new ZipInputStream(
                                new FileInputStream(
                                        zipFile
                                )
                        )
        ) {
            ZipEntry entry;

            while (
                    (entry = zipInputStream.getNextEntry()) != null
            ) {
                File outputFile =
                        new File(
                                targetDirectory,
                                entry.getName()
                        );

                String outputPath =
                        outputFile.getCanonicalPath();

                if (!outputPath.startsWith(targetPath)) {
                    throw new IllegalStateException(
                            "Invalid zip entry path"
                    );
                }

                if (entry.isDirectory()) {
                    if (!outputFile.exists()) {
                        outputFile.mkdirs();
                    }
                } else {
                    File parent =
                            outputFile.getParentFile();

                    if (
                            parent != null
                                    &&
                                    !parent.exists()
                    ) {
                        parent.mkdirs();
                    }

                    try (
                            FileOutputStream outputStream =
                                    new FileOutputStream(
                                            outputFile
                                    )
                    ) {
                        byte[] buffer =
                                new byte[8192];
                        int read;

                        while (
                                (read = zipInputStream.read(buffer)) != -1
                        ) {
                            outputStream.write(
                                    buffer,
                                    0,
                                    read
                            );
                        }
                    }
                }

                zipInputStream.closeEntry();
            }
        }
    }

    private static void copyDirectory(
            File source,
            File target
    ) throws Exception {
        File[] files =
                source.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {
            File targetFile =
                    new File(
                            target,
                            file.getName()
                    );

            if (file.isDirectory()) {
                if (
                        !targetFile.exists()
                                &&
                                !targetFile.mkdirs()
                ) {
                    throw new IllegalStateException(
                            "Failed to create directory"
                    );
                }

                copyDirectory(
                        file,
                        targetFile
                );
            } else {
                copyFile(
                        file,
                        targetFile
                );
            }
        }
    }

    private static void copyFile(
            File source,
            File target
    ) throws Exception {
        File parent =
                target.getParentFile();

        if (
                parent != null
                        &&
                        !parent.exists()
        ) {
            parent.mkdirs();
        }

        try (
                FileInputStream inputStream =
                        new FileInputStream(
                                source
                        );
                FileOutputStream outputStream =
                        new FileOutputStream(
                                target
                        )
        ) {
            byte[] buffer =
                    new byte[8192];
            int read;

            while (
                    (read = inputStream.read(buffer)) != -1
            ) {
                outputStream.write(
                        buffer,
                        0,
                        read
                );
            }
        }
    }

    private static void deleteRecursively(
            File file
    ) {
        if (
                file == null
                        ||
                        !file.exists()
        ) {
            return;
        }

        if (file.isDirectory()) {
            File[] children =
                    file.listFiles();

            if (children != null) {
                for (File child : children) {
                    deleteRecursively(
                            child
                    );
                }
            }
        }

        file.delete();
    }

    private static String sanitizeFileName(
            String value
    ) {
        if (value == null) {
            return "unknown-package";
        }

        return value.replaceAll(
                "[^a-zA-Z0-9._-]",
                "_"
        );
    }
}
