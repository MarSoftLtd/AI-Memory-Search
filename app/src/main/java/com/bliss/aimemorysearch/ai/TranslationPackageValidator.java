package com.bliss.aimemorysearch.ai;

import java.io.File;

public final class TranslationPackageValidator {

    public boolean isValid(
            TranslationPackageLayout layout
    ) {
        boolean validLayout = isExistingDirectory(
                layout.getRootDirectory()
        )
                &&
                isExistingFile(
                        layout.getManifestFile()
                )
                &&
                isExistingFile(
                        layout.getConfigFile()
                )
                &&
                isExistingFile(
                        layout.getModelFile()
                )
                &&
                isExistingFile(
                        layout.getSourceTokenizerFile()
                )
                &&
                isExistingFile(
                        layout.getTargetTokenizerFile()
                );

        if (!validLayout) {
            return false;
        }

        try {
            return TranslationPackageManifestSchema.hasExactFields(
                    layout.getManifestFile()
            );
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isExistingDirectory(
            File directory
    ) {
        return directory != null
                &&
                directory.isDirectory();
    }

    private static boolean isExistingFile(
            File file
    ) {
        return file != null
                &&
                file.isFile()
                &&
                file.length() > 0;
    }
}
