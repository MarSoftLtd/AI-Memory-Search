package com.bliss.aimemorysearch.ai.explanation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable snapshot of completed search observations. */
public final class ExplanationInput {
    private final String searchExecutionId;
    private final String schemaVersion;
    private final QuerySummary querySummary;
    private final List<ResultExplanation> orderedResults;
    private final InterpretationSummary selectedInterpretation;
    private final List<InterpretationSummary> alternativeInterpretations;
    private final SelectionSummary selectionSummary;
    private final Map<String, List<EvidenceReference>> evidenceByResultId;
    private final Map<String, AgreementPresentation> agreementByResultId;
    private final Map<String, ConfidencePresentation> confidenceByResultId;

    public ExplanationInput(
            String searchExecutionId,
            String schemaVersion,
            QuerySummary querySummary,
            List<ResultExplanation> orderedResults,
            InterpretationSummary selectedInterpretation,
            List<InterpretationSummary> alternativeInterpretations,
            SelectionSummary selectionSummary,
            Map<String, List<EvidenceReference>> evidenceByResultId,
            Map<String, AgreementPresentation> agreementByResultId,
            Map<String, ConfidencePresentation> confidenceByResultId
    ) {
        this.searchExecutionId = searchExecutionId;
        this.schemaVersion = schemaVersion;
        this.querySummary = querySummary;
        this.orderedResults = Collections.unmodifiableList(
                new ArrayList<>(orderedResults)
        );
        this.selectedInterpretation = selectedInterpretation;
        this.alternativeInterpretations = Collections.unmodifiableList(
                new ArrayList<>(alternativeInterpretations)
        );
        this.selectionSummary = selectionSummary;
        Map<String, List<EvidenceReference>> evidenceCopy =
                new LinkedHashMap<>();
        for (Map.Entry<String, List<EvidenceReference>> entry
                : evidenceByResultId.entrySet()) {
            evidenceCopy.put(
                    entry.getKey(),
                    Collections.unmodifiableList(
                            new ArrayList<>(entry.getValue())
                    )
            );
        }
        this.evidenceByResultId = Collections.unmodifiableMap(evidenceCopy);
        this.agreementByResultId = Collections.unmodifiableMap(
                new LinkedHashMap<>(agreementByResultId)
        );
        this.confidenceByResultId = Collections.unmodifiableMap(
                new LinkedHashMap<>(confidenceByResultId)
        );
    }

    public String getSearchExecutionId() { return searchExecutionId; }
    public String getSchemaVersion() { return schemaVersion; }
    public QuerySummary getQuerySummary() { return querySummary; }
    public List<ResultExplanation> getOrderedResults() { return orderedResults; }
    public InterpretationSummary getSelectedInterpretation() {
        return selectedInterpretation;
    }
    public List<InterpretationSummary> getAlternativeInterpretations() {
        return alternativeInterpretations;
    }
    public SelectionSummary getSelectionSummary() { return selectionSummary; }
    public Map<String, List<EvidenceReference>> getEvidenceByResultId() {
        return evidenceByResultId;
    }
    public Map<String, AgreementPresentation> getAgreementByResultId() {
        return agreementByResultId;
    }
    public Map<String, ConfidencePresentation> getConfidenceByResultId() {
        return confidenceByResultId;
    }
}
