package com.bliss.aimemorysearch.ai.evidence;

/** Immutable, observational confidence snapshot for one final document. */
public final class DocumentConfidence {

    private final String documentPath;
    private final NormalizedSignal confidence;
    private final NormalizedSignal bm25;
    private final NormalizedSignal semantic;
    private final NormalizedSignal coverage;
    private final NormalizedSignal canonicalCoverage;
    private final NormalizedSignal exactMatch;
    private final NormalizedSignal scoreMargin;

    public DocumentConfidence(
            String documentPath,
            NormalizedSignal confidence,
            NormalizedSignal bm25,
            NormalizedSignal semantic,
            NormalizedSignal coverage,
            NormalizedSignal canonicalCoverage,
            NormalizedSignal exactMatch,
            NormalizedSignal scoreMargin
    ) {
        this.documentPath = documentPath == null ? "" : documentPath;
        this.confidence = confidence;
        this.bm25 = bm25;
        this.semantic = semantic;
        this.coverage = coverage;
        this.canonicalCoverage = canonicalCoverage;
        this.exactMatch = exactMatch;
        this.scoreMargin = scoreMargin;
    }

    public String getDocumentPath() { return documentPath; }
    public NormalizedSignal getConfidence() { return confidence; }
    public NormalizedSignal getBm25() { return bm25; }
    public NormalizedSignal getSemantic() { return semantic; }
    public NormalizedSignal getCoverage() { return coverage; }
    public NormalizedSignal getCanonicalCoverage() { return canonicalCoverage; }
    public NormalizedSignal getExactMatch() { return exactMatch; }
    public NormalizedSignal getScoreMargin() { return scoreMargin; }

    public static final class NormalizedSignal {
        private static final NormalizedSignal UNAVAILABLE =
                new NormalizedSignal(false, 0f);

        private final boolean available;
        private final float value;

        private NormalizedSignal(boolean available, float value) {
            this.available = available;
            this.value = available ? clamp(value) : 0f;
        }

        public static NormalizedSignal available(float value) {
            return new NormalizedSignal(true, value);
        }

        public static NormalizedSignal unavailable() {
            return UNAVAILABLE;
        }

        public boolean isAvailable() { return available; }
        public float getValue() { return value; }

        private static float clamp(float value) {
            if (!Float.isFinite(value)) return 0f;
            return Math.max(0f, Math.min(1f, value));
        }
    }
}
