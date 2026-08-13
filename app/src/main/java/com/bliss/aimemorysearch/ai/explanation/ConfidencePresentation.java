package com.bliss.aimemorysearch.ai.explanation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable presentation of confidence observations calculated upstream. */
public final class ConfidencePresentation {
    private final boolean available;
    private final Float confidence;
    private final String displayLabel;
    private final String calibrationVersion;
    private final Map<String, EvidenceReference> normalizedSignals;

    public ConfidencePresentation(
            boolean available,
            Float confidence,
            String displayLabel,
            String calibrationVersion,
            Map<String, EvidenceReference> normalizedSignals
    ) {
        this.available = available;
        this.confidence = confidence;
        this.displayLabel = displayLabel;
        this.calibrationVersion = calibrationVersion;
        this.normalizedSignals = Collections.unmodifiableMap(
                new LinkedHashMap<>(normalizedSignals)
        );
    }

    public boolean isAvailable() { return available; }
    public Float getConfidence() { return confidence; }
    public String getDisplayLabel() { return displayLabel; }
    public String getCalibrationVersion() { return calibrationVersion; }
    public Map<String, EvidenceReference> getNormalizedSignals() {
        return normalizedSignals;
    }
}
