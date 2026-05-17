# SPEC.md — Waterfall Language Specification (Living Artifact)

**Status: living artifact — not v1.0 yet.** This file is the top-level entry point
for the Waterfall language spec. It captures the vision and points to per-phase
design documents. It will become a full language reference at v1.0 (P16).

---

## Vision

Waterfall is a statically-typed language that compiles to **JavaScript, Python,
and C**. It is designed for **library authors** who need to ship one implementation
to three ecosystems without maintaining three separate codebases.

Design philosophy:

- **Gleam-vibe ergonomics**: immutable-by-default, clear error messages,
  no hidden control flow, exhaustive pattern matching.
- **Library-author first**: the FFI model (`@external`) lets a Waterfall
  function delegate to a native implementation per target; the type system
  ensures the Waterfall call-site is well-typed regardless of which target
  is active.
- **Zero-dependency output**: compiled artifacts are idiomatic JS/Python/C —
  no runtime library, no bundler required.

Target user: a developer who wants to write a parsing library, a serialization
library, or a data-structure library *once* and publish it to npm, PyPI, and a
C header simultaneously.

---

## Current language features (P10 baseline)

- Scalar types: `int`, `dec`, `bool`, `char`; array types: `int[]`, `char[]`, etc.
- Typed and inferred (`x :=`) variable declarations; `readonly` modifier
- Functions with typed parameters and return types; recursion
- `if`/`elif`/`else`, `while`, `for` (iterator-style)
- `readonly x` flow-sensitive freeze statement (Form B)
- Function calls (local, module-qualified, object-method)
- Array literals and indexing; bundle literals
- Lambdas (single-expression body)
- Module system (one module per file)

---

## Roadmap (phase → feature)

| Phase | Key deliverables | Spec doc |
|---|---|---|
| **P10** (current) | Typed IR, typed SymbolTable, central Verifier, JSON-first errors | `notes/PHASE-10-design.md` |
| P11 | Type inference (`:=`), condition type-checking (`bool` only) | TBD |
| P11.5 | C backend execution oracle; cross-target runtime consistency | TBD |
| P12 | Sum types + `match`, exhaustiveness, `@external` enforcement | TBD |
| P13 | LSP, stdlib coherence, package manager (`wfpm`) | TBD |
| P14 | Generics (monomorphization) | TBD |
| P15 | `Vec<T>`, `Map<K, V>` stdlib | TBD |
| P16 | v1.0 — spec freeze, full adversarial review, publishing pipeline | TBD |

Full roadmap with phase-exit checklists: `notes/team-output/04-strategy.md` §3.

---

## Per-phase design documents

Each phase has a dedicated design doc in `notes/`:

- `notes/PHASE-10-design.md` — current; load-bearing spec with 13 PITFALLs and
  §6.2 escalation list. Read this before any P10 implementation work.

Future phases will add `notes/PHASE-11-design.md`, etc. Each doc is locked at a
commit before implementation begins (`phase-N-spec-locked` tag) and carries a
tracked-delta log for any mid-implementation spec changes.

---

## Error code registry

Stable error codes are defined in `notes/error-schema-v1.json`. The `WF1xxx`
range covers P10-era errors; `WF2xxx` covers P12-era (`@external`, sum types);
etc. Codes are stable across versions; renaming a `VerifyError` class is fine,
changing its code is a breaking change.

---

*This document is maintained alongside the implementation. Edit it when the
language adds a feature or when a design decision becomes canonical. The goal
is that a new contributor can read SPEC.md + BUS-FACTOR.md and know where
to look for everything else.*
