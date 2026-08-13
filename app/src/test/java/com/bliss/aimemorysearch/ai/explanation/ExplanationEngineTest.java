package com.bliss.aimemorysearch.ai.explanation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import com.bliss.aimemorysearch.ai.concepts.Interpretation;
import com.bliss.aimemorysearch.ai.concepts.InterpretationExecutionResult;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExplanationEngineTest {

    private final ExplanationEngine engine = new ExplanationEngine();

    @Test
    public void projectionIsDeterministicAndPreservesProductionFields() {
        ResultExplanation second = result("r2", 2, 0.61f,
                ResultExplanation.Completeness.COMPLETE);
        ResultExplanation first = result("r1", 1, 0.91f,
                ResultExplanation.Completeness.COMPLETE);
        ExplanationInput input = input(Arrays.asList(second, first));

        Explanation left = engine.project(input);
        Explanation right = engine.project(input);

        assertEquals("search-1:explanation:1", left.getExplanationId());
        assertEquals(left.getExplanationId(), right.getExplanationId());
        assertEquals(
                left.getGenerationMetadata().getGeneratedAtEpochMillis(),
                right.getGenerationMetadata().getGeneratedAtEpochMillis()
        );
        assertEquals("r2", left.getResultExplanations().get(0).getResultId());
        assertEquals(2, left.getResultExplanations().get(0).getFinalRank());
        assertEquals(Float.valueOf(0.61f),
                left.getResultExplanations().get(0).getFinalScore());
        assertEquals("r1", left.getResultExplanations().get(1).getResultId());
        assertEquals(Explanation.Availability.AVAILABLE,
                left.getAvailability());
        assertUnsupported(() -> left.getResultExplanations().clear());
        assertNotSame(input.getOrderedResults(), left.getResultExplanations());
    }

    @Test
    public void attachesSuppliedEvidenceAgreementConfidenceAndMissingItems() {
        EvidenceReference attachedEvidence = evidence("attached", "r1");
        AgreementPresentation attachedAgreement = agreement(0.88f);
        ConfidencePresentation attachedConfidence = confidence(0.77f);
        MissingEvidenceItem missing = new MissingEvidenceItem(
                EvidenceReference.Category.CANONICAL_COVERAGE,
                "r1",
                "i1",
                MissingEvidenceItem.State.UNAVAILABLE,
                "NOT_SUPPLIED",
                null
        );
        ResultExplanation source = new ResultExplanation(
                "r1",
                "/one.pdf",
                InterpretationExecutionResult.Modality.DOCUMENT,
                1,
                0.9f,
                "i1",
                agreement(0.1f),
                confidence(0.2f),
                Collections.singletonList(evidence("embedded", "r1")),
                Collections.singletonList("original"),
                Collections.singletonList(missing),
                ResultExplanation.Completeness.PARTIAL
        );
        Map<String, List<EvidenceReference>> evidence = new LinkedHashMap<>();
        evidence.put("r1", Collections.singletonList(attachedEvidence));
        Map<String, AgreementPresentation> agreements = new LinkedHashMap<>();
        agreements.put("r1", attachedAgreement);
        Map<String, ConfidencePresentation> confidences = new LinkedHashMap<>();
        confidences.put("r1", attachedConfidence);

        Explanation projected = engine.project(input(
                Collections.singletonList(source),
                evidence,
                agreements,
                confidences
        ));
        ResultExplanation result = projected.getResultExplanations().get(0);

        assertSame(attachedEvidence, result.getEvidenceReferences().get(0));
        assertSame(attachedAgreement, result.getAgreement());
        assertSame(attachedConfidence, result.getConfidence());
        assertSame(missing, projected.getMissingEvidenceReport().get(0));
        assertEquals(Collections.singletonList(attachedEvidence),
                projected.getEvidenceSummary());
        assertEquals(Explanation.Availability.PARTIAL,
                projected.getAvailability());
    }

    @Test
    public void rejectsDuplicateResultIds() {
        ResultExplanation first = result("r1", 1, 0.9f,
                ResultExplanation.Completeness.COMPLETE);
        ResultExplanation duplicate = result("r1", 2, 0.8f,
                ResultExplanation.Completeness.COMPLETE);

        assertInvalid(() -> engine.project(input(Arrays.asList(first, duplicate))));
    }

    @Test
    public void rejectsObservationForUnknownResult() {
        Map<String, AgreementPresentation> agreements = new LinkedHashMap<>();
        agreements.put("unknown", agreement(0.5f));

        assertInvalid(() -> engine.project(input(
                Collections.singletonList(result(
                        "r1", 1, 0.9f,
                        ResultExplanation.Completeness.COMPLETE
                )),
                new LinkedHashMap<>(),
                agreements,
                new LinkedHashMap<>()
        )));
    }

    private static ExplanationInput input(List<ResultExplanation> results) {
        return input(
                results,
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>()
        );
    }

    private static ExplanationInput input(
            List<ResultExplanation> results,
            Map<String, List<EvidenceReference>> evidence,
            Map<String, AgreementPresentation> agreements,
            Map<String, ConfidencePresentation> confidences
    ) {
        InterpretationSummary interpretation = new InterpretationSummary(
                "i1",
                Interpretation.Kind.ATOMIC_CONJUNCTION,
                Arrays.asList("white", "cat"),
                "atomic",
                false,
                true,
                true,
                0.9f,
                results.size(),
                results.size(),
                0
        );
        return new ExplanationInput(
                "search-1",
                "1",
                new QuerySummary("q1", "white cat", true),
                new ArrayList<>(results),
                interpretation,
                Collections.emptyList(),
                new SelectionSummary(
                        results.size(),
                        results.size(),
                        0,
                        "i1",
                        agreement(0.9f),
                        true
                ),
                evidence,
                agreements,
                confidences
        );
    }

    private static ResultExplanation result(
            String id,
            int rank,
            float score,
            ResultExplanation.Completeness completeness
    ) {
        return new ResultExplanation(
                id,
                "/" + id + ".pdf",
                InterpretationExecutionResult.Modality.DOCUMENT,
                rank,
                score,
                "i1",
                agreement(0.8f),
                confidence(0.7f),
                Collections.singletonList(evidence("e-" + id, id)),
                Collections.singletonList("original"),
                Collections.emptyList(),
                completeness
        );
    }

    private static EvidenceReference evidence(String evidenceId, String resultId) {
        return new EvidenceReference(
                evidenceId,
                resultId,
                EvidenceReference.Category.SEMANTIC,
                "DocumentEvidence",
                "bestSemanticScore",
                EvidenceReference.Scope.RESULT,
                true,
                0.7d,
                0.8f,
                Collections.singletonList("original"),
                "i1",
                "1"
        );
    }

    private static AgreementPresentation agreement(float score) {
        return new AgreementPresentation(
                true,
                score,
                2,
                2,
                Arrays.asList("white", "cat"),
                null,
                null,
                "i1",
                "atomic"
        );
    }

    private static ConfidencePresentation confidence(float score) {
        return new ConfidencePresentation(
                true,
                score,
                "Strong",
                "1",
                Collections.emptyMap()
        );
    }

    private static void assertUnsupported(Runnable operation) {
        try {
            operation.run();
            fail("Expected immutable output");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }

    private static void assertInvalid(Runnable operation) {
        try {
            operation.run();
            fail("Expected invalid input to be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }
}
