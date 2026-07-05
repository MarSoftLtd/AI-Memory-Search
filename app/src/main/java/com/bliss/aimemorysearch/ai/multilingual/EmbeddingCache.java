package com.bliss.aimemorysearch.ai.multilingual;

import java.util.LinkedHashMap;
import java.util.Map;

public class EmbeddingCache {

    private static final int MAX_CACHE_SIZE = 500;

    private final LinkedHashMap<String, float[]> cache =
            new LinkedHashMap<String, float[]>(16, 0.75f, true) {

                @Override
                protected boolean removeEldestEntry(Map.Entry<String, float[]> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            };

    public synchronized float[] get(String text) {
        return cache.get(text);
    }

    public synchronized void put(String text, float[] embedding) {
        cache.put(text, embedding);
    }

    public synchronized void clear() {
        cache.clear();
    }
}