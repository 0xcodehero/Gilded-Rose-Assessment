# Characterization Test Review

**Subject:** `Java/src/test/java/com/gildedrose/GildedRoseCharacterizationTest.java`  
**Goal:** Assess how well the suite locks existing behavior before refactoring.

## Verdict

The suite is a **solid smoke + boundary lock** for the four current categories. It covers the decision-table “must-have” rows and most Backstage boundaries. Confidence for a careful extract-method / strategy refactor is **moderate–good**, not excellent: several high-value edge cases, multi-day / approval coverage, and assertion strengths are still missing.

---

## 1. Missing test cases

| Gap | Why it matters during refactoring |
|---|---|
| **Normal `sellIn == 1` → 0** | Proves the last non-expired day is still −1 quality (not −2). Easy to break if “expired” is wrongly defined as `sellIn <= 0` before decrement. |
| **Normal quality == 2 on sell-by day** | End state 0 via −2; distinguishes “double degrade then floor” from “degrade once then stop.” |
| **Aged Brie `sellIn == 1`** | Symmetric to above: last pre-expiry day must be +1, not +2. |
| **Aged Brie quality == 48 after sell-by** | Should end at 50 (+2). Complements B3 (49→50) and proves both increments can land inside the cap. |
| **Backstage mid-band samples** | e.g. `sellIn == 8` (+2) and `sellIn == 3` (+3). Boundaries exist; interior points reduce “lucky boundary only” risk. |
| **Backstage cap in +1 and +2 bands** | Only +3 near-cap (49) and at-cap (50) are tested. A refactor that applies bonuses as a single `+= n` without per-step guards can still pass current tests if only the +3 path is capped in tests. |
| **Backstage quality == 48 / 47 in +3 band** | 48→50 (+2 effective), 47→50 (+3). Locks stepwise capping vs bulk add. |
| **Backstage already at 0 after concert** | `sellIn < 0`, `quality == 0` stays 0 (idempotent zeroing). |
| **Empty inventory** | `new GildedRose(new Item[]{})` then `updateQuality()` — protects loop bounds / NPE assumptions. |
| **Single-element / order sensitivity** | Multi-item test exists, but not “Sulfuras first/last” or alternating types beyond one happy mix. |
| **Case / whitespace name variants** | e.g. `"aged brie"`, `"Aged Brie "` — documents that matching is exact (like the partial-name tests). |
| **“Aged Brie” exactness counterpart** | Partial Sulfuras/Backstage names are tested; Brie typo/`contains` risk is not. |
| **Fixture catalog items** | TextTest names (`Elixir of the Mongoose`, etc.) — optional, but ties unit suite to golden master later. |
| **Multi-day progression** | No test calls `updateQuality()` N times (e.g. Backstage 15→…→0, or Brie over many days to 50). Catches order-of-operations drift that single-day tests miss. |
| **Approval / TextTest golden master** | Not wired as an automated regression. Highest leverage for “whole system still same.” |

*Conjured omission is correct for characterization of current code.*

---

## 2. Weak assertions

| Issue | Current state | Stronger approach |
|---|---|---|
| **Name not asserted in most tests** | Only one dedicated “name unchanged” test. A refactor that rewrites `items[i]` references or recreates items could silently change names. | Assert `name` in `assertItem` (or always assert the full triple). |
| **Array identity / slot stability** | Multi-item test asserts field values but not that `app.items` is the same array / same object instances. | `assertSame(items, app.items)` and `assertSame` per element if the public contract is in-place mutation. |
| **Parameterized Sulfuras hides per-case intent** | Fine technically; failure messages are weaker than named `@Test` methods unless `name =` is rich. | Keep param test but include ids in the name (`S1c/S1/S1b`). |
| **No explicit invariant asserts** | Floor/ceiling are implied by expected values, not stated as `assertTrue(quality >= 0)` / `<= 50` where relevant. | Add invariant checks on outputs for degrading/increasing items (defense in depth). |
| **Exact Δ not documented in assert messages** | Messages are only `"sellIn"` / `"quality"`. | Include expected Δ or test id in the message (`"N3 quality"`). |
| **Partial-name tests use extreme quality (80)** | `"Sulfuras"` at 80 decreasing to 79 proves normal path, but looks like Sulfuras data. | Also use a mundane quality (e.g. 10→9) so the test reads clearly as “normal item.” |
| **No assert on array length** | Multi-item test assumes length 4 unchanged. | `assertEquals(4, app.items.length)`. |

