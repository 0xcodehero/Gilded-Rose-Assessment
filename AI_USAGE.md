# AI Usage Report — Gilded Rose Assessment (PTV Logistics)

This document describes how AI (Cursor / Composer) was used during the Gilded Rose refactoring exercise. It is intended for interview discussion: what the AI contributed, what was verified by a human, and where judgment overrode automation.

**Primary tooling:** Cursor agent (Composer), local Maven/JUnit 5, Git.  
**Language chosen:** Java (aligned with PTV’s JVM-oriented stack).  
**Constraint upheld throughout:** do not alter `Item`; preserve existing behavior as the source of truth.

---

## 1. How AI was used

AI was treated as a **pair-programming and documentation accelerator**, not as an unsupervised code author. Work followed a deliberate sequence:

| Phase | AI role | Human role |
|---|---|---|
| Requirements analysis | Draft behavior tables from requirements + starter `updateQuality` | Confirm code-as-oracle over ambiguous prose |
| Decision table | Turn analysis into testable rows | Accept as test design input |
| Implementation review | Catalogue smells / SOLID / risk | Use as refactor rationale; no premature edits |
| Characterization tests | Generate JUnit 5 suite from decision table | Run suite; treat failures as AI error until proven otherwise |
| Test review | Critique gaps in the suite AI just wrote | Prioritize which gaps mattered before refactoring |
| Design comparison | Compare Strategy / Updaters / Polymorphism / Rules | Select Strategy + updaters as target; reject Item subclassing |
| Refactoring plan | Break target into small commits | Execute only requested steps |
| Incremental refactor (Steps 1–3) | Apply mechanical extractions | Gate every step on `mvn test` |
| Conjured feature | Tests-first, then implementation | Lock −2/−4 interpretation; choose prefix matching |
| PR-style review | Adversarial critique of the whole solution | Capture merge risks and interview talking points |

AI also produced supporting artefacts under `docs/` (analysis, decision table, reviews, plan, approach comparison) and this usage report.

**Not delegated to AI without review:** product interpretation of Conjured rates, final architecture choice, commit narrative for the interview, and any change that could alter characterized behavior.

---

## 2. Prompts used (representative)

Prompts were scoped and sequential. Typical instructions included explicit constraints such as “do not modify production code,” “existing behavior is the source of truth,” and “implement only Step N.”

### Analysis & design

1. Analyze Gilded Rose requirements; produce a behavior specification table for all item types; call out ambiguities and insist existing code is the oracle.
2. Convert requirements into a decision table (condition → expected behavior → quality/sellIn deltas) usable for unit tests.
3. Review the current implementation without changing code: smells, long methods, nesting, duplication, SOLID, refactoring risk.
4. Compare Strategy, Item Updater classes, Polymorphism, and rule-based designs against SOLID / readability / maintainability / extensibility / testability; recommend for production Java.
5. Create an incremental refactoring plan with objective, changes, risks, and benefit per step; keep tests green; end state must make Conjured cheap to add.

### Tests

6. Generate characterization tests only (JUnit 5); cover current categories and edge cases; explain what each test protects; do not change production code.
7. Review those characterization tests for missing cases, weak assertions, and confidence gaps.

### Implementation

8. Implement **only** Step 1 (name constants); no behavior change; explain every modification.
9. Implement **only** Steps 2 and 3 (quality helpers; named dispatch methods).
10. Implement Conjured: tests first → explain design → implement → verify existing tests still pass. Rules: twice as fast as normal; leave other behavior unchanged.

### Critique

11. Staff-engineer PR review of the entire solution: bugs, hidden assumptions, design flaws, overengineering, naming, architecture, testing gaps — be critical.

Where useful, prompts referenced prior artefacts (`docs/decision-table.md`, `docs/refactoring-plan.md`) so the agent stayed aligned with decisions already made.

---

## 3. How suggestions were validated

| Validation method | When applied |
|---|---|
| **Cross-check against starter `GildedRose.updateQuality`** | All requirements and expected values; prose never overruled code for pre-Conjured types |
| **`mvn test` after each production change** | Steps 1–3 and Conjured; exit code 0 required before proceeding |
| **Fail-first for Conjured** | New tests run before feature code; observed −1/−2 (normal) vs expected −2/−4 |
| **Boundary reasoning** | Backstage thresholds (`11`/`10`/`6`/`5`/`1`/`0`) traced against pre-decrement `sellIn` |
| **Adversarial self-review** | Explicit PR-review prompt to surface assumptions the happy-path work might miss |
| **Consistency with own plan** | Step prompts limited scope so the agent could not “helpfully” jump to Strategy/factory early |

Validation principle: **green tests are necessary but not sufficient**. Interpretations (especially Conjured) also needed an explicit human product decision.

---

## 4. What suggestions were accepted

| Suggestion | Why accepted |
|---|---|
| Treat existing implementation as behavioral oracle | Matches assignment brief; avoids “fixing” correct legacy quirks |
| Decision-table → parameterized/ nested JUnit characterization suite | Direct mapping from spec rows to assertions |
| Extract `ItemNames` constants (Step 1) | Zero behavior risk; single source of truth for exact strings |
| Extract `QualityAdjuster` with floor/ceiling guards (Step 2) | Removes duplication without changing semantics; no clamp-down of out-of-range values |
| Positive dispatch + private `update*` methods (Step 3) | Removes nested negated conditionals; prepares later extraction |
| Conjured = 2× normal structurally (−2 before sell-by, −4 after) | Parallel to normal’s −1/−2; easiest to explain and test |
| `startsWith("Conjured")` category match | Matches “supplier of conjured items” as a category; covers `Conjured Mana Cake` from TextTest fixture |
| Extensive `docs/` trail (analysis, plan, reviews) | Supports interview discussion and requested AI artefacts |
| Critical PR review findings (incomplete Strategy, −2/−4 ambiguity, dispatch order) | Honesty about residual risk is preferable to overselling the design |

