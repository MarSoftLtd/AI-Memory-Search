package com.bliss.aimemorysearch.ai;

import java.util.Objects;

public final class SearchRequestQueryContextAdapter {

    public QueryUnderstandingEngine.QueryContext adapt(
            SearchRequest request,
            SearchAnalysis analysis
    ) {
        Objects.requireNonNull(
                request,
                "request"
        );
        Objects.requireNonNull(
                analysis,
                "analysis"
        );

        QueryUnderstandingEngine.QueryContext queryContext =
                new QueryUnderstandingEngine.QueryContext();

        queryContext.originalQuery =
                request.getOriginalQuery();
        queryContext.normalizedQuery =
                request.getNormalizedQuery();
        queryContext.tokens =
                request.getQueryTokens();
        queryContext.semanticTokens =
                analysis.getSemanticTokens();
        queryContext.tokenWeights =
                analysis.getTokenWeights();
        queryContext.embedding =
                analysis.getEmbedding();
        queryContext.documentIntent =
                analysis.isDocumentIntent();
        queryContext.imageIntent =
                analysis.isImageIntent();
        queryContext.personIntent =
                analysis.isPersonIntent();
        queryContext.invoiceIntent =
                analysis.isInvoiceIntent();

        return queryContext;
    }
}
