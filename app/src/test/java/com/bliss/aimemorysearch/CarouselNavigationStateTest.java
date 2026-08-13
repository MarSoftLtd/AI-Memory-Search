package com.bliss.aimemorysearch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CarouselNavigationStateTest {
    @Test public void hidesImpossibleArrowAtEachBoundary() {
        CarouselNavigationState first = CarouselNavigationState.at(0, 3);
        assertFalse(first.hasPrevious);
        assertTrue(first.hasNext);

        CarouselNavigationState middle = CarouselNavigationState.at(1, 3);
        assertTrue(middle.hasPrevious);
        assertTrue(middle.hasNext);

        CarouselNavigationState last = CarouselNavigationState.at(2, 3);
        assertTrue(last.hasPrevious);
        assertFalse(last.hasNext);
    }

    @Test public void singleResultHasNoNavigation() {
        CarouselNavigationState only = CarouselNavigationState.at(0, 1);
        assertFalse(only.hasPrevious);
        assertFalse(only.hasNext);
    }
}
