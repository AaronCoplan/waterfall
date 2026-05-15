# Waterfall — Operational Execution Playbook

Companion to `04-strategy.md`. The strategy answers *what + why*; this answers *what do I do tomorrow morning, and how do I know I'm on track*. The single dominant risk this playbook is built around is **R8 / failure-mode #1: "tests pass, code is wrong."** Every section reflects that.

Read once before P10 starts. Reread §1 + §6 weekly.

---

## §1 — The spec-first loop, operationalized

The single highest-leverage workflow Aaron has. Every non-trivial change runs through it.

**The loop** (one cycle per feature, not per phase):

1. **Open** Claude Code in plan mode against the relevant `SPEC.md` (e.g., `notes/PHASE-10-design.md` for P10 sub-tasks).
2. **Ask** the agent to draft a plan for the feature. Do not approve.
3. **Read** the plan and answer the single load-bearing question: **"What ambiguities did it silently resolve?"** Write them down — even the small ones. Each silent resolution is a candidate bug under failure-mode #4.
4. **Edit the spec** to remove the ambiguities found in step 3. Commit the spec edit as its own diff. Re-plan.
5. **Loop** 2→4 until the agent produces a plan approved without spec edits. Klabnik-style: average 2–3 iterations; ≥3 iterations is the P10 target (per `04-strategy.md` R10).
6. **Exit plan mode and implement.** Then run §3.

**Trip-wires inside the loop:**

| Signal | What it means | Action |
|---|---|---|
| Plan approved on iteration 1 | Spec is too thin OR feature is trivial | If non-trivial, spawn fresh-session skeptic on the spec; demand it find ≥1 ambiguity |
| Plan-mode iterations >5 | Feature is under-specified at a structural level | Stop. Write spec for the feature itself before re-planning |
| Agent asks no clarifying questions across the loop | It is silently resolving | Force it: append "list every assumption you made" to the planning prompt |
| Same ambiguity surfaces in two unrelated plans | Spec has a structural gap, not a wording gap | Edit `SPEC.md` at the section level, not the sentence level |

**Worked example — hypothetical P10 sub-task 5.3 (`verify()` migration).**

Spec ambiguity the loop catches: the plan says "move each `*Data.verify` body into `StatementVerifier.verifyXxx`." Read carefully: what does the plan do with `FunctionImplementationData.verify`, which the spec (§5.3) routes to `ModuleVerifier`? If the plan dumps it into `StatementVerifier` alongside the others, *that's a silent resolution* — the spec routes functions to module-level verification because they introduce names visible across the module. Catch this in plan mode: edit `PHASE-10-design.md` §5.3 to make the routing explicit by *Data class. Re-plan. Now the plan correctly splits the migration.

**Second silent resolution to expect on this same task:** what does the plan do with `VerificationResult` (now deleted)? Per §5.3 it's replaced by `VerifyResult` + `VerifyError`. If the plan says "keep `VerificationResult` for a deprecation cycle," reject — that contradicts §5.3's "delete." Edit spec to add the explicit "no shim, no deprecation cycle" rule. This is the F10 / cross-tree drift hazard.

The discipline is: **the spec is the verifier.** If the spec admits the wrong interpretation, the spec is the bug. Fix the spec, not the implementation.

---

## §2 — Phase rhythm

Each phase has three rituals. Don't skip; don't reorder.

| Ritual | When | Concrete steps | Exit signal |
|---|---|---|---|
| **Phase kickoff** | Day 1 of phase | (a) Write/update `PHASE-Nxx-design.md`. (b) Spawn fresh-session skeptic against the design doc only — instruct it to find ambiguities and missing decisions; require ≥1 material finding before proceeding. (c) Commit the doc (and any skeptic-driven edits). (d) Add an entry to `IMPLEMENTATION-LOG.md`: "phase N started, spec at commit `<sha>`, key open ambiguities: …". | Spec at a known commit; skeptic-pre-review finished and findings either resolved in spec or explicitly noted as accepted. |
| **Mid-phase checkpoint** | ~Week 1 of any multi-week phase (P10, P11.5, P12, P13, P14) | (a) Run the full §3 triad against current state. (b) Inventory: did the implementation accumulate any "wait, the spec didn't say" moments? Each = a spec ambiguity that escaped plan mode. (c) Course-correct: either edit spec + revise impl, or accept and log. (d) Update `IMPLEMENTATION-LOG.md` with the mid-phase state. | Triad green OR specific remediation plan in `IMPLEMENTATION-LOG.md`. |
| **Phase exit** | When deliverables complete | (a) Run all three triad legs at production scale (N=10000 properties, full differential oracle, ≥20 adversarial). (b) Walk the phase-exit checklist from `04-strategy.md` §3 line-by-line. (c) Spawn fresh-session skeptic on the diff. (d) Resolve or accept findings. (e) Tag commit `phase-N-complete`. (f) Write retrospective in `IMPLEMENTATION-LOG.md`: actual vs. budgeted weeks, what the triad caught, what surprised, what to carry into next phase. | All triad green + every checklist box checked or explicitly waived with rationale. |

