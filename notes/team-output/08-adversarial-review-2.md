# Waterfall — Second Adversarial Review (Task #14)

Author: skeptic
Date: 2026-05-14
Inputs read in full: `notes/PHASE-10-design.md` (1922 lines), `notes/team-output/04-strategy.md` (revised, 805 lines), `notes/team-output/03-language-design.md` §2 (revised, ~2400 lines), `notes/team-output/07-ai-augmented-dev-research.md` (509 lines), `notes/team-output/00-FINAL-PLAN.md` (372 lines, stale), `notes/team-output/06-quality-review.md` (234 lines), the codebase (parser, symbol table, statements, backends), and the `.wf` + golden test corpus.

This is the second adversarial pass. Round 1 (`05-adversarial-review.md`) targeted the original 5–7y strategy + the first readonly spec. The team responded with significant pivots: (1) library-author + Gleam-vibe niche, (2) AI-augmented 3.5–5 month build timeline, (3) `readonly` unification flipped back on, (4) modules moved to P11.5, (5) the P10 design doc was written, (6) the transactional symbol-table model from my prior F8 was adopted, (7) F1's lambda-capture issue was correctly downgraded to a P12 deferred decision. This pass evaluates that revised plan.

---

## Summary table

| Area | FATAL | RISK | MINOR |
|---|---:|---:|---:|
| Timeline + verifier-overfitting mitigations | 0 | 5 | 2 |
| P10 design spec | 1 | 6 | 4 |
| Niche pivot + module reorder | 0 | 3 | 2 |
| `readonly` unification (re-flip after F11) | 0 | 2 | 1 |
| `@external` spec readiness | 0 | 2 | 1 |
| Drift vs stale 00-FINAL-PLAN.md | 0 | 1 (synthesis rework, MAJOR) | 0 |
| Missed Q10 / process gaps | 0 | 3 | 2 |
| **Total** | **1** | **22** | **12** |

