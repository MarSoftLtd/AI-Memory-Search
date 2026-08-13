package com.bliss.aimemorysearch;

import com.bliss.aimemorysearch.ai.concepts.MatchTier;
import com.bliss.aimemorysearch.ai.evidence.DocumentConfidence;
import com.bliss.aimemorysearch.ai.evidence.DocumentEvidence;
import com.bliss.aimemorysearch.ai.evidence.ImageEvidence;
import com.bliss.aimemorysearch.db.FileEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Pure formatter input for the scrollable technical results report. */
public final class TechnicalSearchReportBuilder {
    private TechnicalSearchReportBuilder() {}

    public static Report build(List<FileEntity> results) {
        List<FileEntity> safe = results == null
                ? Collections.emptyList() : results;
        int images = 0, emails = 0, pdfs = 0, documents = 0;
        for (FileEntity file : safe) {
            String type = sourceType(file);
            if ("IMAGE".equals(type)) images++;
            else if ("EMAIL".equals(type)) emails++;
            else if ("PDF".equals(type)) pdfs++;
            else if ("DOCUMENT".equals(type)) documents++;
        }

        List<ResultBlock> blocks = new ArrayList<>(safe.size());
        for (int index = 0; index < safe.size(); index++) {
            FileEntity file = safe.get(index);
            String path = file.path;
            String type = sourceType(file);
            List<String> metrics = new ArrayList<>();
            addFloat(metrics, "Final score", SearchExplanationHolder.scores.get(path));
            addCommonMetadata(metrics, file);
            addQueryMetadata(metrics, type);

            if ("IMAGE".equals(type)) {
                MatchTier tier = SearchExplanationHolder
                        .getImageMatchTiers().get(path);
                if (tier != null) metrics.add("Match tier: " + tier.name());
                addFloat(metrics, "CLIP cosine", SearchExplanationHolder
                        .getImageClipCosineByPath().get(path));
                addFloat(metrics, "Atomic alignment", SearchExplanationHolder
                        .getImageAgreementByPath().get(path));
                ImageEvidence imageEvidence = SearchExplanationHolder.getImageEvidence();
                if (imageEvidence != null) {
                    metrics.add("Image candidates: " + imageEvidence.getCandidateCount());
                    metrics.add("Accepted image candidates: "
                            + imageEvidence.getAcceptedCandidateCount());
                    metrics.add("Image pivot agreement: "
                            + number(imageEvidence.getPivotAgreement()));
                    metrics.add("Image metadata available: "
                            + yesNo(imageEvidence.isMetadataAvailable()));
                    metrics.add("Image OCR available: "
                            + yesNo(imageEvidence.isOcrAvailable()));
                }
                String provenance = SearchExplanationHolder
                        .getImageAgreementByPath().containsKey(path)
                        ? "CLIP, ATOMIC_ALIGNMENT" : "CLIP";
                metrics.add("Provenance: " + provenance);
            } else {
                DocumentEvidence evidence = SearchExplanationHolder
                        .getDocumentEvidenceByPath().get(path);
                DocumentConfidence confidence = SearchExplanationHolder
                        .getDocumentConfidences().get(path);
                if (confidence != null && confidence.getConfidence().isAvailable()) {
                    addFloat(metrics, "Confidence",
                            confidence.getConfidence().getValue());
                }
                if (confidence != null) {
                    addSignal(metrics, "Confidence semantic component", confidence.getSemantic());
                    addSignal(metrics, "Confidence BM25 component", confidence.getBm25());
                    addSignal(metrics, "Confidence coverage component", confidence.getCoverage());
                    addSignal(metrics, "Confidence canonical component", confidence.getCanonicalCoverage());
                    addSignal(metrics, "Confidence exact-match component", confidence.getExactMatch());
                    addSignal(metrics, "Confidence score-margin component", confidence.getScoreMargin());
                }
                if (evidence != null) {
                    if (evidence.isSemanticAvailable()) {
                        metrics.add("Semantic: " + number(
                                evidence.getBestSemanticScore()));
                    }
                    DocumentEvidence.Provenance provenance = evidence
                            .getProvenanceByPath().get(path);
                    if (provenance != null && provenance.isLexical()) {
                        metrics.add("BM25: " + number(evidence.getBestBm25()));
                        metrics.add("Coverage: " + number(
                                evidence.getBestCoverage()));
                    }
                    if (provenance != null && provenance.isCanonical()
                            && evidence.isCanonicalAvailable()) {
                        metrics.add("Canonical coverage: " + number(
                                evidence.getBestCanonicalCoverage()));
                    }
                    metrics.add("Exact match: "
                            + (evidence.getExactTokenMatches() > 0 ? "yes" : "no"));
                    metrics.add("Pivot agreement: " + number(
                            evidence.getPivotAgreement()));
                    String provenanceText = provenance(provenance);
                    SearchExplanationHolder.EmailMetadata email =
                            SearchExplanationHolder.getEmailMetadataByPath().get(path);
                    if (email != null) {
                        addText(metrics, "Email provider", email.provider);
                        addText(metrics, "Email account", email.account);
                        addText(metrics, "Email messageId", email.messageId);
                        addText(metrics, "Email threadId", email.threadId);
                        addText(metrics, "Email sender", email.sender);
                        if (email.timestamp > 0L) metrics.add("Email timestamp: " + email.timestamp);
                        if (!email.sender.isEmpty()) {
                            provenanceText = append(provenanceText,
                                    "sender=" + email.sender);
                        }
                        if (email.timestamp > 0L) {
                            provenanceText = append(provenanceText,
                                    "timestamp=" + email.timestamp);
                        }
                    }
                    if (!provenanceText.isEmpty()) {
                        metrics.add("Provenance: " + provenanceText);
                    }
                }
            }
            blocks.add(new ResultBlock(index + 1, path, type,
                    file.name == null ? "" : file.name, metrics));
        }
        return new Report(SearchExplanationHolder.getOriginalQuery(), safe.size(),
                images, emails, pdfs, documents, blocks);
    }

