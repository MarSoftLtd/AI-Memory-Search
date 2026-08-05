package com.bliss.aimemorysearch.ai.evidence;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DocumentEvidence {

    private final int candidateCount;
    private final float bestBm25;
    private final float bestSemanticScore;
    private final float bestCoverage;
    private final float bestCanonicalCoverage;
    private final int exactTokenMatches;
    private final float pivotAgreement;
    private final boolean semanticAvailable;
    private final boolean canonicalAvailable;
    private final boolean metadataAvailable;
    private final boolean ocrAvailable;
    private final float bestDocumentScore;
    private final float secondDocumentScore;
    private final float scoreMargin;
    private final Map<String, Provenance> provenanceByPath;

    public DocumentEvidence(
            int candidateCount,
            float bestBm25,
            float bestSemanticScore,
            float bestCoverage,
            float bestCanonicalCoverage,
            int exactTokenMatches,
            float pivotAgreement,
            boolean semanticAvailable,
            boolean canonicalAvailable,
            boolean metadataAvailable,
            boolean ocrAvailable,
            float bestDocumentScore,
            float secondDocumentScore,
            float scoreMargin,
            Map<String, Provenance> provenanceByPath
    ) {
        this.candidateCount = candidateCount;
        this.bestBm25 = bestBm25;
        this.bestSemanticScore = bestSemanticScore;
        this.bestCoverage = bestCoverage;
        this.bestCanonicalCoverage = bestCanonicalCoverage;
        this.exactTokenMatches = exactTokenMatches;
        this.pivotAgreement = pivotAgreement;
        this.semanticAvailable = semanticAvailable;
        this.canonicalAvailable = canonicalAvailable;
        this.metadataAvailable = metadataAvailable;
        this.ocrAvailable = ocrAvailable;
        this.bestDocumentScore = bestDocumentScore;
        this.secondDocumentScore = secondDocumentScore;
        this.scoreMargin = scoreMargin;
        this.provenanceByPath = Collections.unmodifiableMap(
                new LinkedHashMap<>(provenanceByPath)
        );
    }

    public int getCandidateCount() { return candidateCount; }
    public float getBestBm25() { return bestBm25; }
    public float getBestSemanticScore() { return bestSemanticScore; }
    public float getBestCoverage() { return bestCoverage; }
    public float getBestCanonicalCoverage() { return bestCanonicalCoverage; }
    public int getExactTokenMatches() { return exactTokenMatches; }
    public float getPivotAgreement() { return pivotAgreement; }
    public boolean isSemanticAvailable() { return semanticAvailable; }
    public boolean isCanonicalAvailable() { return canonicalAvailable; }
    public boolean isMetadataAvailable() { return metadataAvailable; }
    public boolean isOcrAvailable() { return ocrAvailable; }
    public float getBestDocumentScore() { return bestDocumentScore; }
    public float getSecondDocumentScore() { return secondDocumentScore; }
    public float getScoreMargin() { return scoreMargin; }
    public Map<String, Provenance> getProvenanceByPath() {
        return provenanceByPath;
    }

    public static final class Provenance {
        private final boolean lexical;
        private final boolean canonical;
        private final boolean original;
        private final boolean translation;
        private final boolean alias;

        public Provenance(
                boolean lexical,
                boolean canonical,
                boolean original,
                boolean translation,
                boolean alias
        ) {
            this.lexical = lexical;
            this.canonical = canonical;
            this.original = original;
            this.translation = translation;
            this.alias = alias;
        }

        public boolean isLexical() { return lexical; }
        public boolean isCanonical() { return canonical; }
        public boolean isOriginal() { return original; }
        public boolean isTranslation() { return translation; }
        public boolean isAlias() { return alias; }
        public boolean hasMultipleSources() {
            int count = 0;
            if (lexical) count++;
            if (canonical) count++;
            if (original) count++;
            if (translation) count++;
            if (alias) count++;
            return count > 1;
        }
    }
}
