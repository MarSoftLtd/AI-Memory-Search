package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SearchRequest {

    private String originalQuery;
    private String normalizedQuery;
    private String translatedQuery;
    private String detectedLanguage;
    private String workingLanguage;
    private List<String> tokens;
    private List<String> concepts;
    private Set<SearchTarget> searchTargets;

    public SearchRequest() {
        originalQuery =
                "";
        normalizedQuery =
                "";
        translatedQuery =
                "";
        detectedLanguage =
                "";
        workingLanguage =
                "";
        tokens =
                new ArrayList<>();
        concepts =
                new ArrayList<>();
        searchTargets =
                EnumSet.noneOf(
                        SearchTarget.class
                );
    }

    public String getOriginalQuery() {
        return originalQuery;
    }

    public void setOriginalQuery(
            String originalQuery
    ) {
        this.originalQuery =
                nonNullString(
                        originalQuery
                );
    }

    public String getNormalizedQuery() {
        return normalizedQuery;
    }

    public void setNormalizedQuery(
            String normalizedQuery
    ) {
        this.normalizedQuery =
                nonNullString(
                        normalizedQuery
                );
    }

    public String getTranslatedQuery() {
        return translatedQuery;
    }

    public void setTranslatedQuery(
            String translatedQuery
    ) {
        this.translatedQuery =
                nonNullString(
                        translatedQuery
                );
    }

    public String getDetectedLanguage() {
        return detectedLanguage;
    }

    public void setDetectedLanguage(
            String detectedLanguage
    ) {
        this.detectedLanguage =
                nonNullString(
                        detectedLanguage
                );
    }

    public String getWorkingLanguage() {
        return workingLanguage;
    }

    public void setWorkingLanguage(
            String workingLanguage
    ) {
        this.workingLanguage =
                nonNullString(
                        workingLanguage
                );
    }

    public List<String> getTokens() {
        return tokens;
    }

    public void setTokens(
            List<String> tokens
    ) {
        this.tokens =
                nonNullList(
                        tokens
                );
    }

    public List<String> getConcepts() {
        return concepts;
    }

    public void setConcepts(
            List<String> concepts
    ) {
        this.concepts =
                nonNullList(
                        concepts
                );
    }

    public Set<SearchTarget> getSearchTargets() {
        return searchTargets;
    }

    public void setSearchTargets(
            Set<SearchTarget> searchTargets
    ) {
        this.searchTargets =
                nonNullSearchTargets(
                        searchTargets
                );
    }

    private static String nonNullString(
            String value
    ) {
        return value != null
                ? value
                : "";
    }

    private static List<String> nonNullList(
            List<String> values
    ) {
        if (values == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(
                values
        );
    }

    private static Set<SearchTarget> nonNullSearchTargets(
            Set<SearchTarget> values
    ) {
        if (values == null) {
            return EnumSet.noneOf(
                    SearchTarget.class
            );
        }

        return new HashSet<>(
                values
        );
    }
}