---

## 5. What suggestions were rejected (or deferred)

| Suggestion / temptation | Decision | Rationale |
|---|---|---|
| Re-implement behavior from markdown requirements alone | **Rejected** | Would drop Aged Brie +2 after expiry and other code-only rules |
| Subclass or modify `Item` | **Rejected** | Explicit goblin constraint |
| Full rule engine / DSL | **Rejected** | Heterogeneous policies; precedence complexity for little gain |
| Jump straight to Strategy + factory before characterization | **Deferred / partially skipped** | Plan required tests-first and small steps; only Steps 1–3 were executed before Conjured |
| Clamp quality into `0..50` on every update | **Rejected** | Starter code does not clamp existing out-of-range values; would change behavior |
| Always −2 for Conjured (no post-expiry doubling) | **Rejected** (for this solution) | Less consistent with normal-item structure; −2/−4 chosen deliberately — still noted as product-ambiguous in review |
| Exact-name-only Conjured (`"Conjured Mana Cake"` solely) | **Rejected** in favor of prefix | Category wording in requirements; documented as a conscious trade-off |
| AI changing expected values to make tests pass | **Rejected by process** | Expectations derived from oracle code / decision table first |
| Mixing Conjured feature into refactor commits | **Rejected** | Feature kept separate from behavior-preserving refactors |

**Known gap called out in review:** the written target architecture (Strategy + factory) was **not fully implemented**. That is recorded as incomplete work, not as “AI finished the design.” Shipping Conjured via another private method was a **scope/time trade-off**, not the ideal end state described in `docs/refactoring-plan.md`.

---

## 6. Testing strategy

### Layering

1. **Characterization (legacy types)** — `GildedRoseCharacterizationTest`  
   Locks Normal, Aged Brie, Sulfuras, Backstage before and during refactor. Expected values come from the starter implementation.

2. **Feature tests (Conjured)** — `ConjuredItemTest`  
   Added red-first; covers before/after sell-by, floor cases, and a second Conjured-named item for prefix matching.

3. **Regression gate** — full `mvn test` after every production edit.

4. **Manual / demo fixture** — `TexttestFixture` still useful for multi-day eyeballing; not yet automated as an approval test (gap noted in review).

### What the suite optimizes for

- End-of-day state only (`sellIn`, `quality`), not internal call order.
- Boundaries that historically break refactors (Backstage bands, sell-by day `0 → -1`, quality floor/ceiling).
- Separation of “do not break the past” vs “specify the new feature.”

### Known testing limitations (intentional honesty)

- Several Priority-A gaps from `docs/characterization-test-review.md` were identified but not all closed before Conjured (e.g. `sellIn == 1` for Normal/Brie, richer Backstage cap matrix, multi-day golden master).
- Weak spots: limited assertion on `name` identity; little coverage of dispatch precedence; Conjured not yet in a mixed multi-item cart test.
- These are follow-ups, not hidden failures.

---

## 7. Human decision-making process

Decisions were made in this order of authority:

1. **Assignment constraints** — `Item` immutable; existing behavior correct; AI artefacts welcome; prefer JVM/C#.
2. **Code oracle over prose** — when requirements were silent or conflicting, starter `updateQuality` won.
3. **Risk before structure** — characterize → small refactors → feature.
4. **Architecture intent vs delivery** — Strategy + updaters chosen as the *target*; only the early plan steps were executed before Conjured because the interview exercise also values incremental delivery and a working feature.
5. **Explicit product calls on ambiguity**
   - Conjured degrade rate: **−2 / −4**
   - Conjured identity: **prefix `Conjured`**
6. **Adversarial pass** — after implementation, invite a critical review and keep unfavorable findings in the record rather than polishing them away.

### Decision log (concise)

| Decision | Owner | Outcome |
|---|---|---|
| Java for the kata | Human | Matches PTV day-to-day environment |
| Behavior oracle = starter code | Human + AI analysis | Documented in `docs/requirements-analysis.md` |
| Target design = Strategy/updaters | Human, informed by AI comparison | Documented; not fully coded |
| Execute Steps 1–3 only before feature | Human (scoped prompts) | Constants, helpers, named methods |
| Conjured −2/−4 + prefix match | Human | Implemented + tested; ambiguity acknowledged in PR review |
| Leave Strategy/factory unfinished | Human trade-off | Called out as design debt / interview topic |

---

## 8. Artefacts for reviewers

| Artefact | Purpose |
|---|---|
| This file (`AI_USAGE.md`) | How AI was guided, validated, and overruled |
| `docs/requirements-analysis.md` | Behavior + ambiguities |
| `docs/decision-table.md` | Testable specification |
| `docs/implementation-review.md` | Pre-refactor critique of starter code |
| `docs/characterization-test-review.md` | Critique of the test suite |
| `docs/refactoring-approaches.md` | Design options and recommendation |
| `docs/refactoring-plan.md` | Incremental commit plan |
| Cursor session / chat transcript | Full prompt–response history (available in the development environment) |
| Git history | Stepwise commits for refactor and feature (when committed for submission) |

---

## 9. Interview summary (30 seconds)

AI accelerated analysis, test drafting, and mechanical refactors under tight prompts. Every behavioral claim was checked against the starter implementation or `mvn test`. Ambiguous Conjured rules were resolved by an explicit human choice (−2/−4, prefix match), and an adversarial review was used to document remaining design debt (unfinished Strategy extraction, matching/policy assumptions). The goal was not “AI wrote the kata,” but **AI-assisted engineering with accountable human judgment**.
