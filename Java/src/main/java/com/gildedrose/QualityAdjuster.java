package com.gildedrose;

/**
 * Shared quality mutations that preserve existing floor/ceiling semantics.
 * Does not clamp values that are already outside 0..50.
 */
final class QualityAdjuster {

    private static final int MAX_QUALITY = 50;
    private static final int MIN_QUALITY = 0;

    private QualityAdjuster() {
    }

    static void increaseQuality(Item item) {
        if (item.quality < MAX_QUALITY) {
            item.quality = item.quality + 1;
        }
    }

    static void decreaseQuality(Item item) {
        if (item.quality > MIN_QUALITY) {
            item.quality = item.quality - 1;
        }
    }

    /**
     * Decreases quality {@code times} times, applying the floor on each step.
     */
    static void decreaseQuality(Item item, int times) {
        for (int i = 0; i < times; i++) {
            decreaseQuality(item);
        }
    }

    static void zeroQuality(Item item) {
        item.quality = 0;
    }
}
