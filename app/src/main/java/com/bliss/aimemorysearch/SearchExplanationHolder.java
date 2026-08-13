package com.bliss.aimemorysearch;

import com.bliss.aimemorysearch.ai.evidence.DocumentEvidence;
import com.bliss.aimemorysearch.ai.evidence.DocumentConfidence;
import com.bliss.aimemorysearch.ai.evidence.ImageEvidence;
import com.bliss.aimemorysearch.ai.concepts.MatchTier;

import java.util.HashMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

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
    private static volatile Map<String, MatchTier> imageMatchTiers =
            Collections.emptyMap();
    private static volatile Map<String, Float> imageAgreementByPath =
            Collections.emptyMap();
    private static volatile String originalQuery = "";
    private static volatile Map<String, Float> imageClipCosineByPath =
            Collections.emptyMap();
    private static volatile Map<String, EmailMetadata> emailMetadataByPath =
            Collections.emptyMap();
    private static volatile QueryMetadata queryMetadata = QueryMetadata.empty();

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

    public static void setImageMatchTiers(Map<String, MatchTier> matchTiers) {
        imageMatchTiers = Collections.unmodifiableMap(
                new LinkedHashMap<>(matchTiers)
        );
    }

    public static Map<String, MatchTier> getImageMatchTiers() {
        return imageMatchTiers;
    }

    public static void setImageAgreementByPath(Map<String, Float> agreement) {
        imageAgreementByPath = Collections.unmodifiableMap(
                new LinkedHashMap<>(agreement)
        );
    }

    public static Map<String, Float> getImageAgreementByPath() {
        return imageAgreementByPath;
    }

    public static void setOriginalQuery(String query) {
        originalQuery = query == null ? "" : query;
    }

    public static String getOriginalQuery() { return originalQuery; }

    public static void setImageClipCosineByPath(Map<String, Float> scores) {
        imageClipCosineByPath = Collections.unmodifiableMap(
                new LinkedHashMap<>(scores));
    }

    public static Map<String, Float> getImageClipCosineByPath() {
        return imageClipCosineByPath;
    }

    public static void setEmailMetadataByPath(
            Map<String, EmailMetadata> metadata) {
        emailMetadataByPath = Collections.unmodifiableMap(
                new LinkedHashMap<>(metadata));
    }

    public static Map<String, EmailMetadata> getEmailMetadataByPath() {
        return emailMetadataByPath;
    }

    public static final class EmailMetadata {
        public final String provider;
        public final String account;
        public final String messageId;
        public final String threadId;
        public final String sender;
        public final long timestamp;

        public EmailMetadata(String sender, long timestamp) {
            this("", "", "", "", sender, timestamp);
        }

        public EmailMetadata(String provider, String account, String messageId,
                             String threadId, String sender, long timestamp) {
            this.provider = safe(provider);
            this.account = safe(account);
            this.messageId = safe(messageId);
            this.threadId = safe(threadId);
            this.sender = sender == null ? "" : sender;
            this.timestamp = timestamp;
        }
    }

    public static void setQueryMetadata(QueryMetadata metadata) {
        queryMetadata = metadata == null ? QueryMetadata.empty() : metadata;
    }

    public static QueryMetadata getQueryMetadata() { return queryMetadata; }

    /** Immutable snapshot of real query/runtime data used by the completed search. */
    public static final class QueryMetadata {
        public final String normalizedQuery;
        public final String translatedQuery;
        public final String detectedLanguage;
        public final String workingLanguage;
        public final String languageFamily;
        public final List<String> pivots;
        public final String interpretation;
        public final String embeddingPipeline;
        public final String embeddingPackage;
        public final String imagePipeline;
        public final String imagePackage;
        public final String translationPackage;

        public QueryMetadata(String normalizedQuery, String translatedQuery,
                String detectedLanguage, String workingLanguage,
                String languageFamily, List<String> pivots, String interpretation,
                String embeddingPipeline, String embeddingPackage,
                String imagePipeline, String imagePackage,
                String translationPackage) {
            this.normalizedQuery = safe(normalizedQuery);
            this.translatedQuery = safe(translatedQuery);
            this.detectedLanguage = safe(detectedLanguage);
            this.workingLanguage = safe(workingLanguage);
            this.languageFamily = safe(languageFamily);
            this.pivots = Collections.unmodifiableList(new ArrayList<>(
                    pivots == null ? Collections.emptyList() : pivots));
            this.interpretation = safe(interpretation);
            this.embeddingPipeline = safe(embeddingPipeline);
            this.embeddingPackage = safe(embeddingPackage);
            this.imagePipeline = safe(imagePipeline);
            this.imagePackage = safe(imagePackage);
            this.translationPackage = safe(translationPackage);
        }

        static QueryMetadata empty() {
            return new QueryMetadata("", "", "", "", "",
                    Collections.emptyList(), "", "", "", "", "", "");
        }
    }

    private static String safe(String value) { return value == null ? "" : value; }

    public static void clear() {

        snippets.clear();
        scores.clear();
        documentEvidenceByPath = Collections.emptyMap();
        imageEvidence = null;
        documentConfidences = Collections.emptyMap();
        imageMatchTiers = Collections.emptyMap();
        imageAgreementByPath = Collections.emptyMap();
        originalQuery = "";
        imageClipCosineByPath = Collections.emptyMap();
        emailMetadataByPath = Collections.emptyMap();
        queryMetadata = QueryMetadata.empty();
    }
}
