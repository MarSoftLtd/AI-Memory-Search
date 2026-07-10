package com.bliss.aimemorysearch.ai;

import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public final class OcrRuntimeLoader {

    private final AiRuntimeManager runtimeManager;
    private final AiPackageManager packageManager;

    public OcrRuntimeLoader(
            AiRuntimeManager runtimeManager,
            AiPackageManager packageManager
    ) {
        this.runtimeManager =
                runtimeManager;
        this.packageManager =
                packageManager;
    }

    public static OcrRuntimeLoader createDefault() {
        return new OcrRuntimeLoader(
                AiPlatform.getRuntimeManager(),
                AiPlatform.getPackageManager()
        );
    }

    public TextRecognizer loadRuntime() {
        AiPackageInfo packageInfo =
                packageManager.getActivePackage(
                        AiCapability.OCR
                );

        if (packageInfo != null) {
            runtimeManager.loadRuntime(
                    AiCapability.OCR,
                    packageInfo
            );
        }

        return TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS
        );
    }
}
