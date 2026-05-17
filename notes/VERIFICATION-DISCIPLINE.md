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

- Property tests: _pending_
- Differential oracle: _pending_
- Adversarial inputs: _pending_

### Sub-task 5.2 — SymbolTable migration

- Property tests: _pending_ (SymbolTable invariants from §2.7)
- Differential oracle: _pending_ (goldens unchanged)
- Adversarial inputs: _pending_

### Sub-task 5.3 — Verifier package + JSON errors

- Property tests: _pending_ (JoinAnalysis intersection, JsonRenderer round-trip)
- Differential oracle: _pending_ (goldens unchanged)
- Adversarial inputs: _pending_

### Sub-task 5.4 — IR package + lowering pass

- Property tests: _pending_ (IrLowering round-trip)
- Differential oracle: _pending_ (goldens unchanged)
- Adversarial inputs: _pending_

### Sub-task 5.5 — Backend migration (JS → Python → C)

- Property tests: _pending_
- Differential oracle: _pending_ (goldens unchanged per backend)
- Adversarial inputs: _pending_

### Sub-task 5.6 — Remove old paths + phase exit

- Property tests: _pending_ (full suite at N=10000)
- Differential oracle: _pending_ (full golden suite)
- Adversarial inputs: _pending_ (≥20 AI-generated, fresh session)

### Phase 10 exit summary

_Filled when phase-exit ritual completes (per playbook §2)._

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
