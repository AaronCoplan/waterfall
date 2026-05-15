# Waterfall — Quality Review (Task #6)

Author: critic
Date: 2026-05-14
Inputs reviewed in full: `01-codebase-audit.md`, `02-pl-landscape.md`, `03-language-design.md`, `04-strategy.md`, `05-adversarial-review.md`.

This is a quality gate, not a re-analysis. I evaluate how the work *reads* and whether the user can act on it Saturday afternoon.

---

## Per-section scorecard

Legend: **S** = Strong, **A** = Adequate, **W** = Weak, **M** = Missing.

Criteria: **Clr** = Clarity, **Con** = Internal consistency, **Ev** = Evidence/citations, **Act** = Actionability, **Comp** = Completeness, **Pri** = Prioritization, **Fit** = Audience fit (single-maintainer hobby project growing up).

### 01 — Codebase audit

| Section | Clr | Con | Ev | Act | Comp | Pri | Fit |
|---|---|---|---|---|---|---|---|
| §1 Capability matrix | S | S | S | S | S | A | S |
| §2 Architecture overview | S | S | S | A | A | A | S |
| §3 Code-quality component walk | S | S | S | S | S | A | S |
| §4 Readonly extension points | S | S | S | S | S | S | S |
| §5 Architectural debt (D1–D10) | S | S | S | S | A | S | S |
| §6 Surprises and worth-knowing facts | S | S | S | A | S | A | S |

Audit is the strongest deliverable in the package. File:line citations are dense and accurate; the capability matrix is reusable as a living spec. Single criticism: §5 lists 10 debt items with no internal priority ordering — D1 and D2 are clearly more load-bearing than D7 and D10, but the doc presents them as a flat list.

### 02 — PL landscape (research)

| Section | Clr | Con | Ev | Act | Comp | Pri | Fit |
|---|---|---|---|---|---|---|---|
| Executive summary | S | S | S | S | A | S | S |
| Part 1 (modern PL summary table) | S | S | S | A | S | A | S |
| Part 2 (transpiled languages, 2.1–2.16) | S | S | S | S | S | A | S |
| Part 2.5 (synthesis / tier framework) | S | S | A | S | A | S | S |
| Part 3 (readonly prior art) | S | A | S | S | A | A | S |
| Part 4 (positioning niches) | S | S | A | A | A | A | A |
| Appendix A (sources) | A | — | S | A | S | — | A |
| Appendix B (what could not be verified) | S | S | A | S | A | — | S |

Strongest piece is Part 2's per-language case studies — each has a "lessons for Waterfall" callback that ties to the user's question. Part 3's "I could find no language with this exact construct" is appropriately hedged. Two concerns: (a) the 2026 dates in many sources read as forward-projected — they're checkable but the reader has no way to distinguish "I read this" from "this is the projected state"; (b) Part 4 hands three niches to the strategist with no weighting, where a partial recommendation would have helped.

### 03 — Language design

| Section | Clr | Con | Ev | Act | Comp | Pri | Fit |
|---|---|---|---|---|---|---|---|
| §0 TL;DR | A | A | A | A | A | A | A |
| §1.1 Records | S | S | S | S | S | S | S |
| §1.2 Unions | S | S | S | S | A | S | S |
| §1.3 Pattern matching | S | A | S | S | A | S | A |
| §1.4 Generics | S | S | S | S | A | S | A |
| §1.5 Modules + import | S | S | S | S | A | S | S |
| §1.6 Result + `?` | A | W | A | A | W | S | A |
| §1.7 Strings | A | A | A | A | W | S | A |
| §1.8–1.13 (T2 items) | A | S | A | A | A | S | A |
| §2a Readonly semantics | A | W | A | A | W | S | A |
| §2a.5 Branch joins | A | A | A | A | W | A | A |
| §2a.10 Lambda captures | W | W | W | W | W | A | W |
| §2b Grammar deltas | S | A | S | S | A | S | S |
| §2c Symbol-table changes | S | W | S | S | A | S | A |
| §2d Verifier algorithm | A | W | A | W | W | A | A |
| §2e Backend lowering | A | W | A | S | A | S | S |
| §2f Worked snippets (1–12) | S | A | S | S | A | S | S |
| §2g `const`/`imm` migration | A | A | A | A | A | A | W |
| §3 Cross-cutting principles | S | S | A | A | A | A | S |
| §4 `@external` model | S | S | S | S | A | S | S |
| Appendix A (grammar diff) | S | S | S | S | A | — | S |
| Appendix B (critical path) | A | S | S | S | A | S | S |

