package com.bliss.aimemorysearch.ai.concepts;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable query context passed to interpretation retrieval strategies. */
public final class RetrievalContext {

    private final Context applicationContext;
    private final String normalizedQuery;
    private final List<String> canonicalQueries;

    public RetrievalContext(
            Context context,
            String normalizedQuery,
            List<String> canonicalQueries
    ) {
        this.applicationContext = context == null
                ? null
                : context.getApplicationContext();
        this.normalizedQuery = normalizedQuery == null ? "" : normalizedQuery;
        this.canonicalQueries = Collections.unmodifiableList(
                new ArrayList<>(canonicalQueries == null
                        ? Collections.emptyList()
                        : canonicalQueries)
        );
    }

    public Context getApplicationContext() { return applicationContext; }
    public String getNormalizedQuery() { return normalizedQuery; }
    public List<String> getCanonicalQueries() { return canonicalQueries; }
}
