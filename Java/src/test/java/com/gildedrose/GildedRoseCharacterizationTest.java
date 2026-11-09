package com.gildedrose;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Characterization tests for the existing GildedRose.updateQuality() behavior.
 * <p>
 * These tests lock current production behavior before any refactoring.
 * Expected values were derived from the starter implementation, not from a
 * re-interpretation of the requirements prose.
 * <p>
 * Conjured coverage lives in {@link ConjuredItemTest}.
 */
@DisplayName("GildedRose characterization (existing behavior)")
class GildedRoseCharacterizationTest {

    private static final String AGED_BRIE = ItemNames.AGED_BRIE;
    private static final String SULFURAS = ItemNames.SULFURAS;
    private static final String BACKSTAGE = ItemNames.BACKSTAGE_PASSES;
    private static final String NORMAL = "+5 Dexterity Vest";

    private static Item afterOneDay(String name, int sellIn, int quality) {
        Item[] items = new Item[]{new Item(name, sellIn, quality)};
        GildedRose app = new GildedRose(items);
        app.updateQuality();
        return app.items[0];
    }

    private static void assertItem(Item item, int expectedSellIn, int expectedQuality) {
        assertEquals(expectedSellIn, item.sellIn, "sellIn");
        assertEquals(expectedQuality, item.quality, "quality");
    }

    @Nested
    @DisplayName("Normal items")
    class NormalItems {

        @Test
        @DisplayName("N1: before sell-by, quality decreases by 1 and sellIn by 1")
        void beforeSellBy_decreasesQualityByOne() {
            // Protects: standard daily degradation for ordinary goods.
            assertItem(afterOneDay(NORMAL, 10, 20), 9, 19);
        }

        @Test
        @DisplayName("N2: quality never goes below 0 before sell-by")
        void beforeSellBy_qualityFloorIsZero() {
            // Protects: the quality > 0 guard on the primary decrease path.
            assertItem(afterOneDay(NORMAL, 10, 0), 9, 0);
        }

        @Test
        @DisplayName("N3: on the day sellIn goes 0→-1, quality decreases by 2")
        void onSellByDay_decreasesQualityByTwo() {
            // Protects: first expired day applies both primary and post-expiry decreases.
            assertItem(afterOneDay(NORMAL, 0, 20), -1, 18);
        }

        @Test
        @DisplayName("N3b: when already expired (sellIn < 0), quality still decreases by 2")
        void afterSellBy_decreasesQualityByTwo() {
            // Protects: sellIn < 0 branch continues to double-degrade past the first expired day.
            assertItem(afterOneDay(NORMAL, -1, 20), -2, 18);
        }

        @Test
        @DisplayName("N4: after sell-by, double decrease is floored at 0 when quality is 1")
        void afterSellBy_qualityOneFloorsAtZero() {
            // Protects: both decrease sites respect quality > 0; end state is 0, not -1.
            assertItem(afterOneDay(NORMAL, 0, 1), -1, 0);
        }

        @Test
        @DisplayName("N5: after sell-by, quality already 0 stays 0")
        void afterSellBy_qualityZeroStaysZero() {
            // Protects: no negative quality from either decrease path when already at floor.
            assertItem(afterOneDay(NORMAL, 0, 0), -1, 0);
        }

        @Test
        @DisplayName("Name is unchanged by updateQuality")
        void nameIsUnchanged() {
            // Protects: updateQuality mutates sellIn/quality only.
            Item item = afterOneDay(NORMAL, 5, 5);
            assertEquals(NORMAL, item.name);
        }
    }

    @Nested
    @DisplayName("Aged Brie")
    class AgedBrie {

        @Test
        @DisplayName("B1: before sell-by, quality increases by 1")
        void beforeSellBy_increasesQualityByOne() {
            // Protects: Brie appreciation path (+1) and sellIn decrement.
            assertItem(afterOneDay(AGED_BRIE, 2, 0), 1, 1);
        }

        @Test
        @DisplayName("B2: on sell-by day (0→-1), quality increases by 2")
        void onSellByDay_increasesQualityByTwo() {
            // Protects: code behavior (not prose) — Brie gains an extra +1 when sellIn < 0.
            assertItem(afterOneDay(AGED_BRIE, 0, 0), -1, 2);
        }

        @Test
        @DisplayName("B2b: when already expired, quality increases by 2")
        void afterSellBy_increasesQualityByTwo() {
            // Protects: continued post-expiry double appreciation.
            assertItem(afterOneDay(AGED_BRIE, -1, 10), -2, 12);
        }

        @Test
        @DisplayName("B3: after sell-by starting at 49, quality caps at 50 (only +1)")
        void afterSellBy_nearCap_stopsAtFifty() {
            // Protects: each +1 checks quality < 50; does not jump past 50.
            assertItem(afterOneDay(AGED_BRIE, 0, 49), -1, 50);
        }

        @Test
        @DisplayName("B4: quality at 50 before sell-by does not increase")
        void beforeSellBy_atCap_qualityUnchanged() {
            // Protects: hard ceiling of 50 on the increasing path.
            assertItem(afterOneDay(AGED_BRIE, 5, 50), 4, 50);
        }

        @Test
        @DisplayName("B4b: quality at 50 after sell-by does not increase")
        void afterSellBy_atCap_qualityUnchanged() {
            // Protects: both pre- and post-expiry increases honor the 50 cap.
            assertItem(afterOneDay(AGED_BRIE, 0, 50), -1, 50);
        }
    }

    @Nested
    @DisplayName("Sulfuras, Hand of Ragnaros")
    class Sulfuras {