Largest doc; uneven quality. Sections 1.1–1.5 and §4 read like a spec. Section 2 is where it weakens: §2a.5 has a self-acknowledged "actually, intersection and pessimistic are both sound for reads" passage that the skeptic correctly caught — and §2a.10 (lambda captures) contradicts the skeptic's view that the lambda body cannot even contain assignments in v1 grammar. §2e visibly contradicts itself within 5 paragraphs about whether to emit a comment for Form B (called out as F7 in the skeptic review). §2c+§2d sketch a destructive-mutation `markReadonly` that won't correctly support intersection rollback (skeptic's F8).

### 04 — Strategy and roadmap

| Section | Clr | Con | Ev | Act | Comp | Pri | Fit |
|---|---|---|---|---|---|---|---|
| §1 Tier A/B/C framework | S | S | A | S | S | S | S |
| §2 Niche evaluation (4 candidates) | S | S | A | S | S | S | S |
| §3 Phased roadmap (P10–P17) | S | A | A | A | A | S | A |
| §4 Risk register (R1–R7) | S | S | A | A | A | S | S |
| §5 Non-goals (NG1–NG10) | S | S | A | S | S | A | S |
| §6 Marketing / community story | S | S | A | A | A | A | S |
| §7 Open questions for Aaron (Q1–Q7) | S | S | — | S | S | S | S |
| Closing thought | S | S | — | A | A | A | S |

Strongest single piece is §7 — the seven yes/no questions are the cleanest "what does Aaron decide?" handoff in the whole package. Roadmap (§3) has the most actionability per page but undersizes P10's cost (skeptic's F16) and the calendar-vs-effort math (F17). Risk register (§4) is well-written but R1 mitigation depends on a contributor pipeline the niche choice doesn't deliver (F18). The teaching-niche → recruiting-users path is hand-wavy (F15) — strategy commits to a positioning that requires a teaching artifact (a book or video series) that the roadmap doesn't budget for.

### 05 — Adversarial review

| Section | Clr | Con | Ev | Act | Comp | Pri | Fit |
|---|---|---|---|---|---|---|---|
| Summary table | S | S | — | S | S | S | S |
| F1–F10 (readonly findings) | S | S | S | S | S | S | S |
| F11–F14 (other T1 design) | S | S | S | S | A | S | S |
| F15–F21 (strategy) | S | S | A | S | A | S | S |
| F22–F23 (omissions) | S | S | A | S | A | A | S |
| Mike-Test | S | S | — | S | S | S | S |
| What the team got right | S | S | — | A | S | S | S |
| Overall assessment | S | S | — | S | A | S | S |

Best-written deliverable in the package. The Mike-Test is the single most valuable section across all five docs — it forces the user to read the language *as the user* would. Each finding has section reference, severity, what-the-claim-is, what-the-problem-is, and a suggested fix — the most actionable structure in the package. Minor: F19 (AI-assist effect on indie language economics) is a more interesting observation than F22 (strings-immutability), which is rare in a skeptic doc — the severities are calibrated reasonably.

---

## Section A — Cross-document consistency

**Where the five docs reinforce each other well:**

1. **Audit's debt items (D1–D6) → designer's symbol-table redesign (§2c) → strategist's P10 (foundation refactor).** This is the cleanest cross-document thread in the package. The audit names the debt with file:line evidence; the design treats it as the foundation; the roadmap front-loads it. All three docs use the same vocabulary (`Any?`, `lookup`, `SymbolInfo`).

2. **Research's "legitimacy bar" (Part 2.5) → design's T1 list → strategy's P12/P13.** The research enumerates required items (LSP, package manager, spec, friendly errors, target-divergence story, semantic value-add). The design's §1 catalog covers all of them. The roadmap's P13 is explicitly the "legitimacy bar" phase. Internal references are consistent.

