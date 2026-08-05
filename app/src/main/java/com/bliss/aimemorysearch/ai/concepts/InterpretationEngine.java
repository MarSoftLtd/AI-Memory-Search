package com.bliss.aimemorysearch.ai.concepts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Builds bounded structural hypotheses without assigning semantic meaning. */
public final class InterpretationEngine {

    public static final int MAX_HYPOTHESES = 8;
    private static final int MAX_PHRASE_WIDTH = 3;

    private InterpretationEngine() {}

    public static List<Interpretation> generate(ConceptGraph graph) {
        if (graph == null || graph.getConcepts().isEmpty()) {
            return Collections.emptyList();
        }

        List<Interpretation> interpretations = new ArrayList<>(
                MAX_HYPOTHESES
        );
        List<ConceptGraph.Concept> atomicConcepts = atomicConcepts(graph);
        ConceptGraph.Concept primary = primaryConcept(graph);

        if (primary != null) {
            interpretations.add(new Interpretation(
                    "i0",
                    Interpretation.Kind.WHOLE_QUERY,
                    Collections.singletonList(primary)
            ));
        }

        Set<String> phraseGroupSignatures = new LinkedHashSet<>();
        for (int width = MAX_PHRASE_WIDTH;
             width >= 2 && interpretations.size() < MAX_HYPOTHESES - 1;
             width--) {
            for (int phase = 0;
                 phase < width
                         && interpretations.size() < MAX_HYPOTHESES - 1;
                 phase++) {
                List<ConceptGraph.Concept> components = new ArrayList<>(
                        atomicConcepts.size()
                );
                boolean containsPhrase = buildPhraseGrouping(
                        graph,
                        atomicConcepts,
                        width,
                        phase,
                        components
                );
                String signature = signature(components);
                if (containsPhrase && phraseGroupSignatures.add(signature)) {
                    interpretations.add(new Interpretation(
                            "i" + interpretations.size(),
                            Interpretation.Kind.PHRASE_GROUP,
                            components
                    ));
                }
            }
        }

        interpretations.add(new Interpretation(
                "i" + interpretations.size(),
                Interpretation.Kind.ATOMIC_CONJUNCTION,
                atomicConcepts
        ));
        return Collections.unmodifiableList(interpretations);
    }

    public static String toDiagnosticString(
            List<Interpretation> interpretations
    ) {
        StringBuilder output = new StringBuilder("interpretations=[");
        for (int index = 0; index < interpretations.size(); index++) {
            if (index > 0) output.append(',');
            Interpretation interpretation = interpretations.get(index);
            output.append(interpretation.getId())
                    .append('{').append(interpretation.getKind())
                    .append(":");
            List<ConceptGraph.Concept> components =
                    interpretation.getComponents();
            for (int componentIndex = 0;
                 componentIndex < components.size();
                 componentIndex++) {
                if (componentIndex > 0) output.append('+');
                ConceptGraph.Concept component = components.get(componentIndex);
                output.append(component.getId())
                        .append("=\"").append(component.getText()).append('"');
            }
            output.append('}');
        }
        return output.append(']').toString();
    }

    private static boolean buildPhraseGrouping(
            ConceptGraph graph,
            List<ConceptGraph.Concept> atomicConcepts,
            int width,
            int phase,
            List<ConceptGraph.Concept> output
    ) {
        boolean containsPhrase = false;
        int tokenIndex = 0;
        while (tokenIndex < atomicConcepts.size()) {
            ConceptGraph.Concept phrase = null;
            if (tokenIndex >= phase
                    && (tokenIndex - phase) % width == 0) {
                phrase = findPhrase(graph, tokenIndex, tokenIndex + width);
            }
            if (phrase != null) {
                output.add(phrase);
                tokenIndex += width;
                containsPhrase = true;
            } else {
                output.add(atomicConcepts.get(tokenIndex));
                tokenIndex++;
            }
        }
        return containsPhrase;
    }

    private static ConceptGraph.Concept findPhrase(
            ConceptGraph graph,
            int start,
            int end
    ) {
        for (ConceptGraph.Concept concept : graph.getConcepts()) {
            if (!concept.isPrimary()
                    && concept.getKind() == ConceptGraph.ConceptKind.PHRASE
                    && concept.getTokenStart() == start
                    && concept.getTokenEnd() == end) {
                return concept;
            }
        }
        return null;
    }

    private static ConceptGraph.Concept primaryConcept(ConceptGraph graph) {
        for (ConceptGraph.Concept concept : graph.getConcepts()) {
            if (concept.isPrimary()) return concept;
        }
        return null;
    }

    private static List<ConceptGraph.Concept> atomicConcepts(
            ConceptGraph graph
    ) {
        List<ConceptGraph.Concept> atomic = new ArrayList<>(
                graph.getTokens().size()
        );
        for (ConceptGraph.Concept concept : graph.getConcepts()) {
            if (concept.getKind() == ConceptGraph.ConceptKind.ATOMIC
                    && !concept.isPrimary()) {
                atomic.add(concept);
            }
        }
        if (atomic.isEmpty()) {
            ConceptGraph.Concept primary = primaryConcept(graph);
            if (primary != null
                    && primary.getKind() == ConceptGraph.ConceptKind.ATOMIC) {
                atomic.add(primary);
            }
        }
        return atomic;
    }

    private static String signature(List<ConceptGraph.Concept> concepts) {
        StringBuilder signature = new StringBuilder();
        for (ConceptGraph.Concept concept : concepts) {
            if (signature.length() > 0) signature.append('|');
            signature.append(concept.getId());
        }
        return signature.toString();
    }
}
