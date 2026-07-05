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

        QueryContext context =
                new QueryContext();

        if (query == null) {
            return context;
        }

        context.originalQuery =
                query;

        context.normalizedQuery =
                normalize(query);

        context.tokens =
                tokenize(
                        context.normalizedQuery
                );

        context.importantTokens =
                extractImportantTokens(
                        context.tokens
                );

        try {

            context.embedding =
                    EmbeddingEngine
                            .getInstance()
                            .generateEmbedding(
                                    context.normalizedQuery
                            );

        } catch (Exception e) {

            e.printStackTrace();

            context.embedding = null;
        }

        context.semanticTokens =
                buildSemanticTokens(
                        context
                );
        context.tokenWeights =
                buildTokenWeights(
                        context
                );
        context.documentIntent =
                detectDocumentIntent(
                        context
                );

        context.imageIntent =
                detectImageIntent(
                        context
                );

        context.personIntent =
                detectPersonIntent(
                        context
                );

        context.invoiceIntent =
                detectInvoiceIntent(
                        context
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
            QueryContext context
    ) {

        List<String> semanticTokens =
                new ArrayList<>();

        if (
                context.tokens == null
        ) {

            return semanticTokens;
        }

        HashSet<String> unique =
                new HashSet<>();

        for (String token : context.tokens) {

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
            QueryContext context
    ) {

        if (
                context.tokens == null
        ) {

            return false;
        }

        for (String token : context.tokens) {

            if (
                    token.length() >= 5
            ) {

                return true;
            }
        }

        return false;
    }

    private static boolean detectImageIntent(
            QueryContext context
    ) {

        if (
                context == null
                        ||
                        context.tokens == null
        ) {
            return false;
        }

        if (
                context.tokens.size() == 1
        ) {
            return true;
        }

        for (String token : context.tokens) {

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
            QueryContext context
    ) {

        if (
                context.tokens == null
        ) {

            return false;
        }

        for (String token : context.tokens) {

            if (
                    token.length() >= 4
            ) {

                return true;
            }
        }

        return false;
    }

    private static boolean detectInvoiceIntent(
            QueryContext context
    ) {

        if (
                context.tokens == null
        ) {

            return false;
        }

        for (String token : context.tokens) {

            if (
                    token.length() >= 5
            ) {

                return true;
            }
        }

        return false;
    }
    private static List<QueryTokenWeight> buildTokenWeights(
            QueryContext context
    ) {

        List<QueryTokenWeight> weights =
                new ArrayList<>();

        if (
                context == null
                        ||
                        context.tokens == null
        ) {

            return weights;
        }

        int tokenCount =
                context.tokens.size();

        for (
                int i = 0;
                i < tokenCount;
                i++
        ) {

            String token =
                    context.tokens.get(i);

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