package com.bliss.aimemorysearch.ai.concepts;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AtomicConjunctionStrategyTest {

    @Test
    public void broaderAgreementAlwaysOutranksSingleConceptStrength() {
        float strongestSingle = AtomicConjunctionStrategy.agreementScore(
                1,
                2,
                1f
        );
        float weakestTwoConceptAgreement =
                AtomicConjunctionStrategy.agreementScore(
                        2,
                        2,
                        0.01f
                );

        assertTrue(weakestTwoConceptAgreement > strongestSingle);
    }

    @Test
    public void agreementRemainsNormalized() {
        float agreement = AtomicConjunctionStrategy.agreementScore(
                3,
                3,
                3f
        );

        assertTrue(agreement >= 0f);
        assertTrue(agreement <= 1f);
    }
}
