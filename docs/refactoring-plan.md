# Gilded Rose — Incremental Refactoring Plan

## Goals

- Preserve existing behavior exactly (starter `updateQuality` is the oracle).
- Small, reviewable commits; **characterization tests green after every step**.
- Land on Strategy + per-type Updater + factory/registry so **Conjured is one new class + one registration**.
- Never modify `Item`.

## Non-negotiables

| Rule | Detail |
|---|---|
| Gate | Run `mvn test` (or Gradle equivalent) after **every** step. |
| Commit policy | One step ≈ one commit. No behavior change in refactor commits. |
| `Item` | Untouched (fields, constructor, visibility). |
| Name matching | Keep exact strings until a dedicated constants step. |
| Order of operations | Quality (using current `sellIn`) → decrement `sellIn` → apply `sellIn < 0` rules. |

## Target design (end state before Conjured feature)

```text
com.gildedrose
├── Item.java                          (unchanged)
├── GildedRose.java                    (orchestrates only)
├── ItemUpdater.java                   (strategy interface)
├── ItemUpdaterFactory.java            (name → updater)
├── QualityAdjuster.java               (shared floor/ceiling helpers)
└── updaters/
    ├── NormalItemUpdater.java
    ├── AgedBrieUpdater.java
    ├── SulfurasUpdater.java
    └── BackstagePassUpdater.java
```

`GildedRose.updateQuality()`:

```java
for (Item item : items) {
    itemUpdaterFactory.updaterFor(item.name).update(item);
}
```

---

## Pre-flight (Step 0)

### Objective
Confirm the safety net before touching production code.

### Code changes
- No production changes.
- Ensure `GildedRoseCharacterizationTest` is complete enough to start (ideally Priority A gaps from `docs/characterization-test-review.md` already added; if not, add them in a **separate test-only commit** before Step 1).
- Optional: record TextTest golden output for later comparison.

### Risks
- Starting with a thin suite → silent regressions. **Mitigation:** do not begin Step 1 until Normal / Brie / Sulfuras / Backstage boundaries are green.

### Expected benefit
Known-good baseline; every later failure is attributable to the last commit.

---

## Step 1 — Extract item-name constants

### Objective
Remove duplicated magic strings without changing control flow.

### Code changes
- Add package-private or public constants, e.g. in `ItemNames`:
  - `AGED_BRIE`
  - `BACKSTAGE_PASSES`
  - `SULFURAS`
- Replace every string literal in `GildedRose` with those constants.
- Use the same constants in characterization tests (optional, same or follow-up commit).

### Risks
- Typo when copying a constant value → widespread mis-routing. **Mitigation:** copy-paste from current literals; run full suite.

### Expected benefit
Single source of truth for names; prepares factory keys; safer Conjured name constant later.

**Commit message idea:** `refactor: extract item name constants`

---

## Step 2 — Extract quality adjustment helpers

### Objective
Centralize floor/ceiling mutations used throughout the nested logic.

### Code changes
- Introduce `QualityAdjuster` (or private methods on `GildedRose` first, then move):
  - `increaseQuality(Item item)` — `if (quality < 50) quality++`
  - `decreaseQuality(Item item)` — `if (quality > 0) quality--`
  - optionally `zeroQuality(Item item)` — `quality = 0` (replaces `quality - quality`)
- Rewrite existing branches to call helpers; **no structural change** to conditions yet.

### Risks
- Accidentally clamping downward when quality is already > 50. **Mitigation:** helpers must only guard increments/decrements, never `Math.min(quality, 50)` on read.
- Sulfuras must never call decrease/increase helpers.

### Expected benefit
Caps/floors defined once; later updaters share identical semantics; clearer Backstage zeroing.

**Commit message idea:** `refactor: extract quality increase/decrease helpers`

---

## Step 3 — Clarify control flow with early structure (still one class)

### Objective
Replace negated “not Brie and not Backstage” nesting with positive, named branches — still inside `GildedRose`.

### Code changes
- Reshape `updateQuality` loop body to something like:

```text
if (Sulfuras) { /* no-op quality & sellIn */ return/continue; }
if (Aged Brie) { updateAgedBrie(item); continue; }
if (Backstage) { updateBackstage(item); continue; }
updateNormal(item);
```

