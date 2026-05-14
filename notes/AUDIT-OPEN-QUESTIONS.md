# Audit — Open Questions

Aggregated from `// TODO(audit):` comments in the source plus the
`notes/PHASE-N-decisions.md` per-phase logs. Each entry below names a
best-guess we made overnight, why we made it, and the cleanest follow-up
path to fix it.

For per-phase context, see `notes/PHASE-{1..5}-decisions.md`.

---

## Grammar / front-end

### ~~G1. No first-class `true` / `false` literals~~ (closed in phase 8a)
~~**Today:** Source identifiers `true` and `false` are parsed as `ID`s. JS and
C accept them because their lowercase booleans are first-class; Python's
`PythonBackend.emitExpression` case-translates the identifier text to
`True` / `False`.~~
**Fix landed:** Added `BOOL_LITERAL: 'true' | 'false';` to the lexer (before
`ID`), a `BOOL_LITERAL` alternative to `expression`, and `Kind.BOOL_LITERAL`
to `ExpressionData`. Python's case-translation hack removed. See
`examples/BoolLiteralsModule.wf`.

### G2. No array type in the grammar
**Today:** `arr[i]` is a valid expression (Phase 6f), but the user can't
declare `int[] arr = ...`. Compounds like `int[].create(26)` in the README
example don't parse. The `ArrayIndexModule` C golden is skipped by
`CRuntimeCheckTest` because `arr` can't be declared.
**Fix:** Extend `type: QUESTION_MARK? ID (L_BRACKET R_BRACKET)?` and teach
`TypedVar` + `FunctionImpl` verifiers to accept the suffix. Per-target type
mapping: JS/Python → ignored; C → `<elt> arr[]` or `<elt>* arr`.
**Where flagged:** `CRuntimeCheckTest.gccAcceptsGolden`, `notes/PHASE-6f`.

### G3. Function-body symbol-table declarations
**Today:** Function-body `int x = 1` decls don't `declare()` into the
function-local symbol table; we kept the original behavior. Phase 1's
verifier intentionally left this alone.
**Fix:** Have `TypedVariableDeclarationAndAssignmentData.verify` declare
into the scope it's given. Requires plumbing a "scope" through the verify
chain in `IfBlockData` / `ForBlockData` / `WhileBlockData` / `FunctionImplData`.
**Where flagged:** `TypedVariableDeclarationAndAssignmentData.verify`,
`UntypedVariableDeclarationAndAssignmentData.verify`.

### G4. Expression type inference is shallow
**Today:** `:=` infers `int` for INT_LITERAL, `dec` for DEC_LITERAL, `char`
for STRING_LITERAL — everything else defaults to `int`. Calls, identifiers,
and arithmetic don't get inferred types.
**Fix:** Add a real type inference pass that propagates types through the
symbol table.
**Where flagged:** `UntypedVariableDeclarationAndAssignmentData.inferType`.

### G5. If/while/for conditions aren't type-checked
**Today:** Any expression is accepted as a condition.
**Fix:** Require the inferred type to be `bool` (or implicitly convertible)
once G1 and G4 land.
**Where flagged:** `IfBlockData.verify`, `ForBlockData.verify`.

### G6. `castas` only accepts a single-token primitive type
**Today:** Grammar's `type: QUESTION_MARK? ID;`. `int castas dec` works,
but `castas dec[]` doesn't (no array types per G2).
**Fix:** Comes for free once G2 lands.

---

## Codegen — universal

### U1. Bundle literals `|a, b|`
**Today:** Best-guess as arrays/lists across every target (JS arrays,
Python lists, C `/* TODO bundle */ {0}` placeholder, legacy pass-through).
The source language hasn't defined what a bundle is.
**Fix:** Decide bundle semantics first (tuple vs. struct vs. tagged record).
**Where flagged:** Every backend's `emitBundleLiteral`.

### U2. Lambdas
**Today:** Map to arrow functions (JS) / `lambda` (Python) / `(x castas T)`
shape (legacy) but emit a `NULL` placeholder in C (no anonymous functions).
**Fix:** For C, lift the lambda body to a static function in the same
compilation unit and reference it by name. Requires a transform pass.
**Where flagged:** `CBackend.emitLambda`.

