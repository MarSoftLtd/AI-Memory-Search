package com.bliss.aimemorysearch.ai.concepts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable structural interpretation hypothesis. */
public final class Interpretation {

    private final String id;
    private final Kind kind;
    private final List<ConceptGraph.Concept> components;

    Interpretation(
            String id,
            Kind kind,
            List<ConceptGraph.Concept> components
    ) {
        this.id = id;
        this.kind = kind;
        this.components = Collections.unmodifiableList(
                new ArrayList<>(components)
        );
    }

    public String getId() { return id; }
    public Kind getKind() { return kind; }
    public List<ConceptGraph.Concept> getComponents() { return components; }

    public enum Kind {
        WHOLE_QUERY,
        PHRASE_GROUP,
        ATOMIC_CONJUNCTION
    }
}