Overall assertion style (end-state sellIn + quality after one day) is the **right characterization shape**; weakness is mostly **completeness of the asserted surface**, not the pattern itself.

---

## 3. Missing edge cases (behavior footguns)

These are the cases most likely to go green while a subtle refactor is wrong:

1. **Expiry predicate timing** — `sellIn == 1` for Normal and Brie (still non-expired after tick).  
2. **Backstage threshold off-by-one** — partially covered (11/10/6/5); add 8 and 3 for interior confirmation.  
3. **Stepwise vs bulk quality changes** — Backstage/Brie near 50 with room for 1–2 increments only.  
4. **Quality already above 50 (non-Sulfuras)** — starter code does not clamp down. A “helpful” refactor that clamps `min(quality, 50)` would change behavior; **no test forbids it** today.  
5. **Sulfuras quality ≠ 80** — code never mutates Sulfuras quality regardless of value. A refactor that forces 80 would change behavior; **untested**.  
6. **Normal item with quality > 50** — decreases without clamping to 50; untested.  
7. **null name** — would NPE today; only needed if you want to lock “throws” vs handle; usually skip unless API hardening is in scope.  
8. **sellIn boundary `Integer` extremes** — optional (`Integer.MIN_VALUE + 1`); low priority for this kata.

---

## 4. Opportunities to improve confidence during refactoring

### Priority A — do before structural refactor

1. Add **`sellIn == 1`** cases for Normal (−1 quality) and Aged Brie (+1 quality).  
2. Extend **Backstage cap matrix**: quality 49/48/47 at sellIn 5; quality 49 at sellIn 10 and 15.  
3. Assert **`name` + sellIn + quality`** in the shared helper.  
4. Add a **multi-day** test (small N, table-driven expected sequence) for one Normal, one Brie, one Backstage path through a threshold.  
5. Capture a **TextTest / approval golden file** for the fixture over ~20 days and run it in CI.

### Priority B — strengthen the safety net

6. Parameterize each category from the decision-table CSV (one row = one case) so coverage is visibly complete vs `docs/decision-table.md`.  
7. Assert **in-place mutation** (`assertSame`) to lock the goblin-friendly public shape.  
8. Add **“no clamp-down”** characterization: Normal or Brie starting at quality 55 → 54 or 56 respectively (documents current lack of absolute clamp).  
9. Add **Sulfuras quality 40** (or any non-80) remains unchanged — locks “never alters,” not “always 80.”  
10. Empty array + exact Brie name negative case.

### Priority C — process / design for the assessment

11. Keep Conjured tests in a **separate** class added only with the feature (avoid red suite / mixed intent).  
12. Refactor under tests in thin steps; run the suite after each extract.  
13. Treat any test change during refactor as a **behavior change** — update only with an explicit decision note.

---

## 5. Coverage map vs decision table

| Decision id | Present? | Notes |
|---|---|---|
| N1–N5, N3b | Yes | Missing Normal `sellIn == 1` |
| B1–B4, B2b, B4b | Yes | Missing Brie `sellIn == 1`, 48→50 |
| S1 / S1b / S1c | Yes (param) | Missing non-80 quality |
| P1, P1b, P2a, P2b, P3a, P3b, P4a, P4b, P5, P5b | Yes | Missing mid-band + richer cap matrix |
| Multi-item | Partial | One mix only |
| Exact names | Partial | Sulfuras/Backstage only |
| Multi-day / TextTest | No | Largest confidence gap |
| Conjured | Correctly absent | — |

---

## 6. Recommendations (concise)

1. **Keep the current suite** as the baseline gate; do not start refactoring on it alone if you will heavily restructure dispatch.  
2. **Add Priority A cases** (especially `sellIn == 1` and Backstage stepwise caps) — small effort, high regression value.  
3. **Add a golden-master / multi-day test** before introducing strategy objects — best detector of accidental behavior drift.  
4. **Harden `assertItem`** to check name (and optionally identity).  
5. **Document known undocumented behaviors** with tests: no clamp-down above 50; Sulfuras unchanged even if quality ≠ 80.  
6. **Do not add Conjured expectations** until the feature is implemented; keep characterization pure.
