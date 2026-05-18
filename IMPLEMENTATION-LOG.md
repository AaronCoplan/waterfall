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

## Sub-task 5.2 outcome — 2026-05-17 (PR opening; awaiting Aaron merge)

- **Triad:** Leg 1 = 12 properties at N=10,000 ✓ • Leg 2 = zero golden diffs ✓ •
  Leg 3 = 48/48 adversarial (25 positive + 23 negative; fresh-context Agent
  reading §2 + §5.2 + branch diff)
- **Plan-mode iterations:** 2 (converged at zero silent resolutions in the
  resumed session; the original session's v1 plan-back pre-dated the post-
  skeptic spec edits and was directed to stand down)
- **Pre-review skeptic (§2):** 0 FATAL + 12 RISK + 6 MINOR → material findings
  (R4 / R6 / R9 / R12 + Kotest style pin + version fallback) folded into spec
  edits during ramp-up; remaining items accepted-as-is per skeptic verdict
- **Post-review skeptic (PR diff):** 0 FATAL + 5 RISK + 6 MINOR → R1–R5 applied
  as commit 7 (this commit); MINOR #4–#6 accepted-with-comment (Arb.string
  printable-ASCII KDoc clarity, top-level error string wording, commitReadonly
  per-name lookup perf — all low-impact)
- **Spec edits during sub-task:** 12 (10 ramp-up edits from IMPLEMENTATION-LOG
  pre-resume + commit 5.5 PINNED-style fix + commit 5.75 OQ-1 function-self-name
  clarification)
- **Commits landed (9):** docs(spec+log), build(Kotest deps), refactor(atomic
  SymbolTable rewrite + callsite migrations), test(SymbolTableTest 14 cases),
  test(SymbolTablePropertyTest 12 properties), spec(PINNED style fix), spec(OQ-1
  self-name shadowing), test(Sub52AdversarialTest + 48-entry fixture),
  refactor(post-skeptic fixups)
- **Carry-forward into §5.3:**
  - OQ-2 deferred: top-level decl-order error position UX — when `func add() {}`
    and `int add = 99` collide at module level, Main.kt processes vars first so
    the error reports at the function's source position regardless of source
    order. Not a correctness bug; surface for P11+ diagnostics quality work.
  - `commitReadonly` calls `lookup(name)` per name (O(names × scope-depth));
    revisit if §5.3's join code measurements show it as hot.
  - `exitScope` is wired in §5.3 (no production caller yet in §5.2); see §2.5
    branch-join API contract.
  - PITFALL #7 (functions implicitly readonly) is now a no-op for the current
    corpus per pre-flight grep; verified no example source reassigns a function
    name.
- **One sentence on what surprised:** the playbook §1 trip-wire (`./gradlew
  test --tests SymbolTablePropertyTest` reports zero tests) fired exactly as
  designed — tester caught the §5.2 PINNED-style expression-body example as a
  spec defect (`InvalidTestClassError: Method should be void`), surfaced as F10
  cross-tree drift rather than silently fixing in code, and the spec was synced
  at commit ac5ab5e.

---

## Sub-task 5.3 outcome — 2026-05-18 (PR opening; awaiting Aaron merge)

- **Triad:** Leg 1 = N/A (JoinAnalysis stubbed per OQ-1=C; verifier dispatch is
  routing logic, no rich invariants to probe) • Leg 2 = zero golden diffs ✓ •
  Leg 3 = 60/60 adversarial (25 positive + 35 negative; fresh-context Agent
  reading post-edit §4 + §5.3 + branch diff). **Leg 3 caught `VoidNotAValueType`
  declared-but-never-emitted bug pre-merge — textbook triad working as designed
  per playbook §5 "1–5 finds/phase = healthy."**
- **Plan-mode iterations:** 1 (engineer's v1 plan-back covered all 13 mandatory
  skeptic spec-edits + 4 SA resolutions; cleanly acked)
- **Pre-review skeptic (§3 + §4 + §5.3):** 5 FATAL + 13 RISK + 6 MINOR → all
  resolved before implementation. The 4 OQs (1, 2, 3, 4) decided by Aaron:
  OQ-1=C (stub JoinAnalysis); OQ-2=B (drop target param); OQ-3=C (document
  identifier-resolution gap, P11 closes); OQ-4=D (ship renderer interface +
  HumanRenderer; JsonRenderer stub)
- **Post-review skeptic (PR diff):** 0 FATAL + 5 RISK + 6 MINOR → R2, R3, R4,
  R5 + coverage gaps C1/C2/C3 applied as commit 7; R1 (function-scope-lifetime
  gap) documented as §5.4 carry-forward (see below); M1 + M5 applied; remaining
  MINOR accepted-with-comment
- **Spec edits during sub-task:** 13 mandatory edits (commit `fa68661`) + small
  follow-ups in commit 7 (R2 iterator-declaration spec comment + R3 message
  wording)
- **Commits landed (8):** spec(13 edits), feat(verifier skeleton), refactor(atomic
  migration), test(VerifierTest 7 cases), test(JoinAnalysisStubTest 3 cases),
  fix(VoidNotAValueType emission — Leg 3 catch), test(Sub53AdversarialTest +
  60-entry fixture), refactor(post-skeptic R2–R5 + coverage + MINOR fixups)