- Implement `updateNormal` / `updateAgedBrie` / `updateBackstage` as **private methods** on `GildedRose`, preserving order: daily quality → sellIn-- → expired handling.
- Sulfuras: explicit no-op (do not decrement sellIn).

### Risks
- Highest risk step so far: easy to flip expiry timing or Backstage bands. **Mitigation:** run suite; compare method bodies line-by-line to old branches before deleting them.
- Using `continue` vs nested ifs must still process all items.

### Expected benefit
Readable type dispatch; sets up 1:1 extraction to updater classes; nesting smell largely gone.

**Commit message idea:** `refactor: replace nested conditionals with named update methods`

---

## Step 4 — Introduce `ItemUpdater` interface and route Normal only

### Objective
Establish the Strategy seam with minimal blast radius.

### Code changes
- Add `ItemUpdater { void update(Item item); }`.
- Add `NormalItemUpdater` moving logic from `updateNormal`.
- `GildedRose` constructs or receives a way to get the normal updater; for this step only Normal is delegated:

```text
if (Brie) updateAgedBrie...
else if (Backstage) updateBackstage...
else if (Sulfuras) /* no-op */
else normalItemUpdater.update(item);
```

### Risks
- Packaging/visibility issues (`Item` public fields are fine; keep updaters package-private if desired).
- Forgetting Sulfuras exclusion so Sulfuras falls into Normal. **Mitigation:** keep Sulfuras branch before the Normal delegate; rely on Sulfuras tests.

### Expected benefit
Proves the Strategy path works end-to-end before moving special cases.

**Commit message idea:** `refactor: introduce ItemUpdater and NormalItemUpdater`

---

## Step 5 — Extract `AgedBrieUpdater`

### Objective
Move Aged Brie policy out of `GildedRose`.

### Code changes
- Create `AgedBrieUpdater` from `updateAgedBrie` (pre-expiry +1, sellIn--, post-expiry +1, via `QualityAdjuster`).
- Replace private method with `agedBrieUpdater.update(item)`.

### Risks
- Losing the post-expiry +2 behavior (prose-only reading). **Mitigation:** B2/B2b characterization tests.

### Expected benefit
Brie isolated; `GildedRose` shrinks; Conjured path becomes clearer by analogy.

**Commit message idea:** `refactor: extract AgedBrieUpdater`

---

## Step 6 — Extract `BackstagePassUpdater`

### Objective
Isolate the most complex policy (bands + zero on expiry).

### Code changes
- Create `BackstagePassUpdater` with:
  - thresholds on **current** `sellIn` before decrement (`< 11`, `< 6`)
  - stepwise `increaseQuality`
  - `sellIn--`
  - if `sellIn < 0` then `zeroQuality`
- Remove private Backstage method from `GildedRose`.

### Risks
- Off-by-one on 11/10/6/5; zeroing vs degrade. **Mitigation:** boundary tests P1b–P4a; do not “simplify” multiple increases into a single `+= 3` without per-step cap checks (or prove equivalence under tests).

### Expected benefit
Hardest logic quarantined; future edits to passes cannot break Normal/Brie.

**Commit message idea:** `refactor: extract BackstagePassUpdater`

---

## Step 7 — Extract `SulfurasUpdater`

### Objective
Make the legendary no-op explicit and uniform with other types.

### Code changes
- Add `SulfurasUpdater.update` as empty body (or comment-only).
- Route Sulfuras through the updater like every other type.

### Risks
- Accidentally decrementing sellIn if someone “implements” ageing later. **Mitigation:** empty update + Sulfuras tests; code review focus.

### Expected benefit
Uniform dispatch — no special case left in the loop; factory becomes straightforward.

**Commit message idea:** `refactor: extract SulfurasUpdater`

---

## Step 8 — Introduce `ItemUpdaterFactory` (or registry)

### Objective
Remove remaining type `if/else` from `GildedRose`; single lookup by name.

### Code changes
- Add `ItemUpdaterFactory.updaterFor(String name)`:
  - map special names → singleton/cached updaters
  - **default** → `NormalItemUpdater`
