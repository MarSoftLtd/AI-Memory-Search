package com.bliss.aimemorysearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.bliss.aimemorysearch.ai.concepts.MatchTier;
import com.bliss.aimemorysearch.ai.evidence.DocumentConfidence;
import com.bliss.aimemorysearch.ai.evidence.DocumentEvidence;
import com.bliss.aimemorysearch.db.FileEntity;

import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class TechnicalSearchReportBuilderTest {

    @After public void clear() { SearchExplanationHolder.clear(); }

    @Test public void transportsQueryOrderTypesAndRealImageMetrics() {
        FileEntity email = file("email://1", "Status update", "EMAIL");
        FileEntity image = file("/photo.jpg", "photo.jpg", "IMAGE");
        FileEntity pdf = file("/report.pdf", "report.pdf", "PDF");
        FileEntity document = file("/notes.docx", "notes.docx", "DOCUMENT");

        SearchExplanationHolder.setOriginalQuery("quarterly status");
        SearchExplanationHolder.setQueryMetadata(
                new SearchExplanationHolder.QueryMetadata(
                        "quarterly status", "quarterly status", "en", "en",
                        "latin", Collections.singletonList("quarterly status"),
                        "DOCUMENT_LOOKUP", "DocumentRuntimeLoader / EmbeddingEngine",
                        "core-model-embedding / 1", "MobileClipTextEmbeddingEngine",
                        "core-model-clip-text / 1", ""));
        image.fileSize = 2048L;
        image.lastModified = 123L;
        SearchExplanationHolder.scores.put(image.path, 12.5f);
        SearchExplanationHolder.setImageClipCosineByPath(
                Collections.singletonMap(image.path, 0.625f));
        SearchExplanationHolder.setImageMatchTiers(
                Collections.singletonMap(image.path, MatchTier.BEST_MATCH));

        TechnicalSearchReportBuilder.Report report =
                TechnicalSearchReportBuilder.build(Arrays.asList(
                        email, image, pdf, document));

        assertEquals("quarterly status", report.query);
        assertEquals(4, report.resultCount);
        assertEquals(1, report.emailCount);
        assertEquals(1, report.imageCount);
        assertEquals(1, report.pdfCount);
        assertEquals(1, report.documentCount);
        assertEquals("EMAIL", report.results.get(0).type);
        assertEquals(email.path, report.results.get(0).path);
        assertEquals("IMAGE", report.results.get(1).type);
        assertEquals(image.path, report.results.get(1).path);
        assertTrue(report.results.get(1).metrics.contains(
                "Final score: 12.500000"));
        assertTrue(report.results.get(1).metrics.contains(
                "CLIP cosine: 0.625000"));
        assertTrue(report.results.get(1).metrics.contains(
                "Match tier: BEST_MATCH"));
        assertTrue(report.results.get(1).metrics.contains("Extension: JPG"));
        assertTrue(report.results.get(1).metrics.contains("File size: 2048 bytes"));
        assertTrue(report.results.get(1).metrics.contains(
                "Detected query language: en"));
        assertTrue(report.results.get(1).metrics.contains(
                "Model package: core-model-clip-text / 1"));
    }

    @Test public void exposesOnlyAvailableDocumentEvidenceAndEmailMetadata() {
        FileEntity email = file("email://2", "Meeting", "EMAIL");
        DocumentEvidence.Provenance provenance =
                new DocumentEvidence.Provenance(true, true, true, false, false);
        Map<String, DocumentEvidence.Provenance> provenanceByPath =
                Collections.singletonMap(email.path, provenance);
        DocumentEvidence evidence = new DocumentEvidence(
                2, 3.5f, 0.4f, 1f, 0.75f, 1, 1f,
                true, true, true, false, 8f, 6f, 2f,
                provenanceByPath);
        SearchExplanationHolder.setEvidence(
                Collections.singletonMap(email.path, evidence), null);
        DocumentConfidence.NormalizedSignal unavailable =
                DocumentConfidence.NormalizedSignal.unavailable();
        DocumentConfidence confidence = new DocumentConfidence(
                email.path, DocumentConfidence.NormalizedSignal.available(0.8f),
                unavailable, unavailable, unavailable, unavailable,
                unavailable, unavailable);
        SearchExplanationHolder.setDocumentConfidences(
                Collections.singletonMap(email.path, confidence));
        SearchExplanationHolder.setEmailMetadataByPath(
                Collections.singletonMap(email.path,
                        new SearchExplanationHolder.EmailMetadata(
                                "gmail", "alice@example.com", "msg-2", "thread-1",
                                "alice@example.com", 123456789L)));

        TechnicalSearchReportBuilder.ResultBlock result =
                TechnicalSearchReportBuilder.build(
                        Collections.singletonList(email)).results.get(0);

        assertTrue(result.metrics.contains("Confidence: 0.800000"));
        assertTrue(result.metrics.contains("Semantic: 0.400000"));
        assertTrue(result.metrics.contains("BM25: 3.500000"));
        assertTrue(result.metrics.contains("Coverage: 1.000000"));
        assertTrue(result.metrics.contains("Canonical coverage: 0.750000"));
        assertTrue(result.metrics.contains("Exact match: yes"));
        assertTrue(result.metrics.contains("Email provider: gmail"));
        assertTrue(result.metrics.contains("Email messageId: msg-2"));
        assertTrue(result.metrics.contains("Provenance: lexical, canonical, "
                + "original, sender=alice@example.com, timestamp=123456789"));
    }

    @Test public void sourceColorsAreDistinctWithoutSpreadsheetType() {
        int email = SourceVisuals.colorResource("EMAIL");
        int image = SourceVisuals.colorResource("IMAGE");
        int pdf = SourceVisuals.colorResource("PDF");
        int document = SourceVisuals.colorResource("DOCUMENT");
        assertNotEquals(email, image);
        assertNotEquals(email, pdf);
        assertNotEquals(email, document);
        assertNotEquals(image, pdf);
        assertNotEquals(image, document);
        assertNotEquals(pdf, document);
    }

    private static FileEntity file(String path, String name, String type) {
        return new FileEntity(path, name, type, null, null,
                0L, 0L, 0L, "DONE", "", "test", null);
    }
}
