package com.bliss.aimemorysearch.ai.evidence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import org.junit.Test;

public class DocumentConfidenceCalculatorTest {

    @Test
    public void unavailableChannelsRemainUnavailable() {
        DocumentEvidence evidence = evidence(
                false, false, 1, 0f, 0f,
                new DocumentEvidence.Provenance(true, false, true, false, false)
        );

        DocumentConfidence confidence =
                DocumentConfidenceCalculator.calculate(evidence, "/document.pdf");

        assertTrue(confidence.getBm25().isAvailable());
        assertFalse(confidence.getSemantic().isAvailable());
        assertFalse(confidence.getCanonicalCoverage().isAvailable());
        assertFalse(confidence.getScoreMargin().isAvailable());
    }

    @Test
    public void exactMatchIsBinaryAndDoesNotChangeCombinedConfidence() {
        DocumentEvidence withoutExact = evidence(
                true, true, 2, 0f, 2f,
                new DocumentEvidence.Provenance(true, true, true, true, false)
        );
        DocumentEvidence withExact = evidence(
                true, true, 2, 4f, 2f,
                new DocumentEvidence.Provenance(true, true, true, true, false)
        );

        DocumentConfidence first = DocumentConfidenceCalculator.calculate(
                withoutExact, "/document.pdf");
        DocumentConfidence second = DocumentConfidenceCalculator.calculate(
                withExact, "/document.pdf");

        assertEquals(0f, first.getExactMatch().getValue(), 0f);
        assertEquals(1f, second.getExactMatch().getValue(), 0f);
        assertEquals(
                first.getConfidence().getValue(),
                second.getConfidence().getValue(),
                0f
        );
    }

    @Test
    public void pathWithoutDocumentProvenanceIsUnavailable() {
        DocumentConfidence confidence = DocumentConfidenceCalculator.calculate(
                evidence(
                        true, true, 2, 1f, 2f,
                        new DocumentEvidence.Provenance(
                                true, true, true, false, false)
                ),
                "/image.jpg"
        );

        assertFalse(confidence.getConfidence().isAvailable());
        assertFalse(confidence.getBm25().isAvailable());
        assertFalse(confidence.getSemantic().isAvailable());
    }

    @Test
    public void normalizedValuesStayInUnitRange() {
        DocumentConfidence confidence = DocumentConfidenceCalculator.calculate(
                evidence(
                        true, true, 2, 3f, 2f,
                        new DocumentEvidence.Provenance(
                                true, true, true, true, true)
                ),
                "/document.pdf"
        );

        assertUnit(confidence.getConfidence());
        assertUnit(confidence.getBm25());
        assertUnit(confidence.getSemantic());
        assertUnit(confidence.getCoverage());
        assertUnit(confidence.getCanonicalCoverage());
        assertUnit(confidence.getExactMatch());
        assertUnit(confidence.getScoreMargin());
    }

    @Test
    public void queryMarginOnlyRewardsTheLeadingDocument() {
        DocumentEvidence.Provenance provenance =
                new DocumentEvidence.Provenance(
                        true, false, true, false, false);
        DocumentEvidence leader = new DocumentEvidence(
                3, 4.5f, 0.7f, 1f, 0f, 1, 1f,
                true, false, false, false,
                12f, 10f, 2f,
                Collections.singletonMap("/leader.pdf", provenance)
        );
        DocumentEvidence lower = new DocumentEvidence(
                3, 4.5f, 0.7f, 1f, 0f, 1, 1f,
                true, false, false, false,
                8f, 10f, 2f,
                Collections.singletonMap("/lower.pdf", provenance)
        );

        DocumentConfidence leaderConfidence =
                DocumentConfidenceCalculator.calculate(
                        leader, "/leader.pdf");
        DocumentConfidence lowerConfidence =
                DocumentConfidenceCalculator.calculate(
                        lower, "/lower.pdf");

        assertTrue(leaderConfidence.getScoreMargin().getValue() > 0f);
        assertEquals(0f, lowerConfidence.getScoreMargin().getValue(), 0f);
    }

    private static DocumentEvidence evidence(
            boolean semanticAvailable,
            boolean canonicalAvailable,
            int candidateCount,
            float exactMatches,
            float margin,
            DocumentEvidence.Provenance provenance
    ) {
        return new DocumentEvidence(
                candidateCount,
                4.5f,
                0.7f,
                1f,
                0.8f,
                (int) exactMatches,
                0.5f,
                semanticAvailable,
                canonicalAvailable,
                false,
                false,
                12f,
                10f,
                margin,
                Collections.singletonMap("/document.pdf", provenance)
        );
    }

    private static void assertUnit(
            DocumentConfidence.NormalizedSignal signal
    ) {
        assertTrue(signal.isAvailable());
        assertTrue(signal.getValue() >= 0f);
        assertTrue(signal.getValue() <= 1f);
    }
}
