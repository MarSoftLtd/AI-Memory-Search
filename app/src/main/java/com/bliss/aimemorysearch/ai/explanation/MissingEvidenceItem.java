package com.bliss.aimemorysearch.ai.explanation;

/** Immutable description of absent or unusable explanatory evidence. */
public final class MissingEvidenceItem {
    private final EvidenceReference.Category category;
    private final String resultId;
    private final String interpretationId;
    private final State state;
    private final String reasonCode;
    private final String sourceReference;

    public MissingEvidenceItem(
            EvidenceReference.Category category,
            String resultId,
            String interpretationId,
            State state,
            String reasonCode,
            String sourceReference
    ) {
        this.category = category;
        this.resultId = resultId;
        this.interpretationId = interpretationId;
        this.state = state;
        this.reasonCode = reasonCode;
        this.sourceReference = sourceReference;
    }

    public EvidenceReference.Category getCategory() { return category; }
    public String getResultId() { return resultId; }
    public String getInterpretationId() { return interpretationId; }
    public State getState() { return state; }
    public String getReasonCode() { return reasonCode; }
    public String getSourceReference() { return sourceReference; }

    public enum State {
        UNAVAILABLE,
        NOT_APPLICABLE,
        UNMATCHED,
        INVALID,
        SUPPRESSED
    }
}
