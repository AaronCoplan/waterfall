# VERIFICATION-DISCIPLINE.md

Per-phase log of verification triad outcomes. Filled as each sub-task lands.
See `notes/team-output/00-EXECUTION-PLAYBOOK.md` §3 for the triad design.

The three legs per phase:

| Leg | What it covers | Pass signal |
|---|---|---|
| **Property tests** (Kotest, N=10000) | Invariant-level correctness over generated inputs | No failing property at N=10000 |
| **Differential oracle** | Byte-equivalent output vs. a named known-good | Zero golden diffs |
| **Adversarial inputs** (≥20 per phase) | Edge cases and silent-resolution traps | No `compiler-broke` cases unfixed |

---

## Phase 10 — Foundation Refactor

**Oracle:** existing golden suite (prior `*Data` AST emission + `GoldenTests`).
Mechanism: for every example, old and new pipeline must produce byte-equivalent
emission per target. Zero golden diffs is the gate.

**Adversarial input location:** `compiler/src/test/resources/adversarial/phase-10/`

### Sub-task 5.1 — WaterfallType, SymbolKind, SymbolInfo

- Property tests: N/A — `WaterfallTypeTest` (28/28 pass) is example-based, not a Kotest forAll suite; mislabeled as Leg 1 in the §5.1 kickoff brief (see retrospective)
- Differential oracle: zero golden diffs ✓ (22 programs × 3 backends = 66 parameterized tests)
- Adversarial inputs: 35/35 pass — fresh-context Agent, §1.2 spec only; `compiler/src/test/resources/adversarial/phase-10/sub-task-5.1/`

### Sub-task 5.2 — SymbolTable migration

- Property tests: 12/12 pass at N=10,000 (`SymbolTablePropertyTest` — SymbolTable invariants from §2.7) ✓
- Differential oracle: zero golden diffs ✓
- Adversarial inputs: 48/48 pass (25 positive + 23 negative; `Sub52AdversarialTest`) ✓; `compiler/src/test/resources/adversarial/phase-10/sub-task-5.2/`

### Sub-task 5.3 — Verifier package + JSON errors

- Property tests: N/A (JoinAnalysis stubbed per OQ-1=C; verifier dispatch is routing logic, no rich generative invariants)
- Differential oracle: zero golden diffs ✓
- Adversarial inputs: 60/60 pass (25 positive + 35 negative; `Sub53AdversarialTest`) ✓; `compiler/src/test/resources/adversarial/phase-10/sub-task-5.3/`; **pre-merge bug catch:** `VoidNotAValueType` declared but never emitted — verifier checked `ErrorType` but not `VoidType`; fixed in commit `1d64587`

### Sub-task 5.4 — IR package + lowering pass

- Property tests: 3/3 pass at N=10,000 (`IrTypeRoundTripPropertyTest` — IrType↔WaterfallType round-trip) ✓
- Differential oracle: zero golden diffs ✓ + 3 IR-oracle golden tests (`golden-ir/`) ✓
- Adversarial inputs: 77/77 pass (37 compile_success + 24 verify_fail + 16 lower_fail; `Sub54AdversarialTest`) ✓; `compiler/src/test/resources/adversarial/phase-10/sub-task-5.4/`; **pre-merge bug catch:** OQ-5.4-1 — Elaboration must store `VoidType` (not absent) for undeclared names; spec-synced in commit `7a`

### Sub-task 5.5 — Backend migration (JS → Python → C)

- Property tests: N/A (backend codegen; no new property family; existing 3 IrType properties still pass)
- Differential oracle: zero golden diffs ✓ at every commit; enforced by `scripts/check-goldens-unchanged.sh`; 22 programs × 3 backends = 66 parameterized golden tests
- Adversarial inputs: N/A (per Aaron D4 — existing 66 golden tests across the full pipeline are the §5.5 oracle)

### Sub-task 5.6 — Remove old paths + phase exit

- Property tests: N/A (deletion only; no new property family; all 15 existing property tests still pass)
- Differential oracle: zero golden diffs ✓
- Adversarial inputs: N/A (pure dead-code removal; no behavioral changes)

### Phase 10 exit summary

- **Leg 1 (property tests):** 15 total at N=10,000 — 12 SymbolTable (§5.2, `SymbolTablePropertyTest`) + 3 IrType (§5.4, `IrTypeRoundTripPropertyTest`). Note: §5.1 `WaterfallTypeTest` (28/28) is example-based, not a Kotest property suite.
- **Leg 2 (differential oracle):** Zero golden diffs across all sub-tasks; oracle = 22 programs × 3 backends = 66 parameterized golden tests + 3 IR-oracle tests; `scripts/check-goldens-unchanged.sh` enforced per-commit from §5.5 onward.
- **Leg 3 (adversarial inputs):** 220 total entries (35 §5.1 + 48 §5.2 + 60 §5.3 + 77 §5.4 + 0 §5.5 + 0 §5.6); 2 pre-merge bugs caught: (1) §5.3 `VoidNotAValueType` never-emitted; (2) §5.4 OQ-5.4-1 Elaboration/VoidType contract.

---

## Phase 11 — Type inference + condition type-checking

_Pending._

---

## Phase 11.5 — C execution oracle + cross-target consistency

_Pending._

---

## Phase 12 — Sum types + match + @external

_Pending._

---

## Phase 13 — LSP + package manager + stdlib coherence

_Pending._

---

## Phase 14 — Generics (monomorphization)

_Pending._

---

## Phase 15 — Vec<T>, Map<K, V> stdlib

_Pending._

---

## Phase 16 — v1.0 freeze + full adversarial review

_Pending._
