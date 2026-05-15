# Waterfall — The Plan (v2, post-iteration)

**Goal of this document.** The strategic plan you can act on. Updated after iteration with the user — nine decisions answered, three sub-decisions answered, two adversarial rounds, four documents rewritten. Companion document: `00-EXECUTION-PLAYBOOK.md` (operational runbook; "what to do tomorrow morning"). This doc is the "what + why"; the playbook is the "how."

Backing material in `notes/team-output/`:

- `01-codebase-audit.md` — what Waterfall is today, file:line
- `02-pl-landscape.md` — modern PL + 16 transpiled-language case studies
- `03-language-design.md` — full feature catalog + the `readonly` design (post-edits)
- `04-strategy.md` — long-term roadmap, niche, risks (post round-3)
- `05-adversarial-review.md` — round-1 findings (1 FATAL, 15 RISK, 10 MINOR)
- `06-quality-review.md` — critic's quality gate
- `07-ai-augmented-dev-research.md` — empirical evidence for the compressed timeline
- `08-adversarial-review-2.md` — round-2 findings on the compressed plan (1 FATAL, 22 RISK, 12 MINOR)
- `notes/PHASE-10-design.md` — the load-bearing spec for P10 (~1920 lines, post round-4)

---

## 1 · Two users named Mike

Two vignettes from the adversarial reviews — read them first.

### Mike, round 1 — early adopter

*Mike, 28, JS background, weekend Rust dabbler. He installs Waterfall in 20 minutes, writes a small program, hits `verification failed`, has to scroll stderr to find the file:line. He hits another error with similar UX ten minutes later and switches back to TypeScript "for now."*

**The first thing that frustrates Mike is not `readonly`. It is the diagnostic UX.** The audit calls this D5; the proposed roadmap puts the fix in P13. *Floor before ceiling.*

### Mike, round 2 — library author

*Mike, library author of a CRC32 implementation he currently maintains in parallel JS/Python/C. He rewrites in Waterfall, runs `wfpm publish`. The npm package has no source maps. The Python package has no docstrings, no type stubs (`.pyi`). The C release has no metadata, no header guards, no version pin. He compares against `serde`'s Rust output. He uninstalls.*

**The library-author niche has a higher idiomatic-output bar than the teaching niche did.** Source maps, `.d.ts`, docstrings, package metadata are not "polish for v1.1" — they are table stakes for v1.0 technical. Absorbed into P14.

Both vignettes resolve to the same operating principle: **invisible foundation work before visible features**. The plan that follows is built around this.

---

## 2 · Recommendations at-a-glance

**The thesis in one sentence**: Waterfall should aim for the upper end of "sustained niche" (Gleam / Crystal / Nim class), serving **polyglot library authors writing focused code that ships across runtimes**, with **Gleam-grade language-design taste** — built via **AI-augmented spec-first development** that compresses *technical completeness* to 5–7 months of weekend/evening work while keeping the *adoption-bound* legitimacy milestones on a 1–3 year calendar.

The defining choice: **v1.0 splits into two milestones**. Technical-complete is compressible; legitimacy is not.

The defining risk: **verification overfitting** ("tests pass, code is wrong"). Mitigated by a tool-agnostic per-phase triad (property tests + differential oracle + AI-generated adversarial inputs), not a mutation-test slogan.

The defining workflow: **spec → plan-mode loop → adversarial review → implement**. Klabnik's Rue, Carlini's C compiler, and Lambeau's Elo all followed it. Hejlsberg and Zig didn't and stopped.

---

## 3 · The twelve decisions (all answered or deferred)

