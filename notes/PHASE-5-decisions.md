# Phase 5 — Best-Guess Decisions (C backend + type-system relaxation)

`./waterfall --target c examples/<X>.wf` now emits C99. Every output passes
`gcc -fsyntax-only` (with `-Wno-implicit-function-declaration`,
`-Wno-int-conversion`, `-Wno-return-type` to suppress warnings that aren't real
syntax errors — see `CRuntimeCheckTest`). The verifier's "only int" rule was
relaxed to accept any Waterfall primitive (`int`, `dec`, `bool`, `char`).

| # | Decision | Best-guess | Alternatives | Where flagged |
|---|---|---|---|---|
| 1 | Type system module | New `compiler/.../typesystem/PrimitiveTypes.java` with the four README primitives and `isPrimitive(name)`. | Inline string compare; richer type lattice with sub-typing. | `PrimitiveTypes` |
| 2 | Verifier relaxation | `TypedVariableDeclarationAndAssignmentData.verify` and `FunctionImplementationData.verify` now accept any name in `PrimitiveTypes.ALL`. | Per-target type whitelists. | both `verify()` methods |
| 3 | Type mapping | `int`→`int`, `dec`→`double`, `bool`→`bool` (with `#include <stdbool.h>`), `char`→`char`. | `dec` → `float`; `bool` → `int`. | `CBackend.cType` |
| 4 | Untyped `:=` inference | Stricter than before: `INT_LITERAL`→`int`, `DEC_LITERAL`→`dec`, `STRING_LITERAL`→`char` (so the C backend can do char-pointer-ish things). Everything else still defaults to `int`. | Full constraint-based inference. | `UntypedVariableDeclarationAndAssignmentData.inferType` |
| 5 | Standard includes | Always emit `<stdio.h>`, `<stdbool.h>`, `<string.h>` regardless of whether the program uses them. | Demand-driven include emission. | `CBackend.emitProgram` |
| 6 | Empty-arg functions | Emit `void` in the parameter list (C `f(void)` is the unambiguous form). | C `f()` which historically means "unspecified args". | `CBackend.emitFunctionImpl` |
| 7 | `for (n in coll)` | Emit `for (int n = 0; n < 0; n++) /* TODO(audit) */ {...}`. Zero-iteration loop that still declares the iterator so the body's references type-check. | Real lowering with collection metadata; do nothing and let it fail. | `CBackend.emitForBlock` (TODO) |
| 8 | `const`/`imm` | Emit C `const` prefix. | Macro-based const; ignore. | `CBackend.emitTypedVarDecl` |
| 9 | `Module::fn(x)` | Mangle to `Module_fn(x)`. | Header includes; namespace-by-prefix. | `CBackend.emitFunctionCall` (TODO) |
| 10 | `obj.path.fn(x)` | Mangle to `obj_path_fn(&obj, ...)` — receiver-as-first-arg, dotted path joined as the receiver name. Almost certainly won't compile in practice, but flagged. | First-class method dispatch via function pointers in structs. | `CBackend.emitFunctionCall` (OBJECT branch, TODO) |
| 11 | Named arguments | Names dropped, only values emitted (positional fallback). | None — C just doesn't have named args. | `CBackend.emitFunctionCall` (named-args branch, TODO) |
| 12 | Lambdas | Emit `/* TODO(audit): lambda */ NULL` placeholder. C lacks anonymous functions. | GCC nested-function extension; emit a static function and reference it. | `CBackend.emitLambda` (TODO) |
| 13 | Array literals | Emit C99 compound literal `(int[]){...}` (hardcoded element type = int). | Element-type inference; emit a typedef. | `CBackend.emitArrayLiteral` (TODO) |
| 14 | Bundle literals | Emit `/* TODO(audit): bundle */ {0}` — a zero-init placeholder. | A struct typedef; a tagged union. | `CBackend.emitBundleLiteral` (TODO) |
| 15 | String literals | Strip backticks, wrap in double quotes. No escape resolution. | `repr`-style escape pass. | `CBackend.emitExpression` (STRING_LITERAL branch, TODO) |
| 16 | gcc syntax-check flags | `-Wno-implicit-function-declaration`, `-Wno-int-conversion`, `-Wno-return-type` so the test only fails on real syntax errors, not on undefined functions or missing returns (those are warnings under modern gcc). | Make the test stricter and force return-type fixes; expect-fail per file. | `CRuntimeCheckTest.gccAcceptsGolden` |
