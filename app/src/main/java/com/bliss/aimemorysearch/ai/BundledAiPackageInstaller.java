package com.bliss.aimemorysearch.ai;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public final class BundledAiPackageInstaller {

    private final AssetManager assetManager;
    private final AiStorageManager storageManager;
    private final AiPackageManager packageManager;

    public BundledAiPackageInstaller(
            Context context,
            AiPackageManager packageManager
    ) {
        Context applicationContext = context.getApplicationContext();
        this.assetManager = applicationContext.getAssets();
        this.storageManager = new AiStorageManager(applicationContext);
        this.packageManager = packageManager;
    }

    public synchronized File ensureInstalled(
            BundledAiPackage bundledPackage
    ) throws Exception {
        if (bundledPackage == null || bundledPackage.getPackageInfo() == null) {
            throw new IllegalArgumentException("Bundled package metadata is missing");
        }

        AiPackageInfo packageInfo = bundledPackage.getPackageInfo();
        File installDirectory =
                storageManager.getInstalledPackageDirectory(packageInfo);

        if (!isValidPackage(installDirectory)) {
            File stagingDirectory =
                    new File(
                            storageManager.getPackageRootDirectory(),
                            packageInfo.getPackageId() + ".staging"
                    );

            deleteRecursively(stagingDirectory);

            if (!stagingDirectory.mkdirs() && !stagingDirectory.isDirectory()) {
                throw new IllegalStateException("Failed to create bundled package staging directory");
            }

            try {
                copyAssetDirectory(
                        bundledPackage.getAssetDirectory(),
                        stagingDirectory
                );

                if (!isValidPackage(stagingDirectory)) {
                    throw new IllegalStateException("Bundled package layout is invalid");
                }

                deleteRecursively(installDirectory);

                if (!stagingDirectory.renameTo(installDirectory)) {
                    throw new IllegalStateException("Failed to activate bundled package");
                }
            } finally {
                deleteRecursively(stagingDirectory);
            }
        }

        packageManager.registerInstalledPackage(packageInfo);
        packageManager.activatePackage(
                bundledPackage.getCapability(),
                packageInfo
        );

        return installDirectory;
    }

    private static boolean isValidPackage(
            File directory
    ) {
        return directory != null
                && directory.isDirectory()
                && isNonEmptyFile(new File(directory, AiPackageFileNames.MANIFEST))
                && isNonEmptyFile(new File(directory, ManifestFileNames.CONFIG))
                && isNonEmptyFile(new File(directory, ManifestFileNames.MODEL))
                && isNonEmptyFile(new File(directory, ManifestFileNames.SOURCE_TOKENIZER))
                && isNonEmptyFile(new File(directory, ManifestFileNames.TARGET_TOKENIZER));
    }

    private static boolean isNonEmptyFile(
            File file
    ) {
        return file.isFile() && file.length() > 0L;
    }

    private void copyAssetDirectory(
            String assetPath,
            File outputDirectory
    ) throws Exception {
        String[] children = assetManager.list(assetPath);

        if (children == null || children.length == 0) {
            copyAssetFile(assetPath, outputDirectory);
            return;
        }

        if (!outputDirectory.exists()
                && !outputDirectory.mkdirs()) {
            throw new IllegalStateException("Failed to create bundled package directory");
        }

        for (String child : children) {
            copyAssetDirectory(
                    assetPath + "/" + child,
                    new File(outputDirectory, child)
            );
        }
    }

    private void copyAssetFile(
            String assetPath,
            File outputFile
    ) throws Exception {
        File parent = outputFile.getParentFile();

        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Failed to create bundled package parent directory");
        }

        try (
                InputStream inputStream = assetManager.open(assetPath);
                FileOutputStream outputStream = new FileOutputStream(outputFile)
        ) {
            byte[] buffer = new byte[16 * 1024];
            int read;

            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        }
    }

    private static void deleteRecursively(
            File file
    ) {
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

        if (!file.delete() && file.exists()) {
            throw new IllegalStateException("Failed to remove stale package path: " + file);
        }
    }
}
