# Phase 1 — Best-Guess Decisions

Phase 1 closed the AST → codegen gap and brought the compiler module back in sync with
the grammar (the master branch's compiler hadn't been updated after the parser switched
to `expression` RHS and `statementBlock` nesting). Phase 2 introduces a backend
abstraction; per-target backends in phases 3–5 will revisit several of the choices below.

| # | Decision | Best-guess taken | Alternatives | Where flagged |
|---|---|---|---|---|
| 1 | Type of an untyped `x := <expr>` when the RHS isn't an int literal | Falls back to `int`. | Implement full type inference; emit `auto` (C++); use a tagged union. | `UntypedVariableDeclarationAndAssignmentData.inferType` |
| 2 | `null` literal | Legacy emitter writes `NULL`. | `null`, `nullptr`, `None`, target-specific. | `ExpressionData.translate` |
| 3 | String literals | Pass through backtick form as-is. | Translate to double-quoted; resolve escapes. | `ExpressionData.translate` |
| 4 | `Module::fn(args)` calls | Mangle to `Module_fn(args)` for the legacy emitter. | C-style `Module.fn`; namespace lookup. | `FunctionCallData.translate` |
| 5 | `obj.field.fn()` chains | Emit verbatim as `obj.field.fn()`. | Resolve receiver type; insert `&obj` first arg for C. | `FunctionCallData.translate` |
| 6 | Lambda emission | Keep source syntax `(args) ==> body`. | C++ `[](args){...}`; JS arrow; Python `lambda`. | `LambdaFunctionData.translate` |
| 7 | Bundle literals `\|a, b\|` | Pass through verbatim. | List, tuple, struct. | `BundleLiteralData.translate` |
| 8 | `for (n in coll)` emission | `for (auto n : coll) { ... }`. | Indexed loop with size; iterator protocol. | `ForBlockData.translate` |
| 9 | If-condition type-check | Skipped (parser already required some expression). | Require a bool — needs a bool token first. | `IfBlockData.verify` |
| 10 | Top-level Gradle wrapper | Bumped 5.2 → 8.10.2 (5.2 was incompatible with JDK 21). | Pin to JDK 8; Gradle 7.x. | `gradle/wrapper/gradle-wrapper.properties` |
| 11 | log4j routing | Switched from STDOUT to STDERR so STDOUT is pure translation output. | Add a CLI flag; route to file. | `*/src/main/resources/log4j2.properties` |
| 12 | `./waterfall` script | Removed `gdate` line (macOS-only; emits errors on Linux). Quoted `$@`. | Cross-platform timestamp shim. | `waterfall` |
| 13 | "Only int" verifier rule | Kept (matches original). Phase 5 relaxes for `dec`/`bool`/`char`. | Relax now. | `TypedVariableDeclarationAndAssignmentData.verify`, `FunctionImplementationData.verify` |
