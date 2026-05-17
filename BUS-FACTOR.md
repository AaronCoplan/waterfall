# BUS-FACTOR.md

Operational guide for anyone picking this up cold. Written at Phase 0 (pre-P10).
Covers: cutting a release, the compiler pipeline, which decisions are locked vs.
reversible, how to run the test suite.

---

## (a) How to cut a release

**Prerequisites**: JDK 17 or 21, `gcc`, `node`, and `python3` on PATH.

```bash
# 1. Build the compiler jar
./gradlew build
# Produces: compiler/build/libs/compiler-0.0.1.jar

# 2. Smoke-test the CLI (the ./waterfall script wraps the jar)
./waterfall --target js examples/FibonacciModule.wf

# 3. Run the full test suite — must be green before tagging
./gradlew test

# 4. Tag and push
git tag vX.Y.Z
git push origin vX.Y.Z
```

A release is the tagged commit plus the jar. There is no automated publish
pipeline yet — `wfpm` (the package manager) is a P13 deliverable.

---

## (b) Compiler pipeline at a glance

The pipeline today (pre-P10). P10 will introduce a typed IR and a central
`Verifier` class; this documents what exists now.

```
parser/src/main/antlr/
  WaterfallLexer.g4 + WaterfallParser.g4      ANTLR 4 grammar
          │
          │  ANTLR generates Java lexer/parser at build time
          ▼
com.aaroncoplan.waterfall.parser.FileParser    wraps ANTLR; returns ParseResult
          │                                   (parse tree + syntax error list)
          ▼
compiler/.../statements/ModuleAst             assembles top-level vars + functions
          │                                   from the parse tree into typed lists
          │
          ├─ *Data.verify(SymbolTable)         distributed verification — no central
          │    → VerificationResult            Verifier class today; each *Data node
          │                                   calls verify() on itself
          │
          └─ *Data.translate(CodeGenerator)   per-node translation dispatch; each
               → String fragment              *Data calls the matching emit* method
                                              on the backend

compiler/.../target/CodeGenerator             interface; one emit* method per node kind
  JavaScriptBackend  PythonBackend  CBackend  three implementations (js is default)
```

**Key classes**:

| Class / file | Role |
|---|---|
| `parser/.../FileParser.kt` | Wraps ANTLR; entry point for parsing a `.wf` file |
| `compiler/.../statements/ModuleAst.kt` | Assembles parsed module into two lists: `topLevelVariables`, `functions` |
| `compiler/.../statements/helpers/Translatable.kt` | Interface: `verify(SymbolTable)` + `translate(CodeGenerator)` |
| `compiler/.../statements/*Data.kt` | One class per AST node kind; each implements `Translatable` |
| `compiler/.../symboltables/SymbolTable.kt` | Scoped name→info map; parent-linked for nested scopes; `declare` (public); `lookup` (made `internal` in P0-PR3 so the verifier can check immutability; P10 will further refine the SymbolInfo API) |
| `compiler/.../statements/helpers/VerificationResult.kt` | Simple success/error wrapper returned by `verify()` |
| `compiler/.../target/CodeGenerator.kt` | Backend interface; `emitProgram(ModuleAst)` is the top-level entry point |
| `compiler/.../target/Backends.kt` | Registry + `DEFAULT_TARGET`; add one line to register a new backend |
| `compiler/.../Main.kt` | Driver: parse → syntax check → verify → translate → print |

**What does not exist yet** (P10 deliverables):
- A typed IR between parse and codegen
- A central `Verifier` class
- A typed `SymbolInfo` (today `SymbolTable` stores `Any?`)

---

## (c) Reversible vs. load-bearing decisions

| Decision | Status | Where locked |
|---|---|---|
| **Q1** Tier B upper-end + library-author/Gleam-vibe niche | **Load-bearing** — changing the niche reshapes the entire roadmap | `notes/team-output/00-FINAL-PLAN.md` §3 |
| **Q5** Legacy backend dropped; `js` is the default target | **Load-bearing** — done; do not re-add | P0-PR1; `Backends.kt` |
| **Q10** JSON-first error format (JSONL on stderr; `--errors human` for colorized output) | **Load-bearing** — LSP and tooling depend on the schema; changing it post-P10 is a breaking change | `notes/PHASE-10-design.md`; not yet implemented |
| **Q11** Kotest as the property-test framework | **Load-bearing** — test infrastructure decision; changing mid-build invalidates existing property suites | `notes/PHASE-10-design.md` §4.9; not yet implemented |
| `Translatable.verify()` / `translate()` split | **Load-bearing until P10** — P10 replaces this with a typed IR + central Verifier; do not add new verify/translate logic outside the P10 plan | `compiler/.../statements/helpers/Translatable.kt` |
| Backend `emit*` method names | **Reversible** — rename freely; one-file change per backend | `compiler/.../target/CodeGenerator.kt` |
| Golden test fixtures | **Reversible** — regenerate with `UPDATE_GOLDEN=1 ./gradlew test --tests GoldenTests` | `compiler/src/test/resources/golden/` |
| Gradle wrapper version | **Reversible** — bump in `gradle/wrapper/gradle-wrapper.properties` | — |
| Log4j calls in `Main.kt` | **Reversible** — cosmetic | `compiler/.../Main.kt` |

---

## (d) Test suite

All tests run via `./gradlew test`. Requires `gcc`, `node`, and `python3` on PATH
(the runtime checks invoke them directly).

| Test class | What it covers |
|---|---|
| `GoldenTests` | Parameterized snapshot tests. Auto-enumerates `compiler/src/test/resources/golden/<target>/`; compares compiler stdout against the `.expected` file. Regenerate with `UPDATE_GOLDEN=1 ./gradlew test --tests GoldenTests`. |
| `CRuntimeCheckTest` | Runs `gcc -fsyntax-only` on each C golden. |
| `JsRuntimeCheckTest` | Runs `node --check` on each JS golden. |
| `PythonRuntimeCheckTest` | Runs `python3 -c "import ast; ast.parse(...)"` on each Python golden. |

All three runtime check classes and their shared `RuntimeCheckBase` superclass live in `compiler/src/test/kotlin/.../tests/RuntimeChecks.kt`.

| `DuplicateInnerDeclarationTest` | Unit tests for duplicate-declaration rejection in nested scopes. |
| `EndToEndSmokeTest` | End-to-end smoke tests on the example files using the JS backend. |

**Coming in P10 and P11.5** (not yet wired):

| Leg | Location (once landed) |
|---|---|
| Property-based tests (Kotest, N=10 000) | `compiler/src/test/kotlin/.../property/` |
| Adversarial-input runs (≥20 per phase) | `compiler/src/test/resources/adversarial/phase-N/` |

The verification triad design is documented in
`notes/team-output/00-EXECUTION-PLAYBOOK.md` §3.

---

## (e) Key pointers

| Resource | What it contains |
|---|---|
| `notes/team-output/00-FINAL-PLAN.md` | Strategic plan: niche, roadmap, risk register, all answered Q decisions |
| `notes/PHASE-10-design.md` | Load-bearing spec for P10 (typed IR, SymbolInfo, central Verifier, JSON errors) |
| `notes/team-output/00-EXECUTION-PLAYBOOK.md` | Operational rhythm: spec-first loop, phase rituals, verification triad, PR template |
| `notes/AUDIT-OPEN-QUESTIONS.md` | Open questions from the codebase audit; tracks what is not yet resolved |
| `examples/` | Working `.wf` programs, one per feature area; the golden suite runs against these |
