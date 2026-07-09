package com.bliss.aimemorysearch.ai;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class QueryUnderstandingEngine {

    public static class QueryContext {

        public String originalQuery = "";

        public String normalizedQuery = "";

        public List<String> tokens =
                new ArrayList<>();

        public List<String> importantTokens =
                new ArrayList<>();

        public List<String> semanticTokens =
                new ArrayList<>();
        public List<QueryTokenWeight> tokenWeights =
                new ArrayList<>();
        public float[] embedding;

        public boolean documentIntent;

        public boolean imageIntent;

        public boolean personIntent;

        public boolean invoiceIntent;
    }
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

    public static QueryContext analyze(
            String query
    ) {

        if (query == null) {
            return new QueryContext();
        }

        SearchRequest request =
                createSearchRequest(
                        query
                );

        SearchAnalysis analysis =
                createSearchAnalysis(
                        request
                );

        QueryContext context =
                new SearchRequestQueryContextAdapter()
                        .adapt(
                                request,
                                analysis
                        );

        context.importantTokens =
                extractImportantTokens(
                        request.getQueryTokens()
                );

        android.util.Log.e(
                "QUERY_DEBUG",
                "QUERY=" + query
                        + " | TOKENS=" + context.tokens
                        + " | DOCUMENT=" + context.documentIntent
                        + " | IMAGE=" + context.imageIntent
                        + " | PERSON=" + context.personIntent
                        + " | INVOICE=" + context.invoiceIntent
        );
        return context;
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
                normalize(query)
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

        try {

            embedding =
                    EmbeddingEngine
                            .getInstance()
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

    private static List<String> extractImportantTokens(
            List<String> tokens
    ) {

        List<String> result =
                new ArrayList<>();

        if (tokens == null) {
            return result;
        }

        for (String token : tokens) {

            if (
                    token == null
            ) {
                continue;
            }

            if (
                    token.length() >= 3
            ) {

                result.add(token);
            }
        }

        return result;
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

    private static boolean detectDocumentIntent(
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

    private static boolean detectImageIntent(
            List<String> tokens
    ) {

        if (
                tokens == null
        ) {
            return false;
        }

        if (
                tokens.size() == 1
        ) {
            return true;
        }

        for (String token : tokens) {

            if (
                    token.equals("photo")
                            ||
                            token.equals("picture")
                            ||
                            token.equals("image")
                            ||
                            token.equals("poza")
                            ||
                            token.equals("imagine")
            ) {

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
