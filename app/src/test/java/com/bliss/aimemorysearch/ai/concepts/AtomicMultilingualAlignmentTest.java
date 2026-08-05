package com.bliss.aimemorysearch.ai.concepts;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class AtomicMultilingualAlignmentTest {

    @Test
    public void createsAtomicInterpretationForEachApprovedPivot() {
        List<AtomicMultilingualAlignment.AlignedInterpretation> aligned =
                AtomicMultilingualAlignment.align(Arrays.asList(
                        "pisica alba",
                        "white cat"
                ));

        assertEquals(2, aligned.size());
        assertEquals("pisica alba", aligned.get(0).getApprovedPivot());
        assertEquals("white cat", aligned.get(1).getApprovedPivot());
        assertEquals(
                Interpretation.Kind.ATOMIC_CONJUNCTION,
                aligned.get(1).getInterpretation().getKind()
        );
        assertEquals(
                "white",
                aligned.get(1).getInterpretation()
                        .getComponents().get(0).getText()
        );
        assertEquals(
                "cat",
                aligned.get(1).getInterpretation()
                        .getComponents().get(1).getText()
        );
    }

    @Test
    public void preservesMorphologyWithoutTranslatingComponents() {
        List<AtomicMultilingualAlignment.AlignedInterpretation> aligned =
                AtomicMultilingualAlignment.align(Arrays.asList(
                        "trandafiri rosii",
                        "red roses"
                ));

        assertEquals(
                "trandafir",
                aligned.get(0).getInterpretation()
                        .getComponents().get(0).getText()
        );
        assertEquals(
                "red",
                aligned.get(1).getInterpretation()
                        .getComponents().get(0).getText()
        );
    }
}
