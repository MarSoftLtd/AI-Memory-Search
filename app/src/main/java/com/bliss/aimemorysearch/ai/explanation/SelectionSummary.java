package com.bliss.aimemorysearch.ai.explanation;

/** Immutable summary of final selection observations supplied by search. */
public final class SelectionSummary {
    private final int resultCount;
    private final int documentCount;
    private final int imageCount;
    private final String selectedInterpretationId;
    private final AgreementPresentation agreement;
    private final boolean resultOrderSupplied;

    public SelectionSummary(
            int resultCount,
            int documentCount,
            int imageCount,
            String selectedInterpretationId,
            AgreementPresentation agreement,
            boolean resultOrderSupplied
    ) {
        this.resultCount = resultCount;
        this.documentCount = documentCount;
        this.imageCount = imageCount;
        this.selectedInterpretationId = selectedInterpretationId;
        this.agreement = agreement;
        this.resultOrderSupplied = resultOrderSupplied;
    }

    public int getResultCount() { return resultCount; }
    public int getDocumentCount() { return documentCount; }
    public int getImageCount() { return imageCount; }
    public String getSelectedInterpretationId() {
        return selectedInterpretationId;
    }
    public AgreementPresentation getAgreement() { return agreement; }
    public boolean isResultOrderSupplied() { return resultOrderSupplied; }
}
