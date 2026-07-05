package com.bliss.aimemorysearch;

import java.util.HashMap;
import java.util.Map;

public class SearchExplanationHolder {

    public static final Map<String, String> snippets =
            new HashMap<>();

    public static final Map<String, Float> scores =
            new HashMap<>();

    public static void clear() {

        snippets.clear();
        scores.clear();
    }
}