**Phase-kickoff sub-template** (paste into `PHASE-Nxx-design.md` if not already there):

```
## §0 Phase N kickoff
- Strategist roadmap entry: `04-strategy.md` §3, P{N}
- Predecessor phase exit tag: phase-{N-1}-complete @ <sha>
- Pre-review skeptic session: <link to transcript or commit>
- Open ambiguities entering implementation: <list>
- Verification-design commitment (the §3 triad concretely):
  - Property invariants to cover: <list>
  - Differential oracle: <name>
  - Adversarial input target: ≥{N}, location compiler/src/test/resources/adversarial/phase-{N}/
```

**Phase-exit sub-template** (paste into `IMPLEMENTATION-LOG.md`):

```
## Phase N retrospective — {date}
- Calendar weeks budgeted vs. actual: {bud} / {act}
- Triad caught (count): properties {n_prop}, differential {n_diff}, adversarial {n_adv}
- Skeptic findings on exit: {fatal} fatal / {risk} risk / {minor} minor
- Spec edits during phase (count): {n}
- Plan-mode iterations average: {x}
- Carry-forward into phase {N+1}: <list>
- One sentence on what surprised: <text>
```

---

## §3 — The verification triad (operationalized)

Three legs; all three must show *no regressions* before phase exit. From `04-strategy.md` §3 round-3 fix. The triad replaces the round-2 mutation-test gate; it is tool-agnostic and survives tool transitions (R9 mitigation). The disciplines below assume Kotest property arbitrers per the designer's round-4 decision.

### Leg 1 — Property-based tests at N=10000

| Item | Concrete |
|---|---|
| Framework | Kotest property arbitrers, `forAll(N=10000)` default. Lower (N=1000) acceptable only for property checks that compile a generated program and run it through a backend — call this out in the test. |
| Location | `compiler/src/test/kotlin/.../property/` — one file per invariant family. Example: `SymbolTablePropertyTest.kt`, `JoinAnalysisPropertyTest.kt`, `IrLoweringPropertyTest.kt`, `JsonRendererRoundTripTest.kt`. |
| Naming | `propertyName` is a sentence: `"symbol table lookup after declare returns the declared SymbolInfo"`, not `test1`. |
| Coverage per phase | ≥1 invariant per *new* IR node kind, per *new* verifier rule, per *new* backend lowering rule. Per `PHASE-10-design.md` §4.9 (Kotest decision). |
| Per-phase invariants — minimums | **P10:** SymbolTable lookup/declare/shadow invariants (per §2.7 cases promoted to properties), `JoinAnalysis` intersection invariants (per §4.5), `IrLowering` round-trip (parse → IR → emit byte-equiv goldens), `JsonRenderer` round-trip. **P11:** "all well-typed programs that pass verifier produce typecheckable backend output." **P11.5:** "no `private` symbol leaks across modules"; "C runtime output stdout matches JS/Python stdout." **P12:** exhaustiveness on `match`, `Result` chain associativity. **P14:** monomorphization correctness, source-map mapping validity. **P15:** `Vec<T>` and `Map<K, V>` round-trip + ordering invariants. |
| Anti-pattern | "Property test" that asserts a specific output for one input. That's an example-based test wearing a `forAll`. Replace with a true invariant or move to the example-based suite. |

### Leg 2 — Differential testing against an oracle

Each phase **names its oracle**. No oracle = leg failed by construction.