    private static void addCommonMetadata(List<String> metrics, FileEntity file) {
        metrics.add("Type: " + sourceType(file));
        addText(metrics, "Filename/title", file.name);
        addText(metrics, "Full path", file.path);
        metrics.add("Source/origin: " + ResultSourceLabel.from(file));
        String extension = extension(file.name, file.path);
        addText(metrics, "Extension", extension);
        if (file.fileSize > 0L) metrics.add("File size: " + file.fileSize + " bytes");
        if (file.lastModified > 0L) metrics.add("Last modified: " + file.lastModified);
        if (file.indexedAt > 0L) metrics.add("Indexed at: " + file.indexedAt);
        addText(metrics, "Index status", file.indexStatus);
        addText(metrics, "Index error", file.errorMessage);
        addText(metrics, "Scan session", file.scanSessionId);
        addText(metrics, "Image path", file.imagePath);
        if (file.embedding != null) metrics.add("Text embedding bytes: " + file.embedding.length);
        if (file.imageEmbedding != null) metrics.add("Image embedding bytes: " + file.imageEmbedding.length);
        addText(metrics, "Detected persons", file.detectedPersons);
        addText(metrics, "Detected organizations", file.detectedOrganizations);
        addText(metrics, "Detected document type", file.detectedDocumentType);
        addText(metrics, "Detected numbers", file.detectedNumbers);
    }

    private static void addQueryMetadata(List<String> metrics, String type) {
        SearchExplanationHolder.QueryMetadata query = SearchExplanationHolder.getQueryMetadata();
        metrics.add("Query original: " + SearchExplanationHolder.getOriginalQuery());
        addText(metrics, "Query normalized", query.normalizedQuery);
        addText(metrics, "Query primary pivot", query.translatedQuery);
        if (!query.pivots.isEmpty()) metrics.add("Query canonical pivots: " + query.pivots);
        addText(metrics, "Interpretation", query.interpretation);
        addText(metrics, "Detected query language", query.detectedLanguage);
        addText(metrics, "Working language", query.workingLanguage);
        addText(metrics, "Language family", query.languageFamily);
        addText(metrics, "Translation package", query.translationPackage);
        if ("IMAGE".equals(type)) {
            addText(metrics, "Embedding/model pipeline", query.imagePipeline);
            addText(metrics, "Model package", query.imagePackage);
        } else {
            addText(metrics, "Embedding/model pipeline", query.embeddingPipeline);
            addText(metrics, "Model package", query.embeddingPackage);
        }
    }

    private static void addSignal(List<String> metrics, String label,
                                  DocumentConfidence.NormalizedSignal signal) {
        if (signal != null && signal.isAvailable()) {
            metrics.add(label + ": " + number(signal.getValue()));
        }
    }

    private static void addText(List<String> metrics, String label, String value) {
        if (value != null && !value.trim().isEmpty()) metrics.add(label + ": " + value);
    }

    private static String extension(String name, String path) {
        String value = name == null || name.isEmpty() ? path : name;
        if (value == null) return "";
        int slash = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        int dot = value.lastIndexOf('.');
        return dot > slash && dot < value.length() - 1
                ? value.substring(dot + 1).toUpperCase(Locale.ROOT) : "";
    }

    private static String yesNo(boolean value) { return value ? "yes" : "no"; }

    static String sourceType(FileEntity file) {
        if (file == null || file.type == null || file.type.trim().isEmpty()) {
            return "DOCUMENT";
        }
        return file.type.trim().toUpperCase(Locale.ROOT);
    }

    private static void addFloat(List<String> output, String label, Float value) {
        if (value != null && Float.isFinite(value)) {
            output.add(label + ": " + number(value));
        }
    }

    private static String number(float value) {
        return String.format(Locale.US, "%.6f", value);
    }

    private static String provenance(DocumentEvidence.Provenance value) {
        if (value == null) return "";
        String result = "";
        if (value.isLexical()) result = append(result, "lexical");
        if (value.isCanonical()) result = append(result, "canonical");
        if (value.isOriginal()) result = append(result, "original");
        if (value.isTranslation()) result = append(result, "translation");
        if (value.isAlias()) result = append(result, "alias");
        return result;
    }

    private static String append(String current, String value) {
        return current.isEmpty() ? value : current + ", " + value;
    }

    public static final class Report {
        public final String query;
        public final int resultCount;
        public final int imageCount;
        public final int emailCount;
        public final int pdfCount;
        public final int documentCount;
        public final List<ResultBlock> results;

        Report(String query, int resultCount, int imageCount, int emailCount,
               int pdfCount, int documentCount, List<ResultBlock> results) {
            this.query = query;
            this.resultCount = resultCount;
            this.imageCount = imageCount;
            this.emailCount = emailCount;
            this.pdfCount = pdfCount;
            this.documentCount = documentCount;
            this.results = Collections.unmodifiableList(new ArrayList<>(results));
        }
    }

    public static final class ResultBlock {
        public final int rank;
        public final String path;
        public final String type;
        public final String title;
        public final List<String> metrics;

        ResultBlock(int rank, String path, String type, String title,
                    List<String> metrics) {
            this.rank = rank;
            this.path = path;
            this.type = type;
            this.title = title;
            this.metrics = Collections.unmodifiableList(new ArrayList<>(metrics));
        }
    }
}
