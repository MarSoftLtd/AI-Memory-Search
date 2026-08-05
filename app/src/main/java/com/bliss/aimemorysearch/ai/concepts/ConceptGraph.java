package com.bliss.aimemorysearch.ai.concepts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable, language-independent shadow representation of query spans. */
public final class ConceptGraph {

    private final String normalizedQuery;
    private final List<String> tokens;
    private final List<Concept> concepts;
    private final List<Edge> edges;
    private final List<SemanticAssertion> semanticAssertions;

    ConceptGraph(
            String normalizedQuery,
            List<String> tokens,
            List<Concept> concepts,
            List<Edge> edges
    ) {
        this(
                normalizedQuery,
                tokens,
                concepts,
                edges,
                Collections.emptyList()
        );
    }

    ConceptGraph(
            String normalizedQuery,
            List<String> tokens,
            List<Concept> concepts,
            List<Edge> edges,
            List<SemanticAssertion> semanticAssertions
    ) {
        this.normalizedQuery = normalizedQuery;
        this.tokens = immutableCopy(tokens);
        this.concepts = immutableCopy(concepts);
        this.edges = immutableCopy(edges);
        this.semanticAssertions = immutableCopy(semanticAssertions);
    }

    public String getNormalizedQuery() { return normalizedQuery; }
    public List<String> getTokens() { return tokens; }
    public List<Concept> getConcepts() { return concepts; }
    public List<Edge> getEdges() { return edges; }
    public List<SemanticAssertion> getSemanticAssertions() {
        return semanticAssertions;
    }

    public String toDiagnosticString() {
        StringBuilder output = new StringBuilder();
        output.append("query=\"").append(normalizedQuery).append("\" concepts=[");
        for (int index = 0; index < concepts.size(); index++) {
            if (index > 0) output.append(',');
            Concept concept = concepts.get(index);
            output.append(concept.id)
                    .append('{').append(concept.kind)
                    .append(concept.primary ? ",primary" : "")
                    .append(',').append(concept.tokenStart)
                    .append(':').append(concept.tokenEnd)
                    .append(",\"").append(concept.text).append("\"}");
        }
        output.append("] edges=[");
        for (int index = 0; index < edges.size(); index++) {
            if (index > 0) output.append(',');
            Edge edge = edges.get(index);
            output.append(edge.kind).append('(')
                    .append(edge.fromId).append(',')
                    .append(edge.toId).append(')');
        }
        return output.append(']').toString();
    }

    private static <T> List<T> immutableCopy(List<T> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    public enum ConceptKind {
        PHRASE,
        ATOMIC
    }

    public enum EdgeKind {
        CONTAINS,
        ADJACENT
    }

    public static final class Concept {
        private final String id;
        private final String text;
        private final int tokenStart;
        private final int tokenEnd;
        private final ConceptKind kind;
        private final boolean primary;

        Concept(
                String id,
                String text,
                int tokenStart,
                int tokenEnd,
                ConceptKind kind,
                boolean primary
        ) {
            this.id = id;
            this.text = text;
            this.tokenStart = tokenStart;
            this.tokenEnd = tokenEnd;
            this.kind = kind;
            this.primary = primary;
        }

        public String getId() { return id; }
        public String getText() { return text; }
        public int getTokenStart() { return tokenStart; }
        public int getTokenEnd() { return tokenEnd; }
        public ConceptKind getKind() { return kind; }
        public boolean isPrimary() { return primary; }
    }

    public static final class Edge {
        private final String fromId;
        private final String toId;
        private final EdgeKind kind;

        Edge(String fromId, String toId, EdgeKind kind) {
            this.fromId = fromId;
            this.toId = toId;
            this.kind = kind;
        }

        public String getFromId() { return fromId; }
        public String getToId() { return toId; }
        public EdgeKind getKind() { return kind; }
    }
}
