# CLAUDE.md — Waterfall Compiler Project Context

This file is read by Claude Code at session start. It tells the AI assistant what
this project is, how to work in it, and what the load-bearing rules are.

---

## Project overview

**Waterfall** is a compiled language targeting JavaScript, Python, and C. The
compiler is written in Kotlin (JDK 17/21, Gradle multi-project build). The
project is in **Phase 10** — a foundation refactor that introduces a typed IR,
a central Verifier, and a typed SymbolTable. No observable language changes in P10;
all existing examples must produce byte-identical output after P10.

Key phases ahead: P10 (this one), P11 (type inference), P12 (sum types + match),
P13 (LSP + package manager), P14 (generics), P16 (v1.0).

---

## Before writing any code

1. **Read the spec first.** The load-bearing spec for any phase is in
   `notes/PHASE-10-design.md` (current). Do not resolve ambiguities in code —
   escalate them. The §6.2 escalation list names the cases explicitly.

2. **Plan-back before coding.** For every non-trivial task, produce an ordered
   commit list and wait for human ack before writing a line of implementation.

3. **Run `./gradlew test` before committing.** Tests must stay green at every
   commit. If a commit breaks tests, fix it before creating the next commit.

---

## Commit format

- Imperative subject line: `feat: add WaterfallType sealed class` not `added WaterfallType`
- Subject ≤ 72 characters
- Body explains the *why*, not the *what*
- **No `Co-Authored-By` trailers** (this repo uses no such convention)
- **Never `--no-verify`** — if a hook fails, fix the underlying issue
- One logical change per commit; don't combine unrelated work

---

## Cross-tree drift rule (F10)

Spec edits and implementation commits must stay in sync. If you write code that
resolves a spec ambiguity without editing the spec to remove that ambiguity, the
spec is wrong and future sessions will re-discover the same ambiguity.

**Protocol:** When you encounter an ambiguity:
1. Stop. Do not silently resolve it in code.
2. Surface it as a `// SPEC-AMBIGUITY: <description>` comment if at end-of-session.
3. In a fresh session, edit the spec to remove the ambiguity, then re-plan.

---

## Key files

| File | Purpose |
|---|---|
| `BUS-FACTOR.md` | Pipeline overview, decision log, how to cut a release |
| `notes/PHASE-10-design.md` | Load-bearing spec for P10 (13 PITFALLs, §6.2 escalations) |
| `notes/team-output/00-EXECUTION-PLAYBOOK.md` | Operational rhythm: spec-first loop, §3 verification triad, §7 PR template |
| `notes/team-output/04-strategy.md` | Roadmap P10–P16, phase-exit checklists |
| `SPEC.md` | Top-level vision + TOC (this file) |
| `notes/VERIFICATION-DISCIPLINE.md` | Per-phase triad outcome log |
| `IMPLEMENTATION-LOG.md` | Phase-kickoff + retrospective entries |

---

## Test suite

```bash
./gradlew test           # full suite (golden + unit + runtime-check)
./gradlew build          # compile only
UPDATE_GOLDEN=1 ./gradlew test --tests GoldenTests   # regenerate goldens
```

**Golden tests are the correctness oracle for P10.** Any golden change in P10 is
a stop-the-line event — escalate immediately. Goldens should be byte-identical
before and after P10.

---

## AI-augmented development disclosure

This project is built with spec-first AI-augmented implementation per
`notes/team-output/00-EXECUTION-PLAYBOOK.md` §1. The dominant risk is
**"tests pass, code is wrong"** (failure mode #1 from the AI-research doc).
Mitigations are the verification triad (§3): property tests at N=10000,
differential oracle vs. goldens, and adversarial inputs (≥20 per phase).

If you are an AI agent working in this repo: read the PITFALLs in
`notes/PHASE-10-design.md` before writing any P10 code. Escalate the §6.2
cases. Do not silently resolve ambiguities.
