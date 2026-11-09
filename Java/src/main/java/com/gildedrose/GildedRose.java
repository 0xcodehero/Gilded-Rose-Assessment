package com.gildedrose;

class GildedRose {
    Item[] items;

    public GildedRose(Item[] items) {
        this.items = items;
    }

    public void updateQuality() {
        for (int i = 0; i < items.length; i++) {
            Item item = items[i];

            if (item.name.equals(ItemNames.SULFURAS)) {
                continue;
            }
            if (item.name.equals(ItemNames.AGED_BRIE)) {
                updateAgedBrie(item);
                continue;
            }
            if (item.name.equals(ItemNames.BACKSTAGE_PASSES)) {
                updateBackstage(item);
                continue;
            }
            if (ItemNames.isConjured(item.name)) {
                updateConjured(item);
                continue;
            }
            updateNormal(item);
        }
    }

    private void updateNormal(Item item) {
        QualityAdjuster.decreaseQuality(item);

        item.sellIn = item.sellIn - 1;

        if (item.sellIn < 0) {
            QualityAdjuster.decreaseQuality(item);
        }
    }

    /**
     * Conjured items degrade twice as fast as normal: −2 before sell-by, −4 after.
     */
    private void updateConjured(Item item) {
        QualityAdjuster.decreaseQuality(item, 2);

        item.sellIn = item.sellIn - 1;

        if (item.sellIn < 0) {
            QualityAdjuster.decreaseQuality(item, 2);
        }
    }

    private void updateAgedBrie(Item item) {
        QualityAdjuster.increaseQuality(item);

        item.sellIn = item.sellIn - 1;

        if (item.sellIn < 0) {
            QualityAdjuster.increaseQuality(item);
        }
    }

    private void updateBackstage(Item item) {
        QualityAdjuster.increaseQuality(item);

        if (item.sellIn < 11) {
            QualityAdjuster.increaseQuality(item);
        }
        if (item.sellIn < 6) {
            QualityAdjuster.increaseQuality(item);
        }

        item.sellIn = item.sellIn - 1;

        if (item.sellIn < 0) {
            QualityAdjuster.zeroQuality(item);
        }
    }
}
