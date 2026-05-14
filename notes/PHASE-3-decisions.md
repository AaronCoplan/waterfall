# Phase 3 — Best-Guess Decisions (JavaScript backend)

`./waterfall --target js examples/<X>.wf` now emits JavaScript. All 9 examples'
output passes `node --check` and is checked in under
`compiler/src/test/resources/golden/js/`.

| # | Decision | Best-guess | Alternatives | Where flagged |
|---|---|---|---|---|
| 1 | Module wrapping | Emit a single `// module <name>` line as the first line and put declarations at file scope. No IIFE, no ESM module, no namespace. | Wrap in `(function() { ... })()`; emit `export const <name> = { ... }`; emit nothing. | `JavaScriptBackend.emitProgram` |
| 2 | Types in `int x = 4` | Drop the type; emit `let x = 4;`. | Emit JSDoc `/** @type {number} */`; require TypeScript-style annotations. | `emitTypedVarDecl` |
| 3 | `const`/`imm` modifiers | Map both to JS `const`. Otherwise `let`. Modifiers are stored as `List<String>` and looked up via `isImmutable()`. | Treat `const` and `imm` differently (e.g. deep-freeze for `imm`). | `*VariableDeclarationAndAssignmentData.isImmutable` |
| 4 | `for (n in coll)` | Emit JS `for (const n of coll) { ... }`. (Note: NOT JS `for...in`, which iterates keys — Waterfall semantics is iteration over a collection.) | Indexed `for (let i = 0; i < coll.length; i++)`; `coll.forEach(...)`. | `emitForBlock` |
| 5 | Lambdas `(args) ==> body` | Emit arrow functions `(args) => body`. Empty `{}` body stays `{}`. | Anonymous `function(args){ ... }`. | `emitLambda` |
| 6 | Array literals `[a, b]` | Emit `[a, b]` verbatim. | `Array.of(a, b)`. | `emitArrayLiteral` |
| 7 | Bundle literals `\|a, b\|` | Emit JS arrays. Source bundle semantics aren't defined yet. | Object literal; tuple via array + tag; Symbol-wrapped struct. | `emitBundleLiteral` (with TODO) |
| 8 | `Module::fn(x)` | Emit `Module.fn(x)`. Assumes a `Module` identifier is in scope. | Concat-mangle to `Module_fn` (the legacy approach); `import { fn } from './Module'`. | `emitFunctionCall` (MODULE branch) |
| 9 | `obj.field.fn(x)` | Emit `obj.field.fn(x)` verbatim. | `Reflect.apply`. | `emitFunctionCall` (OBJECT branch) |
| 10 | Named arguments `fn(a=1, b=2)` | Emit single object literal: `fn({a: 1, b: 2})`. Callee must destructure. | Multiple variants per call site; ignore names and emit positional. | `emitFunctionCall` (named-args branch, TODO) |
| 11 | String literals | Pass through backtick form. Waterfall source already uses backticks; JS template literals also use backticks. Only escape if the literal contains `${` — none of the current examples do, so left for later. | Translate to double-quoted; resolve all escapes. | `emitExpression` (STRING_LITERAL branch, TODO) |
| 12 | `null` literal | Emit `null`. | `undefined`. | `emitExpression` (NULL_LITERAL branch) |
| 13 | Function body formatting | 4-space indentation, one statement per line, opening brace on the same line as `function`. Empty body emitted as `{}` (no indent). | Allman braces; no indent (single-line). | `emitFunctionImpl` |
| 14 | `if`/`elif`/`else` formatting | Inline braces, single-line block bodies (matches legacy compactness). Phase 7 may pretty-print. | Multi-line indented. | `emitIfBlock` |
| 15 | Where diagnostic messages go | `TopLevelSymbolTableGenerator` still printed to STDOUT, which polluted JS output and broke `node --check`. Switched to STDERR (mirrors the Phase 1 log4j change). | Structured diagnostic sink. | `TopLevelSymbolTableGenerator` |
