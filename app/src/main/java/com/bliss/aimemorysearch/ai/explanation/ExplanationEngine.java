package com.bliss.aimemorysearch.ai.explanation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pure deterministic projection of completed search observations. */
public final class ExplanationEngine {

    private static final String ENGINE_VERSION = "M-ASQ-10.2";
    private static final String LIMIT_SELECTION_ONLY =
            "SELECTION_EVIDENCE_ONLY";
    private static final String LIMIT_CONTENT_NOT_VERIFIED =
            "RESULT_CONTENT_NOT_VERIFIED";
    private static final String LIMIT_CONFIDENCE_NOT_FACT_PROBABILITY =
            "CONFIDENCE_NOT_FACTUAL_PROBABILITY";
    private static final String LIMIT_MISSING_NOT_NEGATIVE =
            "MISSING_EVIDENCE_NOT_NEGATIVE_EVIDENCE";

    public Explanation project(ExplanationInput input) {
        validate(input);

        List<ResultExplanation> projectedResults = new ArrayList<>(
                input.getOrderedResults().size()
        );
        List<EvidenceReference> evidenceSummary = new ArrayList<>();
        List<MissingEvidenceItem> missingEvidenceReport = new ArrayList<>();

        for (ResultExplanation suppliedResult : input.getOrderedResults()) {
            String resultId = suppliedResult.getResultId();
            List<EvidenceReference> evidence = suppliedEvidence(
                    input.getEvidenceByResultId(),
                    suppliedResult
            );
            AgreementPresentation agreement = suppliedValue(
                    input.getAgreementByResultId(),
                    resultId,
                    suppliedResult.getAgreement()
            );
            ConfidencePresentation confidence = suppliedValue(
                    input.getConfidenceByResultId(),
                    resultId,
                    suppliedResult.getConfidence()
            );

            ResultExplanation projected = new ResultExplanation(
                    suppliedResult.getResultId(),
                    suppliedResult.getPath(),
                    suppliedResult.getModality(),
                    suppliedResult.getFinalRank(),
                    suppliedResult.getFinalScore(),
                    suppliedResult.getInterpretationId(),
                    agreement,
                    confidence,
                    evidence,
                    suppliedResult.getProvenanceReferences(),
                    suppliedResult.getMissingEvidence(),
                    suppliedResult.getCompleteness()
            );
            projectedResults.add(projected);
            evidenceSummary.addAll(evidence);
            missingEvidenceReport.addAll(projected.getMissingEvidence());
        }

        return new Explanation(
                explanationId(input),
                input.getSearchExecutionId(),
                input.getSchemaVersion(),
                availability(projectedResults),
                input.getQuerySummary(),
                input.getSelectedInterpretation(),
                input.getAlternativeInterpretations(),
                input.getSelectionSummary(),
                projectedResults,
                evidenceSummary,
                missingEvidenceReport,
                limitations(),
                new GenerationMetadata(0L, ENGINE_VERSION, null)
        );
    }

    private static void validate(ExplanationInput input) {
        require(input != null, "input");
        requireText(input.getSearchExecutionId(), "searchExecutionId");
        requireText(input.getSchemaVersion(), "schemaVersion");
        require(input.getQuerySummary() != null, "querySummary");
        require(input.getSelectedInterpretation() != null,
                "selectedInterpretation");
        require(input.getSelectionSummary() != null, "selectionSummary");
        require(input.getOrderedResults() != null, "orderedResults");
        require(input.getAlternativeInterpretations() != null,
                "alternativeInterpretations");
        require(input.getEvidenceByResultId() != null, "evidenceByResultId");
        require(input.getAgreementByResultId() != null,
                "agreementByResultId");
        require(input.getConfidenceByResultId() != null,
                "confidenceByResultId");

        Set<String> resultIds = new HashSet<>();
        for (ResultExplanation result : input.getOrderedResults()) {
            require(result != null, "orderedResults contains null");
            requireText(result.getResultId(), "resultId");
            require(resultIds.add(result.getResultId()),
                    "duplicate resultId: " + result.getResultId());
        }
        validateKeys(input.getEvidenceByResultId(), resultIds,
                "evidenceByResultId");
        validateKeys(input.getAgreementByResultId(), resultIds,
                "agreementByResultId");
        validateKeys(input.getConfidenceByResultId(), resultIds,
                "confidenceByResultId");
    }

    private static void validateKeys(
            Map<String, ?> values,
            Set<String> resultIds,
            String field
    ) {
        for (String resultId : values.keySet()) {
            require(resultIds.contains(resultId),
                    field + " contains unknown resultId: " + resultId);
        }
    }

    private static List<EvidenceReference> suppliedEvidence(
            Map<String, List<EvidenceReference>> evidenceByResultId,
            ResultExplanation result
    ) {
        if (evidenceByResultId.containsKey(result.getResultId())) {
            return evidenceByResultId.get(result.getResultId());
        }
        return result.getEvidenceReferences();
    }

    private static <T> T suppliedValue(
            Map<String, T> suppliedByResultId,
            String resultId,
            T embeddedValue
    ) {
        if (suppliedByResultId.containsKey(resultId)) {
            return suppliedByResultId.get(resultId);
        }
        return embeddedValue;
    }

    private static Explanation.Availability availability(
            List<ResultExplanation> results
    ) {
        if (results.isEmpty()) return Explanation.Availability.UNAVAILABLE;
        for (ResultExplanation result : results) {
            if (result.getCompleteness()
                    != ResultExplanation.Completeness.COMPLETE) {
                return Explanation.Availability.PARTIAL;
            }
        }
        return Explanation.Availability.AVAILABLE;
    }

    private static String explanationId(ExplanationInput input) {
        return input.getSearchExecutionId()
                + ":explanation:"
                + input.getSchemaVersion();
    }

    private static List<String> limitations() {
        List<String> values = new ArrayList<>(4);
        values.add(LIMIT_SELECTION_ONLY);
        values.add(LIMIT_CONTENT_NOT_VERIFIED);
        values.add(LIMIT_CONFIDENCE_NOT_FACT_PROBABILITY);
        values.add(LIMIT_MISSING_NOT_NEGATIVE);
        return values;
    }

    private static void requireText(String value, String field) {
        require(value != null && !value.isEmpty(), field);
    }

    private static void require(boolean condition, String field) {
        if (!condition) {
            throw new IllegalArgumentException("Invalid ExplanationInput: "
                    + field);
        }
    }
}