| # | Decision | Answer |
|---|---|---|
| Q1 | Tier + niche | **Tier B upper end + library-author / Gleam-vibe combined**; teaching = content marketing |
| Q2 | Timeline | **5–7 months for v1.0 technical-complete**; 12–24 months for v1.0 legitimacy; AI-augmented, spec-first |
| Q3 | Second maintainer by P12 | **Deferred** — revisit during P16.5 adoption phase |
| Q4 | `readonly` as headline | **Implement first, decide later**; default to "one of many features" per R5 |
| Q5 | Drop legacy backend | **Yes, now** (pre-P10 cleanup) |
| Q6 | WASM target | **Deferred** — defer until late P13 / P17; R6 monitoring active |
| Q7 | Foundation-style name | **Deferred** — stay personal repo; rebrand if success forces it |
| Q8 | AI transparency posture | **Transparent but not leading** — niche pitch on homepage, AI-augmented build in CONTRIBUTING + engineering blog |
| Q9 | Adversarial review budget | **No budget** — best product, fast; review discipline is the limit, not money |
| Q10 | Error format | **JSON-first** (JSONL on stderr by default; `--errors human` for the colorized formatter) |
| Q11 | Property-based test framework | **Kotest** (sealed-class arbiters, automatic shrinking, JUnit-4 runner integration) |
| Q12 | Gradle T2 deprecation timing | **P10 housekeeping** — clean log signal during AI-heavy sprint |

---

## 4 · What "legitimate" means here

Three tiers evaluated; one chosen.

