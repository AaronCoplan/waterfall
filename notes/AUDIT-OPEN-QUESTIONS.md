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

### ~~G2. No array type in the grammar~~ (closed in phase 8f)
~~**Today:** `arr[i]` is a valid expression (Phase 6f), but the user can't
declare `int[] arr = ...`.~~
**Fix landed:** Extended `type` grammar rule to `QUESTION_MARK? ID (L_BRACKET R_BRACKET)?`.
`PrimitiveTypes` gained `isArray(String)`, `isPrimitiveOrArray(String)`, and
`elementType(String)` helpers. The C backend's `cType` now maps `T[]` → `T*`.
JS and Python drop the type entirely as usual. ArrayIndexModule's `arr` is
now an explicit `int[]` parameter; CRuntimeCheckTest's skip is dropped.
New `ArrayParamsModule.wf` exercises array-typed parameters and a return
type, plus a `castas dec[]` cast.

### ~~G3. Function-body symbol-table declarations~~ (closed in phase 8e)
~~**Today:** Function-body `int x = 1` decls don't `declare()` into the
function-local symbol table; we kept the original behavior.~~
**Fix landed:** `TypedVar`, `UntypedVar`, and `FunctionImpl` verifiers now
call `scope.declare(...)`. `IfBlockData` / `ForBlockData` / `WhileBlockData`
already wrapped child scopes — that hierarchy now actually carries
inner-declared names. `TopLevelSymbolTableGenerator` was deleted; `Main`
builds a fresh empty scope per module and the top-level decls' verify
both declare and check. New `DuplicateInnerDeclarationTest` covers
inner-duplicate, shadow-arg, and distinct-branch cases.

Also closed a latent grammar bug: the `ID` lexer rule's middle group
used `+` instead of `*`, silently rejecting 2-character identifiers like
`go`. Fixed in the same phase.

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

### ~~G6. `castas` only accepts a single-token primitive type~~ (closed in phase 8f)
~~**Today:** Grammar's `type: QUESTION_MARK? ID;`. `int castas dec` works,
but `castas dec[]` doesn't.~~
**Fix landed:** Free with `G2` — the `type` rule is shared. Backends:
  - C: emits `((T *)(operand))` for array casts.
  - JS / Python: array-typed casts are no-ops (types are dropped); the
    operand passes through unchanged.

---

## Codegen — universal

### U1. Bundle literals `|a, b|`
**Today:** Best-guess as arrays/lists across every target (JS arrays,
Python lists, C `/* TODO bundle */ {0}` placeholder).
The source language hasn't defined what a bundle is.
**Fix:** Decide bundle semantics first (tuple vs. struct vs. tagged record).
**Where flagged:** Every backend's `emitBundleLiteral`.

### U2. Lambdas
**Today:** Map to arrow functions (JS) / `lambda` (Python) / emit a `NULL`
placeholder in C (no anonymous functions).
**Fix:** For C, lift the lambda body to a static function in the same
compilation unit and reference it by name. Requires a transform pass.
**Where flagged:** `CBackend.emitLambda`.

### U3. Named arguments
**Today:**
- JS: `fn({a: 1, b: 2})` — callee must destructure.
- Python: native `fn(a=1, b=2)` (clean).
- C: names dropped, positional fallback.
**Fix:** Standardize one ABI per target. The JS object-arg approach forces
callee changes; alternatives include preserving call-site order via a
parameter-name lookup against the function definition (requires symbol-table
work).
**Where flagged:** Every backend's `emitFunctionCall` (named-args branch).

### ~~U4. String literals~~ (closed in phase 8h)
~~**Today:** Backtick-delimited in source. JS keeps backticks (template
literals); Python and C strip them and emit double-quoted; no escape
resolution.~~
**Fix landed:** New `StringLiteralText` helper with `unescape(backtickedSource)`
and `escapeFor(raw, quoteChar)`. Source escapes recognized: `\``, `\\`,
`\n`, `\r`, `\t`. All three real-target backends now go through
`unescape -> escapeFor('"')`, switching JS off template literals for
simplicity. New
`examples/StringLiteralsModule.wf` covers plain strings, embedded
quotes, newline/tab escapes, and an escaped backtick. All goldens
verified by their language's runtime check.

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

### ~~C4. Array literals in C~~ (closed in phase 8g)
~~**Today:** `(int[]){...}` — element type hardcoded to int.~~
**Fix landed:** `CBackend.inferArrayElementType` looks at the first element's
`ExpressionData.kind` and picks the corresponding C type: INT_LITERAL →
`int`, DEC_LITERAL → `double`, BOOL_LITERAL → `bool` (and requests
`<stdbool.h>`), STRING_LITERAL → `const char *`. Identifiers / calls /
arithmetic still fall through to `int` (proper inference is `G4`'s job).
Empty arrays still default to `int` (flagged TODO). New
`examples/ArrayLiteralsModule.wf` exercises int / dec / bool element types
and is verified by `gcc -fsyntax-only`.

### ~~C5. Standard `#include`s~~ (closed in phase 8c)
~~**Today:** Always emit `<stdio.h>`, `<stdbool.h>`, `<string.h>` regardless
of usage.~~
**Fix landed:** `CBackend` now keeps a `TreeSet<String> requiredHeaders` that
emit sites populate during translation. `emitProgram` renders the body first,
then emits only the headers the body actually requested. `<stdbool.h>` is
requested by `cType("bool")` and BOOL_LITERAL; `<math.h>` by the `^` operator.
EmptyModule and most non-bool examples no longer carry unused includes.

### C6. JS module wrapping
**Today:** No wrapping — declarations at file scope plus a `// module X`
comment.
**Fix:** Decide on an emission target: an ESM module (`export const`) vs.
CommonJS vs. an IIFE.
**Where flagged:** `JavaScriptBackend.emitProgram`.

### ~~C7. Python `const`/`imm` not enforced~~ (closed in phase 8d)
~~**Today:** Emitted as plain assignment. Python has no compile-time const.~~
**Fix landed:** `const`/`imm` decls emit `name: Final = value`. The
`PythonBackend` tracks whether any `Final` annotation was emitted and
prepends `from typing import Final` to the module only when needed. Type
checkers (mypy / pyright) honor the annotation; CPython itself still
doesn't enforce at runtime, but `typing.Final` is the canonical
intent-signal.

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
