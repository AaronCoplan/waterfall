# IMPLEMENTATION-LOG.md

Running log of phase kickoffs and retrospectives. One entry per phase.
Templates from `notes/team-output/00-EXECUTION-PLAYBOOK.md` §2.

---

## Phase 10 kickoff — 2026-05-17

- **Strategist roadmap entry:** `notes/team-output/04-strategy.md` §3, P10
- **Predecessor phase exit tag:** `pre-p10-complete` (= `6435e2d`)
- **Spec at implementation start:** `notes/PHASE-10-design.md` at PR-15 merge
  (`5e6beaf`); subsequent §5.2 spec edits land in working tree pending the §5.2 PR.
- **Pre-review skeptic session:** completed; 2 FATAL + 6 RISK + 3 MINOR findings,
  all folded into the spec before implementation began. Key changes: `void[]`
  guard in `fromSourceText`, `forReturnType` adapter, `SourcePosition` data-class
  requirement, `SymbolKind.Function` Pair-ordering KDoc, `fromSymbolTable`
  companion-object fix.
- **Open ambiguities entering implementation:** none at §5.1 boundary; §5.2
  surfaced 8 silent-resolution points during plan-mode + 12 RISK from skeptic
  pre-review, all folded into spec edits.
- **Verification-design commitment (§3 triad):**
  - Property invariants: SymbolTable lookup/declare/shadow (§2.7 cases as forAll
    at N=10000); JoinAnalysis intersection correctness; IrLowering round-trip;
    JsonRenderer schema round-trip
  - Differential oracle: existing golden suite — zero golden diffs gate
  - Adversarial input target: ≥20, location
    `compiler/src/test/resources/adversarial/phase-10/sub-task-N.M/`

---

## Sub-task 5.1 outcome — 2026-05-17 (merged PR #16, master at `755483d`)

- **Triad:** Leg 1 = 28/28 WaterfallTypeTest ✓ • Leg 2 = zero golden diffs ✓ •
  Leg 3 = 35/35 adversarial fixture (fresh-context Agent, §1.2 spec only); full
  convergence including boundary probes (tab prefix, internal whitespace,
  Unicode confusable, 1KB string, `int[][]`, `voidvoid`)
- **Plan-mode iterations:** 3 (converged at zero silent resolutions)
- **Pre-review skeptic (§1):** 2 FATAL + 6 RISK + 3 MINOR → all folded into spec
- **Post-review skeptic (PR diff):** 0 FATAL + 5 RISK + 6 MINOR → 3 in-scope
  fixups landed as commit 8 of PR #16; remaining items deferred to §5.2 or
  accepted-as-is per skeptic verdict
- **Spec edits during sub-task:** 17 (carry-forward 4 + skeptic-driven 10 +
  B1 fromSymbolTable companion 1 + WaterfallTypeTest deliverable 1 +
  fromSourceText whitespace/preservation/totality 3 minus dedup)
- **Commits landed (8):** spec, WaterfallType, SymbolKind, SymbolInfo,
  SourcePosition data-class, WaterfallTypeTest, FromSourceTextAdversarialTest +
  fixture, skeptic-fixup commit
- **Carry-forward into §5.2:** `forReturnType` no-round-trip contract is
  unenforced at type-system level (skeptic R1) — to enforce via §5.2 PR-blocking
  checklist; `SymbolKind.Function.parameters` ordering is opposite of legacy
  `(type, name)` `typedArguments` — explicit code prescription in §5.2 spec;
  PITFALL #8 deferral pattern (function position + TODO comment) established.
- **One sentence on what surprised:** the recurring `git checkout -b` branch-
  base bug (first push of Task #1 was cut from 16 commits behind master,
  silently reverting Phase 0); caught pre-PR-open via diff-stat sanity check.
  Future safeguard: always pre-verify `git rev-parse HEAD == git rev-parse
  origin/master` before `git checkout -b`. Now documented in dispatch briefs.

---

## Sub-task 5.2 status — IN PROGRESS at session checkpoint 2026-05-17

- **State:** plan-mode converged (2 iterations); fresh-context skeptic
  pre-review of §2 completed (0 FATAL + 12 RISK + 6 MINOR); material spec edits
  applied. **Engineer dispatch pending** — they plan-backed v1 against stale
  spec, were redirected to stand down until the full post-skeptic dispatch
  arrives.
- **Branch:** not yet cut. Will be `phase-10/sub-task-5.2-symboltable-typed-api`
  from `origin/master` (currently `755483d`).
- **Working-tree state to preserve:** uncommitted edits to
  `notes/PHASE-10-design.md` (~14 additions across §2.4 + §5.2 spec text); these
  will land as commit 1 of the §5.2 PR.
- **Spec edits applied during §5.2 ramp-up:**
  1. §2.4 — clarified `isImmutable()` preservation (grammar still uses
     `const`/`imm`, not `readonly`)
  2. §2.4 — explicit `forReturnType(returnType)` use (per §1.2 locked contract)
  3. §2.4 — enumerated 6 scope-construction callsite migrations to
     `.enterScope()` (`Main.kt:89`, `ForBlockData:28`, `WhileBlockData:18`,
     `IfBlockData:37,43,50`, `FunctionImplementationData:40`)
  4. §2.4 — PITFALL #8 deferral pattern (function position + TODO comment;
     defer `TypedArgument` to a future sub-task that also touches 3 backends)
  5. §5.2 — added Kotest wiring + `VarInfo.kt` deletion + reader migrations
     (`VariableAssignmentData`, `IncrementStatementData`) + `SymbolTablePropertyTest`
     deliverable
  6. §5.2 — Kotest test-class style PINNED (JUnit-4 `@Test` + `runBlocking { checkAll(...) }`)
  7. §5.2 — Kotest version compatibility note (5.9.1 + Kotlin 2.0.21 fallback)
  8. §5.2 — byte-identical error-strings table (`Duplicate declaration:`,
     `immutable binding` — must survive verbatim for existing tests)
  9. §5.2 — explicit `kotlin.Pair(it.secondVal, ...)` inversion code +
     `functionParametersPreserveNameTypeOrdering` round-trip test (skeptic R9
     mitigation: high-risk silent inversion)
  10. §5.2 — PITFALL #7 behavior change explicitly called out in Expected test
      impact (`fib = 5` after `func fib(...)` now fails verification — pre-flight
      grep across examples/ + test resources required)
- **Team state at pause:** engineer holding, tester holding with 2 drafts
  (`SymbolTableTest.kt` 14 cases including the new parameter-ordering test;
  `SymbolTablePropertyTest.kt` 12 properties at N=10000) plus fixture drafts;
  all background Agents completed; no in-flight work.
- **Resume protocol:** see `notes/team-output/00-KICKOFF-PROMPT-5.2-RESUME.md`.

---

## Phase 10 retrospective — (pending)

_Filled when the phase-exit ritual completes (playbook §2). Template:_

```
## Phase 10 retrospective — {date}
- Calendar weeks budgeted vs. actual: {bud} / {act}
- Triad caught (count): properties {n_prop}, differential {n_diff}, adversarial {n_adv}
- Skeptic findings on exit: {fatal} fatal / {risk} risk / {minor} minor
- Spec edits during phase (count): {n}
- Plan-mode iterations average: {x}
- Carry-forward into phase 11: <list>
- One sentence on what surprised: <text>
```
