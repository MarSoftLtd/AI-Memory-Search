package com.bliss.aimemorysearch;

/** Deterministic mapping between a real result index and the infinite adapter. */
public final class CarouselPositionMapper {
    private CarouselPositionMapper() {}

    public static int initialPosition(int resultCount) {
        if (resultCount <= 0) return 0;
        int middle = Integer.MAX_VALUE / 2;
        return middle - middle % resultCount;
    }

    public static int nearestPosition(
            int currentAdapterPosition,
            int realResultIndex,
            int resultCount
    ) {
        if (resultCount <= 0 || realResultIndex < 0
                || realResultIndex >= resultCount) {
            return RecyclerPosition.INVALID;
        }
        int cycleStart = currentAdapterPosition
                - Math.floorMod(currentAdapterPosition, resultCount);
        int candidate = cycleStart + realResultIndex;
        int previous = candidate - resultCount;
        int next = candidate + resultCount;
        if (distance(previous, currentAdapterPosition)
                < distance(candidate, currentAdapterPosition)) {
            candidate = previous;
        }
        if (distance(next, currentAdapterPosition)
                < distance(candidate, currentAdapterPosition)) {
            candidate = next;
        }
        return candidate;
    }

    private static long distance(int left, int right) {
        return Math.abs((long) left - right);
    }

    public static final class RecyclerPosition {
        public static final int INVALID = -1;
        private RecyclerPosition() {}
    }
}
