package com.bliss.aimemorysearch.ai;

import android.content.Context;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.nl.languageid.IdentifiedLanguage;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions;
import com.google.mlkit.nl.languageid.LanguageIdentifier;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class LanguageDetectionEngine {

    private static final float CONFIDENCE_THRESHOLD = 0.20f;
    private static final float ROUTING_CONFIDENCE_THRESHOLD = 0.25f;
    private static final int SAMPLE_CHARACTERS = 4000;
    private static volatile LanguageDetectionEngine instance;

    private final LanguageIdentifier languageIdentifier;

    private LanguageDetectionEngine(Context context) {
        context.getApplicationContext();
        languageIdentifier = LanguageIdentification.getClient(
                new LanguageIdentificationOptions.Builder()
                        .setConfidenceThreshold(CONFIDENCE_THRESHOLD)
                        .build()
        );
    }

    public static synchronized LanguageDetectionEngine getInstance(Context context) {
        if (instance == null) {
            instance = new LanguageDetectionEngine(context);
        }
        return instance;
    }

    public DetectionResult detectLanguage(String text, String fallbackFamily) {
        String source = text == null ? "" : text.trim();
        if (source.isEmpty()) {
            return new DetectionResult("und", fallbackFamily, 0f, true);
        }
        try {
            String sample = source.substring(0, Math.min(source.length(), SAMPLE_CHARACTERS));
            List<IdentifiedLanguage> candidates = Tasks.await(
                    languageIdentifier.identifyPossibleLanguages(sample),
                    10,
                    TimeUnit.SECONDS
            );
            IdentifiedLanguage best = candidates.stream()
                    .filter(candidate -> !"und".equals(candidate.getLanguageTag()))
                    .max(Comparator.comparingDouble(IdentifiedLanguage::getConfidence))
                    .orElse(null);
            if (best == null) {
                return new DetectionResult("und", fallbackFamily, 0f, true);
            }
            if (best.getConfidence() < ROUTING_CONFIDENCE_THRESHOLD) {
                return new DetectionResult("und", "", best.getConfidence(), true);
            }
            String language = best.getLanguageTag();
            if ("en".equalsIgnoreCase(language)) {
                return new DetectionResult("en", "", best.getConfidence(), false);
            }
            TranslationModelInfo model = TranslationModelRegistry.getModelForLanguage(language);
            if (model == null) {
                return new DetectionResult(language, "", best.getConfidence(), false);
            }
            return new DetectionResult(language, model.getTranslationFamily(),
                    best.getConfidence(), false);
        } catch (Exception error) {
            android.util.Log.e("MULTILINGUAL_PIPELINE",
                    "Query language detection failed; using configured fallback", error);
            return new DetectionResult("und", fallbackFamily, 0f, true);
        }
    }

    public static final class DetectionResult {
        public final String language;
        public final String family;
        public final float confidence;
        public final boolean fallback;

        DetectionResult(String language, String family, float confidence, boolean fallback) {
            this.language = language == null ? "und" : language;
            this.family = family == null ? "" : family;
            this.confidence = confidence;
            this.fallback = fallback;
        }
    }
}
