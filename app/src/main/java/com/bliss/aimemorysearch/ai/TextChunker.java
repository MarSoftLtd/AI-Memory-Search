package com.bliss.aimemorysearch.ai;

import java.util.ArrayList;
import java.util.List;

public class TextChunker {

    private static final int CHUNK_SIZE = 800;

    public static List<String> chunkText(
            String text
    ) {

        List<String> chunks =
                new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return chunks;
        }

        text = text.trim();

        int start = 0;

        while (start < text.length()) {

            int end =
                    Math.min(
                            start + CHUNK_SIZE,
                            text.length()
                    );

            String chunk =
                    text.substring(
                            start,
                            end
                    ).trim();

            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            start = end;
        }

        return chunks;
    }
}