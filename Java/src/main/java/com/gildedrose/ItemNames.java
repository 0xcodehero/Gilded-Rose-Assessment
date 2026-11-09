package com.gildedrose;

/**
 * Item names recognized by {@link GildedRose#updateQuality()}.
 * Special items use exact, case-sensitive equality.
 * Conjured items are a category matched by prefix.
 */
final class ItemNames {

    static final String AGED_BRIE = "Aged Brie";
    static final String BACKSTAGE_PASSES = "Backstage passes to a TAFKAL80ETC concert";
    static final String SULFURAS = "Sulfuras, Hand of Ragnaros";
    static final String CONJURED_PREFIX = "Conjured";

    private ItemNames() {
    }

    static boolean isConjured(String name) {
        return name != null && name.startsWith(CONJURED_PREFIX);
    }
}
