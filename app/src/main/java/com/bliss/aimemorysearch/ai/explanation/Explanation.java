package com.bliss.aimemorysearch.ai.explanation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable structured explanation of supplied search observations. */
public final class Explanation {
    private final String explanationId;
    private final String searchExecutionId;
    private final String schemaVersion;
    private final Availability availability;
    private final QuerySummary querySummary;
    private final InterpretationSummary interpretationSummary;
    private final List<InterpretationSummary> alternativeInterpretations;
    private final SelectionSummary selectionSummary;
    private final List<ResultExplanation> resultExplanations;
    private final List<EvidenceReference> evidenceSummary;
    private final List<MissingEvidenceItem> missingEvidenceReport;
    private final List<String> limitations;
    private final GenerationMetadata generationMetadata;

    public Explanation(
            String explanationId,
            String searchExecutionId,
            String schemaVersion,
            Availability availability,
            QuerySummary querySummary,
            InterpretationSummary interpretationSummary,
            List<InterpretationSummary> alternativeInterpretations,
            SelectionSummary selectionSummary,
            List<ResultExplanation> resultExplanations,
            List<EvidenceReference> evidenceSummary,
            List<MissingEvidenceItem> missingEvidenceReport,
            List<String> limitations,
            GenerationMetadata generationMetadata
    ) {
        this.explanationId = explanationId;
        this.searchExecutionId = searchExecutionId;
        this.schemaVersion = schemaVersion;
        this.availability = availability;
        this.querySummary = querySummary;
        this.interpretationSummary = interpretationSummary;
        this.alternativeInterpretations = Collections.unmodifiableList(
                new ArrayList<>(alternativeInterpretations)
        );
        this.selectionSummary = selectionSummary;
        this.resultExplanations = Collections.unmodifiableList(
                new ArrayList<>(resultExplanations)
        );
        this.evidenceSummary = Collections.unmodifiableList(
                new ArrayList<>(evidenceSummary)
        );
        this.missingEvidenceReport = Collections.unmodifiableList(
                new ArrayList<>(missingEvidenceReport)
        );
        this.limitations = Collections.unmodifiableList(
                new ArrayList<>(limitations)
        );
        this.generationMetadata = generationMetadata;
    }

    public String getExplanationId() { return explanationId; }
    public String getSearchExecutionId() { return searchExecutionId; }
    public String getSchemaVersion() { return schemaVersion; }
    public Availability getAvailability() { return availability; }
    public QuerySummary getQuerySummary() { return querySummary; }
    public InterpretationSummary getInterpretationSummary() {
        return interpretationSummary;
    }
    public List<InterpretationSummary> getAlternativeInterpretations() {
        return alternativeInterpretations;
    }
    public SelectionSummary getSelectionSummary() { return selectionSummary; }
    public List<ResultExplanation> getResultExplanations() {
        return resultExplanations;
    }
    public List<EvidenceReference> getEvidenceSummary() {
        return evidenceSummary;
    }
    public List<MissingEvidenceItem> getMissingEvidenceReport() {
        return missingEvidenceReport;
    }
    public List<String> getLimitations() { return limitations; }
    public GenerationMetadata getGenerationMetadata() {
        return generationMetadata;
    }

    public enum Availability {
        AVAILABLE,
        PARTIAL,
        UNAVAILABLE
    }
}
