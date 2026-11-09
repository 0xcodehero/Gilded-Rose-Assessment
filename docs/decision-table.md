# Gilded Rose — Decision Table (Testable Specification)

Use this document as the oracle for unit / parameterized tests.  
Each **row** is one test case: given the **Condition**, after one call to `UpdateQuality()`, assert the **Quality** and **SellIn** adjustments (or absolute expected values).

**Conventions**

- `SellIn₀` / `Quality₀` = values **before** `UpdateQuality()`
- Adjustments are relative (`Δ`) unless the expected behavior is an absolute assignment (e.g. Backstage → 0)
- “Expired” means `SellIn₀ ≤ 0` (first expired day is `SellIn₀ == 0` → ends at `−1`)
- Item names must match **exactly** (case-sensitive)
- Source of truth for existing types: starter `UpdateQuality` implementation
- Conjured rows are the **new** feature (not yet in starter code)

---

## 1. Normal Items

Name: any name that is **not** `Aged Brie`, `Backstage passes to a TAFKAL80ETC concert`, `Sulfuras, Hand of Ragnaros`, or a Conjured item.

| # | Condition | Expected behavior | Quality adjustment | SellIn adjustment |
|---|---|---|---|---|
| N1 | `SellIn₀ > 0`, `Quality₀ > 0` | Degrades by 1 before sell-by | `ΔQ = −1` | `ΔS = −1` |
| N2 | `SellIn₀ > 0`, `Quality₀ == 0` | Already at floor; no further decrease | `ΔQ = 0` | `ΔS = −1` |
| N3 | `SellIn₀ ≤ 0`, `Quality₀ ≥ 2` | Degrades twice as fast after sell-by | `ΔQ = −2` | `ΔS = −1` |
| N4 | `SellIn₀ ≤ 0`, `Quality₀ == 1` | Double degradation floored at 0 | `ΔQ = −1` (ends at 0) | `ΔS = −1` |
| N5 | `SellIn₀ ≤ 0`, `Quality₀ == 0` | Remains at floor | `ΔQ = 0` | `ΔS = −1` |

### Suggested unit inputs (Normal)

| Test id | Name | SellIn₀ | Quality₀ | Expected SellIn | Expected Quality |
|---|---|---|---|---|---|
| N1 | `"+5 Dexterity Vest"` | 10 | 20 | 9 | 19 |
| N2 | `"+5 Dexterity Vest"` | 10 | 0 | 9 | 0 |
| N3 | `"+5 Dexterity Vest"` | 0 | 20 | −1 | 18 |
| N3b | `"+5 Dexterity Vest"` | −1 | 20 | −2 | 18 |
| N4 | `"+5 Dexterity Vest"` | 0 | 1 | −1 | 0 |
| N5 | `"+5 Dexterity Vest"` | 0 | 0 | −1 | 0 |

---

## 2. Aged Brie

Name: `"Aged Brie"`

| # | Condition | Expected behavior | Quality adjustment | SellIn adjustment |
|---|---|---|---|---|
| B1 | `SellIn₀ > 0`, `Quality₀ < 50` | Improves by 1 before sell-by | `ΔQ = +1` | `ΔS = −1` |
| B2 | `SellIn₀ ≤ 0`, `Quality₀ ≤ 48` | Improves twice as fast after sell-by | `ΔQ = +2` | `ΔS = −1` |
| B3 | `SellIn₀ ≤ 0`, `Quality₀ == 49` | Post-expiry increase capped at 50 | `ΔQ = +1` (ends at 50) | `ΔS = −1` |
| B4 | `Quality₀ == 50` (any SellIn) | Already at cap; no further increase | `ΔQ = 0` | `ΔS = −1` |

### Suggested unit inputs (Aged Brie)

| Test id | Name | SellIn₀ | Quality₀ | Expected SellIn | Expected Quality |
|---|---|---|---|---|---|
| B1 | `"Aged Brie"` | 2 | 0 | 1 | 1 |
| B2 | `"Aged Brie"` | 0 | 0 | −1 | 2 |
| B2b | `"Aged Brie"` | −1 | 10 | −2 | 12 |
| B3 | `"Aged Brie"` | 0 | 49 | −1 | 50 |
| B4 | `"Aged Brie"` | 5 | 50 | 4 | 50 |
| B4b | `"Aged Brie"` | 0 | 50 | −1 | 50 |

