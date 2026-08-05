package com.bliss.aimemorysearch.ai.evidence;

/** Pure normalization and layered confidence calculation over DocumentEvidence. */
public final class DocumentConfidenceCalculator {

    private static final float BM25_HALF_SATURATION = 4.5f;
    private static final float SEMANTIC_HALF_SATURATION = 0.25f;
    private static final float MARGIN_HALF_SATURATION = 0.15f;

    private DocumentConfidenceCalculator() {}

    public static DocumentConfidence calculate(
            DocumentEvidence evidence,
            String documentPath
    ) {
        if (evidence == null) {
            return unavailable(documentPath);
        }

        DocumentEvidence.Provenance provenance =
                evidence.getProvenanceByPath().get(documentPath);
        boolean documentObserved = provenance != null;
        boolean lexicalObserved = documentObserved && provenance.isLexical();
        boolean canonicalObserved = documentObserved && provenance.isCanonical();

        DocumentConfidence.NormalizedSignal bm25 = lexicalObserved
                ? saturating(evidence.getBestBm25(), BM25_HALF_SATURATION)
                : unavailableSignal();
        DocumentConfidence.NormalizedSignal semantic =
                documentObserved && evidence.isSemanticAvailable()
                        ? saturating(
                                Math.max(0f, evidence.getBestSemanticScore()),
                                SEMANTIC_HALF_SATURATION)
                        : unavailableSignal();
        DocumentConfidence.NormalizedSignal coverage = lexicalObserved
                ? bounded(evidence.getBestCoverage())
                : unavailableSignal();
        DocumentConfidence.NormalizedSignal canonicalCoverage =
                canonicalObserved && evidence.isCanonicalAvailable()
                        ? bounded(evidence.getBestCanonicalCoverage())
                        : unavailableSignal();
        DocumentConfidence.NormalizedSignal exactMatch = documentObserved
                && evidence.getCandidateCount() > 0
                ? DocumentConfidence.NormalizedSignal.available(
                        evidence.getExactTokenMatches() > 0 ? 1f : 0f)
                : unavailableSignal();
        DocumentConfidence.NormalizedSignal scoreMargin =
                normalizeMargin(evidence);

        DocumentConfidence.NormalizedSignal confidence = layeredConfidence(
                provenance,
                bm25,
                semantic,
                coverage,
                canonicalCoverage,
                scoreMargin
        );

        return new DocumentConfidence(
                documentPath,
                confidence,
                bm25,
                semantic,
                coverage,
                canonicalCoverage,
                exactMatch,
                scoreMargin
        );
    }

    private static DocumentConfidence.NormalizedSignal layeredConfidence(
            DocumentEvidence.Provenance provenance,
            DocumentConfidence.NormalizedSignal bm25,
            DocumentConfidence.NormalizedSignal semantic,
            DocumentConfidence.NormalizedSignal coverage,
            DocumentConfidence.NormalizedSignal canonicalCoverage,
            DocumentConfidence.NormalizedSignal margin
    ) {
        float lexical = -1f;
        if (bm25.isAvailable() && coverage.isAvailable()) {
            lexical = (float) Math.sqrt(
                    bm25.getValue() * coverage.getValue());
        }

        float semanticLayer = semantic.isAvailable()
                ? semantic.getValue() * 0.85f
                : -1f;
        float canonicalLayer = canonicalCoverage.isAvailable()
                ? canonicalCoverage.getValue() * 0.80f
                : -1f;

        float strongest = Math.max(lexical,
                Math.max(semanticLayer, canonicalLayer));
        if (strongest < 0f) {
            return unavailableSignal();
        }

        float second = secondStrongest(
                lexical, semanticLayer, canonicalLayer);
        float confidence = strongest;
        if (second >= 0f) {
            confidence += (1f - confidence) * 0.20f * second;
        }

        if (hasIndependentProvenanceAgreement(provenance)) {
            confidence += (1f - confidence) * 0.05f;
        }

        if (margin.isAvailable()) {
            confidence *= 0.85f + 0.15f * margin.getValue();
        }
        return DocumentConfidence.NormalizedSignal.available(confidence);
    }

    private static boolean hasIndependentProvenanceAgreement(
            DocumentEvidence.Provenance provenance
    ) {
        if (provenance == null) return false;
        boolean retrievalAgreement =
                provenance.isLexical() && provenance.isCanonical();
        int queryRoutes = 0;
        if (provenance.isOriginal()) queryRoutes++;
        if (provenance.isTranslation()) queryRoutes++;
        if (provenance.isAlias()) queryRoutes++;
        return retrievalAgreement || queryRoutes > 1;
    }

    private static float secondStrongest(float first, float second, float third) {
        float strongest = -1f;
        float runnerUp = -1f;
        float[] values = {first, second, third};
        for (float value : values) {
            if (value < 0f) continue;
            if (value >= strongest) {
                runnerUp = strongest;
                strongest = value;
            } else if (value > runnerUp) {
                runnerUp = value;
            }
        }
        return runnerUp;
    }

    private static DocumentConfidence.NormalizedSignal normalizeMargin(
            DocumentEvidence evidence
    ) {
        if (evidence.getCandidateCount() < 2
                || !finiteNonNegative(evidence.getBestDocumentScore())
                || !finiteNonNegative(evidence.getSecondDocumentScore())
                || !finiteNonNegative(evidence.getScoreMargin())
                || evidence.getBestDocumentScore() <= 0f) {
            return unavailableSignal();
        }
        float queryBestScore = evidence.getSecondDocumentScore()
                + evidence.getScoreMargin();
        if (queryBestScore <= 0f) {
            return unavailableSignal();
        }
        float tolerance = Math.max(0.000001f, queryBestScore * 0.000001f);
        if (evidence.getBestDocumentScore() < queryBestScore - tolerance) {
            return DocumentConfidence.NormalizedSignal.available(0f);
        }
        float relative = evidence.getScoreMargin()
                / queryBestScore;
        return saturating(relative, MARGIN_HALF_SATURATION);
    }

    private static DocumentConfidence.NormalizedSignal saturating(
            float value,
            float halfSaturation
    ) {
        if (!finiteNonNegative(value)) return unavailableSignal();
        return DocumentConfidence.NormalizedSignal.available(
                value / (value + halfSaturation));
    }

    private static DocumentConfidence.NormalizedSignal bounded(float value) {
        if (!Float.isFinite(value) || value < 0f || value > 1f) {
            return unavailableSignal();
        }
        return DocumentConfidence.NormalizedSignal.available(value);
    }

    private static boolean finiteNonNegative(float value) {
        return Float.isFinite(value) && value >= 0f;
    }

    private static DocumentConfidence.NormalizedSignal unavailableSignal() {
        return DocumentConfidence.NormalizedSignal.unavailable();
    }

    private static DocumentConfidence unavailable(String documentPath) {
        DocumentConfidence.NormalizedSignal unavailable = unavailableSignal();
        return new DocumentConfidence(
                documentPath,
                unavailable,
                unavailable,
                unavailable,
                unavailable,
                unavailable,
                unavailable,
                unavailable
        );
    }
}
