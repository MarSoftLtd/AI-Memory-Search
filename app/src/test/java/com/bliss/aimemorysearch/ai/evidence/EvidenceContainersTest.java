package com.bliss.aimemorysearch.ai.evidence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import org.junit.Test;

public class EvidenceContainersTest {

    @Test
    public void documentEvidenceRetainsSnapshotValues() {
        DocumentEvidence evidence = new DocumentEvidence(
                7, 4.5f, 0.7f, 1f, 0.8f, 3, 0.5f,
                true, true, true, false,
                12f, 10f, 2f,
                Collections.singletonMap(
                        "/document.pdf",
                        new DocumentEvidence.Provenance(
                                true, true, true, false, true
                        )
                )
        );

        assertEquals(7, evidence.getCandidateCount());
        assertEquals(4.5f, evidence.getBestBm25(), 0f);
        assertEquals(0.7f, evidence.getBestSemanticScore(), 0f);
        assertEquals(1f, evidence.getBestCoverage(), 0f);
        assertEquals(0.8f, evidence.getBestCanonicalCoverage(), 0f);
        assertEquals(3, evidence.getExactTokenMatches());
        assertEquals(0.5f, evidence.getPivotAgreement(), 0f);
        assertTrue(evidence.isSemanticAvailable());
        assertTrue(evidence.isCanonicalAvailable());
        assertEquals(2f, evidence.getScoreMargin(), 0f);
        assertTrue(evidence.getProvenanceByPath()
                .get("/document.pdf").hasMultipleSources());
    }

    @Test
    public void imageEvidenceRetainsSnapshotValues() {
        ImageEvidence evidence = new ImageEvidence(
                10, 8, 0.31f, 0.28f, 3, 1, 2, 0.5f,
                true, true, true
        );

        assertEquals(10, evidence.getCandidateCount());
        assertEquals(8, evidence.getAcceptedCandidateCount());
        assertEquals(0.31f, evidence.getBestClipScore(), 0f);
        assertEquals(0.28f, evidence.getAverageClipScore(), 0f);
        assertEquals(3, evidence.getMetadataMatches());
        assertEquals(1, evidence.getFilenameMatches());
        assertEquals(2, evidence.getOcrMatches());
        assertEquals(0.5f, evidence.getPivotAgreement(), 0f);
        assertTrue(evidence.isSemanticAvailable());
        assertTrue(evidence.isMetadataAvailable());
        assertTrue(evidence.isOcrAvailable());
    }

    @Test
    public void intentConfidenceRetainsProvidedValuesWithoutCalculation() {
        IntentConfidence confidence = new IntentConfidence(0.2f, 0.3f, 0.5f);

        assertEquals(0.2f, confidence.getDocumentConfidence(), 0f);
        assertEquals(0.3f, confidence.getImageConfidence(), 0f);
        assertEquals(0.5f, confidence.getMixedConfidence(), 0f);
    }
}