        @ParameterizedTest(name = "S: sellIn={0}, quality={1} remains unchanged")
        @CsvSource({
                "5, 80",
                "0, 80",
                "-1, 80"
        })
        @DisplayName("Legendary item never changes sellIn or quality")
        void neverChanges(int sellIn, int quality) {
            // Protects: Sulfuras is skipped for quality mutation and sellIn decrement,
            // including when sellIn is already negative.
            assertItem(afterOneDay(SULFURAS, sellIn, quality), sellIn, quality);
        }
    }

    @Nested
    @DisplayName("Backstage passes to a TAFKAL80ETC concert")
    class BackstagePasses {

        @Test
        @DisplayName("P1: sellIn > 10 → quality +1")
        void moreThanTenDays_increasesByOne() {
            // Protects: far-from-concert appreciation (+1 only).
            assertItem(afterOneDay(BACKSTAGE, 15, 20), 14, 21);
        }

        @Test
        @DisplayName("P1b: sellIn == 11 is still the +1 band (boundary)")
        void elevenDays_increasesByOne() {
            // Protects: threshold is sellIn < 11 (checked before decrement), so 11 ≠ +2.
            assertItem(afterOneDay(BACKSTAGE, 11, 20), 10, 21);
        }

        @Test
        @DisplayName("P2a: sellIn == 10 → quality +2 (boundary)")
        void tenDays_increasesByTwo() {
            // Protects: entering the “10 days or less” band.
            assertItem(afterOneDay(BACKSTAGE, 10, 20), 9, 22);
        }

        @Test
        @DisplayName("P2b: sellIn == 6 → quality +2 (boundary before +3)")
        void sixDays_increasesByTwo() {
            // Protects: “5 days or less” uses sellIn < 6, so 6 stays in the +2 band.
            assertItem(afterOneDay(BACKSTAGE, 6, 20), 5, 22);
        }

        @Test
        @DisplayName("P3a: sellIn == 5 → quality +3 (boundary)")
        void fiveDays_increasesByThree() {
            // Protects: entering the “5 days or less” band (+1 +1 +1).
            assertItem(afterOneDay(BACKSTAGE, 5, 20), 4, 23);
        }

        @Test
        @DisplayName("P3b: sellIn == 1 → quality +3, sellIn becomes 0 (not yet expired)")
        void oneDayLeft_increasesByThree_notExpiredYet() {
            // Protects: expiry rules require sellIn < 0 after decrement; day 1→0 still appreciates.
            assertItem(afterOneDay(BACKSTAGE, 1, 20), 0, 23);
        }

        @Test
        @DisplayName("P4a: sellIn == 0 → quality drops to 0 after concert")
        void sellInZero_qualityDropsToZero() {
            // Protects: after sellIn becomes -1, Backstage quality is zeroed (end state 0).
            assertItem(afterOneDay(BACKSTAGE, 0, 20), -1, 0);
        }

        @Test
        @DisplayName("P4b: sellIn already negative → quality stays/goes to 0")
        void alreadyExpired_qualityIsZero() {
            // Protects: post-concert zeroing continues for sellIn < 0.
            assertItem(afterOneDay(BACKSTAGE, -1, 20), -2, 0);
        }

        @Test
        @DisplayName("P5: near cap with +3 band, quality does not exceed 50")
        void fiveDays_nearCap_stopsAtFifty() {
            // Protects: each incremental +1 is guarded by quality < 50.
            assertItem(afterOneDay(BACKSTAGE, 5, 49), 4, 50);
        }

        @Test
        @DisplayName("P5b: quality already 50 before sell-by does not increase")
        void atCap_qualityUnchangedBeforeConcert() {
            // Protects: no increase when already at ceiling; sellIn still decrements.
            assertItem(afterOneDay(BACKSTAGE, 10, 50), 9, 50);
        }

        @Test
        @DisplayName("After concert, high quality is cleared to 0 (not capped)")
        void afterConcert_highQualityClearedToZero() {
            // Protects: expiry assignment quality = quality - quality (absolute 0), not a -1 degrade.
            assertItem(afterOneDay(BACKSTAGE, 0, 50), -1, 0);
        }
    }

    @Nested
    @DisplayName("Cross-cutting / regression guards")
    class CrossCutting {

        @Test
        @DisplayName("Multiple items are updated independently in one call")
        void updatesAllItemsInOnePass() {
            // Protects: loop processes every element; one type's rules do not skip later items.
            Item[] items = new Item[]{
                    new Item(NORMAL, 10, 20),
                    new Item(AGED_BRIE, 2, 0),
                    new Item(SULFURAS, 0, 80),
                    new Item(BACKSTAGE, 5, 20)
            };
            GildedRose app = new GildedRose(items);
            app.updateQuality();

            assertItem(items[0], 9, 19);
            assertItem(items[1], 1, 1);
            assertItem(items[2], 0, 80);
            assertItem(items[3], 4, 23);
        }

        @Test
        @DisplayName("Exact Sulfuras name is required; similar names degrade as normal")
        void sulfurasRequiresExactName() {
            // Protects: matching is exact string equality, not contains(\"Sulfuras\").
            assertItem(afterOneDay("Sulfuras", 5, 80), 4, 79);
        }

        @Test
        @DisplayName("Exact Backstage name is required; similar names degrade as normal")
        void backstageRequiresExactName() {
            // Protects: short/alternate pass names do not receive Backstage rules.
            assertItem(afterOneDay("Backstage passes", 5, 20), 4, 19);
        }
    }
}