---

## 3. Sulfuras

Name: `"Sulfuras, Hand of Ragnaros"`

| # | Condition | Expected behavior | Quality adjustment | SellIn adjustment |
|---|---|---|---|---|
| S1 | Any `SellIn₀`, `Quality₀ == 80` | Legendary; never sold, never changes | `ΔQ = 0` | `ΔS = 0` |
| S2 | `SellIn₀ ≤ 0`, `Quality₀ == 80` | Expired sell-by still has no effect | `ΔQ = 0` | `ΔS = 0` |

### Suggested unit inputs (Sulfuras)

| Test id | Name | SellIn₀ | Quality₀ | Expected SellIn | Expected Quality |
|---|---|---|---|---|---|
| S1 | `"Sulfuras, Hand of Ragnaros"` | 0 | 80 | 0 | 80 |
| S1b | `"Sulfuras, Hand of Ragnaros"` | −1 | 80 | −1 | 80 |
| S1c | `"Sulfuras, Hand of Ragnaros"` | 5 | 80 | 5 | 80 |

---

## 4. Backstage Passes

Name: `"Backstage passes to a TAFKAL80ETC concert"`  
Thresholds use **`SellIn₀` (before decrement)**. Cap at 50 applies to each incremental step.

| # | Condition | Expected behavior | Quality adjustment | SellIn adjustment |
|---|---|---|---|---|
| P1 | `SellIn₀ > 10`, `Quality₀ ≤ 49` | Far from concert; +1 | `ΔQ = +1` | `ΔS = −1` |
| P2 | `6 ≤ SellIn₀ ≤ 10`, `Quality₀ ≤ 48` | 10 days or less; +2 | `ΔQ = +2` | `ΔS = −1` |
| P3 | `1 ≤ SellIn₀ ≤ 5`, `Quality₀ ≤ 47` | 5 days or less; +3 | `ΔQ = +3` | `ΔS = −1` |
| P4 | `SellIn₀ ≤ 0` (any Quality) | After concert; Quality drops to 0 | `Q = 0` (absolute) | `ΔS = −1` |
| P5 | Would exceed 50 in P1–P3 | Increases stop at 50 | End Quality = 50 | `ΔS = −1` |

### Boundary rows (must cover in tests)

| # | Condition | Expected behavior | Quality adjustment | SellIn adjustment |
|---|---|---|---|---|
| P1b | `SellIn₀ == 11` | Still in +1 band | `ΔQ = +1` | `ΔS = −1` |
| P2a | `SellIn₀ == 10` | Enters +2 band | `ΔQ = +2` | `ΔS = −1` |
| P2b | `SellIn₀ == 6` | Still in +2 band | `ΔQ = +2` | `ΔS = −1` |
| P3a | `SellIn₀ == 5` | Enters +3 band | `ΔQ = +3` | `ΔS = −1` |
| P3b | `SellIn₀ == 1` | Last day before expiry; +3 | `ΔQ = +3` | `ΔS = −1` |
| P4a | `SellIn₀ == 0` | Concert day ends; Quality → 0 | `Q = 0` | `ΔS = −1` |

### Suggested unit inputs (Backstage)

| Test id | Name | SellIn₀ | Quality₀ | Expected SellIn | Expected Quality |
|---|---|---|---|---|---|
| P1 | `"Backstage passes to a TAFKAL80ETC concert"` | 15 | 20 | 14 | 21 |
| P1b | `"Backstage passes to a TAFKAL80ETC concert"` | 11 | 20 | 10 | 21 |
| P2a | `"Backstage passes to a TAFKAL80ETC concert"` | 10 | 20 | 9 | 22 |
| P2b | `"Backstage passes to a TAFKAL80ETC concert"` | 6 | 20 | 5 | 22 |
| P3a | `"Backstage passes to a TAFKAL80ETC concert"` | 5 | 20 | 4 | 23 |
| P3b | `"Backstage passes to a TAFKAL80ETC concert"` | 1 | 20 | 0 | 23 |
| P4a | `"Backstage passes to a TAFKAL80ETC concert"` | 0 | 20 | −1 | 0 |
| P4b | `"Backstage passes to a TAFKAL80ETC concert"` | −1 | 20 | −2 | 0 |
| P5 | `"Backstage passes to a TAFKAL80ETC concert"` | 5 | 49 | 4 | 50 |
| P5b | `"Backstage passes to a TAFKAL80ETC concert"` | 10 | 50 | 9 | 50 |