| Phase | Oracle | Mechanism |
|---|---|---|
| P10 | Prior `*Data` AST emission + existing golden test suite | For every example, old AST and new IR produce byte-equivalent emission per backend. Zero golden diffs is the gate. (Per `04-strategy.md` R8 type-(a).) |
| P11 | Aaron-authored hand-checked type-inference cases + existing examples (no semantic change should land in existing goldens) | Aaron writes 20+ cases like `y := add(1, 2)` infers `y: int`; reject any program where inference disagrees with hand-check. (Type-(b).) |
| P11.5 | Cross-target execution consistency: gcc-compiled C output, Node-executed JS output, python3-executed Python output | New for C: `gcc -o exe && ./exe` over the *runtime-verifiable subset* (arithmetic, control flow, recursion, arrays, strings, multi-module — per `PHASE-10-design.md` P11.5 deliverables). Goldens captured under `compiler/src/test/resources/runtime-golden/c/`. (Type-(c).) |
| P12 | Cross-target runtime consistency on `match` + `Result` chains, using P11.5 oracle infra | Same generator drives JS/Python/C; all three must produce identical stdout. |
| P13 | Per-stdlib-function differential vs. native interpreter (Math vs. native Math, String vs. native String, etc.) | Library-author niche cannot tolerate stdlib drift; this is the substrate. |
| P14 | Full publishing pipeline end-to-end: install in fresh containers, run smoke tests, compare across three artifacts | Use scratch Docker containers. Smoke tests are Aaron-authored, not AI-authored. |
| P15 | JS `Array`/`Map` natives + Python `list`/`dict` natives | Property tests on `Vec<T>`/`Map<K, V>` use natives as known-good. |
| P16 | All prior oracles re-run; fuzz at 1M random inputs (no crashes), 1K well-typed (no false-rejects) | Full-spec adversarial review session against `SPEC.md` v1.0. |

### Leg 3 — AI-generated adversarial inputs (≥20 per phase)

| Item | Concrete |
|---|---|
| Ritual | Pre-merge per phase. Fresh Claude session, no prior conversation context. Prompt: "Read `PHASE-N-design.md` and the diff for phase N. Generate 20 .wf programs that should compile (positive) and 20 that should fail with a useful error (negative), specifically targeting the new feature's edge cases and likely silent-resolution traps." |
| Location | `compiler/src/test/resources/adversarial/phase-N/positive/*.wf` and `…/negative/*.wf`. Each negative file has a sibling `.expected-error` with the expected error class. |
| Run | Compile each through the relevant backend. Categorize outcomes: **passed** (good), **false-rejected** (spec bug — fix spec, retest), **compiler-broke** (impl bug — fix impl, retest). |
| Phase exit gate | No `compiler-broke` cases unfixed. Document outcomes in `notes/VERIFICATION-DISCIPLINE.md`. |
| Anti-pattern | Adversarial inputs generated by the same session that wrote the impl. Use a *fresh* session — failure-mode #8 (AI writes, AI reviews) is the dominant pattern this prevents. |

### The discipline rule: who writes what

**Aaron writes the tests; AI does not — applied selectively.** From `04-strategy.md` F3 fix:

- **Aaron writes** property-test invariants (high leverage, one invariant catches many bugs), differential-test oracles (load-bearing — the oracle defines correctness), and the contract tests for verifier-correctness paths (anything in `compiler/src/main/kotlin/.../verifier/` and `…/symboltables/SymbolTable.kt`).
- **AI writes** example-based tests *inside* Aaron-authored invariants and contracts, trivial backend-output tests (golden capture is largely mechanical), and the adversarial-input generation in §3 leg 3.
- **Trip-wire:** any test file under `compiler/src/test/.../verifier/` or `.../symboltables/` whose `git log --follow` shows AI as the sole author is a red flag. Aaron's name must appear in the authorship for verifier-correctness paths.

