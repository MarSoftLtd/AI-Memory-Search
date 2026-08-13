package com.bliss.aimemorysearch.ai.explanation;

/** Immutable query identity supplied to an explanation. */
public final class QuerySummary {
    private final String queryId;
    private final String normalizedQuery;
    private final boolean available;

    public QuerySummary(String queryId, String normalizedQuery, boolean available) {
        this.queryId = queryId;
        this.normalizedQuery = normalizedQuery;
        this.available = available;
    }

    public String getQueryId() { return queryId; }
    public String getNormalizedQuery() { return normalizedQuery; }
    public boolean isAvailable() { return available; }
}