---

## 5. Conjured Items *(new feature)*

Provisional name for tests: `"Conjured Mana Cake"` (kata convention).  
Rule: degrade **twice as fast as normal** → −2 before sell-by, −4 after; floor at 0.

| # | Condition | Expected behavior | Quality adjustment | SellIn adjustment |
|---|---|---|---|---|
| C1 | `SellIn₀ > 0`, `Quality₀ ≥ 2` | Degrades twice as fast as normal before sell-by | `ΔQ = −2` | `ΔS = −1` |
| C2 | `SellIn₀ > 0`, `Quality₀ == 1` | Floor at 0 | `ΔQ = −1` (ends at 0) | `ΔS = −1` |
| C3 | `SellIn₀ > 0`, `Quality₀ == 0` | Remains at floor | `ΔQ = 0` | `ΔS = −1` |
| C4 | `SellIn₀ ≤ 0`, `Quality₀ ≥ 4` | 2× normal after sell-by (normal −2 → conjured −4) | `ΔQ = −4` | `ΔS = −1` |
| C5 | `SellIn₀ ≤ 0`, `Quality₀ ∈ {1,2,3}` | Double degradation floored at 0 | End Quality = 0 | `ΔS = −1` |

### Suggested unit inputs (Conjured)

| Test id | Name | SellIn₀ | Quality₀ | Expected SellIn | Expected Quality |
|---|---|---|---|---|---|
| C1 | `"Conjured Mana Cake"` | 3 | 6 | 2 | 4 |
| C2 | `"Conjured Mana Cake"` | 3 | 1 | 2 | 0 |
| C3 | `"Conjured Mana Cake"` | 3 | 0 | 2 | 0 |
| C4 | `"Conjured Mana Cake"` | 0 | 8 | −1 | 4 |
| C4b | `"Conjured Mana Cake"` | −1 | 8 | −2 | 4 |
| C5 | `"Conjured Mana Cake"` | 0 | 3 | −1 | 0 |

---

## 6. Cross-cutting invariants (assert in every relevant test)

| # | Condition | Expected behavior | Quality adjustment | SellIn adjustment |
|---|---|---|---|---|
| I1 | Any non-Sulfuras item | SellIn always advances one day | (per type) | `ΔS = −1` |
| I2 | Any degrading item | Quality never negative | End `Quality ≥ 0` | (per type) |
| I3 | Any increasing item (Brie / Backstage) | Quality never rises above 50 | End `Quality ≤ 50` | (per type) |
| I4 | Sulfuras | Quality stays 80; SellIn unchanged | `ΔQ = 0` | `ΔS = 0` |
| I5 | `Item` class / public shape | Unchanged by the exercise | n/a | n/a |

---

## 7. Mapping decision rows → unit tests

Recommended shape (JUnit 5 / xUnit parameterized):

```text
Given: Item(name, sellIn₀, quality₀)
When:  updateQuality()   // once
Then:  item.sellIn  == expectedSellIn
       item.quality == expectedQuality
```

Checklist for a complete characterization suite:

1. Cover **every row id** in sections 1–4 before refactoring (existing behavior lock).
2. Add section **5** only when implementing Conjured.
3. Prefer the **Suggested unit inputs** tables as `@ParameterizedTest` / `[Theory]` data.
4. Include boundary ids (`N3` vs `N3b`, `P1b`/`P2a`/`P3a`/`P4a`, Brie at 49/50).
5. Do **not** assert on intermediate steps inside `UpdateQuality` — only end-of-day state.

### Minimal smoke set (fast red/green)

| Priority | Ids |
|---|---|
| Must-have (existing) | N1, N3, N5, B1, B2, B4, S1, P1, P2a, P3a, P4a, P5 |
| Must-have (Conjured) | C1, C4, C5 |
| Strongly recommended boundaries | N4, B3, P1b, P2b, P3b, S1b |

---

## 8. Name constants for tests

```text
NORMAL_EXAMPLE      = "+5 Dexterity Vest"
AGED_BRIE           = "Aged Brie"
SULFURAS            = "Sulfuras, Hand of Ragnaros"
BACKSTAGE           = "Backstage passes to a TAFKAL80ETC concert"
CONJURED_EXAMPLE    = "Conjured Mana Cake"
```
