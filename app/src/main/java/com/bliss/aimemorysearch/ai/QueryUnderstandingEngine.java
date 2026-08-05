package com.bliss.aimemorysearch.ai;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class QueryUnderstandingEngine {

    private static final Set<String> DOCUMENT_INTENT_TOKENS =
            new HashSet<>(Arrays.asList(
                    "invoice", "factura", "facturi", "bill", "bills",
                    "receipt", "chitanta", "contract", "document", "documents",
                    "pdf", "report", "raport", "statement", "extras",
                    "form", "formular", "passport", "pasaport", "certificate",
                    "certificat", "license", "licence", "adeverinta"
            ));

    private static final Set<String> IMAGE_INTENT_TOKENS =
            new HashSet<>(Arrays.asList(
                    "photo", "photos", "photograph", "photographs",
                    "picture", "pictures", "image", "images", "poza", "poze", "imagine",
                    "fotografie", "fotografii"
            ));

    private static final Set<String> VISUAL_SUBJECT_TOKENS =
            new HashSet<>(Arrays.asList(
                    "cat", "cats", "pisica", "pisici", "dog", "dogs",
                    "caine", "caini", "rose", "roses", "trandafir",
                    "flower", "flowers", "floare"
            ));

    public static class QueryTokenWeight {

        public String token = "";

        public float weight = 1.0f;

        public String role = "NORMAL";

        public QueryTokenWeight(
                String token,
                float weight,
                String role
        ) {

            this.token = token;
            this.weight = weight;
            this.role = role;
        }
    }

    public static SearchRequest createSearchRequest(
            String query
    ) {

        SearchRequest request =
                new SearchRequest();

        if (query == null) {
            return request;
        }

        request.setOriginalQuery(
                query
        );

        request.setNormalizedQuery(
                QueryMorphologyNormalizer.normalize(
                        normalize(query)
                )
        );

        request.setQueryTokens(
                tokenize(
                        request.getNormalizedQuery()
                )
        );

        return request;
    }

    public static SearchAnalysis createSearchAnalysis(
            SearchRequest request
    ) {

        if (request == null) {
            request =
                    new SearchRequest();
        }

        float[] embedding;
        boolean accuracyDiagnostic =
                isAccuracyDiagnostic(request.getOriginalQuery());
        if (accuracyDiagnostic) {
            android.util.Log.i(
                    "ACCURACY_DIAG",
                    "embedding-input"
                            + " | query=" + request.getOriginalQuery()
                            + " | text=" + request.getNormalizedQuery()
            );
        }

        try {

            embedding =
                    DocumentRuntimeLoader
                            .createDefault()
                            .getEmbeddingRuntime()
                            .generateEmbedding(
                                    request.getNormalizedQuery()
                            );

        } catch (Exception e) {

            e.printStackTrace();

            embedding = null;
        }

        List<String> semanticTokens =
                buildSemanticTokens(
                        request.getQueryTokens()
                );

        List<QueryTokenWeight> tokenWeights =
                buildTokenWeights(
                        request.getQueryTokens()
                );

        boolean documentIntent =
                detectDocumentIntent(
                        request.getQueryTokens()
                );

        boolean imageIntent =
                detectImageIntent(
                        request.getQueryTokens()
                );

        boolean personIntent =
                detectPersonIntent(
                        request.getQueryTokens()
                );

        boolean invoiceIntent =
                detectInvoiceIntent(
                        request.getQueryTokens()
                );

        return new SearchAnalysis(
                semanticTokens,
                tokenWeights,
                embedding,
                documentIntent,
                imageIntent,
                personIntent,
                invoiceIntent
        );
    }

    private static boolean isAccuracyDiagnostic(String query) {
        if (query == null) {
            return false;
        }
        String normalized = normalize(query);
        return "factura".equals(normalized)
                || "factura apa".equals(normalized)
                || "invoice".equals(normalized);
    }

    private static String normalize(
            String input
    ) {

        if (input == null) {
            return "";
        }

        String value =
                input
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .trim();

        value =
                Normalizer.normalize(
                        value,
                        Normalizer.Form.NFD
                );

        value =
                value.replaceAll(
                        "\\p{InCombiningDiacriticalMarks}+",
                        ""
                );

        value =
                value.replaceAll(
                        "[^\\p{L}\\p{N}]",
                        " "
                );

        value =
                value.replaceAll(
                        "\\s+",
                        " "
                );

        return value.trim();
    }

    private static List<String> tokenize(
            String normalized
    ) {

        List<String> tokens =
                new ArrayList<>();

        if (
                normalized == null
                        ||
                        normalized.isEmpty()
        ) {

            return tokens;
        }

        String[] split =
                normalized.split("\\s+");

        for (String part : split) {

            if (part == null) {
                continue;
            }

            String token =
                    part.trim();

            if (
                    token.length() >= 2
            ) {

                tokens.add(token);
            }
        }

        return tokens;
    }

    private static List<String> buildSemanticTokens(
            List<String> tokens
    ) {

        List<String> semanticTokens =
                new ArrayList<>();

        if (
                tokens == null
        ) {

            return semanticTokens;
        }

        HashSet<String> unique =
                new HashSet<>();

        for (String token : tokens) {

            if (
                    token == null
                            ||
                            token.length() < 2
            ) {

                continue;
            }

            unique.add(token);

            if (token.length() >= 3) {

                List<String> expanded =
                        PrefixVocabularyCache
                                .expandPrefix(token);

                if (expanded != null) {

                    int count = 0;

                    for (String value : expanded) {

                        unique.add(value);

                        count++;

                        if (count >= 5) {
                            break;
                        }
                    }
                }
            }
        }

        semanticTokens.addAll(unique);

        return semanticTokens;
    }

    static boolean detectDocumentIntent(
            List<String> tokens
    ) {

        if (
                tokens == null
                        || tokens.isEmpty()
        ) {

            return false;
        }

        if (containsAny(tokens, DOCUMENT_INTENT_TOKENS)) {
            return true;
        }

        return !containsAny(tokens, IMAGE_INTENT_TOKENS)
                && !containsAny(tokens, VISUAL_SUBJECT_TOKENS);
    }

    static boolean detectImageIntent(
            List<String> tokens
    ) {

        if (
                tokens == null
                        || tokens.isEmpty()
        ) {
            return false;
        }

        if (containsAny(tokens, IMAGE_INTENT_TOKENS)
                || containsAny(tokens, VISUAL_SUBJECT_TOKENS)) {
            return true;
        }

        return !containsAny(tokens, DOCUMENT_INTENT_TOKENS);
    }

    private static boolean containsAny(
            List<String> tokens,
            Set<String> intentTokens
    ) {
        for (String token : tokens) {
            if (token != null && intentTokens.contains(token)) {
                return true;
            }
        }

        return false;
    }

    private static boolean detectPersonIntent(
            List<String> tokens
    ) {

        if (
                tokens == null
        ) {

            return false;
        }

        for (String token : tokens) {

            if (
                    token.length() >= 4
            ) {

                return true;
            }
        }

        return false;
    }

    private static boolean detectInvoiceIntent(
            List<String> tokens
    ) {

        if (
                tokens == null
        ) {

            return false;
        }

        for (String token : tokens) {

            if (
                    token.length() >= 5
            ) {

                return true;
            }
        }

        return false;
    }
    private static List<QueryTokenWeight> buildTokenWeights(
            List<String> tokens
    ) {

        List<QueryTokenWeight> weights =
                new ArrayList<>();

        if (
                tokens == null
        ) {

            return weights;
        }

        int tokenCount =
                tokens.size();

        for (
                int i = 0;
                i < tokenCount;
                i++
        ) {

            String token =
                    tokens.get(i);

            if (
                    token == null
                            ||
                            token.trim().isEmpty()
            ) {

                continue;
            }

            token =
                    token.trim()
                            .toLowerCase(
                                    Locale.ROOT
                            );

            float weight = 1.0f;

            String role = "NORMAL";

            if (
                    token.length() <= 2
            ) {

                weight = 0.80f;
                role = "SHORT";
            }

            if (
                    token.length() >= 4
            ) {

                weight = 1.40f;
                role = "SPECIFIC";
            }

            if (
                    i == tokenCount - 1
                            &&
                            tokenCount >= 2
            ) {

                weight += 1.20f;
                role = "DISCRIMINATOR";
            }

            if (
                    token.matches(".*\\d.*")
            ) {

                weight += 2.00f;
                role = "IDENTIFIER";
            }

            weights.add(
                    new QueryTokenWeight(
                            token,
                            weight,
                            role
                    )
            );
        }

        return weights;
    }
}
