package com.bliss.aimemorysearch.ai.concept;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueryTokens {

    private final List<String> tokens =
            new ArrayList<>();

    public void add(String token) {

        if (token == null) {
            return;
        }

        token = token.trim();

        if (token.isEmpty()) {
            return;
        }

        tokens.add(token);
    }

    public List<String> getTokens() {
        return Collections.unmodifiableList(tokens);
    }

    public boolean isEmpty() {
        return tokens.isEmpty();
    }

    public int size() {
        return tokens.size();
    }

    @Override
    public String toString() {
        return tokens.toString();
    }
}