| Tier | Bar | Examples | Verdict |
|---|---|---|---|
| A | Credible hobby project | lox, MiniJava | **Floor** — already 60% there in LOC. Too low to be a north star. |
| **B (upper end)** | Sustained niche — LSP, spec, package manager, semantic value-add, ≥3 production users, 1.5k–3k stars | Gleam, Crystal, Nim, ReScript, PureScript | **Target.** Library-author niches punch above star-count for staying power (Rust's `serde` analog). |
| C | Mainstream — corporate backing, conferences | TypeScript, Kotlin, Swift | **Rejected.** No indie-built transpiled language has reached Tier C. |

**v1.0 splits into two milestones under AI-augmented build cadence.** The empirical research (`07-ai-augmented-dev-research.md`) shows technical completeness compresses dramatically; adoption / production-trust / community do not. So:

- **v1.0 technical-complete (5–7 months, median 5–6):** feature-complete per spec; spec frozen; LSP feature-complete; package manager working; all backends polished; verification triad in place; idiomatic output (source maps, `.d.ts`, docstrings, package metadata) included. *No production-user requirement.* This is what an AI-augmented build can plausibly hit.
- **v1.0 legitimacy (12–24 months from kickoff):** adds ≥3 production library users, ≥1,500 stars, ≥50 active community members, ≥1 conference/meetup talk, ≥1 case-study artifact. Calendar-bound — AI doesn't compress relationships.

**Calendar milestones, calibrated against AI-augmented evidence:**

| Horizon | Date | What ships |
|---|---|---|
| **5–7 months** | 2026-10 to 2026-12 | **v1.0 technical-complete.** Spec frozen at v0.9 → v1.0 after one adversarial review; LSP feature-complete; package manager works end-to-end; C backend output runtime-verified (not just syntax-checked); idiomatic output polish; first Aaron-authored library (CRC32 or similar) shipped to npm + PyPI + C header from one source. |
| **12 months** | 2027-05 | **First external library author trying real ports.** ≥250 stars. The 6→12-month gap is *adoption work* — content marketing, case-study cultivation. AI doesn't help. |
| **24 months** | 2028-05 | **v1.0 legitimacy hit.** ≥3 production library users, ≥1,500 stars, ≥50-active-member community. |
| **3 years** | 2029-05 | **v1.x maintained.** ≥3,000 stars, ≥10 production users, annual meet exists, ≥1 conference talk by someone other than Aaron. |
| **5 years** | 2031-05 | **Recognized small-niche language.** ≥5,000 stars; ≥1 Waterfall-originated library is the canonical implementation across multiple ecosystems. |

The 10-year horizon is dropped — at AI-augmented velocity, it's too far out to plan.

---

## 5 · The niche: library-author + Gleam-vibe combined

Five niches were evaluated; two combine into the chosen positioning.

| Candidate | Verdict |
|---|---|
| 1. Friendly Haxe (game scripting) | Saturated |
| 2. Polyglot teaching language | Reframed → content marketing |
| 3. JS-first with C escape hatch | Wrong fit (WASM dominates) |
| 4. Embedded scripting | Wrong fit (Lua entrenched) |
| **5. Polyglot library author + Gleam-vibe combined** | **Recommended** |

**The pitch**: *Waterfall is a small, typed language for shipping one algorithm to many runtimes — write it once in Waterfall, publish it to npm, PyPI, and as a C header.*

**Three sentences**: *Waterfall is a small, typed language for code that needs to ship across runtimes. Write your algorithm once in Waterfall — with sum types, `match`, friendly errors, and Gleam-grade language design — and publish the same source as an npm package, a PyPI wheel, and a vendorable C header. If you've ever maintained parallel JS, Python, and C ports of the same library, Waterfall is the tool that stops the drift.*

**Marketing posture**: general-purpose with personality. *Not* "the language for domain X." Teaching positioning is *content marketing*, not the headline.

**What this commits us to**:
- Every new language feature gets a "how this lowers to each backend" write-up. Roadmap work, not afterthought.
- The four-backend constraint (now three, post-legacy-drop) is the *point*, not a constraint. Backend divergence is the teaching material.
- Documentation stays first-class.
- Idiomatic output is non-negotiable. The `wfpm publish` artifact must look like an npm/PyPI/C package would if hand-written by someone who knows the ecosystem.

**Tier band**: 1,500–3,000 stars at v1.0 legitimacy (library-author niches punch above star-count); ≥3 production library users.

---

## 6 · The roadmap

Eight phases (P10 → P17, continuing existing numbering, with new P11.5 inserted), most expressed in **calendar weeks at AI-augmented pace**.

| Phase | Goal | Calendar weeks | Pivotal? | Audit codes |
|-------|------|---|---|---|
| **★ P10** | Foundation refactor — typed IR, verifier separation, `BUS-FACTOR.md`, `VERIFICATION-DISCIPLINE.md`, Gradle T2 sweep | 2–3 | ★ | D1, D2, D3, D6, T2 |
| P11 | Type inference + condition checking | 1 | | G4, G5 |
| **★ P11.5** | Modules + cross-module linking + visibility + C execution oracle (new — moved from P14) | 2–3 | ★ | C2, C6, D10 |
| **★ P12** | Sum types + `match` + `Result` + `@external` + `readonly` | 2–3 | ★ | U1, partial U2 |
| **★ P13** | Spec + LSP + package manager + friendly errors + cross-target stdlib | **7–13 (median ~10)** | ★ | legitimacy bar |
| **★ P14** | Generics + publish pipeline + **idiomatic-output polish** (source maps, `.d.ts`, docstrings, metadata) | **4–6 (median ~5)** | ★ | publishing |
| P15 | Records + methods + generic containers | 1–2 | | C3, README items |
| **★ P16** | v1.0 technical-complete — stabilize + release | 1–2 | ★ | U1/U2/U3 remainders |
| **★ P16.5** | v1.0 legitimacy — ≥3 production library users, ≥1,500 stars | **12–24 calendar months** | ★ | none (adoption phase) |
| ○ P17 | WASM target + ecosystem | post-v1 | | — |

**Total to v1.0 technical-complete**: 20–31 calendar weeks (median 22–25, ~5–6 months). **Total to v1.0 legitimacy**: ~18–32 months from kickoff.

### Why P10, P12, P13, P16 are pivotal — and P11.5 is now nearly pivotal

- **P10** unblocks everything. The spec is the single most load-bearing artifact in the roadmap. If P10's design has ambiguities the AI silently resolves, every downstream phase inherits the wrong foundation (R10).
- **P11.5** was promoted from P14 because library authors *publish packages that import each other*. Without modules at P11.5, the P13 stdlib is a namespace dump, `wfpm new hello` has nothing to generate, and library authors evaluating Waterfall hit "no multi-file demo" within 30 minutes of first contact.
- **P12** delivers the semantic value-add (`match`, `Result`, `@external`) and the *niche-fit case-study artifact* (an Aaron-authored CRC32 or similar, published across all three targets from one source).
- **P13** clears the 2026 legitimacy bar (LSP, spec, package manager, friendly errors). The longest phase at 7–13 weeks because LSP + friendly errors compress only 2–5×, not 10–20×.
- **P14** absorbs idiomatic-output polish — source maps, type stubs, package metadata — so v1.0 ships with what library authors actually expect from a 2026 transpiler.
- **P16** stabilizes for the v1.0 technical-complete release.

### Build cadence vs adoption cadence

- **Mechanical work** (parser, lexer, lowering, stdlib, monomorphization): AI compresses 10–30×. Klabnik's Rue (11–14 days for compiler core), Carlini's C compiler (2 weeks Linux-buildable) set the floor.
- **Tasted work** (LSP, friendly errors, language-design taste): 2–5×. This is why P13 is the longest phase.
- **Verification rigor** (property tests, differential oracles, adversarial review): does NOT compress. *Requires more care under AI augmentation, not less.*
- **Adoption / community trust**: does not compress at all. Bun's Rust port hit 99.8% test pass in 9 days; six months later, no one runs it in production.

So 20–31 weeks of build + 12–24 months of adoption is the honest range. **Plan against the median, not the optimistic build-floor.**

---

## 7 · The verification triad (the R8 mitigation, load-bearing)

The single decision that determines whether the AI-augmented compression is real or fake. **The dominant AI failure mode is "tests pass, code is wrong" via verifier overfitting** — same shape at every scale: Rue's codegen passed tests but produced segfaulting binaries; Carlini's C compiler "hard-codes values to satisfy the test suite"; Bun's Rust port passes 99.8% of tests with 13,000 unsafe blocks (~180× the density of a comparable hand-written project).

The mitigation is **not** a single tool or threshold — it's a tool-agnostic per-phase triad. All three must show "no regressions" before phase exit:

| Leg | What it is | Where it lives |
|---|---|---|
| **1. Property-based tests** | Kotest property arbiters at N=10000 per generator. ≥1 invariant property per new feature (e.g., "any well-typed program emits code that runs without type errors on every backend"; "exhaustive `match` covers all variants"). | `compiler/src/test/kotlin/.../property/` |
| **2. Differential oracle** | Per phase: a known-good reference the implementation is differentiated against. P10 oracle = current `*Data` AST emission (byte-equivalent goldens). P11–P15 oracle = Aaron-authored hand-checked examples. Backends oracle = gcc/Node/Python interpreters executing the *output*. Phase N+1 features use phase N behavior. | Per-phase test fixtures + `PHASE-N-design.md` |
| **3. AI-generated adversarial inputs** | Per phase: ≥20 "tricky inputs that should break the compiler" generated by a fresh Claude session given only the spec. Results logged. The triad fails if any compiler-broke case ships unfixed. | `compiler/src/test/resources/adversarial/phase-N/` |

**Why this beats a mutation-test slogan**: tool-agnostic (R9 mitigation — survives Claude/Codex quality changes); intrinsically multi-signal (much harder to satisfice than a single threshold); operationally enforceable (each leg has a clear pass/fail per phase).

**The Aaron-writes-the-tests discipline**: verifier-correctness tests are Aaron-authored, not AI-authored. Trip-wire: any verifier-correctness test file whose only author is AI = red flag.

Discipline documented at `notes/VERIFICATION-DISCIPLINE.md` (P10 deliverable).

---

## 8 · The headline feature — `readonly` (unified)

You asked for two forms:

- **Form A**: `readonly x = 4` at declaration time.
- **Form B**: `readonly x` as a standalone statement mid-function, freezing a previously-mutable binding.

The design unifies under a single keyword. **`readonly` is the only modifier**; the legacy keywords `const` and `imm` are removed with a small migration (3 `.wf` files plus tests + goldens). v1 ships a friendly parser-level "use readonly instead" error for `const`/`imm` (custom `LegacyModifierErrorListener`, ~50–80 lines of Kotlin, removed in v1.1).

### What Form B is

```
int x = 0
x = compute()
readonly x        // ← verifier-only; emits nothing
// x = 5          // ERROR: cannot write to readonly binding `x`
```

Form B is a **statement**, not an expression. It records a freeze on `x` in the current scope's local shadow of the symbol table. **It produces no runtime effect** — the JS/Python/C output at this line is empty.

Framing: **a one-line shorthand for Rust's shadowing-and-rebinding idiom, applied without a fresh binding**. Rust users write `let x = x;` for the same effect. Waterfall makes the idiom syntactic. The novelty is *syntactic*, not semantic — the team explicitly does not lean on it as the pitch.

### Semantic calls

1. **Scope**: freeze applies from the `readonly x` statement to the end of the lexical scope in which `x` was originally declared.
2. **Branch joins**: `readonly` after `if/else` iff **every non-terminating path** promoted it. Intersection rule. Terminating branches (`return`) are ignored.
3. **Loops**: freezes only for the rest of the current iteration. Loop-back intersection means top of next iteration is mutable. Promote *before* the loop for whole-loop freeze.
4. **Aliasing**: binding-only, not transitive. Freezing `a` does not freeze the record `a` points to.
5. **Subfield (`readonly x.field`)**: deferred to post-records (P15).
6. **Lambda captures**: deferred to P12 when multi-statement lambda bodies land.
7. **`commitReadonly` walk-depth**: writes only to the current scope's shadow, never to the parent (post round-4 fix). Each control-flow join is independent.

### Implementation cost

Trivial after P10. The symbol-table redesign (`SymbolInfo` with `isReadonly` field, `markReadonlyLocal` / `commitReadonly` API) is P10 work anyway. Form A + Form B together = ~1 week of focused work in P12.

### Marketing

**Implement first, decide later.** Per R5: don't market `readonly` as the headline until P12 implementation experience confirms it earns the weight. Use it for internal examples; if it feels good, promote it. If it feels awkward, downgrade to "one of several modifiers." The library-author pitch ("ship one algorithm to many runtimes") does not depend on `readonly` working great.

---

## 9 · The legitimacy-bar feature set

Required for Tier B in 2026. Each fully spec'd in `03-language-design.md`.

| Feature | Why | Priority | Sketch |
|---|---|---|---|
| **Records / structs** | Product types are table stakes | T1 | `type Point = { x: int, y: int }` |
| **Sum types / `union`** | ADTs are the 2026 baseline | T1 | `type Shape = Circle(r: dec) \| Square(side: dec)` |
| **Pattern matching** | Goes with sum types; exhaustiveness check | T1 | `match s { Circle(r) =>... }` |
| **Generics — monomorphized for C, erased for JS/Python** | Clean C output matters for library-author niche | T1 | `func map<T, U>(xs: T[], f: (T) ==> U) returns U[]` |
| **Modules + `import` + visibility** | Library authors publish packages that import each other (P11.5) | T1 | `import Math`; `pub`/`pkg`/`private` |
| **`Result<T, E>` + `?`** | Cross-target error story without exceptions | T1 | `func parse(s: string) returns Result<int, string>` |
| **Real `string` type** | `STRING_LITERAL → char` is actively buggy today | T1 | First-class, not `char[]` |
| **`@external`** | The library-author headline (P12, firm deliverable) | T1 | See §10 |
| **Package manager (`wfpm`)** | Required for legitimacy; registry-as-GitHub initially | T2 | `wfpm new`, `wfpm install`, `wfpm publish` |
| **LSP / formatter** | Required for legitimacy | T2 | hover, go-to-def, diagnostics |
| **Cross-target-coherent stdlib** | `Math` / `String` / `Array` / `IO` behave identically on JS/Python/C | T2 | depends on `@external` |
| **Idiomatic output polish** | npm source maps, Python `.pyi`, C metadata (P14, in v1.0 technical) | T2 | per-target ecosystem expectations |

---

## 10 · `@external` — the library-author headline (firm P12 deliverable)

The cross-target FFI story. Library authors *live* by this — the single biggest practical concern for "ship one algorithm to many runtimes."

### Syntax

```
@external(js, "Math", "sqrt")
@external(python, "math", "sqrt")
@external(c, "math.h", "sqrt")
func sqrt(x: dec) returns dec
```

### Decisions made (post round-4)

- **Reserved target keywords**: `js`, `python`, `c`, `wasm` (the last reserved-but-not-live). Unknown keywords are a verification error, not silently ignored.
- **Partial-target-support semantics**: Gleam-style. An `@external(js)` only function rejects at the type level when compiling to Python. The verifier becomes target-aware: `Verifier.verifyModule(module, scope, target: TargetKeyword? = null)`. `null` = verify against all targets (union check); a specific target = verify only that one.
- **Per-target lowering**: JS = ESM `import`; Python = `from X import Y`; C = `#include` + forward declarations; named `header="..."` for non-stdlib C.
- **Accepts**: primitives, primitive arrays, locally-defined records and sum types, stdlib types. **Does not accept**: lambdas (U2 unresolved), cross-package records (no v1 ABI-stability story).
- **Cross-module `@external` × visibility**: deferred to P11.5 design doc. Probable answer: externals follow the same `pub`/`pkg`/`private` rules as regular functions.

### Why Gleam-style over Haxe's `#if`

Function-level granularity composes with the type system. Statement-level `#if` fragments code and makes type-checking conditional. Gleam's pattern is what works at scale.

---

## 11 · JSON-first error format (Q10)

Errors are emitted as **JSONL on stderr by default**. Plain-text humans get `--errors human` (or auto-detected for TTY) piping the JSON through a built-in formatter (`HumanRenderer.kt`).

### Schema (versioned, v1)

```json
{
  "schemaVersion": 1,
  "severity": "ERROR",
  "code": "WF1042",
  "message": "Cannot assign to readonly binding 'x'.",
  "primaryLocation": { "file": "src/lib.wf", "line": 12, "column": 4, "endLine": 12, "endColumn": 9 },
  "relatedInfo": [{ "file": "src/lib.wf", "line": 8, "column": 4, "message": "readonly applied here" }],
  "suggestedFix": "Use a fresh binding or remove the `readonly x` statement above.",
  "tags": []
}
```

- **Required**: `schemaVersion`, `severity`, `code`, `message`, `primaryLocation` (with `file/line/column` required).
- **Optional**: `relatedInfo`, `suggestedFix`, `tags`.
- **Code allocation**: `WF1xxx` = P10-era; `WF2xxx` = P12-era; 1:1 with each `VerifyError` variant; stable across versions.
- **Schema file**: `notes/error-schema-v1.json`, shipped at `compiler/src/main/resources/error-schema-v1.json`.

### Why JSON-first

LSP consumes the JSON directly — no parsing of human text. This is the load-bearing reason. AI tooling (Claude Code, Codex) also benefits — structured errors compose with agent workflows. Humans pay the cost of a converter, but the converter is built-in and TTY-default.

---

## 12 · Idiomatic output polish (in v1.0 technical, P14)

Library-author niche credibility is decided on day 1 by what the published artifact looks like. Shipping v1.0 to npm/PyPI in 2026 without source maps and type declarations is the 2018-transpiler equivalent.

P14 deliverables:

- **JS**: source maps (`.js.map`), ESM with named exports, `package.json` with declared `exports`, README in the package, semver-tagged releases.
- **Python**: `.pyi` type stubs, `__doc__` docstrings preserved from Waterfall source, `pyproject.toml` with PEP 621 metadata, type-hint emission per PEP 484.
- **C**: header guards, version pin, README in the release tarball, `pkg-config` `.pc` file, vendor-friendly layout.

Cost: +2–3 weeks of P14 work (total 4–6 weeks median 5). Total v1.0 technical horizon lands at 5–7 months, upper end of the original window.

---

## 13 · Top risks (R1 → R10)

Ten risks, two introduced by AI augmentation.

| # | Risk | Severity | Mitigation summary |
|---|---|---|---|
| **R1** | Burnout mid-build (weeks 4–16) | HIGH | Phase-exit checklist as discipline anchor; `BUS-FACTOR.md` in P10; trip-wires on skipped triad legs + 2-week build gaps |
| R2 | Multi-target divergence becomes debt | MEDIUM | `@external` model; reject mixed-support at type level; quarterly C-backend TODO audit |
| R3 | Library-author niche misfit | MEDIUM | Aaron-authored case study by end of P12; trip-wire = no inbound interest in 90 days |
| R4 | Feature creep / Tier C drift | MEDIUM | Phase plan discipline; trip-wire = phase >1.5× budget |
| R5 | "Novel readonly" doesn't pan out | MEDIUM | Don't market until P12 confirms; trip-wire = examples don't read well to fresh eyes |
| R6 | WASM eats the library-author niche | HIGH | Anchor in idiomatic output + readability + Gleam-grade design; quarterly WASM ecosystem monitoring; trip-wire = credible single-source-to-idiomatic-npm WASM tool ships |
| R7 | Long-tail post-build burnout (P16.5) | MEDIUM | Realistic 6-month checkpoints; keep coding work as engagement vehicle alongside outreach |
| **R8** | Verification overfitting (dominant AI failure) | **HIGH** | The triad. Trip-wire = any triad leg skipped in any P10–P15 phase |
| R9 | AI tooling regression | MEDIUM | Tool-agnostic specs; trip-wire = AI-tooling abandonment doubles phase budget |
| **R10** | Verifier-design quality in week 1 | **HIGH** | Adversarial pre-review of P10 spec by skeptic agent before any implementation. The 13 PITFALL callouts and 7 ESCALATE items in `notes/PHASE-10-design.md` are the on-ramp. |

R1, R6, R8, R10 are the four that materially threaten the plan. The rest are recoverable.

---

## 14 · What Waterfall is NOT

- Not a JVM-compatible language (despite the compiler running on the JVM).
- Not aiming for vendor adoption — no time pitching Microsoft, Google, Apple, JetBrains.
- Not a research vehicle for novel type theory — Hindley-Milner-lite is the ceiling through P15.
- Not a "fast" language — no benchmarks vs Zig or Rust.
- Not a systems / OS language — no kernel writing, no C-replacement pitch.
- Not a concurrency-focused language — no goroutines, async/await, STM, or actors in v1.
- Not a domain-specific language — no "for games" or "for data science" framing.

---

## 15 · The first three commits (pre-P10 cleanup)

Before P10 proper begins, three small landing PRs. ~1 evening total.

1. **Drop the legacy backend** (Q5). Delete `LegacyTextBackend.kt`, drop `compiler/src/test/resources/golden/legacy/`, update `Backends.kt` registration, change default `--target` to `js`, strip references from README + examples + CLI help.
2. **`BUS-FACTOR.md` at repo root** (R1 cheapest mitigation). 1–2 pages on how to cut a release, how the verifier/IR pipeline is laid out, which decisions are reversible vs load-bearing.
3. **Verifier enforces `const`/`imm` today** (audit's no-op verifier). `VariableAssignmentData.verify` and `IncrementStatementData.verify` are no-ops today; even `const x = 4; x = 5` slips past Waterfall and is caught only by the target. Fix in a small targeted PR before P10 begins; sets up the symbol-table redesign with a real test bed and 5 negative tests that keep paying dividends.

After those three, P10 proper begins — start with the spec-first plan-mode loop against `notes/PHASE-10-design.md`. See the execution playbook (`00-EXECUTION-PLAYBOOK.md`) for the operational rhythm.

---

## 16 · Appendices

In `notes/team-output/`:

- **A — Codebase audit** (`01-codebase-audit.md`): capability matrix; architecture overview; component review; readonly extension points; architectural debt D1–D10.
- **B — PL landscape** (`02-pl-landscape.md`): 16 transpiled-language case studies; flow-sensitive immutability prior art; positioning recommendations.
- **C — Language design** (`03-language-design.md`, ~2,800 lines post-edits): full feature catalog; full `readonly` spec; cross-target divergence model with `@external`.
- **D — Strategy** (`04-strategy.md` post round-3): tier definitions; 5-niche evaluation; phase budgets in calendar weeks; risk register R1–R10 with trip-wires; non-goals.
- **E — Adversarial review round 1** (`05-adversarial-review.md`).
- **F — Quality review** (`06-quality-review.md`).
- **G — AI-augmented dev research** (`07-ai-augmented-dev-research.md`): empirical case studies, failure modes, spec-first loop.
- **H — Adversarial review round 2** (`08-adversarial-review-2.md`): findings on the compressed plan, new Mike-Test.
- **I — P10 design** (`notes/PHASE-10-design.md`, ~1920 lines): the load-bearing implementation spec. Read before the first P10 plan-mode session.

Also in this directory:

- **`00-EXECUTION-PLAYBOOK.md`** — operational runbook (the "how").
- **`00-KICKOFF-PROMPT.md`** — self-contained prompt to spin up a fresh `/teamwork` implementation team.

---

*Total source material: ~600 KB across nine docs. This synthesis is the strategic plan; the playbook is the operational companion; the kickoff prompt is what you paste into a fresh session tomorrow morning.*
