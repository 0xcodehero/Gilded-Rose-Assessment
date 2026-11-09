# Gilded Rose — Requirements Analysis & Behavior Specification

## Purpose

This document captures the observed inventory update behavior for the Gilded Rose kata. The written requirements are incomplete and sometimes ambiguous. Per the assignment brief, **the existing `UpdateQuality` implementation is functionally correct and is the source of truth** for all currently supported item types. Conjured items are the only new behavior to add; their rules are inferred from the written requirements and by analogy with normal items.

Exact item names used by the starter code:

| Spec name | Exact name in code |
|---|---|
| Aged Brie | `Aged Brie` |
| Sulfuras | `Sulfuras, Hand of Ragnaros` |
| Backstage passes | `Backstage passes to a TAFKAL80ETC concert` |
| Conjured | Not implemented yet (see ambiguities) |

---

## Behavior Specification Table

| Item Type | SellIn Change | Quality Change Before Sell Date | Quality Change After Sell Date | Minimum Quality | Maximum Quality | Special Rules |
|---|---|---|---|---|---|---|
| **Normal Items** | −1 per day | −1 | −2 | 0 | 50 (not enforced on decrease; see notes) | After sell-by, degrades twice as fast. Quality never goes below 0. |
| **Aged Brie** | −1 per day | +1 | +2 | n/a (does not decrease) | 50 | Increases with age. Rate doubles after sell-by (observed in code; not stated in the prose requirements). |
| **Sulfuras** | 0 (never changes) | 0 | 0 | 80 (fixed) | 80 (fixed) | Legendary: never sold, never changes. Quality remains 80. Exempt from the Quality ≤ 50 rule. |
| **Backstage Passes** | −1 per day | +1 if SellIn > 10<br>+2 if 6 ≤ SellIn ≤ 10<br>+3 if 1 ≤ SellIn ≤ 5 | Set to **0** | 0 | 50 | Thresholds are evaluated against SellIn **before** it is decremented. On the day SellIn becomes negative (concert day ends), Quality is set to 0. |
| **Conjured Items** *(new)* | −1 per day | −2 | −4 | 0 | 50 (same constraint model as normal) | Degrade **twice as fast as normal items**. By analogy with normal items that means −2 before sell-by and −4 after. Not present in the starter code. |

### How “before / after sell date” is defined

Observed from the existing code’s order of operations:

1. Apply the primary Quality change (using the **current** SellIn).
2. Decrement SellIn by 1 (unless Sulfuras).
3. If `SellIn < 0`, apply the expired-item Quality adjustment.

Therefore:

- **Before sell date** = item starts the day with `SellIn ≥ 1` (ends the day with `SellIn ≥ 0`).
- **After sell date** = item starts the day with `SellIn ≤ 0` (ends the day with `SellIn < 0`).

The day SellIn moves from `0` → `−1` is the first day expired rules apply.

---

## Detailed Per-Type Notes (from existing code)

### Normal items

- Primary step: `Quality − 1` if `Quality > 0`.
- After `SellIn` becomes negative: another `Quality − 1` if `Quality > 0`.
- Net: −1 before expiry, −2 after expiry, floored at 0.

### Aged Brie

- Primary step: `Quality + 1` if `Quality < 50`.
- After `SellIn` becomes negative: another `Quality + 1` if `Quality < 50`.
- Net: +1 before expiry, +2 after expiry, capped at 50.
- The prose only says Brie “increases”; the **double increase after sell-by is code behavior**, not an explicit requirement sentence.

### Sulfuras, Hand of Ragnaros

- Skipped for every Quality mutation and for SellIn decrement.
- Clarification in the requirements: Quality is **80** and never alters.
- Name match is exact and case-sensitive.

### Backstage passes to a TAFKAL80ETC concert

Evaluated against SellIn **before** decrement:

| SellIn at start of day | Quality delta (capped at 50) |
|---|---|
| `> 10` | +1 |
| `6 … 10` | +2 |
| `1 … 5` | +3 |
| `≤ 0` | Primary increase may briefly apply, then Quality is set to `0` once `SellIn < 0` |

So “Quality drops to 0 after the concert” means: once SellIn has gone negative, Quality becomes 0 (typically the transition `0 → −1`).

### Conjured items (to implement)

Requirement text: *“Conjured items degrade in Quality twice as fast as normal items.”*

Using normal items as the baseline:

| Phase | Normal | Conjured (2×) |
|---|---|---|
| Before sell date | −1 | −2 |
| After sell date | −2 | −4 |
| Floor | 0 | 0 |