### U3. Named arguments
**Today:**
- JS: `fn({a: 1, b: 2})` — callee must destructure.
- Python: native `fn(a=1, b=2)` (clean).
- C: names dropped, positional fallback.
- Legacy: `fn(a=1, b=2)` source form.
**Fix:** Standardize one ABI per target. The JS object-arg approach forces
callee changes; alternatives include preserving call-site order via a
parameter-name lookup against the function definition (requires symbol-table
work).
**Where flagged:** Every backend's `emitFunctionCall` (named-args branch).

### U4. String literals
**Today:** Backtick-delimited in source. JS keeps backticks (template
literals); Python and C strip them and emit double-quoted; no escape
resolution. No example currently exercises strings.
**Fix:** Add an escape-handling helper and a `STRING_LITERAL.text()` accessor
that returns the unwrapped+unescaped string. Each backend re-escapes
according to its rules.
**Where flagged:** `emitExpression` (STRING_LITERAL branch) in every
backend.

---

## Codegen — per-target

### C1. C `for (n in coll)` lowering is a stub
**Today:** Emits `for (int n = 0; n < 0; n++)` — declares the iterator so
the body type-checks, but never iterates.
**Fix:** Requires array types (G2) and a known representation (pointer +
length pair, iterator struct, etc.).
**Where flagged:** `CBackend.emitForBlock`.

### C2. `Module::fn(x)` mangling in C
**Today:** `Module_fn(x)`. Will only link if the callee was also emitted
with the same mangling.
**Fix:** Emit a header per module with the mangled prototypes; require the
compiler to consume all modules at once and emit a single TU.
**Where flagged:** `CBackend.emitFunctionCall` MODULE branch.

### C3. `obj.fn(x)` in C
**Today:** `obj_fn(&obj, x)` — assumes a free function exists with that
name and a receiver-as-first-arg ABI. Won't link in any real example.
**Fix:** Define method-dispatch semantics first (vtables? function
pointers in structs?). Requires class/struct support in the grammar.
**Where flagged:** `CBackend.emitFunctionCall` OBJECT branch.

### C4. Array literals in C
**Today:** `(int[]){...}` — element type hardcoded to int.
**Fix:** Element-type inference; or emit a typedef and a compound literal
of that type.
**Where flagged:** `CBackend.emitArrayLiteral`.

### C5. Standard `#include`s
**Today:** Always emit `<stdio.h>`, `<stdbool.h>`, `<string.h>` regardless
of usage.
**Fix:** Demand-driven — track which headers are needed during emit and
emit only those.
**Where flagged:** `CBackend.emitProgram`.

### C6. JS module wrapping
**Today:** No wrapping — declarations at file scope plus a `// module X`
comment.
**Fix:** Decide on an emission target: an ESM module (`export const`) vs.
CommonJS vs. an IIFE.
**Where flagged:** `JavaScriptBackend.emitProgram`.

### C7. Python `const`/`imm` not enforced
**Today:** Emitted as plain assignment. Python has no compile-time const.
**Fix:** Use `typing.Final` annotations or `types.MappingProxyType` for
collections.
**Where flagged:** `PythonBackend.emitTypedVarDecl`.

---

## Tooling

### ~~T1. Exit codes~~ (closed in phase 8b)
~~**Today:** `Main.main` returns normally on every error path so the unit
test (which calls `Main.main` in-process) can keep running. The `./waterfall`
script always exits 0 even on error.~~
**Fix landed:** Split the entry points. `Main.run(String[])` is the in-process
entry point and throws `CompilerError` on any failure; `Main.main(String[])`
is the JVM entry point and catches `CompilerError` then `System.exit(1)`s.
`EndToEndSmokeTest` + `GoldenTests` switched to `Main.run` and either let
the exception propagate (assertion-style) or swallow it where the error
output is the test target (Duplicate*Module goldens).

### T2. log4j Gradle 9 deprecations
**Today:** The build prints "Deprecated Gradle features were used in this
build, making it incompatible with Gradle 9.0." after every command.
**Fix:** Run with `--warning-mode all`, read the specific deprecations,
fix the build scripts. Not urgent — Gradle 8.10.2 is the current LTS-ish.
