package com.bliss.aimemorysearch.ai.explanation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable presentation of an agreement observation calculated upstream. */
public final class AgreementPresentation {
    private final boolean available;
    private final Float agreementScore;
    private final Integer matchedComponentCount;
    private final Integer totalComponentCount;
    private final List<String> matchedComponentReferences;
    private final Float documentAgreement;
    private final Float imageAgreement;
    private final String interpretationId;
    private final String strategyId;

    public AgreementPresentation(
            boolean available,
            Float agreementScore,
            Integer matchedComponentCount,
            Integer totalComponentCount,
            List<String> matchedComponentReferences,
            Float documentAgreement,
            Float imageAgreement,
            String interpretationId,
            String strategyId
    ) {
        this.available = available;
        this.agreementScore = agreementScore;
        this.matchedComponentCount = matchedComponentCount;
        this.totalComponentCount = totalComponentCount;
        this.matchedComponentReferences = Collections.unmodifiableList(
                new ArrayList<>(matchedComponentReferences)
        );
        this.documentAgreement = documentAgreement;
        this.imageAgreement = imageAgreement;
        this.interpretationId = interpretationId;
        this.strategyId = strategyId;
    }

    public boolean isAvailable() { return available; }
    public Float getAgreementScore() { return agreementScore; }
    public Integer getMatchedComponentCount() { return matchedComponentCount; }
    public Integer getTotalComponentCount() { return totalComponentCount; }
    public List<String> getMatchedComponentReferences() {
        return matchedComponentReferences;
    }
    public Float getDocumentAgreement() { return documentAgreement; }
    public Float getImageAgreement() { return imageAgreement; }
    public String getInterpretationId() { return interpretationId; }
    public String getStrategyId() { return strategyId; }
}
