package com.bliss.aimemorysearch.ai.explanation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable reference to evidence supplied by an upstream search stage. */
public final class EvidenceReference {
    private final String evidenceId;
    private final String resultId;
    private final Category category;
    private final String sourceObjectType;
    private final String sourceField;
    private final Scope scope;
    private final boolean available;
    private final Double rawValue;
    private final Float normalizedValue;
    private final List<String> provenance;
    private final String interpretationId;
    private final String calibrationVersion;

    public EvidenceReference(
            String evidenceId,
            String resultId,
            Category category,
            String sourceObjectType,
            String sourceField,
            Scope scope,
            boolean available,
            Double rawValue,
            Float normalizedValue,
            List<String> provenance,
            String interpretationId,
            String calibrationVersion
    ) {
        this.evidenceId = evidenceId;
        this.resultId = resultId;
        this.category = category;
        this.sourceObjectType = sourceObjectType;
        this.sourceField = sourceField;
        this.scope = scope;
        this.available = available;
        this.rawValue = rawValue;
        this.normalizedValue = normalizedValue;
        this.provenance = Collections.unmodifiableList(
                new ArrayList<>(provenance)
        );
        this.interpretationId = interpretationId;
        this.calibrationVersion = calibrationVersion;
    }

    public String getEvidenceId() { return evidenceId; }
    public String getResultId() { return resultId; }
    public Category getCategory() { return category; }
    public String getSourceObjectType() { return sourceObjectType; }
    public String getSourceField() { return sourceField; }
    public Scope getScope() { return scope; }
    public boolean isAvailable() { return available; }
    public Double getRawValue() { return rawValue; }
    public Float getNormalizedValue() { return normalizedValue; }
    public List<String> getProvenance() { return provenance; }
    public String getInterpretationId() { return interpretationId; }
    public String getCalibrationVersion() { return calibrationVersion; }

    public enum Category {
        LEXICAL,
        SEMANTIC,
        COVERAGE,
        CANONICAL_COVERAGE,
        EXACT_MATCH,
        SCORE_MARGIN,
        AGREEMENT,
        AVAILABILITY,
        PROVENANCE,
        FINAL_SCORE
    }

    public enum Scope {
        RESULT,
        INTERPRETATION,
        QUERY,
        GLOBAL
    }
}
