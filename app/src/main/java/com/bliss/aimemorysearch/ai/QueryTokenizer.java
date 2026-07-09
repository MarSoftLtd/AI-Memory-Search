package com.bliss.aimemorysearch.ai;

public interface QueryTokenizer {

    void tokenize(
            SearchRequest searchRequest
    );
}