3. **Research's Gleam comparison → designer's `@external` (§1.9, §4) → strategist's R2 mitigation.** All three converge on Gleam's expression-level model over Haxe's `#if`. No conflict here.

**Where the docs conflict or partially miss:**

1. **Phase-ordering vs feature-dependency mismatch (mild).** Strategist's P11 (type inference + condition checking) sits between P10 (refactor) and P12 (sum types + match). But designer's pattern matching (§1.3) needs type-of-each-arm to compute exhaustiveness and arm-body return type — that's part of the same type system P11 is supposed to deliver. Designer's Appendix B sequences P11 work *during* P12 ("type-inference fills in later"). This is a strategist-vs-designer ordering nit, but it's an actual ambiguity for the implementer.

2. **`Result<T, E>` special-casing depth.** Design §1.6 says `Result` is "special-cased for v1" because generic unions are Tier 2. Strategy P12 says "Sum types + pattern matching + `Result`-style errors" are pivotal in P12 *with no mention of the special case*. The skeptic flagged this in F12 — what types is `Result` parameterized over? Are records allowed? The strategy treats `Result` as solved; the design admits it's underspecified. A reader synthesizing the two will not know which is right.

3. **`const`/`imm` deprecation (cross-doc churn).** Design §2g recommends deprecate-`const`-and-`imm`-rename-to-`readonly`. Strategy doesn't endorse or reject this; it leaves it implicit. Skeptic F11 argues against deprecation (use `const` for Form A and `readonly` for Form B). This is unresolved across the package. The user needs a single recommendation, not three.

4. **Lambda captures.** Design §2a.10 says all lambda captures are implicitly readonly. Skeptic F1 calls this FATAL because (a) v1 lambdas can't write anything anyway and (b) when multi-statement bodies land in P12, this rule contradicts every developer's mental model. Strategy P12 mentions "multi-statement lambda bodies" without flagging that landing them activates the F1 issue. This is a real timebomb embedded in the roadmap.

5. **Three-year timeline.** Strategy claims v1.0 in 3 calendar years. Skeptic F17 compares to peer indie languages (Gleam: 8 years; Crystal: 7; Nim: 11; Zig: 11+) and argues 5-7 years is realistic. The two timelines aren't reconciled. If synthesis presents the 3-year version without F17's adjustment, the user will plan against an unrealistic schedule.

6. **Teaching niche → contributor recruitment.** Strategy R1 mitigation is "attract a second contributor at P12; sum types are a fun feature to attract one on." Skeptic F18 points out that the teaching niche attracts students (not contributors) — these are different demographics. Strategy doc has no fallback if R1 mitigation fails because of niche-vs-contributor mismatch.

**Where skeptic findings should be addressed inline vs. deferred:**

- **Address inline before the user reads** (synthesis must reflect these or the docs are misleading): F1 (lambda captures soundness gap), F3 (branch-join nested-if cases), F8 (destructive markReadonly mutation), F11 (`const`/`imm` deprecation reversal), F17 (timeline reality check).
- **Address as flagged future work** (no need to rewrite, but the user needs the heads-up): F12 (`Result` parameterization), F13 (`@external` side effects), F14 (`Mod::fn` breaking change in v1), F15 (teaching artifact commitment).
- **Defer / monitor** (lower urgency, real but not blocking): F2 (intersection rule justification rewording), F4 (loop break/continue future), F5–F9 (spec polish), F16 (P10 cost), F18 (R1 mitigation), F19–F23 (gaps the user can act on after v1 scoping).

---

## Section B — The single biggest weakness across the package

**The package treats `readonly` as solved but the design contains two unresolved internal contradictions plus one timebomb that the strategy doesn't disarm.**

