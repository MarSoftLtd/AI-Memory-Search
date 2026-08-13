package com.bliss.aimemorysearch;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CarouselPositionMapperTest {

    @Test public void initialPositionCentersRealRankOne() {
        int position = CarouselPositionMapper.initialPosition(7);
        assertEquals(0, position % 7);
    }

    @Test public void mapsEveryReportIndexToExactCarouselIdentity() {
        int count = 5;
        int current = CarouselPositionMapper.initialPosition(count) + 4;
        for (int realIndex = 0; realIndex < count; realIndex++) {
            int target = CarouselPositionMapper.nearestPosition(
                    current, realIndex, count);
            assertEquals(realIndex, Math.floorMod(target, count));
        }
    }

    @Test public void choosesNearestInfiniteCycleAcrossBoundary() {
        int count = 5;
        int cycle = CarouselPositionMapper.initialPosition(count);
        assertEquals(cycle + 5,
                CarouselPositionMapper.nearestPosition(cycle + 4, 0, count));
        assertEquals(cycle - 1,
                CarouselPositionMapper.nearestPosition(cycle, 4, count));
    }
}
