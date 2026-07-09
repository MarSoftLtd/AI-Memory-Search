package com.bliss.aimemorysearch.ai;

public interface StopWordFilter {

    void filter(
            SearchRequest searchRequest
    );
}
