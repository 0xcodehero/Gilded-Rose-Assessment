# Gilded Rose — Refactoring Approach Comparison

**Context:** Existing `updateQuality()` is functionally correct. `Item` must not change. Conjured is the next feature. Characterization tests should gate any rewrite.

**Shared target shape (all viable approaches):**

```text
GildedRose.updateQuality()
  for each Item:
      resolve updater/policy for item.name
      updater.update(item)   // mutate sellIn + quality in place
```

---

## Approaches (defined)

| Approach | What it means here |
|---|---|
| **1. Strategy Pattern** | A `QualityUpdateStrategy` (or `ItemUpdateStrategy`) interface with `update(Item)`. `GildedRose` selects a strategy (map/registry/factory) and delegates. Strategies are peers selected by name. |
| **2. Item Updater Classes** | Same family as Strategy, but framed as dedicated updater types (`NormalItemUpdater`, `AgedBrieUpdater`, …) often behind a factory. Emphasis on one class per product rule set rather than on the GoF name. |
| **3. Polymorphism** | Classic OOP: subclasses of `Item` (`AgedBrie`, `Sulfuras`, …) with overridden `update()`. **Blocked** by the goblin rule unless polymorphism is applied to a *wrapper/adapter* around `Item`, not to `Item` itself. |
| **4. Rule-based solution** | Declarative rules: predicates + quality/sell-in deltas (table, list of `Rule`, or small DSL). An engine evaluates matching rules each day. |

> Note: In Java practice, **(1) and (2) are nearly the same design**. The useful distinction is naming/packaging: “Strategy” stresses the interface + interchangeable policies; “Updater classes” stresses domain-named types and a factory. Evaluation below treats them as close siblings and highlights where they diverge.

---

## 1. Strategy Pattern

**Sketch**

```text
interface ItemUpdateStrategy { void update(Item item); }

class NormalStrategy implements ItemUpdateStrategy { ... }
class AgedBrieStrategy implements ItemUpdateStrategy { ... }
class SulfurasStrategy implements ItemUpdateStrategy { ... }
class BackstagePassStrategy implements ItemUpdateStrategy { ... }

class StrategySelector {
  ItemUpdateStrategy select(String name) { ... }  // map or switch
}
```

### Evaluation

| Criterion | Score | Notes |
|---|---|---|
| **SOLID** | **Strong** | **SRP:** each strategy owns one policy. **OCP:** add Conjured by new strategy + registry entry. **LSP:** strategies interchangeable via interface. **ISP:** tiny interface. **DIP:** `GildedRose` depends on abstraction. |
| **Readability** | **High** | Day loop becomes obvious; open a strategy to read one item type. Avoids nested negatives. |
| **Maintainability** | **High** | Changes are localized. Shared helpers (`increaseQuality`, `decreaseQuality`, `ageOneDay`) keep caps/floors consistent. |
| **Extensibility** | **High** | Conjured = new class + one registration. Low risk to other types if tests are green. |
| **Testability** | **High** | Strategies unit-testable in isolation; `GildedRose` tested with selector + thin orchestration. Characterization tests still validate end-to-end. |

**Pros:** Clear, idiomatic Java, interview-friendly, production-proven.  
**Cons:** Slight class proliferation; name→strategy mapping must stay exact; overkill if only one item type ever existed (not the case here).

---

## 2. Item Updater Classes

**Sketch**

```text
abstract class ItemUpdater {
  final void update(Item item) { updateQuality(item); updateSellIn(item); updateExpired(item); }
  protected abstract void updateQuality(Item item);
  protected void updateSellIn(Item item) { item.sellIn--; }
  protected void updateExpired(Item item) { ... }
}

class NormalItemUpdater extends ItemUpdater { ... }
class AgedBrieUpdater extends ItemUpdater { ... }
...
class ItemUpdaterFactory { ItemUpdater forName(String name) { ... } }
```

Often includes a **template method** for the shared day phases (quality → sellIn → expiry), which matches the load-bearing order of operations in the current code.

### Evaluation

| Criterion | Score | Notes |
|---|---|---|
| **SOLID** | **Strong** | Same SRP/OCP/DIP benefits as Strategy. Template method can encode the invariant day pipeline (**good for this domain**). Risk: fat base class if too much logic is pushed upward (**SRP/OCP tension**). |
| **Readability** | **High** | Domain names (`AgedBrieUpdater`) read well. Template method makes order of operations explicit — valuable given Backstage thresholds use pre-decrement `sellIn`. |
| **Maintainability** | **High** | Excellent if the base class stays thin (shared clamp helpers only). Degrades if every special case leaks into the abstract updater. |
| **Extensibility** | **High** | Conjured updater + factory branch. Same as Strategy. |
| **Testability** | **High** | Per-updater tests + factory tests. Template method makes phase testing easy (or invites testing through `update` only — prefer the latter for characterization parity). |

**Pros:** Natural fit to “phases of a day”; names match inventory language; easy for JVM teams.  
**Cons:** Inheritance can tempt a god-base-class; prefer composition (shared `QualityLimits` helper) over deep hierarchies.

**Relation to Strategy:** Prefer **updater classes that implement a strategy interface**, with a factory/registry — best of both.

---

## 3. Polymorphism (on `Item`)

**Sketch (forbidden form)**

```text
abstract class Item { abstract void update(); }
class AgedBrie extends Item { ... }  // ALTERS Item hierarchy — NOT ALLOWED
```

**Sketch (allowed form: polymorphic wrappers)**

```text
interface UpdatableItem { void update(); Item unwrap(); }
class AgedBrieItem implements UpdatableItem { private final Item item; ... }
// GildedRose wraps Items once, or wraps on the fly each day
```

### Evaluation

