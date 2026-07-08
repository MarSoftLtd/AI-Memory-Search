package com.bliss.aimemorysearch.ai;

import java.io.File;

public final class TranslationPackageLayout {

    private final File rootDirectory;
    private final File manifestFile;
    private final File configFile;
    private final File modelFile;
    private final File sourceTokenizerFile;
    private final File targetTokenizerFile;
    private final File sharedVocabularyFile;

    public TranslationPackageLayout(
            File rootDirectory
    ) {
        this.rootDirectory =
                rootDirectory;
        this.manifestFile =
                new File(
                        rootDirectory,
                        ManifestFileNames.MANIFEST
                );
        this.configFile =
                new File(
                        rootDirectory,
                        ManifestFileNames.CONFIG
                );
        this.modelFile =
                new File(
                        rootDirectory,
                        ManifestFileNames.MODEL
                );
        this.sourceTokenizerFile =
                new File(
                        rootDirectory,
                        ManifestFileNames.SOURCE_TOKENIZER
                );
        this.targetTokenizerFile =
                new File(
                        rootDirectory,
                        ManifestFileNames.TARGET_TOKENIZER
                );
        this.sharedVocabularyFile =
                new File(
                        rootDirectory,
                        ManifestFileNames.SHARED_VOCABULARY
                );
    }

    public File getRootDirectory() {
        return rootDirectory;
    }

    public File getManifestFile() {
        return manifestFile;
    }

    public File getConfigFile() {
        return configFile;
    }

    public File getModelFile() {
        return modelFile;
    }

    public File getSourceTokenizerFile() {
        return sourceTokenizerFile;
    }

    public File getTargetTokenizerFile() {
        return targetTokenizerFile;
    }

    public File getSharedVocabularyFile() {
        return sharedVocabularyFile;
    }
}
