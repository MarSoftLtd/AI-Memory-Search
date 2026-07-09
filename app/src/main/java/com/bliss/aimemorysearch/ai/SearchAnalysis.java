package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SearchAnalysis {

    private final List<String> semanticTokens;
    private final List<QueryUnderstandingEngine.QueryTokenWeight> tokenWeights;
    private final float[] embedding;
    private final boolean documentIntent;
    private final boolean imageIntent;
    private final boolean personIntent;
    private final boolean invoiceIntent;

    public SearchAnalysis(
            List<String> semanticTokens,
            List<QueryUnderstandingEngine.QueryTokenWeight> tokenWeights,
            float[] embedding,
            boolean documentIntent,
            boolean imageIntent,
            boolean personIntent,
            boolean invoiceIntent
    ) {
        this.semanticTokens =
                immutableStringList(
                        semanticTokens
                );
        this.tokenWeights =
                immutableTokenWeightList(
                        tokenWeights
                );
        this.embedding =
                embedding == null
                        ? null
                        : embedding.clone();
        this.documentIntent =
                documentIntent;
        this.imageIntent =
                imageIntent;
        this.personIntent =
                personIntent;
        this.invoiceIntent =
                invoiceIntent;
    }

    public List<String> getSemanticTokens() {
        return semanticTokens;
    }

    public List<QueryUnderstandingEngine.QueryTokenWeight> getTokenWeights() {
        return tokenWeights;
    }

    public float[] getEmbedding() {
        return embedding == null
                ? null
                : embedding.clone();
    }

    public boolean isDocumentIntent() {
        return documentIntent;
    }

    public boolean isImageIntent() {
        return imageIntent;
    }

    public boolean isPersonIntent() {
        return personIntent;
    }

    public boolean isInvoiceIntent() {
        return invoiceIntent;
    }

    private static List<String> immutableStringList(
            List<String> values
    ) {
        if (values == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(
                new ArrayList<>(
                        values
                )
        );
    }

    private static List<QueryUnderstandingEngine.QueryTokenWeight> immutableTokenWeightList(
            List<QueryUnderstandingEngine.QueryTokenWeight> values
    ) {
        if (values == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(
                new ArrayList<>(
                        values
                )
        );
    }
}
