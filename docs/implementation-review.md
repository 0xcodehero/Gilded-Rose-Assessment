# Gilded Rose — Implementation Review

**Scope:** Starter `GildedRose.updateQuality()` (Java / equivalent C#).  
**Constraint:** No code was modified. Behavior is assumed correct and must be preserved.  
**Constraint:** `Item` class and `Items` property must not be altered.

---

## Verdict

`updateQuality` is a single, deeply nested procedural method that encodes all item policies via negated string comparisons. It is functionally correct but hard to read, hard to extend (Conjured), and weakly testable at the unit-of-policy level. Refactoring is **medium risk without characterization tests**, **low risk once those tests exist**.

---

## 1. Code Smells

| Smell | Evidence | Why it matters |
|---|---|---|
| **Long method** | Entire inventory policy lives in one `updateQuality` loop (~50+ lines, one responsibility blob). | Cannot reason about one item type without reading all types. |
| **Deep nesting** | Up to ~5–6 levels of `if` inside the loop. | Cognitive load; easy to break a branch while editing another. |
| **Feature envy / primitive obsession** | Logic operates on `items[i].name`, `.quality`, `.sellIn` with magic strings and magic numbers (`50`, `11`, `6`, `0`). | Policy is scattered as literals; names/thresholds are not domain concepts. |
| **Mysterious names / opaque boolean logic** | Primary branch is `if (!Aged Brie && !Backstage)` — i.e. “everything else”, with Sulfuras carved out deeper. | Intent (“degrading items”) is expressed only by exclusion. |
| **Duplicate switches on type** | Same three names compared in the first block **and** again in the `sellIn < 0` block. | Two “type dispatch” sites can drift apart when Conjured is added. |
| **Odd idiom** | `quality = quality - quality` instead of `quality = 0` for expired Backstage. | Correct but obscures intent. |
| **God method / shotgun surgery risk** | Any new item type (Conjured) requires touching multiple nested branches. | High chance of missing a path (e.g. only change pre-expiry degrade, forget post-expiry). |
| **Public mutable data** | `Item` fields are public (`name`, `sellIn`, `quality`). | Out of scope to fix (goblin owns `Item`), but it forces all invariants into `GildedRose`. |
| **No characterization coverage in starter** | Default unit test is a stub (`assertEquals("fixme", ...)`). | Refactoring without first adding tests is unsafe. |

---

## 2. Long Methods

| Method | Assessment |
|---|---|
| `GildedRose(Item[])` / constructor | Fine — trivial. |
| `updateQuality()` | **Too long / too dense.** Not long in raw LOC alone, but **cyclomatic complexity is high**: one method handles degrade, appreciate, legendary exemption, sell-in tick, and expiry side-effects for all types. |

Logical responsibilities currently fused inside the loop:

1. Classify item by name  
2. Apply daily quality delta (pre-tick)  
3. Decrement sell-in (unless Sulfuras)  
4. Apply expired-day quality delta  

A maintainable shape would separate **orchestration** (loop + day tick) from **per-type policy**.

---

## 3. Nested Conditionals

Structure (simplified):

```text
for each item:
  if NOT Brie AND NOT Backstage:
      if quality > 0:
          if NOT Sulfuras:
              quality--
  else:                              // Brie OR Backstage
      if quality < 50:
          quality++
          if Backstage:
              if sellIn < 11:
                  if quality < 50: quality++
              if sellIn < 6:
                  if quality < 50: quality++

  if NOT Sulfuras:
      sellIn--

  if sellIn < 0:
      if NOT Brie:
          if NOT Backstage:
              if quality > 0:
                  if NOT Sulfuras:
                      quality--
          else:
              quality = 0
      else:
          if quality < 50:
              quality++
```

Problems caused by this nesting:

- **Negated outer conditions** force mental double-negatives (“not Brie and not Backstage” ⇒ normal/Sulfuras/other).
- **Sulfuras checks are buried** inside quality branches instead of an early continue / dedicated policy.
- **Backstage bonus thresholds** are nested under the shared “increasing” path with Brie, coupling two different policies.
- **Expiry handling is a second nested tree** that re-encodes type, duplicating classification.

---

## 4. Duplication

| Duplicated concern | Where |
|---|---|
| Name literals | `"Aged Brie"`, `"Backstage passes to a TAFKAL80ETC concert"`, `"Sulfuras, Hand of Ragnaros"` appear multiple times each. |
| Type classification | Once for the main quality update; again for the `sellIn < 0` block. |
| Quality floor guard | `quality > 0` before decrease — pre-expiry and post-expiry. |
| Quality ceiling guard | `quality < 50` before increase — repeated for Brie base bump, each Backstage bonus, and Brie expiry bump. |
| Sulfuras exemption | Checked separately for quality decrease (×2 sites) and sell-in decrement (×1 site). |
| ±1 quality mutation | Written as `quality = quality ± 1` repeatedly instead of small helpers (`increaseQuality`, `decreaseQuality`). |

Impact: adding Conjured “twice as fast” means finding **every** normal-item decrease path (currently two) and applying the doubled rate consistently — easy to miss one under nesting.

---

## 5. SOLID Principle Violations

### Single Responsibility (SRP) — **violated**

`GildedRose` / `updateQuality` owns:

- iteration over inventory  
- sell-in progression  
- quality rules for every product category  
- special cases (legendary, concert expiry, caps)

Changing one product rule risks affecting unrelated categories.

### Open/Closed (OCP) — **violated** (main design issue for Conjured)

Adding Conjured requires **modifying** the existing conditional maze, not extending with a new policy object. The design is closed against extension without editing the core method.

### Liskov Substitution (LSP) — **N/A / latent**

No inheritance hierarchy for item behaviors today. If a future refactor introduces subclasses of `Item`, that would violate the goblin constraint; prefer composition/strategy **outside** `Item`.

### Interface Segregation (ISP) — **weak / N/A**

No interfaces. Not a current pain point; becomes relevant if introducing a small `ItemUpdatePolicy` (or similar) with a focused `update(Item)` contract.

### Dependency Inversion (DIP) — **violated in spirit**

High-level day-update flow depends directly on low-level concrete string comparisons and inline arithmetic. There is no abstraction for “how this item ages.”

---

## 6. Refactoring Risk Assessment

### Risk summary

| Area | Risk without tests | Risk with characterization tests | Notes |
|---|---|---|---|
| Extract constants for names / thresholds | Low | Very low | Behavior-preserving if values unchanged. |
| Extract `increaseQuality` / `decreaseQuality` helpers (cap/floor) | Low–Medium | Low | Must preserve “no clamp-down if already >50” and Sulfuras untouched. |
| Invert nested conditionals → positive branches | Medium | Low | Easy to flip a Backstage/Brie path. |
| Split pre-expiry / sell-in tick / post-expiry phases | Medium | Low | Order of operations is load-bearing (Backstage thresholds use pre-decrement `sellIn`). |
| Replace string dispatch with strategy / map | Medium–High | Low–Medium | Highest design payoff for Conjured; wrong matcher = silent wrong type. |
| Change `Item` or its fields | **Forbidden** | **Forbidden** | Out of scope; do not touch. |
| Implement Conjured before locking current behavior | **High** | Medium | Can corrupt normal/Brie/Backstage paths while adding a feature. |

### Highest-risk behavioral footguns

1. **Order of operations** — quality (using current `sellIn`) → decrement `sellIn` → apply `sellIn < 0` rules. Reordering breaks Backstage bands and “first expired day.”
2. **Backstage on `sellIn₀ == 0`** — end state must be Quality `0` (even if a naive rewrite skips the transient increase).
3. **Aged Brie after expiry = +2** — easy to “simplify” to +1 if following prose only.
4. **Quality cap semantics** — prevent further increase above 50; do not force-clamp existing values downward.
5. **Exact name matching** — including full Sulfuras and Backstage strings.
6. **Sulfuras** — no quality change **and** no sell-in change.

### Recommended risk controls (process, not code changes)

1. Add characterization / parameterized tests from `docs/decision-table.md` **before** any structural refactor.  
2. Refactor in small commits: extract helpers → clarify branches → introduce per-type policies → then add Conjured.  
3. Prefer approval/golden tests (TextTest) as a second safety net for multi-day runs.  
4. Treat Conjured as a new policy beside Normal, not as another nested exception inside the current tree.

### Overall assessment

| Question | Answer |
|---|---|
| Is the code safe to ship as-is functionally? | Yes — for current item types. |
| Is it safe to extend (Conjured) in place? | Poorly — high regression risk due to nesting + duplicated type checks. |
| Is a behavior-preserving refactor justified? | Yes — primarily to satisfy OCP before adding Conjured. |
| Gate for refactoring? | Characterization tests covering decision-table rows for Normal, Brie, Sulfuras, Backstage. |

---

## 7. Implications for the assessment solution

Do **not** rewrite behavior from the markdown requirements alone.  
Preserve starter semantics, reduce complexity so Conjured can be added as an isolated policy, and keep `Item` untouched.