Weekly time budget for this work: **~8 h/week** of contract-test design + adversarial review (Aaron's hands on keyboard, not AI). If sustainable < 8 h/week, the timeline is wrong; surface to §6 escalation.

---

## §4 — The first 100 hours

Literal, ordered task list. After ~hour 40 the §2 phase rhythm takes over and the list becomes "execute P10 sub-tasks 5.1 → 5.6 per spec."

### Hours 1–10 — pre-P10 cleanup

| # | Task | Output |
|---|---|---|
| 1 | Drop the legacy backend (Q5 answer) | Delete `LegacyTextBackend.kt`, its goldens, and the `legacy` target registration; update `Backends.DEFAULT_TARGET`. Single commit. |
| 2 | Write `BUS-FACTOR.md` at repo root (R1 mitigation) | ~4 h. Covers: how to cut a release, where the verifier/IR pipeline lives (post-P10), which decisions are reversible, key invariants. |
| 3 | Write/refresh `CLAUDE.md` at repo root | Project conventions, commit format, "always run `./gradlew test` before committing", cross-tree drift rule from F10, AI-augmented disclosure for R9 and §6 narrative. |
| 4 | Stub `SPEC.md` at repo root | Top-level vision + a TOC pointing to per-phase design docs. Living artifact; not v1.0 yet. |
| 5 | Stub `notes/VERIFICATION-DISCIPLINE.md` | Sections for each phase's triad outcomes; empty initially. Filled per phase. |
| 6 | Verifier-enforces-const fix (audit Section 4 Form B blocker if still open) | Confirm verifier rejects assignment-to-const today; add a test if missing. Quick win, low risk. |
| 7 | Gradle T2 sweep (Q12 answer) | Address Gradle 9 deprecation warnings; clean build log. Few hours; the noise reduction pays for itself across the entire build sprint. |

### Hours 10–25 — the P10 design weekend (validate the spec via plan-mode loop)

| # | Task | Output |
|---|---|---|
| 8 | Re-read `PHASE-10-design.md` start to finish | Note any sentence that admits two readings. |
| 9 | Spawn fresh-session skeptic against `PHASE-10-design.md` only | Demand ≥3 material findings. Resolve in spec edits or explicitly accept and log. The `08-adversarial-review-2.md` F6–F12 findings are the floor — verify each is closed or accepted. |
| 10 | Run plan-mode against sub-task 5.1 (introduce `WaterfallType`, `SymbolKind`, `SymbolInfo`) | Average ≥3 iterations to approved plan. If 1 iteration: spec is too thin, loop back to §1 trip-wire. |
| 11 | Same for sub-tasks 5.2–5.6 in turn | Each gets its own plan-mode loop. Do not skip ahead. |
| 12 | Lock `PHASE-10-design.md` at a commit. Tag `phase-10-spec-locked` | This is the artifact every later session reads. From now on, spec edits during P10 are tracked deltas. |

### Hours 25–100 — P10 implementation per `PHASE-10-design.md` §5 (sub-tasks 5.1 → 5.6)

| Hours | Sub-task | Notes |
|---|---|---|
| 25–35 | 5.1 + 5.2 (types + SymbolTable migration) | Highest-error-rate step per PITFALL #12. Klabnik-style commit-by-commit review. Property tests for SymbolTable (§2.7 cases as `forAll`) ship in this window. |
| 35–50 | 5.3 (verifier extraction) | F8 / `commitReadonly` walking semantics — verify the resolution from `08-adversarial-review-2.md` lands. F9 / accumulate-vs-bail — confirm the explicit choice is reflected in both per-statement and driver paths. |
| 50–65 | 5.4 (IR + lowering) | Run differential oracle continuously: any golden change = stop. PITFALL #13 is the dominant hazard. |
| 65–85 | 5.5 (migrate backends to IR, one at a time: JS → Python → C) | One backend per session minimum. After each: run full golden suite filtered to that target. **Mid-phase checkpoint lands here** (§2 ritual). |
| 85–95 | 5.6 (remove old `Translatable.translate`) + adversarial inputs (≥20) | Adversarial inputs run last, on the integrated tree. |
| 95–100 | Phase-exit ritual: triad at production scale, fresh-session skeptic on full P10 diff, retrospective in `IMPLEMENTATION-LOG.md`, tag `phase-10-complete` | Do not skimp on the retrospective — P10's spec discipline becomes the template for every subsequent phase. |

After hour 100, follow §2 rhythm. P11 is budgeted ≤1 week calendar; P11.5 next; etc.

---

## §5 — Signals and dashboards

How to know at week 2 / week 4 / week 8 whether the project is on track.

### The single load-bearing weekly signal

**Did Aaron write at least one verifier-correctness property test or contract test this week?** Track in `IMPLEMENTATION-LOG.md`. **If "no" for 2 weeks running: the project is in R8 territory. Stop feature work; do a full triad design review for the current phase.** This is the dominant tell that the AI-augmented compression has gone fake.

### Leading vs. lagging indicators

| Indicator | Type | Healthy range | What red means | Action when red |
|---|---|---|---|---|
| Spec-stability rate (spec edits per feature plan-mode loop) | Leading | Falling toward 0 over a phase | Rising; same ambiguity types recur | Stop. Restructure SPEC section, not sentences |
| Plan-mode iterations per feature | Leading | 2–3 average; 3–5 OK for novel | 1 (too thin) or >5 (under-specified) | See §1 trip-wires |
| "Aaron wrote a verifier-correctness test this week" | Leading | Yes, every week | No, 2 weeks running | R8 stop-the-line |
| Adversarial-input "compiler-broke" finds per phase | Leading | 1–5 finds (triad working) | 0 finds (theater) or >10 (impl quality slipping) | 0 → make adversarial prompt sharper; >10 → pause phase, restructure impl approach |
| Skeptic-session find rate on phase exit | Leading | ≥30% of phase exits surface ≥1 finding | 0% across 2 phases | Skeptic context is wrong; re-frame |
| Phase calendar slippage | Lagging | ≤25% of phase budget | >25% in a single phase | §6 recovery: explicit re-budget, surface to Aaron, update strategy doc |
| Test-suite growth (tests / week) | Lagging | 5–15 / week in build sprint | <3 / week | Either over-relying on AI tests or feature volume slipping; check which |
| Golden-suite divergence vs. P10 baseline | Lagging | Zero in P10; documented diffs only in P11+ | Undocumented golden change | Stop. Find which feature added it. Was it intentional? |
| Property-test surprises post-merge | Lagging | 0–1 per phase | ≥2 surprises in one phase | The triad missed something; revisit the property invariants and add coverage |

### "On track" by phase

| Checkpoint | Healthy state |
|---|---|
| Week 2 | P10 sub-task 5.2 merged or close. Property test suite seeded. `BUS-FACTOR.md`, `CLAUDE.md`, `SPEC.md`, `notes/VERIFICATION-DISCIPLINE.md` exist. |
| Week 4 | P10 sub-tasks 5.3–5.4 merged. Mid-phase checkpoint done. Differential oracle green vs. existing goldens. |
| Week 8 | P10 complete and tagged; P11 in flight or complete. Triad-leg green rate ≥95% of PRs. |
| Week 12 | P11.5 complete; C execution oracle wired in CI. First niche-fit case study artifact (CRC32 before/after) in cultivation if not yet shipped. |
| Week 16 | P12 complete; sum types + match + `@external` + `readonly` shipping. First case study published per R3 trip-wire. |
| Week 22 | P13 complete (median); LSP, spec, package manager, friendly errors, stdlib coherence all green. |
| Week 25 | P14 complete (median); idiomatic-output polish shipping; seed library on npm + PyPI + C header. |
| Week 28 | v1.0 technical-complete (median). Adoption phase (P16.5) begins. |

If a checkpoint slips by >2 weeks: re-budget the phase explicitly in `04-strategy.md` rather than letting the slip compound silently.

---

## §6 — Recovery playbook

What to do when things go wrong. Each scenario has the same shape: pause, diagnose, fix the underlying cause, then resume.

| Scenario | Recovery |
|---|---|
| **AI session goes off the rails** (wrong direction, ignoring spec, hallucinating APIs) | Cancel the session immediately. Do NOT keep iterating with a confused session — that compounds the wrong context. Re-read the SPEC for the ambiguity it found (it always found one; identify which). Edit spec to remove the ambiguity. Restart in a fresh session. Log the incident: which ambiguity, how caught, what spec edit fixed it. Pattern recognition over weeks tells you which spec sections need structural work. |
| **Triad fails on phase exit** | Do NOT advance. Identify which leg failed: (a) property test → invariant is wrong or impl is wrong; usually impl. Add the failing input as an example test, fix impl. (b) differential oracle → the oracle is right, impl is wrong (almost always — the oracle has been gating the phase the whole way). Fix impl. (c) adversarial input "compiler-broke" → fix the impl bug; consider whether the input shape suggests a missing invariant in leg (a). |
| **Stuck on a spec ambiguity at 11pm** | Don't try to resolve in code. Commit the ambiguity as a `// SPEC-AMBIGUITY:` comment in the relevant spec section. Pick it up the next session with fresh eyes. The fatigue-induced silent resolution is exactly failure-mode #4 in its worst form. |
| **Mutation / property test signal stays red across multiple sessions** | Pause implementation entirely. Do a full skeptic pass on the spec, not the impl — the impl is following the spec, so red signal means the spec is incomplete. Fix spec, then resume impl. |
| **Calendar slipping >25% of phase budget** | Don't extend silently. Explicitly: (a) re-budget the phase in `04-strategy.md` Section 3 table; (b) communicate to anyone tracking the project (case-study cultivation partners, content-marketing cadence); (c) if the slip is at P10 or P13 (the two phases that compound downstream), re-budget all downstream phases too. The honest re-budget preserves trust; silent slippage destroys it. |
| **Spec-writing fatigue** (R1, weeks 1–6) | Do not let it degrade to vibe-coding. If the maintainer cannot face another EARS-notation acceptance criterion, the path is: pause feature work for a session; spawn fresh-session skeptic to *help write* the next spec section (skeptic produces draft, Aaron edits). Pair-with-the-skeptic is sustainable in a way solo spec-writing isn't. |
| **Adversarial-review fatigue** (R1, weeks 8–16) | When skeptic findings start feeling like nitpicks, that's the trip-wire. Pause and reset the skeptic's context: re-prompt with the most recent FATAL/RISK examples from `notes/team-output/08-adversarial-review-2.md` to recalibrate severity. |
| **2-week build gap during P10–P16** (R1 trip-wire) | Sprint has stalled. Investigate root cause: spec ambiguity? motivational? life-event? Don't restart with feature work; restart with a §2 mid-phase checkpoint to re-ground in the current state. |

---

## §7 — PR template

Mirrors the spec-first loop; enforces the discipline at the merge boundary.

```
## Phase: P{N} — {sub-task name}
## Spec reference: notes/PHASE-{N}-design.md §{section}

## Plan-mode loop
- Iterations to approved plan: {count}
- Ambiguities resolved into spec (commit refs): {list with line refs}

## Implementation
{2–4 sentences on what changed and why this PR rather than a different cut}

## Verification triad
- Property tests: {N tests added}, file refs: {paths}
- Differential testing: oracle = {name}; results = {pass / fail with details}
- Adversarial inputs: {N AI-generated}; outcomes = {passed: X / spec-bug-fixed: Y / impl-bug-fixed: Z}; storage = compiler/src/test/resources/adversarial/phase-{N}/

## Aaron-written tests (verifier-correctness paths)
- {list of test files Aaron authored manually}

## Open ambiguities for next phase
- {anything that didn't make this PR's spec edit cycle}
```

**When to enforce strict vs. relax:**

| PR shape | Template enforcement |
|---|---|
| Any change touching `compiler/src/main/kotlin/.../verifier/`, `…/symboltables/SymbolTable.kt`, `…/typesystem/`, `…/ir/`, or any `IrLowering*` | **Strict.** All sections filled in. No exceptions. |
| Backend output change (any change to `.../target/`) | **Strict if it touches goldens; relaxed if mechanical lowering with no golden diff.** Golden diffs require a "why this change is correct" rationale. |
| Documentation, comments, build config, gradle housekeeping | **Skip template.** One-line PR description fine. |
| One-line typo or whitespace fixes | **Skip template.** |
| Type-inference, sum-type, generic, FFI, or module-system features (P11–P15 main work) | **Strict.** These are the phases where silent resolution compounds worst. |

---

## §8 — When to escalate to Aaron

For the team (whether AI agents or future human contributors) executing in Aaron's absence or during his deep-work blocks.

| Decision type | Action |
|---|---|
| Within-spec implementation details, idiomatic Kotlin choices, test-naming conventions, file layout *inside* a package | Decide autonomously |
| Test framework, linter, formatter choices already specified in `PHASE-N-design.md` (Kotest for properties, etc.) | Decide autonomously |
| Choice between two valid implementations both consistent with spec | Decide autonomously; document choice in `IMPLEMENTATION-LOG.md` for traceability |
| Spec ambiguity that blocks implementation | **Pause.** Surface to Aaron with proposed resolution. Do not silently pick one — that's failure-mode #4 |
| Schedule slippage >25% of phase budget | **Pause.** Surface to Aaron with proposed re-budget |
| Discovered architectural debt or scope drift (e.g., an audit D-item that turns out to block the phase) | **Surface to Aaron.** Don't auto-absorb into the current phase; that turns one phase into two and compounds the slip |
| Cross-phase invariant changes (e.g., changing what `SymbolKind` means in a way that breaks P14's planned `GenericFunction` extension) | **Pause.** This is the kind of decision that needs the strategist's view, not the implementer's |
| Adversarial review surfaces FATAL findings | **Pause.** Surface to Aaron with the finding and the proposed fix |
| Strategic direction changes (niche, timeline, tier, target list) | **Always Aaron.** Never autonomous |
| Cost of an adversarial-review or property-test pass feels disproportionate to its value | **Surface to Aaron** with the call (Q9 was "no budget" — discipline is the limit, not money — but disproportionate spend on a single review is still worth a sanity check) |
| External outreach (PR comments on other projects, posts published, case-study coordination) | **Always Aaron.** Project's voice is human |

---

End of playbook. Revise after week 1 of P10 — first contact with reality will surface what doesn't survive. The discipline is the artifact; the document is the discipline made legible.
