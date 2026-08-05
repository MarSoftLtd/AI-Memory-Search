package com.bliss.aimemorysearch.ai.concepts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class InterpretationEngineTest {

    @Test
    public void generatesRequiredStructuralInterpretations() {
        ConceptGraph graph = SemanticConceptExtractor.extract(
                "poza cu pisica alba langa flori"
        );

        List<Interpretation> interpretations =
                InterpretationEngine.generate(graph);

        assertEquals(
                Interpretation.Kind.WHOLE_QUERY,
                interpretations.get(0).getKind()
        );
        assertEquals(
                Interpretation.Kind.ATOMIC_CONJUNCTION,
                interpretations.get(interpretations.size() - 1).getKind()
        );
        assertTrue(interpretations.stream().anyMatch(
                value -> value.getKind() == Interpretation.Kind.PHRASE_GROUP
        ));
        assertTrue(interpretations.size() <= InterpretationEngine.MAX_HYPOTHESES);
    }

    @Test
    public void hypothesesAndComponentsAreImmutable() {
        List<Interpretation> interpretations = InterpretationEngine.generate(
                SemanticConceptExtractor.extract("water invoice from february")
        );

        boolean hypothesesImmutable = false;
        try {
            interpretations.clear();
        } catch (UnsupportedOperationException expected) {
            hypothesesImmutable = true;
        }
        assertTrue(hypothesesImmutable);

        boolean componentsImmutable = false;
        try {
            interpretations.get(0).getComponents().clear();
        } catch (UnsupportedOperationException expected) {
            componentsImmutable = true;
        }
        assertTrue(componentsImmutable);
    }

    @Test
    public void emptyGraphProducesNoHypotheses() {
        List<Interpretation> interpretations = InterpretationEngine.generate(
                SemanticConceptExtractor.extract("")
        );

        assertTrue(interpretations.isEmpty());
    }
}
