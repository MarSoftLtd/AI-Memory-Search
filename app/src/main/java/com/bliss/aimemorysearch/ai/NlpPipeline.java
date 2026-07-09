package com.bliss.aimemorysearch.ai;

import android.content.Context;

import java.util.Collections;

public final class NlpPipeline {

    private final QueryNormalizer queryNormalizer;
    private final QueryTokenizer queryTokenizer;
    private final StopWordFilter stopWordFilter;
    private final ConceptExtractor conceptExtractor;

    public NlpPipeline() {
        this(
                new DefaultQueryNormalizer(),
                new DefaultQueryTokenizer(),
                new DefaultStopWordFilter(
                        languageCode -> Collections.emptySet()
                ),
                new DefaultConceptExtractor()
        );
    }

    public NlpPipeline(
            Context context
    ) {
        this(
                new DefaultQueryNormalizer(),
                new DefaultQueryTokenizer(),
                new DefaultStopWordFilter(
                        new AssetsStopWordRepository(
                                context
                        )
                ),
                new DefaultConceptExtractor()
        );
    }

    public NlpPipeline(
            QueryNormalizer queryNormalizer,
            QueryTokenizer queryTokenizer,
            StopWordFilter stopWordFilter,
            ConceptExtractor conceptExtractor
    ) {
        this.queryNormalizer =
                queryNormalizer;
        this.queryTokenizer =
                queryTokenizer;
        this.stopWordFilter =
                stopWordFilter;
        this.conceptExtractor =
                conceptExtractor;
    }

    public SearchRequest process(
            String query
    ) {
        SearchRequest searchRequest =
                new SearchRequest();

        searchRequest.setOriginalQuery(
                query
        );

        queryNormalizer.normalize(
                searchRequest
        );

        queryTokenizer.tokenize(
                searchRequest
        );

        stopWordFilter.filter(
                searchRequest
        );

        conceptExtractor.extract(
                searchRequest
        );

        return searchRequest;
    }
}
