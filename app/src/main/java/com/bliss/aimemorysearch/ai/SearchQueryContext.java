package com.bliss.aimemorysearch.ai;

import java.util.List;

public class SearchQueryContext {

    public String originalQuery;

    public String normalizedQuery;

    public List<String> tokens;

    public List<String> semanticTerms;

    public List<String> importantTokens;

    public float[] embedding;

    public boolean documentIntent;

    public boolean imageIntent;

    public boolean personIntent;

    public boolean invoiceIntent;
}