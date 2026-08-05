package com.bliss.aimemorysearch.ai.concepts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;

public class SemanticConceptExtractorTest {

    @Test
    public void twoTokenQueriesPreferTheCompletePhrase() {
        for (String query : Arrays.asList(
                "white cat",
                "red roses",
                "pisica alba",
                "trandafiri rosii",
                "water invoice",
                "factura apa",
                "contract Engie"
        )) {
            ConceptGraph graph = SemanticConceptExtractor.extract(query);
            assertEquals(3, graph.getConcepts().size());
            assertTrue(graph.getConcepts().get(0).isPrimary());
            assertEquals(
                    ConceptGraph.ConceptKind.PHRASE,
                    graph.getConcepts().get(0).getKind()
            );
            assertEquals(query.toLowerCase(),
                    graph.getConcepts().get(0).getText());
        }
    }

    @Test
    public void extractionIsLanguageIndependentAndImmutable() {
        ConceptGraph personGraph = SemanticConceptExtractor.extract(
                "poza cu Daniela"
        );
        assertEquals("poza cu daniela",
                personGraph.getConcepts().get(0).getText());
        assertTrue(personGraph.getConcepts().stream().anyMatch(
                concept -> "cu daniela".equals(concept.getText())));

        ConceptGraph graph = SemanticConceptExtractor.extract(
                "poza cu pisica alba langa flori"
        );

        assertEquals(6, graph.getTokens().size());
        assertEquals("poza cu pisica alba langa flori",
                graph.getConcepts().get(0).getText());
        assertTrue(graph.getConcepts().stream().anyMatch(
                concept -> "pisica alba".equals(concept.getText())));

        boolean immutable = false;
        try {
            graph.getConcepts().clear();
        } catch (UnsupportedOperationException expected) {
            immutable = true;
        }
        assertTrue(immutable);
    }

    @Test
    public void extractionIsBounded() {
        ConceptGraph graph = SemanticConceptExtractor.extract(
                "one two three four five six seven eight nine ten "
                        + "eleven twelve thirteen fourteen"
        );

        assertEquals(12, graph.getTokens().size());
        assertTrue(graph.getConcepts().size() <= 34);
    }

    @Test
    public void semanticAssertionLayerStartsEmptyAndIsImmutable() {
        ConceptGraph graph = SemanticConceptExtractor.extract("white cat");

        assertTrue(graph.getSemanticAssertions().isEmpty());

        boolean immutable = false;
        try {
            graph.getSemanticAssertions().add(null);
        } catch (UnsupportedOperationException expected) {
            immutable = true;
        }
        assertTrue(immutable);
    }
}