| Criterion | Score | Notes |
|---|---|---|
| **SOLID** | **Mixed** | Subclass-per-type is classic OCP **if you own the model**. Here you **do not** own `Item`. Wrapper polymorphism restores SOLID but adds indirection. |
| **Readability** | **Medium** | Domain model looks elegant in textbooks; in this kata it fights the constraint and confuses “what is an Item?” |
| **Maintainability** | **Medium–Low** | Dual representation (`Item` + wrapper) must stay in sync. Construction/factory becomes central complexity. |
| **Extensibility** | **Medium** | New type = new subclass/wrapper + factory mapping — same as Strategy, with more moving parts. |
| **Testability** | **Medium** | Wrappers testable, but end-to-end still mutates goblin `Item`; extra surface to test. |

**Pros:** Pure OOP narrative.  
**Cons:** **Direct polymorphism on `Item` violates the assignment.** Wrapper polymorphism is viable but usually **worse ROI** than Strategy/Updaters for the same behavior. Easy to over-design for an interview exercise.

**Verdict for this kata:** Reject subclassing `Item`. Treat “polymorphism” as satisfied by **polymorphic strategies/updaters** instead.

---

## 4. Rule-based solution

**Sketch**

```text
record Rule(Predicate<Item> when, Consumer<Item> apply) {}

List<Rule> rules = List.of(
  rule(nameEquals("Sulfuras..."), noop),
  rule(isBackstage.and(sellIn < 0), setQuality(0)),
  rule(isBackstage.and(sellIn < 6), increase(3)),  // careful: composition order!
  ...
);
// engine: filter matching rules, apply in order, or first-match-wins
```

Or a data table: `{type, sellInRange, qualityDelta, sellInDelta, caps...}`.

### Evaluation

| Criterion | Score | Notes |
|---|---|---|
| **SOLID** | **Medium** | OCP possible (add rules). SRP can blur: engine + rule ordering + conflict resolution become one complex unit. DIP depends on how rules are registered. |
| **Readability** | **Low–Medium** | Attractive when rules are truly uniform (±N quality). **Poor fit** when policies differ in kind (Sulfuras freeze, Backstage zeroing, stepwise caps, Brie double after expiry). Order of rules becomes tribal knowledge. |
| **Maintainability** | **Low–Medium** | Subtle bugs from rule precedence (Backstage +3 vs +2 vs expiry 0). Harder for newcomers than named classes. |
| **Extensibility** | **Medium** | Simple delta items (Conjured −2/−4) extend easily; special lifecycle events do not. |
| **Testability** | **Medium** | Engine needs thorough tests; individual rules are small but **interactions** need combinatorial coverage. Characterization suite becomes more critical, not less. |

**Pros:** Compact for homogeneous tax/fee engines; can be data-driven later.  
**Cons:** Gilded Rose policies are **heterogeneous**. A rule engine often recreates the original nested conditionals as an ordered list — complexity moves, clarity does not improve.

---

## Side-by-side summary

| Criterion | Strategy | Item Updaters | Polymorphism on `Item` | Rule-based |
|---|---|---|---|---|
| SOLID | Excellent | Excellent (if base stays thin) | Blocked / awkward via wrappers | Fair |
| Readability | Excellent | Excellent | Poor under constraint | Fair–Poor |
| Maintainability | Excellent | Excellent | Fair | Fair |
| Extensibility (Conjured) | Excellent | Excellent | Fair | Good for deltas only |
| Testability | Excellent | Excellent | Fair | Fair |
| Fits `Item` constraint | Yes | Yes | **No** (direct) | Yes |
| Over-engineering risk | Low–Medium | Low–Medium | High | Medium–High |

---

## Recommendation for a production Java application

### Best approach: **Strategy interface + per-type Updater classes + factory/registry**

Use **(1) and (2) together**:

1. Define a small interface, e.g. `ItemUpdateStrategy { void update(Item item); }`.
2. Implement one class per category (`NormalItemUpdater`, `AgedBrieUpdater`, `SulfurasUpdater`, `BackstagePassUpdater`, later `ConjuredItemUpdater`).
3. Resolve via an explicit map/factory keyed by **exact item name** (default → Normal).
4. Share **composition helpers** for quality floor/ceiling and sell-in ageing — not a heavy inheritance tree.
5. Optionally use a **package-private template** (quality → sellIn → expired) only if it stays identical across types; Sulfuras/Backstage expiry differences mean prefer explicit `update()` per class over a clever base.

### Why this wins in production

- Aligns with how JVM teams already structure policy code (payments, pricing, shipping).
- Satisfies **OCP** for Conjured without touching other policies.
- Preserves the **goblin `Item`** contract (in-place mutation).
- Maximizes **readability** for code review and onboarding.
- Gives **unit-testable** units plus existing characterization tests as a contract suite.
- Avoids rule-engine precedence bugs and forbidden `Item` hierarchies.

### What to avoid in production here

| Avoid | Reason |
|---|---|
| Subclassing `Item` | Assignment constraint; brittle shared ownership. |
| Full rule engine / DSL | Heterogeneous policies; precedence complexity; weaker readability. |
| One mega-`switch` left in `GildedRose` after “refactor” | Still OCP-hostile; only a slight improvement on nesting. |
| Deep abstract updater hierarchy | Inheritance coupling; prefer interface + helpers. |

### Suggested rollout (risk-aware)

1. Keep characterization tests green.  
2. Extract shared quality helpers (behavior-neutral).  
3. Introduce interface + `NormalItemUpdater`; route only normal items.  
4. Peel off Brie, Sulfuras, Backstage one commit each.  
5. Add `ConjuredItemUpdater` last with new tests.

That sequence keeps production risk low while landing on the recommended architecture.
