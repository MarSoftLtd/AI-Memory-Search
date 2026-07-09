package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class DefaultStopWordFilter implements StopWordFilter {

    private final StopWordRepository stopWordRepository;

    public DefaultStopWordFilter(
            StopWordRepository stopWordRepository
    ) {
        this.stopWordRepository =
                stopWordRepository;
    }

    @Override
    public void filter(
            SearchRequest searchRequest
    ) {
        if (searchRequest == null) {
            return;
        }

        Set<String> stopWords =
                getStopWords(
                        searchRequest
                );

        if (stopWords.isEmpty()) {
            return;
        }

        List<String> filteredTokens =
                new ArrayList<>();

        for (String token : searchRequest.getTokens()) {
            if (!stopWords.contains(token)) {
                filteredTokens.add(
                        token
                );
            }
        }

        searchRequest.setTokens(
                filteredTokens
        );
    }

    private Set<String> getStopWords(
            SearchRequest searchRequest
    ) {
        if (stopWordRepository == null) {
            return Collections.emptySet();
        }

        String languageCode =
                searchRequest.getWorkingLanguage();

        if (languageCode == null || languageCode.trim().isEmpty()) {
            languageCode =
                    searchRequest.getDetectedLanguage();
        }

        return stopWordRepository.getStopWords(
                languageCode
        );
    }
}
