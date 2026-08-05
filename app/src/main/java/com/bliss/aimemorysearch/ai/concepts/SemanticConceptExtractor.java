package com.bliss.aimemorysearch.ai.concepts;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Bounded structural concept extraction with no language-specific knowledge. */
public final class SemanticConceptExtractor {

    private static final int MAX_TOKENS = 12;
    private static final int MAX_LOCAL_PHRASE_TOKENS = 3;
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "[\\p{L}\\p{N}][\\p{L}\\p{N}\\p{M}_'’-]*"
    );

    private SemanticConceptExtractor() {}

    public static ConceptGraph extract(String normalizedQuery) {
        String query = normalize(normalizedQuery);
        List<String> tokens = tokenize(query);
        List<ConceptGraph.Concept> concepts = new ArrayList<>();
        List<ConceptGraph.Edge> edges = new ArrayList<>();

        if (tokens.isEmpty()) {
            return new ConceptGraph(query, tokens, concepts, edges);
        }

        int tokenCount = tokens.size();
        String primaryId = "c0";
        concepts.add(new ConceptGraph.Concept(
                primaryId,
                join(tokens, 0, tokenCount),
                0,
                tokenCount,
                tokenCount == 1
                        ? ConceptGraph.ConceptKind.ATOMIC
                        : ConceptGraph.ConceptKind.PHRASE,
                true
        ));

        int nextId = 1;
        for (int length = Math.min(MAX_LOCAL_PHRASE_TOKENS, tokenCount - 1);
             length >= 2;
             length--) {
            for (int start = 0; start + length <= tokenCount; start++) {
                concepts.add(new ConceptGraph.Concept(
                        "c" + nextId++,
                        join(tokens, start, start + length),
                        start,
                        start + length,
                        ConceptGraph.ConceptKind.PHRASE,
                        false
                ));
            }
        }

        int atomicStartIndex = concepts.size();
        for (int index = 0; index < tokenCount; index++) {
            concepts.add(new ConceptGraph.Concept(
                    "c" + nextId++,
                    tokens.get(index),
                    index,
                    index + 1,
                    ConceptGraph.ConceptKind.ATOMIC,
                    tokenCount == 1
            ));
        }

        for (int index = 1; index < concepts.size(); index++) {
            edges.add(new ConceptGraph.Edge(
                    primaryId,
                    concepts.get(index).getId(),
                    ConceptGraph.EdgeKind.CONTAINS
            ));
        }
        for (int phraseIndex = 1;
             phraseIndex < atomicStartIndex;
             phraseIndex++) {
            ConceptGraph.Concept phrase = concepts.get(phraseIndex);
            for (int atomicIndex = atomicStartIndex;
                 atomicIndex < concepts.size();
                 atomicIndex++) {
                ConceptGraph.Concept atomic = concepts.get(atomicIndex);
                if (atomic.getTokenStart() >= phrase.getTokenStart()
                        && atomic.getTokenEnd() <= phrase.getTokenEnd()) {
                    edges.add(new ConceptGraph.Edge(
                            phrase.getId(),
                            atomic.getId(),
                            ConceptGraph.EdgeKind.CONTAINS
                    ));
                }
            }
        }
        for (int index = atomicStartIndex;
             index + 1 < concepts.size();
             index++) {
            edges.add(new ConceptGraph.Edge(
                    concepts.get(index).getId(),
                    concepts.get(index + 1).getId(),
                    ConceptGraph.EdgeKind.ADJACENT
            ));
        }

        return new ConceptGraph(query, tokens, concepts, edges);
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static List<String> tokenize(String query) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(query);
        while (matcher.find() && tokens.size() < MAX_TOKENS) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    private static String join(List<String> tokens, int start, int end) {
        StringBuilder output = new StringBuilder();
        for (int index = start; index < end; index++) {
            if (output.length() > 0) output.append(' ');
            output.append(tokens.get(index));
        }
        return output.toString();
    }
}