- `GildedRose` holds factory (constructor injection optional; `new ItemUpdaterFactory()` is fine for kata):

```java
public void updateQuality() {
    for (Item item : items) {
        itemUpdaterFactory.updaterFor(item.name).update(item);
    }
}
```

- Delete leftover private update methods.

### Risks
- Defaulting too aggressively (e.g. mis-typed Backstage name silently becomes Normal) — **this matches current behavior** and must be preserved.
- Mutable static maps in tests — prefer instance factory.

### Expected benefit
`GildedRose` is pure orchestration; **OCP achieved** for new item categories.

**Commit message idea:** `refactor: add ItemUpdaterFactory and simplify GildedRose`

---

## Step 9 — Package cleanup & documentation only (optional)

### Objective
Align structure with team conventions without behavior change.

### Code changes
- Move updaters to `com.gildedrose.updaters` if desired.
- Short class-level Javadoc on factory and interface stating exact-name matching and day phases.
- No logic edits.

### Risks
- Broken imports / test classpath. **Mitigation:** compile + test.

### Expected benefit
Clearer navigation for reviewers; interview narrative matches package layout.

**Commit message idea:** `refactor: move item updaters into dedicated package`

---

## Step 10 — Add Conjured support (feature commit, not a refactor)

### Objective
Implement the new supplier category with **minimal modification**.

### Code changes
1. Add tests first (`ConjuredItemTest` or nested class): −2 before sell-by, −4 after, floor at 0 (see `docs/decision-table.md` C1–C5).
2. Add `ConjuredItemUpdater` (same shape as Normal with doubled decreases; reuse `QualityAdjuster`, call `decreaseQuality` twice or `decreaseQualityBy(item, 2)` carefully with floor).
3. Register in `ItemUpdaterFactory` for the chosen name match (`"Conjured Mana Cake"` and/or `startsWith("Conjured")` — **decide explicitly** and document).
4. No changes to other updaters; no changes to `Item`; `GildedRose` untouched if factory owns registration.

### Risks
- Name matching ambiguity (exact vs prefix).  
- After sell-by −4 vs −2 interpretation. **Mitigation:** lock with tests; prefer −2/−4 parallel to Normal.  
- Implementing decrease-by-2 without respecting floor between steps (1→ should end 0, not −1).

### Expected benefit
Feature delivery validates the design: **one class + one factory entry (+ tests)**.

**Commit message idea:** `feat: add Conjured item quality degradation`

---

## Commit sequence checklist

| Step | Type | Tests must stay green |
|---|---|---|
| 0 | test/baseline | ✓ |
| 1 | refactor (constants) | ✓ |
| 2 | refactor (helpers) | ✓ |
| 3 | refactor (named methods) | ✓ |
| 4 | refactor (Normal updater) | ✓ |
| 5 | refactor (Brie) | ✓ |
| 6 | refactor (Backstage) | ✓ |
| 7 | refactor (Sulfuras) | ✓ |
| 8 | refactor (factory) | ✓ |
| 9 | refactor (packaging, optional) | ✓ |
| 10 | **feat** (Conjured) | ✓ (new tests + old suite) |

---

## What “minimal modification” for Conjured means after this plan

| File | Change required for Conjured? |
|---|---|
| `Item.java` | No |
| `GildedRose.java` | No |
| Existing updaters | No |
| `QualityAdjuster` | Maybe a small `decreaseBy` helper if not already present |
| `ItemUpdaterFactory` | **Yes — one registration line** |
| New `ConjuredItemUpdater` | **Yes — new class** |
| Tests | **Yes — new cases** |

---

## Rollback strategy

- If a step fails tests: fix forward if trivial; otherwise `git revert` that commit and redo in a smaller slice (e.g. split Step 3 into “extract methods without changing conditions” then “invert ifs”).
- Never combine Steps 3 and 8 in one commit.
- Never mix Step 10 (feature) into a refactor commit.

---

## Success criteria

1. Characterization suite green throughout Steps 1–9 with **no expected-value edits**.  
2. `GildedRose.updateQuality` is a short loop + factory lookup.  
3. Each item category’s rules live in one class.  
4. Conjured lands as Step 10 with no edits to sibling updaters.  
5. Git history tells the story for the PTV interview review.
