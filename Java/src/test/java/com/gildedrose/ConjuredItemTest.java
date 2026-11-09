package com.gildedrose;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Feature tests for Conjured items.
 * <p>
 * Rule: degrade twice as fast as normal items (−2 before sell-by, −4 after),
 * floored at 0. SellIn still decreases by 1 per day.
 */
@DisplayName("Conjured items")
class ConjuredItemTest {

    private static final String CONJURED = "Conjured Mana Cake";

    private static Item afterOneDay(String name, int sellIn, int quality) {
        Item[] items = new Item[]{new Item(name, sellIn, quality)};
        new GildedRose(items).updateQuality();
        return items[0];
    }

    private static void assertItem(Item item, int expectedSellIn, int expectedQuality) {
        assertEquals(expectedSellIn, item.sellIn, "sellIn");
        assertEquals(expectedQuality, item.quality, "quality");
    }

    @Test
    @DisplayName("C1: before sell-by, quality decreases by 2")
    void beforeSellBy_decreasesQualityByTwo() {
        assertItem(afterOneDay(CONJURED, 3, 6), 2, 4);
    }

    @Test
    @DisplayName("C2: before sell-by, quality 1 floors at 0")
    void beforeSellBy_qualityOneFloorsAtZero() {
        assertItem(afterOneDay(CONJURED, 3, 1), 2, 0);
    }

    @Test
    @DisplayName("C3: before sell-by, quality 0 stays 0")
    void beforeSellBy_qualityZeroStaysZero() {
        assertItem(afterOneDay(CONJURED, 3, 0), 2, 0);
    }

    @Test
    @DisplayName("C4: on sell-by day (0→-1), quality decreases by 4")
    void onSellByDay_decreasesQualityByFour() {
        assertItem(afterOneDay(CONJURED, 0, 8), -1, 4);
    }

    @Test
    @DisplayName("C4b: when already expired, quality decreases by 4")
    void afterSellBy_decreasesQualityByFour() {
        assertItem(afterOneDay(CONJURED, -1, 8), -2, 4);
    }

    @Test
    @DisplayName("C5: after sell-by, quality 3 floors at 0")
    void afterSellBy_lowQualityFloorsAtZero() {
        assertItem(afterOneDay(CONJURED, 0, 3), -1, 0);
    }

    @Test
    @DisplayName("Other Conjured-named items use the same rules")
    void conjuredPrefixIsRecognized() {
        assertItem(afterOneDay("Conjured Health Potion", 5, 10), 4, 8);
    }
}