Concretely: design §2a.10 (lambda captures) and §2e (Form B comment emission) both contradict themselves within the same section. Then design §2c's `markReadonly` mutates parent scopes destructively in a way that §2d's intersection-rollback cannot undo, which the skeptic's F8 spells out. The strategy schedules multi-statement lambda bodies for P12, which is precisely when §2a.10's defect becomes user-visible (skeptic's F1). The user is about to plan an 18-month investment whose pivotal feature has a spec that contradicts itself and an implementation sketch that won't compile correctly.

This matters more than the timeline or niche issues because the `readonly` feature is the project's headline differentiator. Every other weakness in the package can be patched after the user reads; this one undermines the user's ability to act on `readonly` until the spec text is fixed.

---

## Section C — Three concrete polish items

Ranked by user-value impact.

### C1 — Rewrite design §2c + §2d to spec the symbol-table's transactional semantics for branch verification.

**Where**: `03-language-design.md`, §2c (lines 1414–1465) and §2d (lines 1519–1556).

**Concrete suggestion**: Replace the destructive `markReadonly` API with a *scoped* model. Under the new model: `markReadonly` inside a branch's child scope mutates *only* the child scope's entry (a shadow record), not the parent's. At join time, the verifier explicitly computes the intersection and *promotes the parent scope's binding only when every reaching predecessor promoted it*. Add a worked code snippet showing snapshot-then-restore around branch verification. Cite skeptic F8 as the source. Without this rewrite, the implementer will start P12 with a spec that produces wrong answers on the first nested-if test.

### C2 — Split §2a.10 into two sections and downgrade the "novelty" framing.

**Where**: `03-language-design.md`, §0 (line 20) and §2a.10 (lines 1240–1278); strategy `04-strategy.md` §6 elevator pitch (line 449).

**Concrete suggestion**: Section §2a.10 currently solves two different problems with the same rule ("implicit readonly capture") and doesn't notice the conflation. Split into (a) "v1 lambda capture semantics — vacuous since lambda bodies are one functionCall," explicitly NOT-YET-RESERVED for the multi-statement case; and (b) "when multi-statement bodies arrive in P12, this section will be revisited — the current proposed rule will be reconsidered against Kotlin/Rust/JS user expectations." In §0, downgrade "Form B is a Waterfall-only compile-time concept" to "Form B is a one-line shorthand for Rust's shadowing-and-rebinding idiom, applied without a fresh binding." In strategy §6, ensure the elevator pitch doesn't lean on novelty — the strategy doc already mostly avoids this, but a sentence reaffirming "ergonomic improvement, not a category creator" would close the loop.

### C3 — Add a one-paragraph timeline reconciliation in strategy §3.

**Where**: `04-strategy.md`, §3 (lines 318–333) and Q2 (lines 467–469).

**Concrete suggestion**: After the roadmap summary table, insert a paragraph explicitly distinguishing "effort quarters" (full-time-equivalent) from "calendar quarters" (side-project pace), with a worked conversion: 7 effort-quarters × 2–3x calendar-stretch = 14–21 calendar months *if a side-project pace is steady*, more if interrupted. Then redefine the 3y / 5y / 10y milestones in §1 as v0.x / v1.0 / v1.x rather than v1.0 / production-user / surveys. The current §1 milestones bake in optimism the rest of the doc can't deliver. Skeptic F17 already worked out the math; just import it.

---

## Section D — Readiness gate

| Doc | Verdict |
|---|---|
| 01 — Codebase audit | **READY** |
| 02 — PL landscape | **READY** |
| 03 — Language design | **NEEDS-EDITS** |
| 04 — Strategy and roadmap | **NEEDS-EDITS** |
| 05 — Adversarial review | **READY** |

### 03 — NEEDS-EDITS (not NOT-READY)

The design doc is structurally sound but has the three issues called out in Section C. Specifically: §2c+§2d's destructive-mutation bug (must fix before P12 implementation), §2a.10's conflated lambda-capture rule (must clarify before users build a mental model), and §2e's self-contradictory comment-emission decision (must pick one). None of these are big rewrites — each is a section-level edit and could be done in an afternoon. The rest of §1 (records through @external) is genuinely strong. **Recommend: edit, then ship.**

### 04 — NEEDS-EDITS (not NOT-READY)

Strategy doc is opinionated and useful, but has two issues that materially affect user decisions: (a) the 3-year-to-v1.0 timeline is presented without the effort-vs-calendar math, which would land the realistic target at 5–7 years; (b) R1 (bus factor) mitigation depends on attracting contributors from a niche (teaching) that historically supplies students. Both are paragraph-scale fixes — the structure is right, the calibration is off. **Recommend: edit Section 1 milestones and Section 4 R1 mitigation, then ship.**

---

## Section E — Synthesis recommendation

The team lead is about to merge these five documents into a single user-facing deliverable. Here is the order I would synthesize in, what to lead with, and what to defer or drop.

### (a) Ordering of synthesis sections

1. **Lead — the decision the user must make** (Q1–Q7 from strategy §7, recast as the spine). Frame the document around "here are the seven decisions; here is the recommendation for each; here is the evidence."
2. **The recommendation** (Tier B + teaching niche + Gleam playbook + bundle Form A and Form B). One page. No reasoning yet.
3. **Why these recommendations** (synthesis of strategy §1, §2; landscape's Tier framework from Part 2.5). Two pages.
4. **What you'd ship by phase** (strategy §3, P10–P16 with calibrated timeline). Two pages.
5. **The headline feature: `readonly` Form A and Form B** (design §0 + §2a, *post* the edits from Section C above). Two pages, end with a "what could go wrong" callout pulled from skeptic F1/F8.
6. **The legitimacy bar features** (design §1.1–1.9, summarized; lean on the priority table at §1 summary). Two pages.
7. **Open risks and decisions to defer** (strategy §4 R1–R7; design §2a.7/§1.6/§1.9 tensions). One page.

### (b) What to lead with

**Lead with the Mike-Test from the skeptic doc.** It's the single page in the package that puts the user inside a Tier-B 2027 user's head and shows them exactly where the project will succeed or fail. It's concrete; it's grounded; it's the only piece of writing in the package that competes with prose (which the strategy correctly notes is the teaching-niche battlefield). Reading the Mike-Test first will calibrate every later decision.

Right after the Mike-Test, the seven Q1–Q7 questions. That's the user's job, restated as a checklist. Everything else is supporting material.

### (c) What to keep but bury in appendices

- **Audit §1 capability matrix** — load-bearing as reference, distracting in the main body. Appendix A.
- **Audit §3 component-by-component code review** — useful when implementing P10, irrelevant for the Saturday-afternoon read. Appendix B.
- **Landscape Part 2 (per-language case studies)** — these are excellent reading but 16 of them; collapse to a 3-language deep-dive (Haxe, Gleam, CoffeeScript) in the main body, rest in Appendix C.
- **Design §2f (worked snippets)** — the 12 snippets are great for the implementer but a wall to the user; pull two (Snippet 1 and Snippet 8) into the main body, rest in Appendix D.
- **Skeptic F11–F23** — the strategic findings are part of the main body; the design polish findings (F7, F8, F9) go to Appendix E.

### (d) What to drop entirely

- **Audit §6 "Surprises" — most are implementation trivia.** Keep #2 (legacy is C++-flavored, default), #6 (STRING_LITERAL→char bug), and #16 (JVM 1.8 target) — the rest is for the maintainer's working notebook, not the synthesis.
- **Design §2g (`const`/`imm` migration plan).** Skeptic F11 argues persuasively against the unification. Recommend the synthesis adopts the F11 stance (keep `const` for Form A, introduce `readonly` for Form B) and drops the migration ceremony entirely. This is one fewer thing for the user to decide.
- **Strategy NG1–NG10 (non-goals).** Useful for the strategist's discipline; not useful for the user's first read. Cite them inline (e.g., "this is not a JVM language") only where the user might assume otherwise.
- **Skeptic F19 (AI-assist effect) and F20 (AI-codegen as user)** — interesting observations but speculative; the synthesis should not commit to a 2026 AI-economic stance in a 5y plan.
- **All Appendix sources lists.** The user doesn't need them in the synthesis; the originals remain in `notes/team-output/` for verification.

---

## Final verdict

**Ship the package with edits.** The audit and landscape research are ready as-is; the design doc and strategy doc need the section-level fixes called out in Section C; the skeptic review is the strongest piece and should drive synthesis ordering. None of the deliverables is NOT-READY. The single biggest weakness — the `readonly` spec contradictions that activate in P12 — is fixable in an afternoon of focused editing.

If the team lead must choose between (a) shipping the package now with the contradictions present and (b) delaying for one editing pass, **delay for the editing pass**. The user is about to make a multi-year decision on a feature whose spec contradicts itself; one more day of work prevents months of rework downstream.