This interpretation preserves the same “after sell-by doubles again” structure that normal items already have.

---

## Ambiguous Requirements & Source-of-Truth Decisions

| Ambiguity | What the prose leaves unclear | How existing code / assignment brief resolves it |
|---|---|---|
| **When has the sell-by date “passed”?** | “Once the sell by date has passed” could mean `SellIn ≤ 0` before or after the daily tick. | Code decrements SellIn first, then checks `SellIn < 0`. Expired rules start on the day SellIn goes from 0 to −1. |
| **Aged Brie after expiry** | Prose only says Quality increases; no rate after sell-by. | Code applies **+2 after expiry**. Treat that as required behavior. |
| **Backstage threshold boundaries** | “10 days or less” / “5 days or less” — inclusive? Based on pre- or post-decrement SellIn? | Code uses `SellIn < 11` and `SellIn < 6` **before** decrement ⇒ inclusive of 10 and 5 on that day’s starting value. |
| **Backstage on concert day (`SellIn == 0`)** | Does Quality still increase then drop, or drop immediately? | Code may increase first, then sets Quality to 0 after SellIn becomes negative. End state for the day is **0**. |
| **Quality maximum of 50** | Absolute clamp vs. only prevent further increases? | Code never reduces an already-high Quality (except Backstage expiry → 0). It only skips increments when `Quality >= 50`. Do not invent a clamp-down. |
| **Sulfuras Quality 80 vs. max 50** | Conflict between “never more than 50” and Sulfuras = 80. | Explicit clarification: Sulfuras is exempt; Quality stays 80 and never changes. |
| **Item name matching** | Spec uses short names (“Sulfuras”, “Backstage passes”, “Conjured”). | Matching is **exact string equality** on the names used in code/tests. Do not use `contains` / prefix matching unless tests or product owners require it for Conjured. |
| **Conjured naming** | Spec says `"Conjured"` items; fixture examples often use `"Conjured Mana Cake"`. | Not in starter logic. Prefer matching the kata’s conventional name(s) / tests once characterization coverage exists; document the chosen matcher (exact name vs. `startsWith("Conjured")`) in the solution notes. |
| **Conjured after sell-by** | “Twice as fast as normal” could mean always −2, or 2× the *current* normal rate (−2 / −4). | Prefer **−2 / −4** so Conjured stays structurally parallel to normal items. Validate with characterization tests before locking the choice. |
| **Conjured vs. specials** | Could an item be both Conjured and Aged Brie / Backstage? | Out of scope unless requirements say otherwise. Implement Conjured as a degrading category like normal items. |
| **Immutability of `Item`** | Tempting to redesign the data model. | Assignment forbids altering the `Item` class / `Items` property. Behavior changes belong in `UpdateQuality` (and new collaborators you introduce). |

### Guiding principle for the exercise

> The existing implementation is the oracle for all pre-Conjured behavior.  
> When the markdown requirements and the code disagree or the markdown is silent, **preserve code behavior**.  
> For Conjured items only, derive behavior from the written rule *and* keep the same degradation structure (including post-sell-by doubling and the Quality ≥ 0 floor) that normal items already follow.

---

## Order of Operations (canonical)

For every item, each day:

```
IF item is not Aged Brie AND not Backstage:
    IF Quality > 0 AND not Sulfuras:
        Quality -= 1          // Conjured (new): subtract 2 instead, floored at 0
ELSE:  // Aged Brie or Backstage
    IF Quality < 50:
        Quality += 1
        IF Backstage:
            IF SellIn < 11 AND Quality < 50: Quality += 1
            IF SellIn < 6  AND Quality < 50: Quality += 1

IF not Sulfuras:
    SellIn -= 1

IF SellIn < 0:
    IF Aged Brie:
        IF Quality < 50: Quality += 1
    ELSE IF Backstage:
        Quality = 0
    ELSE IF not Sulfuras AND Quality > 0:
        Quality -= 1          // Conjured (new): subtract 2 instead, floored at 0
```

This flowchart is a readable restatement of the starter `UpdateQuality` method, with Conjured deltas annotated where the new feature plugs in.

---

## Implications for the Assessment

1. **Characterization first** — lock current behavior with tests (or approval/golden tests) before refactoring.
2. **Refactor for clarity** — replace the nested conditionals with explicit strategies/handlers per item type, without changing outcomes.
3. **Then add Conjured** — implement −2 / −4 degradation with the same floor and sell-by timing as normal items.
4. **Do not change `Item`** — keep the goblin happy.
5. **Document AI usage** — retain session transcripts / steering artefacts as requested in the invitation email.
