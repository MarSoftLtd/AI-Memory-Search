package com.bliss.aimemorysearch.ai.explanation;

import com.bliss.aimemorysearch.ai.concepts.InterpretationExecutionResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable explanation data associated with one supplied final result. */
public final class ResultExplanation {
    private final String resultId;
    private final String path;
    private final InterpretationExecutionResult.Modality modality;
    private final int finalRank;
    private final Float finalScore;
    private final String interpretationId;
    private final AgreementPresentation agreement;
    private final ConfidencePresentation confidence;
    private final List<EvidenceReference> evidenceReferences;
    private final List<String> provenanceReferences;
    private final List<MissingEvidenceItem> missingEvidence;
    private final Completeness completeness;

    public ResultExplanation(
            String resultId,
            String path,
            InterpretationExecutionResult.Modality modality,
            int finalRank,
            Float finalScore,
            String interpretationId,
            AgreementPresentation agreement,
            ConfidencePresentation confidence,
            List<EvidenceReference> evidenceReferences,
            List<String> provenanceReferences,
            List<MissingEvidenceItem> missingEvidence,
            Completeness completeness
    ) {
        this.resultId = resultId;
        this.path = path;
        this.modality = modality;
        this.finalRank = finalRank;
        this.finalScore = finalScore;
        this.interpretationId = interpretationId;
        this.agreement = agreement;
        this.confidence = confidence;
        this.evidenceReferences = Collections.unmodifiableList(
                new ArrayList<>(evidenceReferences)
        );
        this.provenanceReferences = Collections.unmodifiableList(
                new ArrayList<>(provenanceReferences)
        );
        this.missingEvidence = Collections.unmodifiableList(
                new ArrayList<>(missingEvidence)
        );
        this.completeness = completeness;
    }

    public String getResultId() { return resultId; }
    public String getPath() { return path; }
    public InterpretationExecutionResult.Modality getModality() {
        return modality;
    }
    public int getFinalRank() { return finalRank; }
    public Float getFinalScore() { return finalScore; }
    public String getInterpretationId() { return interpretationId; }
    public AgreementPresentation getAgreement() { return agreement; }
    public ConfidencePresentation getConfidence() { return confidence; }
    public List<EvidenceReference> getEvidenceReferences() {
        return evidenceReferences;
    }
    public List<String> getProvenanceReferences() {
        return provenanceReferences;
    }
    public List<MissingEvidenceItem> getMissingEvidence() {
        return missingEvidence;
    }
    public Completeness getCompleteness() { return completeness; }

    public enum Completeness {
        COMPLETE,
        PARTIAL,
        UNAVAILABLE
    }
}
