package com.bliss.aimemorysearch.ai;

import java.io.File;

public final class TranslationPackageValidator {

    public boolean isValid(
            TranslationPackageLayout layout
    ) {
        return isExistingDirectory(
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
