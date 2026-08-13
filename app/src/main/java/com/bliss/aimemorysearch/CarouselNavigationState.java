package com.bliss.aimemorysearch;

/** Pure finite-carousel boundary state used by Search Results arrows. */
public final class CarouselNavigationState {
    public final boolean hasPrevious;
    public final boolean hasNext;

    private CarouselNavigationState(boolean hasPrevious, boolean hasNext) {
        this.hasPrevious = hasPrevious;
        this.hasNext = hasNext;
    }

    public static CarouselNavigationState at(int position, int resultCount) {
        if (resultCount <= 0) return new CarouselNavigationState(false, false);
        int safe = Math.max(0, Math.min(position, resultCount - 1));
        return new CarouselNavigationState(safe > 0, safe < resultCount - 1);
    }
}
