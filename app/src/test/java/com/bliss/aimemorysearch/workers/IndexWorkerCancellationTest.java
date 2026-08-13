package com.bliss.aimemorysearch.workers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

public class IndexWorkerCancellationTest {

    @Test
    public void stoppedWorkerDoesNotInvokeEmbedding() throws Exception {
        AtomicInteger invocationCount = new AtomicInteger();

        try {
            IndexWorker.runEmbeddingIfActive(
                    () -> true,
                    () -> {
                        invocationCount.incrementAndGet();
                        return new float[] {1.0f};
                    });
            fail("Expected cancellation");
        } catch (CancellationException expected) {
            // Expected: the embedding callback must not be entered.
        }

        assertEquals(0, invocationCount.get());
    }

    @Test
    public void activeWorkerInvokesEmbeddingExactlyOnceAndReturnsItsResult() throws Exception {
        AtomicInteger invocationCount = new AtomicInteger();
        float[] expected = new float[] {1.0f, 2.0f};

        float[] actual = IndexWorker.runEmbeddingIfActive(
                () -> false,
                () -> {
                    invocationCount.incrementAndGet();
                    return expected;
                });

        assertSame(expected, actual);
        assertEquals(1, invocationCount.get());
    }
}
