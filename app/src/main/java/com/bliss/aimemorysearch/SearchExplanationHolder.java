package com.bliss.aimemorysearch;

import com.bliss.aimemorysearch.ai.evidence.DocumentEvidence;
import com.bliss.aimemorysearch.ai.evidence.DocumentConfidence;
import com.bliss.aimemorysearch.ai.evidence.ImageEvidence;

import java.util.HashMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class SearchExplanationHolder {

    public static final Map<String, String> snippets =
            new HashMap<>();

    public static final Map<String, Float> scores =
            new HashMap<>();

    private static volatile Map<String, DocumentEvidence> documentEvidenceByPath =
            Collections.emptyMap();
    private static volatile ImageEvidence imageEvidence;
    private static volatile Map<String, DocumentConfidence> documentConfidences =
            Collections.emptyMap();

    public static void setEvidence(
            Map<String, DocumentEvidence> documents,
            ImageEvidence images
    ) {
        documentEvidenceByPath = Collections.unmodifiableMap(
                new LinkedHashMap<>(documents)
        );
        imageEvidence = images;
    }

    public static Map<String, DocumentEvidence> getDocumentEvidenceByPath() {
        return documentEvidenceByPath;
    }

    public static ImageEvidence getImageEvidence() {
        return imageEvidence;
    }

    public static void setDocumentConfidences(
            Map<String, DocumentConfidence> confidences
    ) {
        documentConfidences = Collections.unmodifiableMap(
                new LinkedHashMap<>(confidences)
        );
    }

    public static Map<String, DocumentConfidence> getDocumentConfidences() {
        return documentConfidences;
    }

    public static void clear() {

        snippets.clear();
        scores.clear();
        documentEvidenceByPath = Collections.emptyMap();
        imageEvidence = null;
        documentConfidences = Collections.emptyMap();
    }
}
