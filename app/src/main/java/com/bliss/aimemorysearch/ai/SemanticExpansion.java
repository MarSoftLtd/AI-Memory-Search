package com.bliss.aimemorysearch.ai;

public final class SemanticExpansion {

    private final SemanticConcept concept;
    private final SemanticExpansionOrigin origin;
    private final int depth;

    public SemanticExpansion(
            SemanticConcept concept,
            SemanticExpansionOrigin origin,
            int depth
    ) {
        this.concept =
                concept;
        this.origin =
                origin;
        this.depth =
                depth;
    }

    public SemanticConcept getConcept() {
        return concept;
    }

    public SemanticExpansionOrigin getOrigin() {
        return origin;
    }

    public int getDepth() {
        return depth;
    }
}