The single FATAL finding is **F1 (Mutation-testing kill-rate is the load-bearing gate but the strategy doesn't name a tool, doesn't define what counts as a "mutant," and doesn't say what to do when a critical-path mutant requires bypassing the gate).** This kills the most important verification-overfitting mitigation if not closed before P10 starts.

The single most important drift finding is that **00-FINAL-PLAN.md is stale on every load-bearing decision** — niche, timeline, module ordering, readonly unification, P10 design status. Synthesis rework needed is full rewrite, not edit pass.

---

## Findings

### Section A — The compressed timeline and verifier-overfitting mitigations

#### F1 — Mutation-test kill rate ≥80% is the load-bearing R8 gate but is operationally undefined. **FATAL.**

- **Section reference**: `04-strategy.md` R8 (line 615), every phase-exit checklist that cites "mutation-test kill rate ≥80%" (P10, P11.5, P12, P14, P16); `PHASE-10-design.md` §2.7 (test cases) and §6.3 (pre-merge checklist).
- **Claim challenged**: "Mutation testing as a discipline anchor. Per-phase mutation-test kill rate ≥80%. This is the single most reliable signal that verification is real." (04 R8). The strategy uses this as the single most-cited verification metric across the entire plan — it appears in 5 separate phase-exit checklists.
- **The empirical problem (FATAL part)**: The strategy never specifies *which mutation-testing tool to use on this Kotlin compiler*. There are exactly two viable options: **Pitest** (mainstream Java-ecosystem mutation tester, supports Kotlin via JVM bytecode but with known limitations on Kotlin-specific constructs like `data class`, `when` exhaustive matching, sealed-class hierarchies, and `?.` operators), and **Kotlin-specific mutation testers** (none are mainstream as of 2026; `pitest-kotlin-plugin` exists but is experimental and last released years ago). The Waterfall codebase uses *exactly* the Kotlin constructs Pitest handles poorly: `sealed class WaterfallType`, `data class SymbolInfo`, exhaustive `when (kind)`, `?.let { }` everywhere. **Pitest's kill rate on idiomatic Kotlin is structurally lower than on equivalent Java**, and `pitest-kotlin-plugin` is not actively maintained.
- **The semantic problem**: "Kill rate ≥80%" is not well-defined for compiler code in particular. Three mutants of a `when (kind)` exhaustive match in `verifyStatement` that all produce a missing-branch error are *all* killed by any test that exercises the function. Three mutants that hardcode the result to `emptyList()` are *all* killed by any test that asserts non-empty result on the error path. But three mutants that change `>` to `>=` in a loop bound or change `intersect` to `union` in JoinAnalysis may not be killed by *any* of the proposed test suite — because the proposed test suite (PHASE-10-design.md §4.7) uses *example-based* tests, exactly the kind that AI tunes to.
- **The trip-wire that doesn't fire**: R8 says "if any P10–P15 phase ships with mutation-test kill rate <80%, treat that as a red flag. Do not advance to the next phase until the verification gap is closed." But: what if the *first* run on P10 produces 64% kill rate because Pitest doesn't understand `data object` (Kotlin 1.9+) properly? The trip-wire fires immediately on phase 1 and the entire plan stalls. The strategy never says what to do here — Aaron will either (a) lower the bar silently (defeating the gate), or (b) waste a week tuning Pitest config (defeating the timeline). Neither is acceptable.
- **The deeper logical issue**: A mutation-testing gate without a tool spec is *aspirational verification*, exactly the failure mode the AI-research doc (07 §2 failure mode #1) warns against. The strategy correctly identifies that AI agents satisfice the verification signal — but then proposes a verification signal that *humans cannot operationalize*, which is functionally identical to no signal at all under AI augmentation.
- **What other compiler projects do**: TypeScript's compiler uses *snapshot tests + property-based tests*, not mutation testing. Rust's compiler uses *bug-suite-as-tests + fuzzing*, not mutation testing. Carlini's C compiler used *differential testing against GCC*, not mutation testing. **No major compiler project I can find uses mutation-test kill rate as its primary verification gate.** Mutation testing is a useful supplementary signal; it isn't a primary one for compilers.
- **Suggested fix**: replace "mutation-test kill rate ≥80%" with a *concrete tool spec* in P10. Options: (a) **Pitest with documented Kotlin limitations** — define which mutator types count (`MATH`, `CONDITIONALS_BOUNDARY`, `NEGATE_CONDITIONALS`, `RETURN_VALS`) and which to skip (`VOID_METHOD_CALLS` on Kotlin), with an explicit "anything below 70% on this restricted set is a fail." (b) **Drop mutation testing in favor of property-based + differential testing + fuzzing**, which the strategy already names but doesn't gate on. (c) **Specify mutation testing as a *post-phase quality metric*, not a gate** — measure it, track the trend, but don't block phase progression on the absolute number. Pick one explicitly before P10 starts.

#### F2 — The 3.5–5 month timeline references aren't sole-maintainer evenings-and-weekends references.

- **Section reference**: `04-strategy.md` §1 (line 35), §3 (line 178), `07-ai-augmented-dev-research.md` §1.1 (Carlini), §1.2 (Klabnik).
- **Claim challenged**: "Klabnik shipped Rue in 11–14 days," "Carlini's C compiler in 2 weeks with 16 parallel agents and $20K," "Lambeau's Elo in 24 hours." These are cited as evidence for Waterfall's 14–22 week timeline.
- **Why the reference class is wrong**:
  - **Carlini's compiler**: $20K API spend, 16 parallel agents, 2 billion input tokens, ~2000 sessions, professional Anthropic-internal context. Aaron is one person at $50–500/phase budget (per Q9). Cost ratio is *minimum* 40×. The strategy implicitly assumes 16-parallel-agent throughput compresses to single-agent throughput linearly. It doesn't — Anthropic's setup deduplicates work across agents in real time; single-agent has no equivalent.
  - **Klabnik's Rue**: "11–14 days of evening/after-work effort." Klabnik is one of the most experienced compiler-adjacent engineers alive (co-author of *The Rust Programming Language*, 13-year Rust contributor); his **first attempt failed** ("months of work" with bad technique), then restarted with better workflow. The "11–14 days" is the *successful second attempt*. Reference class for Aaron is closer to "Klabnik's first attempt" because Aaron is starting cold on this workflow.
  - **Lambeau's Elo**: expression language, "non-Turing-complete," no general control flow, no module system, no LSP. Strategy doc itself acknowledges this in 07 §1.3: "**best read as a lower bound on tiny language scope**, not a calibration for Waterfall's scope." But then 04 §1 cites Lambeau's 24 hours as if it bounds anything for Waterfall.
- **What the research's own honest assessment says** (which the strategy partially adopts but doesn't price in): 07 §4 explicitly says **"Worst case: 9+ months"** and "the single biggest predictor of which case Aaron ends up in is how serious the verification design is in week 1." The strategy adopts the median (5–6 months) as the *plan*, not the worst-case as a *risk buffer*. This is asymmetric planning — the upside is the median, the downside isn't accounted for.
- **The empirical anchor**: Klabnik says "Simply knowing how to write code isn't actually enough to truly use large models well. They are a new category of tools in their own right." Translation: there's a learning curve. The first phase will be slower than the median *for any operator*; the median assumes operator-fluency that develops over the project.
- **Suggested fix**: re-anchor the timeline. State explicitly: "Best case 14 weeks if everything compresses; **expected case 22 weeks**; worst case 9+ months if verification design slips or operator-fluency takes longer to develop than expected." Drop the Lambeau citation entirely from the strategy doc — it's noise. Strengthen the Carlini caveat ($20K + 16 agents is not a comparable). Use Klabnik's Rue as the *single* primary reference; acknowledge his first attempt failed and treat Aaron's first 4 weeks as "tooling-up time" rather than feature-shipping time.

#### F3 — "Aaron writes the tests, AI writes implementation" is the load-bearing R8 mitigation but is unsustainable over 14–22 weeks.

- **Section reference**: `04-strategy.md` R8 (line 612), 07 §3 (line 308), `PHASE-10-design.md` §2.7.
- **Claim challenged**: "Tests Aaron writes, not the AI writes. The AI writes implementation tests inside the contract Aaron specifies; Aaron writes the contract tests. Spec EARS-notation acceptance criteria translate directly to Aaron-authored tests." (04 R8).
- **The math**: PHASE-10-design.md §2.7 lists 12 SymbolTable test cases — that's about 90 minutes of careful test-authoring by Aaron, one weekend of focused work. P10 also requires §4.7's 7 Verifier tests, plus IR-lowering tests in §5.4 (unspecified count, likely 10–20). So P10 alone needs ~30 Aaron-authored tests, *just to seed the foundation*. P11 adds 20+ negative type-inference tests (per P11 deliverables). P11.5 adds 10+ module visibility tests. P12 adds tests for sum types, match, Result, `@external`, readonly. Cumulative: ~200 Aaron-authored tests across P10–P15.
- **The sustainability problem**: 200 Aaron-authored tests over 14–22 weeks is 9–14 tests per week of test-only work. Each contract test must be carefully designed to *not* be tunable by AI; that's at least 30 minutes per test of "what's the right way to assert this that catches the failure mode, not just any failure," plus understanding the spec deeply enough to write the test. **Total Aaron-time on contract tests alone**: ~100 hours, distributed across the build sprint.
- **The drift problem**: the strategy correctly identifies "adversarial review fatigue (weeks 8–16)" as an R1 failure mode. Test-writing fatigue is a *direct precursor* — and once Aaron starts letting AI-written tests "good enough" through, R8 (verification overfitting) wins. The strategy's own data point: Klabnik says he reads every commit before merging. Klabnik's project ran for 14 days. Aaron's is ~100+ days at median.
- **The hidden cost (which Q9 only partially captures)**: the $50–500/phase API budget for adversarial review is the *visible* cost. The *invisible* cost is Aaron's time on contract-test design + Klabnik-style every-commit review + adversarial-review fatigue. Q9 should be a *time budget* question, not just a $ budget question.
- **Suggested fix**: split R8's "Aaron writes the tests" rule into a more nuanced policy. (a) **Aaron writes the property-based test *invariants*** (high leverage — one invariant catches many bug shapes). (b) **AI writes example-based tests against Aaron's invariants** (low leverage but high volume; safe because the invariant is the actual gate). (c) **Differential test oracles are Aaron-authored** (the most load-bearing — getting these right determines whether the gate fires correctly). (d) **Add a weekly Aaron-time budget** (say, 8h/week of "test work + adversarial review") explicitly in the strategy. If Aaron can't sustain that, the timeline is wrong.

#### F4 — Differential testing has no oracle for the C backend specifically.

- **Section reference**: `04-strategy.md` R8 (line 611), `PHASE-10-design.md` §5.5 (line 1716).
- **Claim challenged**: "Differential testing against a known-good oracle. Wherever possible, use an external oracle (the current `*Data` AST during P10; native Python `Math` during P13 stdlib; JS `Array` during P15 generic containers). Carlini's GCC-as-oracle pattern is the model." (04 R8).
- **The structural problem**: Carlini's oracle worked because *GCC compiles C, just like the AI-built compiler*. The Waterfall C backend emits C *as a target*, but there's no "known-good Waterfall-to-C compiler" to differential against. The strategy's listed oracles all work for the JS and Python *backends* (compare against native JS / native Python semantics) — but the C backend's correctness has *no equivalent oracle*. The audit (01 surprise #8) confirms the C runtime check *suppresses three classes of warnings* because the C backend's emitted code is genuinely incorrect for `Mod::fn`, `obj.method(x)`, and missing return statements. Removing those suppressions in P11.5 (as the strategy promises) means the C backend has to produce *actually correct* C — but the differential test oracle for "is this C correct?" doesn't exist.
- **Why this matters under AI augmentation**: per 07 §1.2, Klabnik's Rue ELF codegen bug (hardcoded instruction sizes) survived test pass for exactly this reason — there was no oracle for "is this ELF binary correct?" until execution-time crash. The Waterfall C backend has the same shape: emitted C looks plausible, passes `gcc -fsyntax-only`, but produces wrong runtime behavior. Without an execution oracle (running the compiled binary), AI agents will write C backends that pass syntax check and produce wrong code.
- **The strategy's implicit answer** (which it doesn't make explicit): the JS and Python backends *are* the oracle for the C backend. If JS and Python produce identical output (verified by running them) and the C backend produces *different* output when its emitted .c is compiled and executed, the C backend is wrong. But this requires actually *executing* the emitted C, which the current test infrastructure (`gcc -fsyntax-only`) doesn't do.
- **Suggested fix**: P11.5 must add **execution-based testing for the C backend** alongside dropping the warning suppressions. Run each example's emitted C through `gcc -o exe && ./exe`, capture stdout, compare against JS and Python execution. Without this, the C backend's correctness gate doesn't exist, and the "ship a library to npm + PyPI + a C header" promise (the niche thesis) is unverifiable.

#### F5 — P13's 4–6 week budget compresses LSP only 2–5x but the entire phase only 2–3x; the math doesn't work.

- **Section reference**: `04-strategy.md` §3 P13 (lines 355–387).
- **Claim challenged**: "Effort: 4–6 weeks calendar (AI-augmented). The longest phase. LSP + friendly errors are the parts that don't compress fully under AI augmentation."
- **The component-wise math**:
  - LSP server (Kotlin + LSP4J): Gleam's LSP took ~9 person-months to ship (per their changelog). At 2–5× AI compression: 2–5 months. **Already exceeds the phase budget.**
  - VS Code extension: typically 2–4 weeks even with AI; integration with LSP4J introduces glue work. ~1 week compressed.
  - Package manager (`wfpm` skeleton): npm and pip APIs alone are real integration work. ~2 weeks compressed.
  - Stdlib coherence (cross-target differential testing across `Math`, `String`, `Array`, `IO`): every function on three targets, byte-equivalence. ~2–3 weeks of test-design + per-function verification.
  - Spec at v0.x: ~1 week if done seriously.
  - Friendly errors (taste task, 2–5× compression): ~1–2 weeks for the initial panel of 10.
- **Total**: 7–13 weeks even at the optimistic compression. The strategy's 4–6 weeks is at the *floor* of optimistic estimates and assumes everything goes right.
- **Why this matters**: P13 is the longest phase by design — but the strategy's 4–6 week budget *understates* it. If P13 slips to 8–10 weeks (median realistic), the 14–22 week total slips to 18–26 weeks (4.5–6.5 months), pushing toward the worst-case 9 months from 07 §4.
- **The hidden risk**: P13 is where the *adoption-recruitment work* starts (cultivating library-author case studies). If P13 takes 10 weeks of pure-build, the recruitment work that needs to happen *during* P13 doesn't — and the v1.0 legitimacy milestone slips correspondingly.
- **Suggested fix**: rebudget P13 to **6–10 weeks** in the strategy. Mark stdlib coherence and LSP as the two specific deliverables most likely to slip. Add a Q11 to §7 — "is splitting P13 into P13a (LSP + spec) and P13b (package manager + stdlib) acceptable?" — to give Aaron an explicit decompression option if the original P13 stretches.

#### M1 — The cross-vendor adversarial review mitigation (R9) needs concrete tooling.

- **Section reference**: `04-strategy.md` R9 (line 622).
- **Claim**: "Cross-vendor verification. When adversarial review fires, use a *different* model for the skeptic than for the builder where possible (Claude builds, Codex reviews; or vice versa)."
- **Issue**: This is operationally underspecified. Does Aaron run two Claude Code sessions (different models, same vendor) or genuinely switch to Codex? The cited [alecnielsen/adversarial-review](https://github.com/alecnielsen/adversarial-review) tool is one option; the other is just running `/ultrareview` per phase. The strategy doesn't pick.
- **Suggested fix**: pick one. Default to "team-skeptic from Claude Code" for routine reviews (cheap, fast); use `/ultrareview` or Codex-as-skeptic for major phase boundaries (P10, P12, P13, P16). Document in `CLAUDE.md`.

#### M2 — Fuzzing target (≥1M random inputs) is named at P16 but the corpus isn't.

- **Section reference**: `04-strategy.md` P16 (line 459).
- **Issue**: "Fuzz the parser and verifier: ≥1M random inputs, no crashes; ≥1K random *well-typed* inputs, no false-rejects." Where do the well-typed inputs come from? A program generator. Who writes it? Not specified.
- **Suggested fix**: name the tool (probably `jqwik` for Kotlin property-based testing, plus a hand-rolled Waterfall program generator) in P10 spec. The program generator is *itself* a deliverable that takes ~1 week.

### Section B — The P10 design spec

#### F6 — ESCALATE list has the wrong 7. **RISK.**

- **Section reference**: `PHASE-10-design.md` §6.2 (line 1776) lists 7 escalation triggers.
- **What's there**: per-arg source positions; existing verify() methods doing more than declaration; *Data.verify mutating state; legacy backend quirks; BINARY_OP type inference; StringLiteral type; accumulate-vs-bail.
- **What's missing (higher-priority candidates for ESCALATE)**:
  - **The `void` type ambiguity in `WaterfallType.fromSourceText`**. PITFALL #1 explicitly says "Escalate if you discover any other site that conflated `void` with primitive types." This is named as a PITFALL but not as an ESCALATE — yet it's exactly the kind of cross-cutting issue that should be escalated. A user can write `void x = ...` today; what does the new verifier do? The spec says reject, but the migration callsites in §2.4 don't show how this is enforced.
  - **Generic monomorphization landing in P14 needs SymbolKind extension**. PITFALL #2 says "Don't add variants speculatively." Right call for P10. But P14's monomorphization needs a `SymbolKind.GenericFunction(typeParams, parameters)` variant. The migration path from `SymbolKind.Function(parameters)` to that needs a *call-site sweep* and is exactly the kind of cross-phase ambiguity that should be escalated when the AI hits it in P10.
  - **`SymbolTable.commitReadonly` walks to parent. What if `name` is in `nameToInfoMap` of `this`?** The §2.2 code handles this — it overwrites the local entry. But §2.3 table row "Both C and C' shadow `x`, then A `commitReadonly({"x"})`" says "A's info with `isReadonly = true` (durable)" — but what if a *grandchild* committed? The spec implies it walks up. Verify the semantics with a worked example. This is exactly the kind of subtle case that an AI agent will silently resolve wrong (per 07 failure mode #4).
- **What's questionable on the existing list**:
  - "Legacy backend quirks" is named as ESCALATE but per Aaron's Q5 answer the legacy backend is dropped. The escalate item is for a backend that won't exist by mid-P10. Remove this.
  - "BINARY_OP type inference" is a P11 concern that surfaces as a placeholder in P10. Escalating in P10 is right but the spec says "placeholder" — what triggers escalation? An AI agent inferring "actually let me improve this" — which is exactly when the agent goes off-script. Be more specific: "if you find yourself writing more than `type = left.type` for BinaryOp, escalate."
- **Suggested fix**: rewrite §6.2 with 5–7 items focused on cross-phase invariants the AI cannot see. Add `void`-handling, `SymbolKind` extension paths, and the `commitReadonly` walk semantics. Drop the legacy-backend item.

#### F7 — `WaterfallType` sealed class doesn't cover `?T` nullable types.

- **Section reference**: `PHASE-10-design.md` §1.2 (line 89, `ErrorType`).
- **Issue**: The current grammar parses `?int` (audit row 79, surprise #5) but the verifier rejects it. The spec's `fromSourceText` returns `ErrorType("?int")` for nullable-prefixed types. This is "correct" in that it matches current behavior — but means **the verifier rejects any `?T` type before reaching downstream code**.
- **The cross-phase problem**: post-P12, the `@external` design (03 §4) wants `@external(js, "Math.sqrt")` for math functions. But what about `setTimeout` (taking `(callback, ms) => void`)? Or `JSON.parse` which can fail? Either Waterfall develops `?T` (nullable) or `Result<T, E>` for everywhere fail might happen. The design picks `Result`. But `Result` is a generic union — Tier 2 in 03 §1.4, special-cased in v1.
- **What happens during P10**: if AI encounters `?int x = NULL` in any source (even a test fixture), it must produce a verifier error. The spec says ErrorType handles this. **But the verifier doesn't currently reject — it errors at `PrimitiveTypes.isPrimitiveOrArray("?int")` which returns false.** The new error path needs to fire structurally the same way. Check: does `VerifyError.UnknownType` cover this case? The §4.3 ErrorType variant returns `<error:?int>` and §4.5's verifyTypedVarDecl checks `if (type is WaterfallType.ErrorType)` and emits `UnknownType` — yes, this works. But the error message "Type '?int' is not a recognized primitive..." is **misleading** because `?int` is not a typo; the user is asking for a nullable. The friendly-errors infrastructure (P11+) needs to special-case the `?`-prefix to say "Nullable types aren't supported yet; use `Result<int, E>` instead."
- **Suggested fix**: add to §1.2 a comment that `?T` parses-but-errors is *intentional* and the error message will be replaced at P11 with a specific "nullable types aren't supported" diagnostic. This is a small fix but matters for the F1-style "AI silently makes a choice" hazard — without the comment, an AI agent reading the spec might decide to implement nullable handling speculatively.

#### F8 — The transactional readonly-shadow model is correct but the `commitReadonly` walking semantics have a hidden bug. **RISK.**

- **Section reference**: `PHASE-10-design.md` §2.2 (line 403, `commitReadonly`), §2.3 (line 451, behavior matrix), §2.5 (line 488).
- **The spec**: `commitReadonly(names)` walks up the parent chain for each name to find the owning scope and mutates that scope's entry.
- **The hidden problem (worked example)**:
  ```
  func f() {
      int x = 0
      if (a) {
          if (b) {
              readonly x         // child2: x shadowed
          } else {
              readonly x         // child2': x shadowed
          }
          // Inner if joins: child1's parent.commitReadonly({"x"}) called.
          // Walks up: x not in child1's owned, walks to f's owned, sets isReadonly=true.
          // Now f-scope's nameToInfoMap[x].isReadonly = TRUE.
      } else {
          // x = 1     // Is this rejected?
      }
  }
  ```
  Per the spec, at the inner join inside the outer-if-branch, `commitReadonly` walks from child1 to f-scope and *durably* mutates f's binding. Then when the outer-else-branch (the implicit one, since there's no `else`) runs, its lookup returns f's now-readonly binding. Per §2.5's algorithm, the implicit-else is a "skip the if entirely" predecessor with empty shadow — and the intersection of `{}` with whatever child1 had is `{}`. So `commitReadonly` shouldn't fire at the *outer* join. But it *already fired at the inner join* — the parent f-scope binding is now durably readonly.
- **Why this is wrong**: the design §2.5 step 5 says "Filter out terminating branches" — the implicit-else path is non-terminating. It contributes `{}` to the intersection. Outer intersection: `{} ∩ {"x"}` = `{}`. So outer `commitReadonly` doesn't fire. But the *inner* `commitReadonly` already mutated f's binding, and that mutation cannot be rolled back. So the implicit-else path *cannot observe* the pre-promotion state — exactly the F8 bug from my round-1 review, just one level deeper.
- **The fix the spec needs**: `commitReadonly` should walk *only up to a defined boundary*, not unconditionally to the owning scope. Specifically, the boundary is the *parent of the joining scope*. So at the inner join, `commitReadonly` should mutate child1 (the outer-if-branch's scope), not f. That way, when the outer join evaluates predecessors, child1 has the correct state and the outer intersection still computes correctly.
- **The interaction with the snapshot model**: `localReadonlyShadow()` only returns the *immediate* scope's shadow. If `commitReadonly` writes to child1 (not f), then when child1 exits, its snapshot doesn't include the freshly-committed binding (because the commit went to `nameToInfoMap` not `readonlyShadow`). The outer join code then has to *also* observe child1's freshly-committed names as part of its snapshot. This means either (a) `commitReadonly` writes to both `nameToInfoMap` AND `readonlyShadow`, or (b) the snapshot includes both, or (c) `commitReadonly` only writes to `readonlyShadow` (not `nameToInfoMap`) until the very outermost join.
- **Suggested fix**: explicitly spec what scope `commitReadonly` mutates. The cleanest semantics: `commitReadonly` writes to the *receiver scope's* `readonlyShadow`, *not* to the owning scope's `nameToInfoMap`. The `nameToInfoMap` is durably mutated only at the *outermost* control-flow construct's join — i.e., when verifying the function body completes and we know all child joins are resolved. This is a non-trivial change to §2.2 but is correct.

#### F9 — Verifier "accumulate-vs-bail" decision is escalated but the default is fail-fast which contradicts P11's spec.

- **Section reference**: `PHASE-10-design.md` §4.3 (line 1246, VerifyResult), §4.4 (line 1267), §6.2 escalation #7 (line 1791).
- **Claim**: "NOTE: at the boundary, the existing `Main.run` aborts on the *first* error, which is fine for P10 — but the verifier is structurally ready for the P11 'show all errors' upgrade. The driver change to accumulate-then-bail comes with friendly errors at P11."
- **The contradiction**: `StatementVerifier.verifyStatement` returns `List<VerifyError>` (accumulating per statement), and `JoinAnalysis.verifyIfBlock` (line 1450) explicitly says "Collect errors from every branch (don't bail on the first)." So *intra-statement* accumulation is happening in P10. But the *driver* (`Main.run`) bails on the first error. This means: per-statement verification can return 5 errors, but the driver throws away 4 of them. The phase-exit checklist doesn't catch this because no test exercises "verify a program with 5 errors and see 5 reported."
- **Why this matters under AI augmentation**: an AI implementer reading the spec sees both patterns and has to pick. The escalation #7 says "*Don't speculatively switch* the driver to accumulate during P10 — escalate if doing so seems tempting." So the AI is told to keep driver bail-on-first. But the verifier *already* accumulates per-statement. This means P10 ships with verifier capable of N errors but driver shows 1. **This is the exact "tests pass, code wrong" pattern**: the per-statement test passes (returns multiple errors), but the user-visible behavior (only sees one error) doesn't match what the tests cover.
- **Suggested fix**: make P10's design explicit. Either (a) per-statement verification also bails on first error in P10 (matches driver), or (b) driver accumulates in P10 too (matches per-statement). Pick one. The §4.3 VerifyResult.errors as a list is fine; the question is whether anything else stops at error[0].length.

#### F10 — The "fields not deleted" for *Data classes leaves a structural hazard.

- **Section reference**: `PHASE-10-design.md` §5.6 (line 1730).
- **Claim**: "**Files NOT deleted (yet):** The `*Data` classes themselves. They're still produced by the parser and consumed by `IrLowering`. Deleting them is the long-term path but P10 is 'introduce IR alongside, lowering bridges them.'"
- **The hazard**: after P10, every `*Data` class has `translate(backend: CodeGenerator)` removed (per §5.6) but the class still exists as the parse-result. Future phases that walk *Data (e.g., debug printing, source-map generation) need clear rules about when *Data vs IR is the right tree. Without a clear rule, AI agents in P11+ will:
  - Add new fields to `*Data` (because that's where parsing lands).
  - Forget to propagate them to IR.
  - Result: IR lowering loses information that the parser captured.
- **The specific case to watch**: per-arg source positions (PITFALL #8) — the spec says introduce a `TypedArgument(type, name, position)` class. The `*Data` classes use it; does `IrParameter` (defined in §3.4) carry the same position? Yes, per §3.4 `IrParameter` has `sourcePosition: SourcePosition`. Good. But there's no spec-level rule that says "any new field on a `*Data` class must propagate to its IR counterpart." Without it, the gap widens phase-by-phase.
- **Suggested fix**: add a rule to `CLAUDE.md` (the persistent-context doc the strategy requires): "When a field is added to a `*Data` class, the corresponding IR class must be updated in the same PR. Cross-tree drift is a P10 contract."

#### F11 — `IrType` and `WaterfallType` are intentionally redundant; the rationale is thin.

- **Section reference**: `PHASE-10-design.md` §3.3 (line 656).
- **Claim**: "It exists as a separate sealed class for two reasons: (a) it lets the verifier and IR evolve independently when type inference grows the type lattice at P11; (b) it documents at the package boundary that the IR has its own type representation."
- **The challenge**: structurally-identical sealed classes with bidirectional converters is exactly the kind of duplication AI agents create when they don't realize two things are the same. Two months from P10 ship, when P11 lands and `WaterfallType` grows a generic-type-parameter variant, will the AI agent extending it remember to extend `IrType` too? Per 07 failure mode #4 (silent spec resolution), no.
- **The alternative**: a single `WaterfallType` shared across verifier and IR. The "evolve independently" rationale assumes the verifier and IR want different shapes — but for P10 they're identical, and any P11 divergence would be a single-language-level design call, not a verifier-vs-codegen call.
- **Counter-argument** (the spec is right): the IR boundary protects the backends from verifier internals. If `WaterfallType` grows a `TypeVariable` variant for inference, that's only meaningful to the verifier; the IR should see resolved types. So separate hierarchies *do* make sense — but only if the resolution boundary is enforced.
- **Suggested fix**: keep `IrType` separate but add a §3.3 rule: "Every `IrType` variant must correspond to a resolved type — never a type variable, never a constraint. If you find yourself wanting to put a `TypeVariable` in `IrType`, it belongs in `WaterfallType` and should be resolved before lowering."

#### F12 — `Verifier.verifyModule(module, scope, target)` target parameter (lead-added) is mentioned only in the brief, not in the design.

- **Section reference**: Team-lead brief mentions "target-aware `Verifier.verifyModule(module, scope, target)` parameter the lead added to absorb the `@external` requirement." `PHASE-10-design.md` §4.4 (line 1267) shows the signature *without* a target parameter.
- **The drift**: the brief says the lead "added" the parameter; the design doc as written doesn't reflect it. Either (a) the brief is wrong about the lead having added it, (b) the design needs an edit to add the parameter, or (c) the parameter is implicit and the design will add it at P12 when `@external` lands.
- **If (b) or (c)**: the parameter has a "default value" question. What does `verifyModule(module, scope, target = null)` do? If `target = null` means "verify against all targets simultaneously," that's a sound default — but the spec doesn't say. An AI agent silently chooses: probably "ignore @external annotations entirely when target is null." That would be the wrong default — it should be "fail-closed: reject @external code at null-target verify time, because we can't know if it's safe."
- **The deeper question**: does `verifyModule` need a target at all? Targets are a codegen concern, not a verification concern. `@external` annotations are part of the AST regardless of target; verifying them means checking that *for each backend that will be emitted*, the function has either a Waterfall body or an `@external` annotation. That's a *cross-target* check, not a per-target check.
- **Suggested fix**: clarify the signature in PHASE-10-design.md §4.4 before P10 starts. Either (a) drop the target parameter (cleaner: cross-target check), (b) make it a `Set<TargetName>` (verify against all targets simultaneously), or (c) make it a single target with a fail-closed default. Pick before P12 implementation begins. This is exactly the kind of silent-resolution hazard the design discipline is supposed to avoid.

#### M3 — `localReadonlyShadow()` and `exitScope()` both return the local shadow.

- **Section reference**: `PHASE-10-design.md` §2.2 (line 297, exitScope), §2.2 (line 412, localReadonlyShadow).
- **Issue**: Two methods, same return shape. `exitScope(child)` takes a child and returns its shadow snapshot; `localReadonlyShadow()` returns this scope's shadow. The redundancy is fine but unnecessary — the §2.5 algorithm uses `exitScope`, the §2.7 tests (line 590) use `exitScope`, and `localReadonlyShadow` isn't actually called anywhere in the spec.
- **Suggested fix**: drop `localReadonlyShadow()` from the public API. Keep it private if internally useful. One fewer method = one less ambiguity for AI implementers.

#### M4 — `DeclareResult.Failure` carries the error but `Verifier` wraps it back.

- **Section reference**: `PHASE-10-design.md` §2.2 (line 430), §4.3 (line 1232).
- **Issue**: `SymbolTable.declare` returns `DeclareResult` (Success/Failure with `DuplicateDeclarationError`), and `VerifyError.fromSymbolTable` wraps it back into a `VerifyError.DuplicateDeclaration`. Why two error types? Because the symbol-table is meant to be reusable outside the verifier? But the spec doesn't have any non-verifier callers.
- **Suggested fix**: have `SymbolTable.declare` return `VerifyError?` directly (null on success). Drop `DeclareResult` and `DuplicateDeclarationError`. One fewer indirection. Minor polish.

#### M5 — IR `BundleLiteral` has a placeholder type for P10 — but bundles are deprecated per the strategy.

- **Section reference**: `PHASE-10-design.md` §3.6 (line 956); `04-strategy.md` P12 (line 332, "Bundles either deprecated or redefined").
- **Issue**: BundleLiteral is in the IR with a placeholder type, but P12 plans to deprecate or redefine. So P10 ships IR with a structural variant that's marked for removal/change in the next phase. Risk: AI agent in P10 sees the variant, writes IR-lowering code for it, then P12 has to rewrite. Sunk cost.
- **Suggested fix**: skip IR-side BundleLiteral entirely in P10. Either keep `BundleLiteralData` *Data class but don't have an IR counterpart yet, or merge IR-bundle-as-array (which is what JS does anyway). P12's decision then determines whether to introduce a real IR variant.

### Section C — Niche pivot + module reorder

#### F13 — The library-author niche's "serde precedent" comparison is questionable.

- **Section reference**: `04-strategy.md` §1 (line 24).
- **Claim**: "library-author niches historically punch above their stars-count for staying power — Rust's `serde` carries enormous mindshare relative to its star count; the same dynamic applies to libraries that become load-bearing in multiple ecosystems."
- **The challenge**: `serde` is a *library within Rust*, not a *language for library authors*. The dynamic the strategy invokes — "serde's mindshare is large despite few stars" — is about *consumers* of a library who don't star the GitHub repo. That's load-bearing-status, which is different from what Waterfall is asking for.
- **Why this matters**: the strategy uses the serde precedent to justify the upper-end-of-Tier-B targets (1500–3000 stars + 3 production users). But serde is at *11k stars* and is *used by millions of crates*. The reverse-direction inference — "a library *built with* Waterfall could be load-bearing" — requires that Waterfall itself reach load-bearing-status in three ecosystems (npm, PyPI, C), which is the Tier B legitimacy bar. The serde precedent doesn't prove this can happen; it just shows that *successful* libraries punch above star-count.
- **The honest math**: for one library built with Waterfall to "punch above its weight," it has to *first* be successful in its own niche. Waterfall is the build tool, not the library. So Waterfall's traction depends on (a) library authors choosing Waterfall, *and then* (b) those libraries succeeding. Two layers of selection. Star-count probably *under*-counts Waterfall's adoption *if* libraries built with it succeed — but only after the first 2–3 library successes. Pre-success, star-count *over*-counts mindshare relative to actual library use.
- **Suggested fix**: replace the serde citation with a more directly-comparable analog. Closest fit: `cargo-make` or `cross-rs` (build-tool-for-multi-target users) — both have <1k stars but real cross-platform-publishing use cases. The strategy's mental model is more like a build tool than like a library; pick the right analog.

#### F14 — Modules-at-P11.5 requires P12 `@external` to be designed *now*, not at P12.

- **Section reference**: `04-strategy.md` P11.5 (line 295, "Spec section for `@external` × visibility interaction: written but not yet implemented").
- **Issue**: The strategy correctly identifies that `@external` × visibility is a P11.5 spec deliverable. But "spec written, implementation in P12" splits the design work from the implementation by ~3 weeks. Under AI augmentation, the spec author (designer) hands off to the implementer (AI agent) with no review loop. If the spec has ambiguities (per 07 failure mode #4), the implementer silently resolves them — and the issue surfaces in P12 when `@external` is being implemented, by which time fixing the spec means rewriting P11.5 code.
- **The cross-phase coupling**: P11.5 must include enough `@external` design to know what visibility means. Specifically:
  - Can a `pub @external(js, ...)` function be called from another module on the Python target? (No, because Python has no @external annotation — fail-closed.)
  - Can a `pkg @external(js, ...)` function be called from a sibling module? (Yes, but only when emitted to JS.)
  - Can `@external` be applied to a `private` function? (Probably not — privacy + external is incoherent — but the spec doesn't say.)
- **Suggested fix**: include the `@external × visibility` design in P11.5's spec deliverable, not just a "spec section to be written." Specifically: write the truth table for (visibility × target × import-vs-call-site). Defer only implementation to P12.

#### F15 — Library-author candidates "trying real ports during P13" is the substantive cultivation milestone, but P13 is only 4–6 weeks.

- **Section reference**: `04-strategy.md` P13 (line 384), P16 (line 463), §6 first-100-users (lines 737–745).
- **Issue**: The strategy says by end of P13 (~weeks 9–14): "1–2 library-author candidates trying real ports as evaluation." That's a *recruiting* milestone — meeting a library author, getting them to install Waterfall, supporting them through a port. Recruiting a single library author takes (per industry norms) 1–3 months of relationship building, not weeks. The strategy puts it on a 5–6 week clock.
- **The math**: by week 9–14, Waterfall is *just shipping* its package manager + LSP. The library author needs (a) a working language (P12 ships at week 5–8), (b) a working package manager (P13 ships at week 14), and (c) Aaron-personal-time to support them. The earliest a candidate can *start* a port is week 14 — and finishing a port plus evaluating it is another 2–3 weeks. So the earliest a candidate finishes a port is week 16–17, after P13.
- **What the strategy implies but doesn't say**: case-study cultivation runs *in parallel* with the build sprint. Aaron is not just building P12 + P13 in weeks 5–14, he's *also* identifying library authors, demoing the language, supporting their evaluation. That's ~5–10 hours/week of non-build work on top of the build sprint.
- **The hidden cost**: if Aaron is doing the build sprint at evenings+weekends (~10–15 hours/week of build) *plus* 5–10 hours/week of recruiting, that's a 15–25 hour/week investment. Sustainable for 1–2 months, dangerous for 3–6 months (R1 burnout).
- **Suggested fix**: explicitly budget Aaron's *recruitment time* in the strategy. Either (a) shift recruitment to P14–P15 (after build sprint stabilizes), pushing first case study to ~6 months post-P16 instead of "during P13," or (b) acknowledge that P12 + P13 require 20+ hours/week including recruitment and adjust the timeline accordingly. The current implicit "all this happens in parallel without time budgeting" is the F15-shape risk from round 1.

#### M6 — The Gleam-vibe positioning requires actually delivering Gleam-grade error messages, which the team has never built.

- **Section reference**: `04-strategy.md` §2 Candidate 5 (line 135), §6 friendly errors (line 700).
- **Issue**: "Gleam-grade language design" is the secondary positioning. Gleam's error messages are widely praised; ReScript took years to match them; Elm took years to set the bar. The team has no track record on error-message taste. AI-augmented development is specifically poor at this (07 failure mode #6).
- **Risk**: the positioning promises a quality the team has not yet demonstrated. If P13 ships errors that are "AI-adequate" rather than "Gleam-grade," the positioning is false on day 1.
- **Suggested fix**: in P11 or P12, ship 3–5 hand-crafted error messages and post them publicly *before* committing to the Gleam-vibe pitch. If the panel-review on those is mediocre, downgrade the positioning to "small, typed, multi-target" and drop "Gleam-grade" from the pitch.

#### M7 — Niche-fit case study (CRC32) is named but isn't Aaron-tested for "is this actually compelling."

- **Section reference**: `04-strategy.md` §6 case study 1 (line 723), R3 trip-wire (line 583).
- **Issue**: The CRC32 case study is *the* niche-fit test. But "CRC32 implementation across 3 languages" is a *very common* tutorial topic — every Rosetta-Code-style site has it. A library author looking at "I rewrote CRC32 in Waterfall" might think "nice but I'd have just copied from Rosetta Code." The case study needs to demonstrate *consolidation pain*, not just "look, three outputs from one source."
- **Suggested fix**: pick a case study with *active maintenance burden* as the differentiator. CRC32 is implementations are mature and stable; pick something where maintaining three ports has visible drift cost. Examples: a recent algorithm (Blake3, recent post-quantum), a complex protocol parser (msgpack, CBOR), or a domain-specific algorithm (a specific compression scheme with known cross-language bugs). The "before/after diff" matters less than "this is currently a recurring bug source." Validate with a real library author before committing to CRC32.

### Section D — readonly unification (re-flip after F11)

#### F16 — The `const`/`imm` removal in v1 is a breaking change even if user-facing impact is tiny — and the friendly-migration-error is harder than it looks.

- **Section reference**: `03-language-design.md` §2g (line 1945), §2b (line 1349).
- **Claim**: "v1 unifies all three under `readonly` and removes `const`/`imm` from the language."
- **Empirical state I verified**: 3 `.wf` example files use `const`. 0 use `imm`. Plus 3 golden files contain emitted `const`/`let` (which is JS/C codegen, not source).
- **The friendly-migration-error problem**: the grammar keeps `CONST` and `IMM` as tokens for one release (per §2b line 1347) so the parser can emit "use `readonly` instead." But this requires either (a) `CONST`/`IMM` as *modifier alternatives* that the verifier later rejects with a friendly message (cleaner UX), or (b) `CONST`/`IMM` as *parser-level errors* that produce custom diagnostics (cleaner spec).
- **What the spec actually says**: §2b line 1361 — "`CONST` and `IMM` are *not* alternatives of `modifier`. If they appear in modifier position, parsing fails." So path (b) — parser-level errors. But ANTLR's error messages on a missing-alternative are *terrible* by default ("mismatched input 'const' expecting one of {...}"). The friendly-migration story requires a *custom error listener* that intercepts these and produces the friendly message.
- **The hidden work**: writing the custom error listener is ~1 day of work; testing it across all the ways `const` could appear (modifier-position, name-position, anywhere else) is another day. Plus the *legacy goldens* (3 files) need to migrate. That's ~3 days for a "tiny migration."
- **Risk**: AI agent reading the spec sees "parsing fails" and implements the simplest interpretation — which is the default ANTLR error. The friendly-migration story dies silently.
- **Suggested fix**: the spec must explicitly call out the custom error listener as a *deliverable* of the migration. Better yet: take F11 from round 1 seriously and keep `const` as a working synonym for `readonly` in v1. Migration is genuinely free; the user-facing argument for unification (one keyword = clearer pitch) is real but small. Aaron flipped *back* to unification under the new niche; the F11 cost-benefit math wasn't re-examined under that lens. Worth re-asking.

#### R1 — The Form B-only-readonly pitch has been *removed* from the spec.

- **Section reference**: `03-language-design.md` §2 throughout, esp §0.
- **Issue**: My round-1 F6 finding said the novelty pitch was overstated. The team responded by *removing* the novelty framing — the design now says "the novelty is **syntactic**, not semantic" (line 1046). Good.
- **What's missing**: the strategy doc §6 marketing story still has the Q4 answer pending ("Implement it; use it internally for 1-2 examples; if it feels good, promote it"). The Mike-Test in 00-FINAL-PLAN.md (stale) leads with "Waterfall: a small language that compiles to JavaScript, Python, and C — and lets you freeze variables mid-function." That's the *old* marketing positioning under the *old* niche.
- **Suggested fix**: when synthesis is rewritten post-pivot, the Mike-Test for the new niche has to be different. Mike is now a *library author* in the new niche, not a TypeScript-learning Rustacean. F1 in this round (the new Mike-Test below) addresses this.

#### M8 — `readonly` and `imm record` are still potentially confusing.

- **Section reference**: `03-language-design.md` §2a.6 (line 1165, "imm record" Tier 2 future).
- **Issue**: Spec says "Future work: a Tier 2 `imm record` declaration that marks a record as deeply immutable from the type level. This is a *type modifier*, not a binding modifier — distinct from `readonly`, composable with it."
- **The friction**: under the new unified-keyword push, `readonly` is the only modifier. Then Tier 2 introduces `imm record` — bringing back `imm`. The strategy doc supposedly closed this loop with `readonly` unification, but the design doc Tier 2 plan reopens it.
- **Suggested fix**: pick one. Either `readonly record` (consistent with the unification), or commit to *transitive* `readonly` for record types (the keyword carries different semantics by what it modifies). Decide in the v1 spec to avoid an awkward v2 keyword reintroduction.

### Section E — `@external` spec readiness

#### F17 — `@external` × generics is unspecified.

- **Section reference**: `03-language-design.md` §4 (line 2047), `04-strategy.md` P12 (line 332) + P14 (line 391).
- **The case**: P12 ships `@external` and `readonly` and sum types and `match`. P14 ships generics with monomorphized C output. What happens at:
  ```
  @external(js, "Array.from")
  func toArray<T>(Iterable<T> source) returns T[]
  ```
- **The question**: in P12 (no generics), this is a syntax error. In P14 (generics arrive), this becomes valid. But the C monomorphization needs to know: does `@external(c, ...)` work *per-instantiation* (each `T` produces a different external name?), or is the external name fixed (each `T` calls the same C function, which then handles the typing)? Both interpretations exist in real languages (Rust's `extern "C"` with generic wrappers vs. C++'s template instantiation).
- **The spec doesn't say**: §4 covers `@external` for non-generic functions only. P14's spec doesn't extend it. The first AI agent that hits this hardcodes whichever interpretation the test suite happens to demand — exactly failure mode #4.
- **Suggested fix**: P14 design (to be written) must spec `@external × generics` before any `@external` implementation lands in P12 — because the P12 implementation will set the precedent. Either ship the design now or commit to "@external functions cannot be generic in v1" and reject it at the parser/verifier level.

#### F18 — `@external` and `Result<T, E>` interaction is unspecified.

- **Section reference**: `03-language-design.md` §4 (line 2047), §1.6 (line 619).
- **The case**:
  ```
  @external(js, "JSON.parse")
  func parseJson(string input) returns Result<JsonValue, string>
  ```
- **The semantic question**: `JSON.parse` in JS throws on invalid input. The Waterfall signature returns `Result`. How does the emitted JS bridge throw → `Result::Err`? Wrap in try/catch automatically? Force the user to write a Waterfall body that does it?
- **What the spec actually says**: Nothing. §4 doesn't address effectful `@external` functions; §1.6 doesn't address `@external` `Result` return types.
- **Why this matters under the library-author niche**: most useful library FFIs are effectful (parse, network, IO). The pitch is "ship one algorithm to npm + PyPI + C header" — but a *pure* algorithm rarely needs `@external` (it's just code). The realistic use case is *library that uses* some host APIs (parse, hash, IO) — and those are effectful. Without an answer for effectful `@external`, the niche use case is unsupported.
- **Suggested fix**: P12 design must include an `@external` × effects design. Recommendation: any `@external` function returning `Result` automatically wraps thrown exceptions in `Result::Err` in JS / Python; in C, the C function is expected to return the `Result` shape directly (no exceptions). Document the bridging in §4.

#### M9 — `@external` target keywords vs target identifiers in target-symbol strings.

- **Section reference**: `03-language-design.md` §4.1 (line 2124).
- **Issue**: `@external(js, "Math.sqrt")` — `js` is a target keyword (one of the reserved names per §4.1); `"Math.sqrt"` is a string. Is `js` an identifier or a keyword? §4.1 says "reserved name list" — but what about case sensitivity? Could a user write `@external(JS, ...)`? `@external(JavaScript, ...)`?
- **Suggested fix**: spec exact case-sensitivity and exact list: `js` (not `javascript`, not `JS`), `python` (not `py`), `c` (not `clang`), `legacy` (until dropped). Lowercase, no aliases.

### Section F — Drift between 00-FINAL-PLAN.md and current docs

#### R-DRIFT-1 — 00-FINAL-PLAN.md is stale on every load-bearing decision. **Synthesis rework needed = full rewrite.**

- **Section reference**: `notes/team-output/00-FINAL-PLAN.md`.
- **The drifts** (in order of severity):
  1. **Section 1 Mike vignette** leads with TypeScript user, not library author. Under the new niche this is *the wrong user persona*.
  2. **Section 2 Q1–Q7 table** doesn't include Q8 (transparency) or Q9 (adversarial budget). Aaron answered Q1 with a *niche pivot* (Candidate 5), not the original recommendation. Q2 answered with timeline split. Q4 already answered (one-of-many).
  3. **Section 3 Tier B description** matches old draft. Under the new strategy, the §1 milestone language is "v1.0 technical-complete" + "v1.0 legitimacy" split — that distinction is absent from 00-FINAL-PLAN.
  4. **Section 4 niche section** recommends Candidate 2 (teaching) as primary. The new niche is Candidate 5 (library author + Gleam vibe). Teaching is *content marketing*, not the primary niche.
  5. **Section 5 timeline math** uses "7 effort-quarters × 2.5× = 5–7 years." The new strategy uses "14–22 weeks AI-augmented + 12–24 months adoption = 18–30 months total."
  6. **Section 5 roadmap table** doesn't include P11.5 (modules). Sequence is P10 → P11 → P12 (sum types) — but new sequence is P10 → P11 → P11.5 → P12 → P13.
  7. **Section 6 readonly section** says "Form A keeps the existing `const` / `imm` keywords (no migration, no deprecation, no churn)." But the design re-flipped to unification. The 00-FINAL-PLAN matches the *post-F11* spec, which the design then *re-flipped against*. So the synthesis is now stale in *both* directions.
  8. **Section 7 risk register** doesn't include R8 (verifier overfitting), R9 (AI tooling regression), or R10 (P10 spec quality). These are the three highest-severity new risks added under the AI-augmented model.
  9. **Section 8 Mike's first frustration** ("diagnostic UX") is still correct *in spirit* but the niche-specific failure mode would be different. Library-author Mike's first frustration might be "can I actually publish to npm from this?" — diagnostic UX is downstream.
  10. **Section 10 "first three commits"** is correct in spirit (P10 design, BUS-FACTOR, verifier-enforces-const) but commit #3 ("verifier enforces const") is obsolete since `const` is being removed in P10.
- **Severity assessment**: This isn't "needs edit pass." This is "full rewrite required, treating 00-FINAL-PLAN as a v1 draft and writing a v2." The structure (7-decisions framing, Mike-test lead) is still right; every section's content needs replacement.
- **Suggested fix**: synthesis rework should be a *new* document. Don't try to edit 00-FINAL-PLAN.md in place — too many sections need replacement and the old framing will leak through.

### Section G — Q10 hiding in the package?

#### F19 — Q10 is the structured-error format.

- **Section reference**: `PHASE-10-design.md` §4.3 (line 1140), `04-strategy.md` §6 friendly errors (line 700).
- **Claim**: The strategy commits to friendly errors. The P10 design commits to structured `VerifyError` types with `message` field. But:
  - Are errors emitted as JSON for LSP? Plain text for CLI? Both?
  - Do errors have stable error *codes* (e.g., `WF-E0042`) for users to look up?
  - Is the `message` field the only stderr output, or does the renderer add file-snippet context?
  - Does the LSP get a different format than the CLI?
- **Why this is Q10**: AI agents reading the spec will pick *something*. Without a decision, the v1 LSP will have a different error format than the v1 CLI, and a user moving from CLI to LSP sees different messages for the same error.
- **Suggested fix**: add Q10 to the strategy: "Stable error codes (WF-Exxxx) + structured JSON for LSP + friendly text for CLI / yes / no / pick later." Recommend: yes, but defer the *naming scheme* until P11 when actual errors land.

#### F20 — Q11 is the test framework for property-based tests.

- **Section reference**: `04-strategy.md` R8 mitigation (line 611), `PHASE-10-design.md` §2.7 (line 511).
- **Issue**: The strategy commits to property-based tests. The P10 design uses JUnit 4 (per `01-codebase-audit.md` weakness "JUnit 4 build from 2014"). Property-based tests in JVM-land use *Jqwik* (modern, JUnit 5 native) or *kotest-property* (Kotlin-native). Both require JUnit 5. Migration: ~1 day. Done in P10 setup.
- **Why this is Q11**: AI agent reading the spec sees "property-based tests" but no library. Picks one. May pick wrong (e.g., picks *junit-quickcheck* which is stale).
- **Suggested fix**: add Q11: "Property-based test framework: kotest-property / jqwik / junit-quickcheck / other." Recommend: **kotest-property** (Kotlin-native, integrates with JUnit 5, actively maintained). Pin before P10 starts.

#### F21 — Q12 is gradle-warning hygiene (audit T2).

- **Section reference**: `01-codebase-audit.md` weaknesses, `04-strategy.md` P13 (line 371, "T2 — Gradle deprecations swept as housekeeping").
- **Issue**: The audit notes T2 (Gradle deprecations) as housekeeping. P13 says it's swept then. But: under AI augmentation, Gradle deprecation warnings during P10–P12 create *noise* that masks real issues. AI agents reading build output may dismiss real errors as "just more deprecation noise."
- **Suggested fix**: sweep T2 in P10 as part of setup, not in P13. ~1 day of work. Reduces signal-to-noise during the build sprint where it matters most.

#### M10 — Kotlin version pin is not specified.

- **Section reference**: `01-codebase-audit.md` (Kotlin 2.0.21), `PHASE-10-design.md` (no version pin).
- **Issue**: P10 design uses `data object` (Kotlin 1.9+). Code uses Kotlin 2.0.21 today. Newer Kotlin may break things. P10 should pin a Kotlin version explicitly.
- **Suggested fix**: pin Kotlin 2.0.21 in `build.gradle` and document in CLAUDE.md. Bump only at phase boundaries with explicit testing.

#### M11 — JVM target pin (audit notes 1.8) is questioned but no decision.

- **Section reference**: `01-codebase-audit.md` weakness, `04-strategy.md` (silent).
- **Issue**: Audit notes JVM 1.8 target with Kotlin 2.0 and Java 17/21 to compile. This works but is unusual. Should P10 bump to JVM 11 or 17? Affects what stdlib features are available (e.g., `Files.readString` is Java 11+).
- **Suggested fix**: defer to P11. Not blocking. But add a note in P10 that the build pinning is intentional, not legacy.

---

## The Mike-Test (new for the compressed plan)

*Mike is a 31-year-old library author. He maintains [hash-wasm-lite](https://hypothetical), a tiny SHA-256 / CRC32 / xxhash implementation that he ships to npm and PyPI as parallel ports. He learned of Waterfall in late 2026 via a HN post from Aaron titled "I shipped CRC32 to npm, PyPI, and as a C header from one Waterfall source." He's tried Rust + wasm-bindgen for this before and bounced — too heavy, JS users complained about the WASM bundle size. He spends a Friday evening trying Waterfall.*

He installs the tooling, reads `wfpm new`. Runs it. Looks at the scaffolded project. **First impression: the scaffold compiles to JS but the emitted JS is unexpected** — no source maps yet (Tier 2 per design 1.11), no comments mapping back to the .wf source. Mike scrolls the emitted JS and thinks "I won't be able to debug this when a user files an issue."

He writes his CRC32 in Waterfall (~50 lines). `wfpm publish --target js --dry-run` prints a tarball. He opens it. The package.json is generated but missing the `types` field — there's no TypeScript declaration file (this is *not in any v1 spec*). Mike's existing npm users expect `.d.ts`. **He files an issue but doesn't subscribe to it** — npm without types is a deal-breaker for his ecosystem.

He looks at the Python output. The wheel is generated; he checks the type hints — they're correct (Python `: int`, `: str`, etc., emitted from the PythonBackend). But there's no docstring on any function (auto-generation not in v1). His existing PyPI users expect docstrings. Mike notes this but moves on.

He looks at the C header. **This is the surprise** — it works. He runs it through `gcc -fsyntax-only` and it passes. He tries to vendor it into his Zig project; the include works; the symbols link. His first "this is actually impressive" moment is here, in C, not JS.

**The first thing that frustrates Mike (new niche)**: the *npm experience is shallow*. No source maps. No `.d.ts`. The package looks like a Waterfall artifact, not a native npm package. He drops Waterfall not because of `readonly` or sum types or anything in the design — he drops it because **the niche promise ("ship to npm idiomatically") isn't honored at v1.0 technical-complete**.

This is the new R-shape risk: **the library-author niche has a much higher idiomatic-output bar than the teaching niche did, and the v1.0 technical-complete spec doesn't include source maps, TypeScript declarations, Python docstrings, or proper npm package metadata.** All of those are in "Tier 2 — competitive footing," not "v1.0 — legitimacy."

Library-author Mike won't accept v1.0 without them. The niche pivot raises the bar; the spec doesn't.

---

## What the team got right (round 2)

These decisions hold up under the second pass.

1. **The transactional readonly-shadow model** (PHASE-10-design.md §2.2). The team adopted my F8 from round 1 correctly. `markReadonlyLocal` writes only to local shadow; `commitReadonly` is the only durable mutation, invoked at branch joins from the snapshot-then-intersect algorithm. The §2.7 test cases enforce the property. This is the highest-leverage architectural decision in the plan and it's right. (F8 in this round flags a deeper edge case but the core model is correct.)

2. **Lambda captures correctly deferred to P12** (03 §2a.10.A/B). The team listened to my F1 from round 1, split the section into "v1 has no problem" and "P12 will decide with evidence." Removes the soundness gap I flagged. Clean recovery.

3. **PITFALL callouts as a primary documentation device** (PHASE-10-design.md §1.2, §2.2, §3.9, §4.5, §5.2, §5.5, §6.1). 13 explicit pitfalls cross-referenced to the AI-research failure modes. This is *exactly* the discipline 07 §3 ("spec-first") requires. The pitfalls are the single most effective mechanism in the entire P10 design.

4. **The v1.0 split into "technical-complete" and "legitimacy"** (04 §1, line 26). Round 1's F17 ("3-year timeline unrealistic for evenings-and-weekends") forced this. The split honors what AI compresses (technical work) and what it doesn't (adoption/trust). Internally consistent.

5. **Phase-exit checklists with verification rigor criteria** (04 each phase). The "mutation-test kill rate ≥80%" gate has problems (F1 in this round) but the *concept* — every phase has an explicit verification gate — is right. Catching it at phase-exit beats catching it at v1.0 launch.

6. **The README/SPEC/CLAUDE.md trio as persistent context** (04 §3 workflow precondition, PHASE-10-design.md §6.3). The 07 research §3 explicitly called this out as the highest-leverage discipline. The strategy made it a P10 deliverable rather than a "later" item. Right call.

7. **The R8 risk explicitly elevates verification design to "the load-bearing decision in week 1"** (04 R8, R10). Even with F1's tool-spec gap, the *prominence* of verification-as-load-bearing in the strategy is correct. The error is in operationalization, not in priority.

8. **The skeptic findings from round 1 are visibly addressed** (designer's §2 rewrite + transactional model + deferred lambda decision). The team treated round-1 findings as inputs, not as items to dismiss. This is the right relationship between adversarial review and design.

---

## Overall assessment

**The plan holds up better than round 1's plan did, but has one FATAL and an updated drift problem.**

The single FATAL is operationalization of the mutation-testing gate (F1). The strategy uses this metric in 5 separate phase-exit checklists as the single most concrete verification signal — but doesn't specify a tool, doesn't define what counts, and doesn't say what to do when the tool's limitations make the gate impossible to satisfy. Without fixing this before P10 starts, the verification discipline collapses to the no-AI baseline plus optimistic LOC.

The drift problem (R-DRIFT-1) is real but unsurprising: the synthesis at 00-FINAL-PLAN.md was written before the niche pivot, timeline split, module reorder, and readonly re-flip. Synthesis rework is a full rewrite, not edit-pass.

The team's response to round 1 was substantively correct on the highest-severity items (F1 lambda captures, F8 transactional model, F17 timeline reality check). The new RISK items in this round are *finer-grained* — operational, not architectural. That's the right trajectory.

**The single highest-leverage fix**: pick a mutation-testing tool (or drop the metric in favor of property-based + differential + fuzzing) and document it in PHASE-10-design.md before any P10 implementation begins. The strategy's compression promise depends on the verification gate firing correctly; without operationalization the gate doesn't fire at all.

**The single most important pivot**: rewrite the synthesis from scratch. The 00-FINAL-PLAN.md is stale on every load-bearing decision and an edit pass will produce a hybrid that satisfies neither the old framing nor the new niche.

---

*End of review.*
