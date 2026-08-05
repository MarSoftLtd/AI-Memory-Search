package com.bliss.aimemorysearch.ai.concepts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import org.junit.Test;

public class InterpretationRetrievalFrameworkTest {

    @Test
    public void budgetIsClampedToHardAndroidLimits() {
        RetrievalBudget budget = new RetrievalBudget(
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE
        );

        assertEquals(8, budget.getMaxInterpretations());
        assertEquals(12, budget.getMaxComponents());
        assertEquals(8, budget.getMaxExecutions());
        assertEquals(10, budget.getMaxResultsPerModality());
    }

    @Test
    public void contextAndExecutionResultAreImmutable() {
        RetrievalContext context = new RetrievalContext(
                null,
                "white cat",
                Collections.singletonList("white cat")
        );
        InterpretationExecutionResult result =
                new InterpretationExecutionResult(
                        "i0",
                        Interpretation.Kind.WHOLE_QUERY,
                        "test",
                        Collections.emptyList(),
                        0f,
                        0,
                        0,
                        0,
                        0L,
                        false
                );

        assertEquals("white cat", context.getNormalizedQuery());
        assertTrue(result.getRetrievedResults().isEmpty());
        assertImmutable(context.getCanonicalQueries());
        assertImmutable(result.getRetrievedResults());
    }

    private static void assertImmutable(java.util.List<?> values) {
        boolean immutable = false;
        try {
            values.clear();
        } catch (UnsupportedOperationException expected) {
            immutable = true;
        }
        assertTrue(immutable);
    }
}