- **Carry-forward into §5.4 — Aaron decision required at §5.4 plan-mode:**
  - **F1 (function-scope-lifetime gap):** `verifyFunctionDeclaration` discards
    function-body scopes after `exitScope`. §5.4's `IrLowering.lowerExpression`
    cannot resolve function-local names (parameters, local vars) from the
    top-level `symbolTable`. Three options:
    - **(a)** Verifier returns per-function-scope map (`Map<String, SymbolTable>`).
    - **(b)** IrLowering recreates scopes during lowering (skeptic: "bad").
    - **(c)** Verifier elaborates each `ExpressionData` with
      `resolvedType: WaterfallType?` at verify time; IrLowering reads from there.
      Standard elaboration pattern; P11 inference builds on top.
      **Skeptic-recommended option.** Aaron decides at §5.4 plan-mode.
  - **OQ-3=C + OQ-5.4-1 (identifier-resolution gap + F1=C interaction):**
    `verifyVarAssignment` and `verifyIncrement` silently no-op on null lookup.
    §5.4 Elaboration stores `WaterfallType.VoidType` for undeclared names (not
    absent); IrLowering produces `IrExpression.Identifier(name, IrType.Void)`
    without throwing — preserves differential-oracle invariant. P11 closes the
    gap with `VerifyError.UnknownIdentifier`. (Leg 3 Agent caught the
    divergence from plan-back v1 "throw if absent" during fixture validation.)
  - **`commitReadonly` per-name lookup perf** (§5.2 carry-forward): still
    applicable; revisit if §5.5 backend migration makes it hot.
  - **`exitScope` snapshot consumers:** JoinAnalysis and StatementVerifier
    discard the returned snapshot in §5.3; P12 join-intersection will consume.
- **One sentence on what surprised:** Leg 3 fresh-context Agent caught the
  `VoidNotAValueType`-never-emitted bug at adversarial-fixture-validation time —
  the verifier guard checked `ErrorType` but not `VoidType`, so `void x = 5`
  compiled cleanly; bug + regression tests landed atomically in commit `1d64587`
  before the fixture committed.

---

## Sub-task 5.4 outcome — 2026-05-18 (PR opening; awaiting Aaron merge)

- **Triad:** Leg 1 = 4 IrType round-trip properties at N=10,000 ✓ •
  Leg 2 = zero golden diffs ✓ + 3 golden-IR oracles ✓ •
  Leg 3 = 77/77 adversarial (37 compile_success + 24 verify_fail + 16
  lower_fail; fresh-context Agent reading §3 + §5.4 + branch diff).
  **Leg 3 caught OQ-5.4-1 (Elaboration stores VoidType, not absent, for
  undeclared names — preserved UX per OQ-3=C; spec-synced commit 7a).**
- **Plan-mode iterations:** 1 (v1 plan-back covered all 7 mandatory pre-
  review skeptic edits + 5 SA resolutions; cleanly acked)
- **Pre-review skeptic:** 3 FATAL + 7 RISK + 6 MINOR → all resolved via
  F1=C (side-table elaboration), Q3=Void, OQ-3=C handoff + 7 spec edits
- **Post-review skeptic:** 0 FATAL + 5 RISK + 6 MINOR → R1–R5 + M1/M2/M4/M5
  applied as commit 8; M3 (LAMBDA_POS) + M6 (projectRoot normalization)
  deferred-with-comment; R3 (forward function reference bug) fixed + regression test
- **Spec edits during sub-task:** 7 mandatory (commit 1) + 1 OQ-5.4-1 sync (commit 7a) +
  2 from commit 8 (§3.7 lowerModule KDoc + §5.4 source-position TODO note)
- **Commits landed (9):** spec(7 edits), feat(elaboration + VerifyResult.resolvedTypes),
  feat(ir/ skeleton), feat(IrLowering.lowerModule), test(IrLoweringTest golden-IR),
  test(IrTypeRoundTripPropertyTest 4 props), spec(OQ-5.4-1 sync),
  test(Sub54AdversarialTest + 77-entry fixture), refactor(post-skeptic R1–R5 + MINOR)
- **Carry-forward into §5.5:**
  - Pre-condition guard: backends MUST check `verifyResult.isSuccessful` before IrLowering
  - Source position granularity: per-expression positions collapse to parent statement (P11)
  - BinaryOp.type is left operand type (P10 placeholder; P11 fixes for comparison ops)
  - FunctionCall.type for MODULE/OBJECT kinds is always Void (P10 placeholder)
  - Forward function references now resolve correctly via Pass 1.5 (commit 8 R3 bug fix)
- **One sentence on what surprised:** the Elaboration two-pass approach required
  re-declaring variables into nested child scopes (if/while/for bodies) so subsequent
  statements in the same scope could look up earlier-declared variables during
  elaboration — the verifier's child scopes are discarded before elaboration runs,
  so elaboration must reconstruct them independently.

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
