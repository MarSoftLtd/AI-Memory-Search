package com.bliss.aimemorysearch.ai.concepts;

/** Immutable semantic statement emitted exclusively by an assertion provider. */
public final class SemanticAssertion {

    private final AssertionType assertionType;
    private final ConceptGraph.Concept sourceConcept;
    private final ConceptGraph.Concept targetConcept;
    private final float confidence;
    private final String provenance;
    private final boolean available;
    private final String providerId;

    public SemanticAssertion(
            AssertionType assertionType,
            ConceptGraph.Concept sourceConcept,
            ConceptGraph.Concept targetConcept,
            float confidence,
            String provenance,
            boolean available,
            String providerId
    ) {
        this.assertionType = assertionType;
        this.sourceConcept = sourceConcept;
        this.targetConcept = targetConcept;
        this.confidence = confidence;
        this.provenance = provenance;
        this.available = available;
        this.providerId = providerId;
    }

    public AssertionType getAssertionType() { return assertionType; }
    public ConceptGraph.Concept getSourceConcept() { return sourceConcept; }
    public ConceptGraph.Concept getTargetConcept() { return targetConcept; }
    public float getConfidence() { return confidence; }
    public String getProvenance() { return provenance; }
    public boolean isAvailable() { return available; }
    public String getProviderId() { return providerId; }

    public enum AssertionType {
        HEAD,
        MODIFIER,
        ENTITY,
        TEMPORAL_CONSTRAINT,
        METADATA_CONSTRAINT,
        RELATION
    }
}
