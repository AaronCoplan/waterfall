# Waterfall — Implementation Kickoff Prompt

**How to use this file.** Open a fresh Claude Code session at the Waterfall repo root (`/Users/afcoplan/Documents/github/waterfall`). Invoke `/teamwork` and paste the prompt below as the task. The `/teamwork` skill will design and approve a team; the team then executes the pre-P10 cleanup + P10 foundation refactor autonomously, with explicit escalation rules for when to pause and ping you.

This prompt is self-contained — the new team starts with no memory of the planning session. All necessary context lives in the linked artifacts.

---

## The prompt (paste into `/teamwork`)

```
You are spinning up an implementation team for Waterfall (https://github.com/AaronCoplan/waterfall — local repo at /Users/afcoplan/Documents/github/waterfall). The strategic planning team has completed its work and produced a full plan plus a load-bearing P10 design doc. Your job is to execute pre-P10 cleanup + P10 (foundation refactor), end-to-end, with the AI-augmented spec-first workflow.

## Required reading (in this order)

1. `notes/team-output/00-FINAL-PLAN.md` — the strategic plan. Read in full. Sections 6, 7, and 15 are load-bearing.
2. `notes/team-output/00-EXECUTION-PLAYBOOK.md` — the operational runbook. **This is your day-to-day reference.** Sections on the spec-first loop, the verification triad, phase rhythm, and PR template are mandatory.
3. `notes/PHASE-10-design.md` — the load-bearing spec for P10. Includes 13 PITFALL callouts and 7 ESCALATE items. Treat these as binding.
4. `notes/team-output/03-language-design.md` (post round-4) — feature catalog and `readonly` design. Reference, not required cover-to-cover.
5. `notes/team-output/07-ai-augmented-dev-research.md` — empirical evidence for the spec-first loop and the AI failure modes you are defending against.

## Your mission

Execute, in order:

### Phase 0 — Pre-P10 cleanup (target: 1 evening)

Three small landing PRs, each on its own branch off `master`:

1. **Drop the legacy backend.** Delete `compiler/src/main/kotlin/.../target/LegacyTextBackend.kt`. Delete `compiler/src/test/resources/golden/legacy/`. Update `Backends.kt` registration. Change default `--target` from `legacy` to `js`. Strip references from `README.md`, `examples/` (none should mention legacy), CLI help text, and `notes/AUDIT-OPEN-QUESTIONS.md` (mark legacy items as resolved). Test plan: `./gradlew test` green; `./waterfall examples/FibonacciModule.wf` defaults to JS output.

2. **`BUS-FACTOR.md` at repo root.** Roughly 1–2 pages covering: (a) how to cut a release; (b) the verifier/IR pipeline layout at a glance; (c) which decisions are reversible vs load-bearing; (d) the three test invocations (`./gradlew test`, the runtime checks, the adversarial-input runs); (e) pointers to `notes/team-output/` for strategy and `notes/PHASE-10-design.md` for the load-bearing spec.

3. **Verifier enforces `const`/`imm` today.** Audit found `VariableAssignmentData.verify` and `IncrementStatementData.verify` are no-ops; even `const x = 4; x = 5` slips through Waterfall (caught only by the target). Implement the simplest correct enforcement: track immutability in the existing `SymbolTable.nameToInfoMap` (this is interim — `SymbolInfo` replaces it in P10 proper). Add 5 negative tests covering: assignment to const, increment of const, compound assignment to const, assignment-to-imm, increment-of-imm.

After all three land on master, tag the commit `pre-p10-complete`.

### Phase 1 — P10 (target: 2–3 weeks of focused evening/weekend work)

Execute the six sub-tasks of P10 per `notes/PHASE-10-design.md` Section 5 (5.1–5.6) in the documented order, each as its own PR. Before EACH sub-task:

1. **Spec-first plan-mode loop.** Open Claude Code in plan mode against the relevant section of `notes/PHASE-10-design.md`. Have it draft a plan. Read the plan and explicitly identify "what ambiguities did it silently resolve?" Edit the spec to remove those ambiguities. Re-plan. Loop until plan is approved without spec edits.
2. **Adversarial pre-review.** Spawn a fresh skeptic agent (no shared context with the implementer) to review the spec section. Apply any FATAL or RISK fixes before implementation begins.
3. **Implement.** Plan-mode-approved spec drives the implementation session.
4. **Adversarial post-review.** Skeptic reviews the PR. Property tests + differential oracle + adversarial inputs must all pass before merge.

The verification triad is non-negotiable per phase-exit:
- Property-based tests using Kotest (N=10000 generators)
- Differential testing against the existing `*Data` AST emission as the oracle (byte-equivalent goldens preserved)
- ≥20 AI-generated adversarial inputs per sub-task

P10 phase-exit gate: `notes/team-output/04-strategy.md` §3 P10 phase-exit checklist passes; tag commit `phase-10-complete`; write retrospective in `notes/IMPLEMENTATION-LOG.md` (create if missing).

## Guardrails (non-negotiable)

- **Spec-first.** No implementation session starts without a plan-mode approval. The dominant AI failure is "tests pass, code is wrong" via verifier overfitting — see `07-ai-augmented-dev-research.md` §2. Spec ambiguities are the upstream cause.
- **The triad is mandatory.** All three legs (property tests, differential oracle, adversarial inputs) must show "no regressions" before any phase or sub-task is marked complete. Skipping a leg = explicit Aaron escalation.
- **Aaron writes the verifier-correctness tests.** AI does NOT write tests for verifier correctness paths (symbol-table invariants, JoinAnalysis correctness, type-system soundness). AI may write backend-output tests, parser tests, and trivial helpers. Trip-wire: any verifier-correctness test file with only AI authorship = red flag, pause and escalate.
- **JSON-first errors.** Per Q10. Errors emit as JSONL on stderr by default; `--errors human` for the formatted version. Schema at `notes/error-schema-v1.json` (designer's round-4 spec).
- **Kotest for property tests.** Per Q11. `kotest-property` 5.9.1 + `kotest-runner-junit4` 5.9.1.
- **No co-author trailers in commits.** Per Aaron's global CLAUDE.md. Do NOT add `Co-Authored-By: Claude` lines.
- **Git hygiene.** Never `--no-verify`, never force-push to master, always conventional one-feature-per-PR. Each PR's description follows the template in `00-EXECUTION-PLAYBOOK.md` §7.

## When to pause and escalate to Aaron

| Trigger | Action |
|---|---|
| A FATAL skeptic finding before or after a sub-task | Pause, surface the finding + proposed resolution, wait for Aaron's call. |
| A spec ambiguity that blocks implementation and resists the plan-mode loop | Pause, propose 2–3 candidate resolutions, ask Aaron to pick. |
| Schedule slippage on a sub-task >1.5× the estimate in `04-strategy.md` §3 | Pause, document why, propose either continued slippage with reason or scope cut. |
| The verification triad fails on phase exit | Pause, do NOT advance. The failing leg dictates the remediation (property test = add invariant; differential = add oracle; adversarial = generate more inputs). |
| Discovery of architectural debt not catalogued in audit's D1–D10 | Surface; do not auto-absorb into the current phase. |
| Strategic direction question (niche, tier, timeline) | Always Aaron. |
| Decision about which Kotlin / Gradle / library version to pin | Decide autonomously; document in `IMPLEMENTATION-LOG.md`. |
| Implementation detail within the spec | Decide autonomously. |

## Done criteria for this kickoff

Two milestones — declare done only when both are verifiably true:

1. **Pre-P10 complete**: all three pre-P10 PRs merged; `./gradlew test` green; tag `pre-p10-complete` on master.
2. **P10 complete**: all six sub-tasks of `notes/PHASE-10-design.md` §5 implemented and merged; phase-exit checklist from `04-strategy.md` §3 P10 passes; tag `phase-10-complete` on master; retrospective in `notes/IMPLEMENTATION-LOG.md`.

When both are done, send Aaron a SendMessage summary with: (a) commits landed (with hashes), (b) any open ambiguities surfaced for P11, (c) deviations from the spec (if any) with rationale, (d) honest assessment of whether P10 should have been bigger or smaller given what you learned. Then go idle. Do NOT proceed to P11 without Aaron's go-ahead.

## Team composition

Design the team to match these roles (defer to the `/teamwork` skill's team-design step for final composition):

- **Team lead** (coordination, synthesis, routing)
- **Implementation engineer** (writes production Kotlin; consumes specs, produces PRs)
- **Tester** (writes property tests with Kotest, generates adversarial inputs, runs differential oracle setup) — note role discipline: this agent does NOT do implementation, only testing
- **Skeptic** (per-PR adversarial review; fresh context per session for the highest-stakes PRs)
- **Designer** (only spawned when a spec ambiguity needs resolution mid-sprint)

The model tier policy from the strategic team applies: Skeptic + Lead on Opus; Implementation Engineer + Tester on Sonnet; Designer on Opus when invoked.

## A note on rhythm

This is a multi-week sprint. Pace matters. Reference `00-EXECUTION-PLAYBOOK.md`:
- §2 (phase rhythm) for the three rituals
- §4 (first 100 hours) for the first sprint's literal task list
- §5 (signals/dashboards) for staying-on-track signals
- §6 (recovery playbook) for when something breaks

Aaron is at side-project pace (evenings + weekends). The team should respect this — don't expect 8-hour blocks of attention; expect 2-hour bursts. Plan-mode sessions are short and reviewable; implementation sessions are bounded; adversarial reviews can run in background.

Good luck. The spec is load-bearing. The triad is non-negotiable. Build the floor before the ceiling.
```

---

## After P10 — kicking off P11 / P11.5 / P12 etc.

When `phase-10-complete` is tagged, open a NEW `/teamwork` session with a similar prompt — substitute "execute P11 + P11.5" and reference the new phase design doc that should exist by then (the P10 team should write `notes/PHASE-11-design.md` and `notes/PHASE-11.5-design.md` as part of their phase-exit retrospective). Each phase = its own kickoff. Do not let one team drive multiple phases sequentially without your gate in between.

The pattern: **one phase per team session, gated by your review of the phase-exit checklist + retrospective.** This is the human-in-the-loop signal that prevents the "tests pass, code is wrong" failure mode from compounding across phases.
