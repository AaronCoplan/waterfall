# Waterfall — Codebase Audit (Task #1)

Author: codebase-auditor
Scope: descriptive only. No roadmap proposals, no design changes.

Repo: `/Users/afcoplan/Documents/github/waterfall` (53 source files, Kotlin 2.0
on JVM 1.8, ANTLR 4.7.1, Gradle 8.10.2). Recent commits `56f0070` and `c6cec11`
migrated the compiler and parser sources from Java to Kotlin (phases 9b/9c).
Master is clean.

---

## Section 1 — Capability matrix

Status legend:
- **Supported**: grammar, AST, verifier, AND all four backends produce sensible output.
- **Partial**: works in some backends but stubbed or wrong in others; documented in `AUDIT-OPEN-QUESTIONS.md`.
- **Stubbed**: parsed and represented in the AST but at least one backend emits a `/* TODO(audit): … */` placeholder or no-op.
- **Missing**: not in the grammar at all.

### Declarations and modifiers

| Feature | Status | Notes |
|---|---|---|
| Single-module-per-file (`module Name { ... }`) | Supported | One module = one `.wf` file; `Main.kt` walks every file and emits one program each. `parser/src/main/antlr/WaterfallParser.g4:9-12`. |
| Top-level typed variable decl `int x = 4` | Supported | All four backends. `TypedVariableDeclarationAndAssignmentData.kt:24`. |
| Top-level function decl | Supported | All four backends. `FunctionImplementationData.kt:27`. |
| Inner typed var decl `int x = 1` | Supported | Phase 8e wired inner decls into the symbol table. `compiler/.../symboltables/SymbolTable.kt`. |
| Inner untyped var decl `x := 14` | Supported | Type inferred from literal kind only (G4). `UntypedVariableDeclarationAndAssignmentData.kt:35-44`. |
| Re-assignment `x = expr` | Supported | All backends. `VariableAssignmentData.kt`. |
| Compound assignment `+= -= *= /= %=` | Supported | Grammar `WaterfallParser.g4:77`. All backends pass the op text through. |
| Increment/decrement `x++` / `x--` (statement) | Supported | Statement-level only, on plain `ID`. Python lowers to `+= 1` / `-= 1` (`PythonBackend.kt:111-114`). |
| Modifier `const` | Supported | JS → `const`, Python → `: Final` (with `from typing import Final` prelude), C → `const`. Detected via `isImmutable()` in both var-decl `*Data` classes. |
| Modifier `imm` | Supported | Treated identically to `const`. `TypedVariableDeclarationAndAssignmentData.kt:22`. |
| Inner-decl shadow rejection | Supported | `SymbolTable.declare` walks ancestors and rejects any name in scope. Exercised by `DuplicateInnerDeclarationTest.shadowingOuterParameterIsRejected`. |
| Distinct-branch reuse of a name (`if{int x...} else{int x...}`) | Supported | Each branch gets its own child scope. `IfBlockData.kt:37-55`. |

### Control flow

| Feature | Status | Notes |
|---|---|---|
| `if` / `elif` / `else` | Supported | Multiple `elif`s allowed. `WaterfallParser.g4:61-74`. |
| `while(cond) {...}` | Supported | `WhileBlockData.kt`. |
| `for (ID in ID) {...}` | Partial | JS/Python/Legacy work. C emits a zero-iteration placeholder with TODO comment (C1). `CBackend.kt:128-134`. |
| Bare `return` | Supported | All backends. `ReturnStatementData.kt:13`. |
| `return expr` | Supported | All backends. |
| Typed for-in iterator (`for(char c in chars)`) | Missing | README roadmap mentions this. Grammar only accepts `name=ID IN collection=ID`. |
| `break` / `continue` | Missing | Not in lexer or parser. |
| Pattern matching / `switch` / `case` / `match` | Missing | Not in grammar. |
| `do { } while` | Missing | Not in grammar. |
| `goto`, labels | Missing | Not in grammar. |
| Exceptions (`try`/`catch`/`throw`) | Missing | No tokens reserved. |

### Functions and call styles

| Feature | Status | Notes |
|---|---|---|
| `func name(args) returns T { ... }` | Supported | Typed args required; return type optional → void. `FunctionImplementationData.kt`. |
| `func name() { }` empty body | Supported | All backends. Python emits `pass`; C/legacy emit `{}`. |
| Local function call `fn(args)` | Supported | All backends. |
| Module-qualified call `Mod::fn(args)` | Partial | JS/Python → `Mod.fn(args)` (assumes binding in scope). Legacy/C mangle to `Mod_fn(args)`. C2 flagged: won't link without header-per-module. |
| Method call `obj.fn(args)` and chained `a.b.c.fn(args)` | Partial | JS/Python emit `obj.fn(args)` verbatim. Legacy mirrors. C emits `obj_fn(&obj, ...)` (C3). README admits "won't link in any real example." `CBackend.kt:193-198`. |
| Method call on type literals (`int[].create(26)`) | Missing | README roadmap acknowledges this. Grammar only allows `ID DOT ...`. |
| Positional args | Supported | `WaterfallParser.g4:167-169`. |
| Named args `fn(a=1, b=2)` | Partial | All-or-nothing per call (no mixing — `FunctionCallTest.testModuleFunctionCallMixedArgs` asserts mixing fails). JS uses object literal `fn({a:1, b:2})` — requires callee changes. Python uses native named args. C drops names. Legacy preserves `a=1` source form. (U3.) |
| Mixed positional + named in one call | Missing | Grammar `functionCallArguments` is XOR. |
| Variadic args | Missing | Not in grammar. |
| Default parameter values | Missing | Not in grammar. |
| Function overloading | Missing | Duplicate names rejected — `FunctionImplementationData.kt:34-38`. |
| Recursion | Supported | Self-declaration into outer scope happens *before* body verification (`FunctionImplementationData.kt:34-37`), so `fib` calls itself fine. |
| Forward references between sibling functions | Working in practice | Top-level functions are all `declare`d into the module scope as they're verified in source order. A function calling a later sibling resolves at codegen time (no function-call type-check exists). |

### Types

| Feature | Status | Notes |
|---|---|---|
| Primitives: `int`, `dec`, `bool`, `char` | Supported | `typesystem/PrimitiveTypes.kt:13-19`. |
| Array of primitive: `int[]`, `dec[]`, `bool[]`, `char[]` | Supported | `PrimitiveTypes.isArray` recognizes the suffix; backends emit `T*` (C), drop type (JS/Python/legacy). |
| Nullable annotation `?T` | Parsed but unused | Grammar accepts `QUESTION_MARK? ID ...` (`WaterfallParser.g4:101-103`). Nothing in the verifier or backends reads it. `TypedVariableDeclarationAndAssignmentData.type` is `ctx.type().text` which would include the `?`, but `PrimitiveTypes.isPrimitiveOrArray("?int")` returns false → verification rejects. No example uses it. |
| Multi-dim arrays (`int[][]`) | Missing | Grammar has exactly one `(L_BRACKET R_BRACKET)?` — no recursion. |
| Generic types `T<U>` | Missing | No `<` / `>` reserved for type position; only used as comparator. |
| User-defined types (struct/record/class) | Missing | Grammar has no `struct`, `class`, `record`, `type`, `enum`, or `interface` keyword. |
| Type aliases | Missing | No `typedef` / `type X = Y` keyword. |
| Enum types | Missing | Not in grammar. |
| Union/sum types | Missing | Not in grammar. |
| Type inference for `:=` from literal kind | Partial | Only INT/DEC/STRING literals; everything else → `int`. (G4.) |
| Cross-expression type inference (calls, arithmetic) | Missing | (G4.) |
| Type-checking of arithmetic operands | Missing | No verifier rule on `BINARY_OP.left` / `right`. |
| Type-checking of `if`/`while`/`for` condition as bool | Missing | (G5, flagged in IfBlockData.kt:36, ForBlockData.kt:27.) |
| Function-arg type-checking at call sites | Missing | `FunctionCallStatementData.verify` is a no-op (`FunctionCallStatementData.kt:14-17`). |
| Return-type type-checking | Missing | `ReturnStatementData.verify` is a no-op (`ReturnStatementData.kt:15`). |

### Operators

| Feature | Status | Notes |
|---|---|---|
| Arithmetic `+ - * / %` | Supported | All backends. Precedence in grammar. |
| Exponent `^` | Supported | C lowers to `pow()` and requests `<math.h>` (`CBackend.kt:165-169`). JS/Python use `**`. Legacy emits `^` verbatim. |
| Comparison `< > <= >=` | Supported | All backends. |
| Equality `equals` | Supported | JS → `===`, Python/C → `==`. (`JavaScriptBackend.kt:141`, `PythonBackend.kt:151`, `CBackend.kt:174`.) |
| Inequality / `not equals` | Missing | No token. Must be `not(a equals b)` — but `not` isn't a keyword either. |
| Boolean `and`, `or` | Supported | Mapped per target (`&&` / `\|\|` / native). |
| Boolean `not` / `!` | Missing | Not in lexer. |
| Bitwise operators | Missing | Not in lexer. |
| Cast `expr castas T` | Supported | All backends. JS uses `Math.trunc/Number/Boolean/String`. C uses `((T)(...))`. Python uses `int/float/bool/str`. Array casts are no-ops on JS/Python; C emits `((T *)(...))`. |
| Array index `arr[i]` | Supported | Read-side only. |
| Array index on LHS of assignment (`arr[i] = x`, `arr[i]++`) | Missing | `variableAssignment` only accepts `name=ID`, not `arrayIndex`. README roadmap acknowledges this. |
| Ternary `cond ? a : b` | Missing | Tokens `?` and `:` exist (`?` for nullable, `:` doesn't — only `::` and `:=`). |
| String concatenation | Implicit only | `+` between strings would parse but no backend resolves what that means; no test exercises it. |

### Expressions and literals

| Feature | Status | Notes |
|---|---|---|
| `NULL` literal | Supported | All backends: `null` / `None` / `NULL`. |
| `true` / `false` literals | Supported | Phase 8a closed G1 — `BOOL_LITERAL` token. Python case-translates to `True`/`False` (`PythonBackend.kt:118`). |
| Integer literal | Supported | `INT_LITERAL`. Decimal only — no `0x`, `0b`, or `_` separators. |
| Decimal literal `3.14` | Supported | `DEC_LITERAL` requires both sides of the dot. No `.5` or `5.` or scientific notation. |
| Char literal (single-quoted) | Missing | No grammar rule. `char` type stores via STRING_LITERAL inference (untyped `:=` of a string infers `char`, see `UntypedVariableDeclarationAndAssignmentData.kt:40`). |
| String literal | Supported | Backtick-delimited. Phase 8h added escape resolution via `StringLiteralText`. |
| Array literal `[1, 2, 3]` | Supported | All backends. C uses C99 compound literal. |
| Empty array literal `[]` | Partially Missing | Grammar's `arrayLiteral` requires `positionalArgumentList` which requires at least one element. C codegen has dead branch for empty (`CBackend.kt:220-223`). |
| Bundle literal `\|a, b\|` | Stubbed | Parsed, AST'd. JS/Python emit arrays. C emits `/* TODO(audit): bundle */ {0}`. Legacy passes through. Semantics undefined (U1). |
| Lambda `(int x) ==> body` (call body) | Partial | JS → arrow, Python → `(lambda x: body)`. Legacy passes through. C emits `/* TODO(audit): lambda */ NULL` placeholder (U2). |
| Lambda empty body `(...) ==> {}` | Supported | All backends. |
| Lambda with multi-statement body | Missing | Grammar's `lambdaFunction` body is exactly one `functionCall` (or empty). |

### Modules and imports

| Feature | Status | Notes |
|---|---|---|
| One module per file | Supported | `Main.kt:75-83` rejects duplicate module names across the file list passed on the CLI. |
| `Mod::fn(x)` qualified call | Partial | See call styles row above. |
| `import` statement | Missing | No `import`/`use`/`include` keyword. The whole multi-file model is "pass several .wf paths to `./waterfall`". |
| Module visibility (`pub`/`private`) | Missing | All decls effectively public. |
| Module hierarchy / packages | Missing | Module names are flat strings; the cross-module call `Mod::fn` doesn't carry path info. |

### Backends and tooling

| Feature | Status | Notes |
|---|---|---|
| `--target legacy` | Supported | Default. The original C-like dump. `LegacyTextBackend.kt`. |
| `--target js` | Supported | `node --check` verifies every example. (`JsRuntimeCheckTest`.) |
| `--target python` | Supported | `python3 ast.parse` verifies. |
| `--target c` | Supported | `gcc -fsyntax-only` verifies, with `-Wno-implicit-function-declaration`, `-Wno-int-conversion`, `-Wno-return-type` (these would otherwise catch the unresolved Mod_fn / receiver-as-first-arg cases). |
| JS module wrapping (ESM / CJS / IIFE) | Stubbed | Decisions deferred (C6). Currently a single `// module X` comment. |
| C per-module headers / linking | Missing | All output is single-TU. C2/C3 flagged. |
| Source maps | Missing | No backend emits position info into output. |
| Diagnostic format | Plain text | `System.err.println` lines like `"$path at $line:$column"` (`SourcePosition.kt:8`). No structured (JSON) form, no error codes, no recoverable parse. |
| LSP / IDE integration | Missing | Not in scope. |

### Effects, error handling, concurrency, FFI

| Feature | Status | Notes |
|---|---|---|
| Exceptions or `Result`-style error types | Missing | The compiler itself throws `CompilerError` but the language has no equivalent. |
| Defer / RAII / destructors | Missing | Not in grammar. |
| Concurrency primitives (threads, async, channels) | Missing | Not in grammar or stdlib. |
| FFI / extern declarations | Missing | No way to declare an external function whose body lives in the target. The C backend's emitted code calls undeclared `doSomething()` etc., but only `gcc -fsyntax-only -Wno-implicit-function-declaration` accepts that — anything stricter would reject. |
| Stdlib | Missing | No prelude module ships with the compiler. Programs must reference external identifiers and rely on the target language's globals (e.g., `Math::sqrt(2)` becomes `Math.sqrt(2)` in JS, `Math_sqrt(2)` in C). |
| Memory management hints / ownership | Missing | No borrow checker, no ref counting, no GC story. |
| Generics / type parameters | Missing | Acknowledged in user prompt as worth exploring later. |
| Traits / interfaces / typeclasses | Missing | Not in grammar. |
| Pattern matching | Missing | Not in grammar. |
| Algebraic data types (struct, enum-with-payload) | Missing | Not in grammar. |
| Sealed types / variants | Missing | Not in grammar. |

---

## Section 2 — Architecture overview

**Pipeline shape**: `parser` is a Gradle subproject (Kotlin + ANTLR-generated
Java); `compiler` is the Kotlin frontend + verifier + four backends. There is
no intermediate representation — the front-end AST is consumed directly by each
backend.

**Lexer**: `parser/src/main/antlr/WaterfallLexer.g4`. Hand-rolled tokens for
keywords (`module`, `func`, `return`, `if`, `else`, etc.), the unusual
word-shaped operators (`and`, `or`, `equals`, `castas`), and the literal kinds.
`BOOL_LITERAL` is defined *before* `ID` so a `true`/`false` identifier never
shadows the keyword. The `ID` rule was historically broken (rejected
two-character identifiers like `go`) — fixed in phase 8e by changing `+` → `*`
on the middle group. STRING_LITERAL is backtick-delimited; comments are not in
the grammar (so there is no source-level comment syntax). NEWLINE is returned
to the parser as the end-of-statement signal; whitespace `[ \t]+` is skipped.

**Parser**: `WaterfallParser.g4`. A standard ANTLR LL(*) grammar with
left-recursive `expression` for precedence. `program → module EOF`. A module
has a name and a `(typedVariableDeclarationAndAssignment | functionImplementation)*`
list at the top level. Functions take a typed-arg list and have an optional
returns clause. Statements include declarations, assignments, function-call
expressions, control-flow, return, and the `incrementStatement` (`x++`/`x--`).
The `expression` rule covers everything from literals through `castas` and the
ten arithmetic/comparison/boolean alternatives at the bottom. There is one
unusual pair of constructs: `lambdaFunction` whose body is exactly one
`functionCall` (or empty), and `bundleLiteral` pipes whose semantics aren't yet
defined.

Three notable parser quirks: (1) named arguments are XOR with positional args
at the call site — you can't mix them in a single call; (2) `for-in` accepts
only `ID IN ID` (no typed iterator); (3) the `?` nullable prefix on `type` is
parsed but unused everywhere downstream.

**Parser frontend (Kotlin)**: `parser/src/main/kotlin/.../FileParser.kt`
wraps the ANTLR-generated lexer/parser, removing the default error listener
and installing a `SyntaxErrorListener` that captures `file line N:M msg`
strings into a `ParseResult`. `ParseResult` carries the `ProgramContext`
parse tree, errors, and the source file path forward. `FileUtils` reads
files line-by-line via `Scanner` (which trims the trailing newline on each
line then `append('\n')`s — exactly equivalent to `Files.readString` if the
file has a final newline, slightly different otherwise).

**AST**: there isn't a clean separation. Each grammar production has a
corresponding `*Data` class under `compiler/src/main/kotlin/.../statements/`
that wraps the ANTLR `ParserRuleContext` *and stores all relevant fields as
plain JVM-friendly `@JvmField`s*. They double as the AST. The dispatch from
parse tree to `*Data` is `StatementDispatcher.fromStatement` (a manual
`stmt.xxx()?.let { ... }` chain) and `ModuleAst.init` (the same pattern for
top-level decls). `ExpressionData` is the most interesting one: it eagerly
walks the parse-tree alternative for cast vs. binary vs. leaf in its
`init` block and stores everything as nullable fields keyed by a `Kind`
enum (`ExpressionData.kt:7-21`). Every other expression flavor — bundles,
arrays, lambdas, function calls, array index — has its own small `*Data`
class hanging off `ExpressionData`.

**Verifier**: there is no separate pass. `Translatable.verify(symbolTable)`
is implemented on each `*Data`; the entry point is `Main.run` which loops
over top-level vars then functions calling `verify()` on each, threading a
single `SymbolTable` per module through. Nested verifies recursively
construct child `SymbolTable` scopes (`IfBlockData.verify`,
`ForBlockData.verify`, `WhileBlockData.verify`,
`FunctionImplementationData.verify`). The verifier currently does only two
real checks: (1) declared type is a known primitive-or-array; (2)
re-declaration of a name in the same or an ancestor scope is rejected via
`SymbolTable.declare`. Most other `verify()` methods are no-ops returning
`VerificationResult(true, null)` — e.g. `VariableAssignmentData`,
`FunctionCallStatementData`, `ReturnStatementData`,
`IncrementStatementData`.

**Symbol table**: `SymbolTable.kt` is 26 lines. It's a name → `Any?` map
with a parent pointer. `declare` walks the parent chain to reject any name
already in any enclosing scope, throwing `DuplicateDeclarationException`.
`lookup` is `private` — nothing else can look up the type of a known
binding, which is a major reason there's no type inference yet. The
stored info is currently the type *string* (e.g. `"int"`, `"int[]"`,
`"void"`) — see all the `symbolTable.declare(name, type)` callsites. There
is no mutability flag, no kind discriminator (`var` vs. `func` vs. `arg`),
and no position info attached to declarations.

**Code generation interface**: `target/CodeGenerator.kt`. One method per AST
node kind (`emitTypedVarDecl`, `emitIfBlock`, ..., `emitBundleLiteral`).
Each `*Data` has a `translate(backend)` method that dispatches back via
`backend.emitX(this)` — the visitor pattern without ANTLR's `accept`
machinery. New AST kinds require updating `CodeGenerator` *and* every
backend (no default implementation). New backends register one entry in
`Backends.REGISTRY` (`target/Backends.kt:11-16`).

**Backends**:

- **LegacyTextBackend** is the original output format. Curly-brace C-like
  syntax with statements jammed onto adjacent lines and zero whitespace
  inside braces — preserves byte-for-byte what the compiler emitted before
  the backend abstraction landed (see the comment at `LegacyTextBackend.kt:30-37`).
  It serves mostly as a regression anchor.

- **JavaScriptBackend** is the cleanest backend. Strips types, `const`/`imm`
  → `const` else `let`, `for-in` → `for...of` (note: NOT JS `for...in`),
  lambdas → arrow functions, `equals` → `===`, `^` → `**`, casts wrap in
  `Math.trunc`/`Number`/`Boolean`/`String`. Array literals pass through.
  Bundle literals fall back to JS arrays with a TODO. Named arguments
  collapse to a single object literal — a hostile decision that requires
  callee changes.

- **PythonBackend** drops braces and re-indents via `indent(text, level)`
  which prefixes every non-empty line. Each `emit*` returns its node at
  "indent level 0" and the enclosing block adds whitespace. `const`/`imm`
  emit `name: Final = value` with a conditional `from typing import Final`
  prelude (a `usesFinal` flag is reset in `emitProgram` and the prelude is
  emitted after the body finishes). Increment lowers to `x += 1` / `x -= 1`.
  Two-blank-line separator between `def`s (PEP-8).

- **CBackend** is by far the most complex. `cType()` translates type names
  with array suffix handling; bool requests `<stdbool.h>`. Headers are
  populated into a `TreeSet` as the body is emitted (so unused headers
  are omitted, post-phase 8c). For-in is a zero-iteration stub. Lambdas
  return `NULL` with a TODO. `obj.fn(x)` mangles to `obj_fn(&obj, x)`.
  `Mod::fn(x)` mangles to `Mod_fn(x)`. Named arguments are silently dropped
  to positional. The C99 array-element-type inference looks at the first
  element's `Kind` (`CBackend.kt:219-234`) — empty arrays default to `int`
  with a TODO. `^` is lowered to `pow()` and requests `<math.h>` (note: no
  `-lm` is emitted, and there is no separate compile/link step in the
  toolchain — the runtime check uses `-fsyntax-only` and never links).

**Surprising design decisions**:

1. **No separate AST type.** The `*Data` classes own their construction by
   walking the ANTLR `ParserRuleContext` directly. So an AST node holds
   the *post-processed* fields (e.g., the resolved list of statements in
   a body) but no longer the rule context after construction. There is no
   pre-AST validation pass, no AST-rewrite pass, and no IR.

2. **Verification and codegen are intertwined.** `Translatable.verify` is on
   the same interface as `Translatable.translate`. The `Main` driver calls
   `verify` only on top-level decls; verification of inner statements
   happens transitively as the top-level `verify` walks its body. Backends
   can be called on an unverified AST in principle, but in practice
   `Main.run` always verifies first.

3. **Backends own indentation and whitespace.** There's no shared
   pretty-printer abstraction. The legacy emitter is intentionally
   whitespace-minimal; JS uses 4-space indent only at function bodies;
   Python has its own `indent(text, level)`; C splits the difference (4
   spaces inside functions; loops/if-bodies are flat). This is why some
   golden outputs have if-bodies on one line but function bodies indented
   — it's purely cosmetic-per-backend.

4. **One module per parsed file but multi-file is partially wired.** The
   CLI accepts `nargs("+")` files; the loop emits each module's code to
   `stdout` in turn. There's nothing connecting them — a `Module::fn(x)`
   reference assumes the runtime provides `Module` (so JS/Python lean on
   the host's module system, C just produces a name mangle that won't link).

---

## Section 3 — Component-by-component code quality

### `parser/` (Kotlin frontend wrapper)

**Strengths**: very small surface (`FileParser`, `ParseResult`,
`SyntaxErrorListener`, `FileUtils`, `Pair`). `SyntaxErrorListener` collects
errors instead of letting ANTLR's default printer log them — clean
separation.

**Weaknesses**:
- `FileUtils.readFile` uses `Scanner.nextLine()` + `append('\n')` — re-adds
  newlines, which is fine for parsing but means the file path won't round-trip
  if you read+write the same source (`parser/src/main/kotlin/.../FileUtils.kt:27-36`).
- The custom `Pair<K, V>` class predates Kotlin's `kotlin.Pair` and is used
  pervasively across the compiler (`SymbolTable`, `VerificationResult`,
  `FunctionImplementationData.typedArguments`, etc.). The `@JvmField`
  annotation suggests it was originally optimized for Java callers; the
  comment at `Pair.kt:5` acknowledges this. The Kotlin migration left it
  intact rather than swapping to `kotlin.Pair` — likely correct in this
  phase but a long-term simplification.
- `VerificationResult` *extends* `Pair<Boolean, String?>` (`VerificationResult.kt:6`)
  — a class-hierarchy choice that's unusual for what's effectively a record.

### `compiler/argumentparsing/`

**Strengths**: thin wrapper over argparse4j. `Backends.knownTargetsList()`
is queried for the `--target` help text and validation — nice single source
of truth.

**Weaknesses**: tiny but cluttered — `Arguments.kt` has only two fields and
two getters; could be a data class. The unknown-target check happens twice
(`ArgParser.kt:29` and again in `Main.run` at `Main.kt:47`). The
`HelpScreenException` branch returns empty string instead of `null` for the
error message and then Main checks `arguments == null`, which works but is
roundabout.

### `compiler/typesystem/PrimitiveTypes.kt`

**Strengths**: tight, well-commented, accurate. The
`isPrimitive`/`isArray`/`isPrimitiveOrArray`/`elementType` quad is the right
shape for current needs.

**Weaknesses**: it's strings all the way down. There's no `enum class Type`
or sealed hierarchy yet. Any future type system work will rewrite this
module entirely. No `void`-as-a-type — `void` is treated specially in
`FunctionImplementationData.verify` at line 35 (`symbolTable.declare(name, returnType ?: "void")`),
which means `void` *is* a string in the symbol table — an inconsistency
relative to `PrimitiveTypes.ALL` not containing it.

### `compiler/symboltables/`

**Strengths**: `SymbolTable.kt` is 26 lines and does its one job correctly
— scope chaining with re-declaration rejection. The single
`DuplicateDeclarationException` is enough for current needs.

**Weaknesses** (most consequential, given the upcoming `readonly` work):
- `lookup` is `private`. Nothing outside the symbol table can ask "what is
  the type of `x`?", which is why type inference (G4) and condition
  type-checking (G5) haven't landed (`SymbolTable.kt:21-25`).
- Stored info is `Any?`. Today it's the type string. There's no place to
  attach a mutability flag, an originating-source position, or a kind
  discriminator. Adding flow-sensitive `readonly` requires either a new
  mutable per-name `SymbolInfo` value class or a parallel mutability table.
- No iteration over entries. Listing all bindings in a scope (for shadowing
  diagnostics, for IDE features later) isn't possible.
- Re-declaring in the *current* scope and shadowing-an-ancestor are
  conflated — both throw the same exception. The error message
  ("Duplicate declaration") covers both at `TypedVariableDeclarationAndAssignmentData.kt:32`,
  which is technically misleading for the shadowing case.
- `SymbolTable.declare`'s parent walk uses recursion via the private
  `lookup`. With deeply nested for-loops/if-blocks, this is O(depth) per
  declare — fine for now, but won't scale.

### `compiler/statements/`

**Strengths**:
- Every `*Data` extends `TranslatableStatement` which captures
  `SourcePosition` from the start token (`TranslatableStatement.kt:9-14`).
  So *every node has a position*, available via `getSourcePosition()`.
  Diagnostic error messages use this.
- The `StatementDispatcher` is explicit (`StatementDispatcher.kt:17-28`)
  — easy to read, easy to extend.
- `StringLiteralText` (introduced in phase 8h) is a proper escape-handling
  module, with both decoding (source → raw) and encoding (raw → target).
  This is the kind of focused utility the codebase should have more of.

**Weaknesses**:
- The visitor pattern is hand-rolled: `Translatable.translate(backend) →
  backend.emitX(this)`. Adding a new node kind means *four* edits per
  backend (the interface method + each impl). No `default` keyword usage,
  no helper base class.
- `ExpressionData.init` is a 60-line if/else that touches every alternative
  of the `expression` grammar rule (`ExpressionData.kt:47-107`). It's
  exhaustively initialized to keep `@JvmField` non-null where applicable,
  but the structure forces every new expression kind to add a new field
  *and* update the constructor. A sealed-class-per-kind design would let
  each kind own its fields.
- Most `verify()` implementations are no-ops:
  `VariableAssignmentData.verify` (no LHS-exists check, no
  type-compat check) — `VariableAssignmentData.kt:17`;
  `FunctionCallStatementData.verify` (no callee resolution) —
  `FunctionCallStatementData.kt:14-17`;
  `ReturnStatementData.verify` (no return-vs-function-signature check) —
  `ReturnStatementData.kt:15`;
  `IncrementStatementData.verify` (no exists check, no type check, no
  immutability check) — `IncrementStatementData.kt:16`.
- `FunctionImplementationData.verify` self-declares the function name with
  return type as the symbol info (`FunctionImplementationData.kt:34-38`).
  So the symbol table stores `add: "int"`, indistinguishable from a
  variable `int add = 0`. There's no separate "function" kind. The
  duplicate-name check works because both throw, but anything that wants
  to call into the symbol table later (call resolution, signature lookup)
  will need to separate these.
- `ModuleAst` separates `topLevelVariables` and `functions` into two lists
  (`ModuleAst.kt:14-15`), losing the source-order interleaving. Currently
  every backend emits vars first then functions, which is fine, but a
  future "emit in source order" couldn't recover the original ordering.
- `FunctionCallData.kt` line 49 has subtle logic: it builds `receiverPath`
  from the `obj.ID()` list and then removes the last one if it equals
  `functionName`. The grammar's `objectFunctionCall` has the function name
  as a *labeled* `ID` and the receiver path as *unlabeled* `ID`s — and
  ANTLR's `ctx.ID()` returns ALL of them, so the removal is needed. It
  works, but it's fragile to label changes in the grammar.

### `compiler/target/`

**Strengths**:
- The `CodeGenerator` interface is small and uniform. Backends can be added
  without touching the front-end. (Demonstrated by phases 3/4/5.)
- The `Backends` registry uses lambdas — fresh backend per `--target`
  invocation, no shared state between runs (important for `CBackend.requiredHeaders`
  and `PythonBackend.usesFinal` which are reset per `emitProgram`).
- Each backend's leading comment-block summarizes its mapping decisions.
  `CBackend.kt:24-30` is the best example.

**Weaknesses**:
- **No shared pretty-printer.** Every backend reinvents indentation. The
  legacy/JS/C backends all have a `joinBody(body)` helper, but each writes
  its own. There's no concept of a "block" — just string concatenation
  with curly braces. This is why some emitted code has if-bodies on a
  single line and function-bodies indented across multiple lines
  (`golden/c/FibonacciModule.expected` shows the inconsistency in the same file).
- **`emitFunctionCall` is duplicated four times** with target-specific
  munging in each. There's no shared "function call structure" object the
  backends specialize against.
- **`emitFunctionImpl` is duplicated four times.** Same.
- **`emitIfBlock` is duplicated four times.** Same — `LegacyTextBackend.kt:55-71`,
  `JavaScriptBackend.kt:70-86`, `PythonBackend.kt:83-96`, `CBackend.kt:107-123`
  all walk `ifBranch + elifBranches + elseBody?` with the same loop shape.
- `JavaScriptBackend`'s cast lowering picks `Math.trunc` for `castas int`
  (`JavaScriptBackend.kt:127`). This is fine for positive numbers but
  rounds toward zero rather than toward negative infinity — a subtle
  semantic difference from C/Python. No test exercises a negative cast,
  so it might be wrong silently.
- `LegacyTextBackend.emitForBlock` emits `for (auto x : c)` (C++ syntax)
  — `LegacyTextBackend.kt:74`. The legacy backend is C-like but borrows
  C++ range-for. Acknowledged in `PHASE-1-decisions.md` row 8.
- `LegacyTextBackend.emitExpression` for `STRING_LITERAL` returns
  `e.literalText!!` verbatim — the *source* backtick form. So legacy
  output has backticks in strings. Other backends decode. This is
  technically a different language than the others.
- `CBackend.emitForBlock` emits a comment containing a parameter from
  the source (the collection name). If `collectionName` happened to
  contain `*/`, the comment would unterminate. No test exercises this;
  the identifier lexer rule wouldn't allow `*/` in a name, so it's
  effectively safe — but the pattern is fragile.
- `CBackend.emitArrayLiteral` infers element type from the first element
  only (`CBackend.kt:219-234`). Mixed-type arrays (e.g., `[1, 2.5]`) would
  emit `(int[]){1, 2.5}` — gcc would warn but `-Wno-int-conversion` swallows it.
- `requiredHeaders` is on the instance, not in the call signature. So the
  same `CBackend` instance can't be reused across modules without resetting
  in `emitProgram`. It does reset, but the contract is hidden.

### `compiler/Main.kt`

**Strengths**: linear pipeline (parse → existence check → syntax check →
verify → translate). Easy to read. Splits in/out streams cleanly:
diagnostic info goes to stderr, emitted code goes to stdout (`println(backend.emitProgram(moduleAst))`).
The `Main.run` / `Main.main` split lets tests observe `CompilerError`.

**Weaknesses**:
- Verification and translation are in the same loop (`Main.kt:74-109`).
  If module A verifies but module B fails, A's code already printed
  before B's failure surfaces. No batched verification before any
  translation.
- Duplicate `Backends.forTarget` call (once at `ArgParser.kt:29` for
  validation, again at `Main.kt:47`). The first call's result is discarded.
- Error messages are stringly-typed. Throwing `CompilerError("verification failed")`
  loses the underlying VerificationResult's structured info — only stderr
  gets the real message.

### Tests

**Strengths**:
- Parameterized golden tests with `UPDATE_GOLDEN=1` regeneration
  (`GoldenTests.kt:62-65`) — pleasant DX.
- Runtime-check tests probe for tool availability and `Assume.assumeTrue`-skip
  when absent (`RuntimeCheckBase.assumeAvailable`) — CI-friendly.
- Inner-duplicate / shadow-arg / distinct-branch coverage in
  `DuplicateInnerDeclarationTest` is appropriately scoped.
- Parser tests are organized by grammar feature
  (`ModuleParsingTests`, `IfBlockTests`, `ForBlockTests`,
  `FunctionImplementationParsingTests`, `FunctionCallTest`,
  `AssignmentExpressionTests`).

**Weaknesses**:
- `DummyTest` (`compiler/src/test/kotlin/DummyTest.kt`) is an empty
  scaffold from an earlier era. Not in the package tree — sits at root.
- The C runtime check uses `-Wno-implicit-function-declaration`,
  `-Wno-int-conversion`, `-Wno-return-type` (`RuntimeChecks.kt:139-145`).
  Anything stricter would reject most golden outputs because `Mod::fn(x)`
  mangles to `Mod_fn(x)` and `Other_helper` is never declared.
- No negative tests for type errors. Verifier rejection paths are tested
  only for duplicate decls.
- No tests for the legacy `LegacyTextBackend` specifically beyond the
  goldens (no semantic test that distinguishes legacy quirks from real C++).
- Parameterized golden tests at `GoldenTests.kt:35-56` walk the *golden*
  directory rather than the *examples* directory — so an example that
  exists in `examples/` but has no `.expected` file is silently untested
  across that target. (Currently every example has a golden in all four
  targets — checked by counting.)
- `GoldenTests.runCompiler` swallows `CompilerError` for the Duplicate*
  cases. The swallowed exception's message is never asserted — the only
  signal is "stdout matches". The golden files for those duplicate cases
  are effectively *empty* outputs (`golden/legacy/DuplicateDeclarationsModule.expected`
  contains just a newline; verified by reading it). So the test pins
  "compiler emits nothing then fails," which is intentional but easy to
  miss.

---

## Section 4 — Extension points for flow-sensitive `readonly`

The user wants two forms:
- Form A — at declaration: `readonly x = 4` (already covered by today's
  `const` / `imm` modifier slot, *if* the grammar accepts a new keyword
  in the modifier position).
- Form B — mid-function: a standalone statement `readonly x` that
  promotes an already-mutable binding to immutable from that point
  onward. Potentially with subfield support (`readonly x.foo`).

Below is a precise inventory of every file that would need to change. No
fixes proposed — just where the wiring lives today.

### Grammar (Form A: declaration modifier)

- `parser/src/main/antlr/WaterfallLexer.g4:10-11`. The `CONST` / `IMM` tokens
  are defined here. A new `READONLY: 'readonly';` token goes here, before
  the `ID` rule at line 35 (lexer ordering matters — keywords must come
  first or the lexer picks `ID` on ties).
- `parser/src/main/antlr/WaterfallParser.g4:179-182`. The `modifier` rule
  is `: CONST | IMM`. Adding `READONLY` makes it `: CONST | IMM | READONLY`.
  No structural changes elsewhere — `modifier*` is already used in
  `untypedVariableDeclarationAndAssignment` (line 81) and
  `typedVariableDeclarationAndAssignment` (line 85).
- The generated ANTLR parser/lexer Java sources need regen after these
  edits — handled automatically by `generateGrammarSource` in
  `parser/build.gradle`.

### Grammar (Form B: standalone statement)

- `WaterfallParser.g4:19-29` — the `statement` rule. A new alternative
  like `readonlyPromotion` would slot in here. Today's similar pattern is
  `incrementStatement` at lines 31-33 (`name=ID op=(PLUS_PLUS | MINUS_MINUS) NEWLINE+`)
  — that's the closest model: a one-line statement that takes a single
  identifier. For subfield support, the rule would need either an
  `objectFunctionCall`-style dotted path (line 159) or a new dotted-ID
  fragment.

### AST (Form A)

- `compiler/src/main/kotlin/.../statements/TypedVariableDeclarationAndAssignmentData.kt:18`.
  `modifiers: List<String>` already captures every modifier text. The
  `isImmutable()` helper (line 22) currently checks `"const" in modifiers || "imm" in modifiers`.
  Adding `"readonly"` to that check enables Form A across all backends
  *without further edits* — because backends call `isImmutable()` to
  decide whether to emit `const`/`Final`/`const`.
- `compiler/.../statements/UntypedVariableDeclarationAndAssignmentData.kt:21`.
  Same `isImmutable()` story.

### AST (Form B)

- A new data class lives next to `IncrementStatementData.kt` — model:

  - `compiler/.../statements/IncrementStatementData.kt` is the structural
    template (extends `TranslatableStatement`, holds `name` and `op`,
    has a no-op `verify` and a single-line `translate`).

  - For subfield support, the data class would need a `path: List<String>`
    instead of a single `name`, similar to `FunctionCallData.receiverPath`
    (`FunctionCallData.kt:14`).

- The dispatcher must be updated:
  `compiler/.../statements/helpers/StatementDispatcher.kt:17-28`. Add one
  `stmt.readonlyPromotion()?.let { return ReadonlyPromotionData(filePath, it) }`
  line.

### Symbol table — mutability tracking

**Today's state of mutability tracking is the most consequential gap.**

- `SymbolTable.kt:5` stores `MutableMap<String, Any?>`. The value is set
  at `declare(key, info)` and `info` is whatever the caller passes. Today
  every caller passes the *type string* — `"int"`, `"int[]"`, `"void"`.
  *No mutability information is stored.*

- Callsites that declare a binding:
  - `TypedVariableDeclarationAndAssignmentData.verify` at line 30:
    `symbolTable.declare(name, type)`.
  - `UntypedVariableDeclarationAndAssignmentData.verify` at line 25:
    `symbolTable.declare(name, inferredType)`.
  - `FunctionImplementationData.verify`: line 35 (self for recursion),
    line 47 (each typed argument).

- `lookup` is `private` (`SymbolTable.kt:21`). External code cannot ask
  "is `x` mutable in this scope?" — there is no API for it.

- To support `readonly`, the symbol table either needs:
  - A richer info type (e.g., `SymbolInfo(type: String, isImmutable: Boolean, ...)`),
    plus public `lookup`, plus a `markImmutable(name)` mutator for Form B; or
  - A parallel `mutableNames: MutableSet<String>` to track Form B's
    runtime promotions, with the scope chain consulted on assignment.

- The scope semantics for Form B need a decision the architect won't
  flag in code — but the existing `SymbolTable` *walks parents* on
  `declare`, *resolves through* parents on `lookup`. A Form B promotion
  could either (a) mark the binding in the scope it was originally
  declared in (requires `lookup`-with-owner-scope), or (b) install a
  shadowing immutable entry in the current scope. Today's API supports
  neither directly.

### Verifier — assignment-to-immutable

**There is no immutability check anywhere in the verifier today.**

- `VariableAssignmentData.verify` (`VariableAssignmentData.kt:17`) is a
  no-op. This is where `x = 5` lands after `const int x = 0` — and
  nothing rejects it. The reason it appears to work is that all four
  backends emit raw `const` (or `Final`), and the *target language*
  ultimately catches it (or in Python's case doesn't). Adding a
  Waterfall-level check requires:
  - Symbol-table lookup of `name` for `isImmutable`.
  - A new failure path returning `VerificationResult(false, "Cannot assign to immutable binding $name")`.

- `IncrementStatementData.verify` (`IncrementStatementData.kt:16`) is
  also a no-op. `x++` on a `const int x` is silently accepted. Same
  fix-shape needed.

- Compound assignment is the same path as plain `=` (handled by
  `VariableAssignmentData`).

- For Form B (`readonly x` as a statement), the verifier needs to
  *change* the symbol table state — currently `verify()` is treated as a
  pure check that returns a `VerificationResult`. Mutating state during
  verify is consistent with what `declare` already does (a side effect),
  but it's a contract widening worth noting.

### Backends — current `const` / `imm` handling

Each backend's existing immutability emission is the codepath a new
`readonly` modifier would re-use unchanged at the codegen layer (for Form
A; Form B requires per-target lowering decisions but those are downstream).

- **JavaScript**: `JavaScriptBackend.kt:51-52` and `JavaScriptBackend.kt:56-57`.
  Both `emitTypedVarDecl` and `emitUntypedVarDecl` switch on
  `s.isImmutable()` to pick `const` vs `let`. Already correct *if*
  `isImmutable()` is updated to include `"readonly"`.

- **Python**: `PythonBackend.kt:57-71`. Both var-decl methods set
  `usesFinal = true` when immutable and emit `name: Final = value`.
  The `usesFinal` flag controls the `from typing import Final` prelude
  emission at `PythonBackend.kt:52`. Already correct via `isImmutable()`.

- **C**: `CBackend.kt:85-93`. Both var-decl methods prepend `const` when
  immutable. Already correct via `isImmutable()`.

- **Legacy**: `LegacyTextBackend.emitTypedVarDecl` and
  `emitUntypedVarDecl` at lines 39-43 *don't read modifiers at all* —
  legacy passes through the type and name without a `const` prefix.
  This is a known quirk (mirrors the original pre-backend output). Adding
  `readonly` support to the legacy backend would need a new conditional;
  it's the only backend that would need explicit codegen work for Form A.

### Backends — Form B lowering

For Form B (`readonly x` mid-function), the target-language lowering is
target-specific:

- **JavaScript**: there's no in-scope mutation from `let` to `const`. The
  most direct emission is a no-op (rely on the verifier's compile-time
  check), or a runtime check via `Object.freeze` (only relevant for
  objects).
- **Python**: same — no statement-level `Final` retrofit; Python honours
  `Final` only at declaration. Verifier-only enforcement is the natural
  fit.
- **C**: `const` is a declaration attribute, not a statement. Same:
  verifier-only.
- **Legacy**: no need.

So Form B lowering across all backends would be a no-op statement at the
codegen layer (or a `// readonly` comment for documentation). The
*meaning* of `readonly x` is entirely a verifier-time contract.

### Summary of Form B touchpoints (file:line)

1. `parser/src/main/antlr/WaterfallParser.g4:19-29` — add `readonlyPromotion`
   to `statement`.
2. `parser/src/main/antlr/WaterfallParser.g4` — add the new rule itself
   (model: lines 31-33 for `incrementStatement`).
3. New file: `compiler/.../statements/ReadonlyPromotionData.kt` — model
   after `IncrementStatementData.kt`.
4. `compiler/.../statements/helpers/StatementDispatcher.kt:17-28` — add
   dispatch line.
5. `compiler/.../symboltables/SymbolTable.kt:5,8-19,21-25` — promote
   info from `Any?` to a typed struct, expose `lookup` publicly, add a
   `markImmutable(name)` mutator. (Architect's choice between parallel
   set or rich info object.)
6. `compiler/.../statements/VariableAssignmentData.kt:17` — implement
   `verify` to lookup `name` and reject if immutable.
7. `compiler/.../statements/IncrementStatementData.kt:16` — same.
8. New file: `compiler/.../target/...` four `emitReadonlyPromotion` impls
   on `CodeGenerator.kt` — likely no-ops or `// readonly` comments.
9. `compiler/.../target/CodeGenerator.kt:33-49` — add interface method.

### Summary of Form A touchpoints (file:line)

1. `parser/src/main/antlr/WaterfallLexer.g4:10-11` — add `READONLY: 'readonly';`
   token.
2. `parser/src/main/antlr/WaterfallParser.g4:179-182` — add `READONLY` to
   `modifier` alternatives.
3. `compiler/.../statements/TypedVariableDeclarationAndAssignmentData.kt:22` —
   extend `isImmutable()` to include `"readonly"`.
4. `compiler/.../statements/UntypedVariableDeclarationAndAssignmentData.kt:21` —
   same.
5. `compiler/.../target/LegacyTextBackend.kt:39-43` — only backend that
   would need new codegen work (other three already route through `isImmutable()`).

---

## Section 5 — Architectural debt that blocks growth

These are the structural properties of today's code that make adding new
language features cost more than it should. No fixes here — just the
debt.

### D1. No intermediate representation

There is exactly one tree shape — the ANTLR-derived `*Data` AST — and
each backend walks it directly. There is no IR for resolved names,
inferred types, or lowering decisions. Consequences:

- The "what is `x`'s type at this site" question cannot be answered
  without re-walking the symbol table at codegen time. Backends instead
  duck-type: JS drops types, Python drops types, C uses the declared
  type *string*.
- Lowering passes (e.g., C-lambda lifting, U2) require either rewriting
  the AST in place or duplicating the entire walk. Neither pattern
  exists today.
- Cross-cutting concerns like "all assignments to immutable bindings"
  require checking each verify call by hand. There's no `forEach`
  visitor for the tree.

**Where to see it**: `compiler/.../statements/*Data.kt` (the only tree)
+ the four backends in `compiler/.../target/`. There is no folder for
passes.

### D2. Symbol table info is `Any?`

`SymbolTable.nameToInfoMap: MutableMap<String, Any?>` (`SymbolTable.kt:5`).
Every caller passes the type as a string. So today's symbol table holds
nothing distinguishing:
- a variable from a function (both stored as a type string);
- a `const`/`imm` binding from a mutable one (no flag);
- the declaration's source position (no field);
- the original type *kind* (scalar vs array — only recoverable by
  string suffix);
- forward declarations (everything is the actual decl).

Any feature requiring "is this name immutable?", "what is its full type?",
"where was it declared?", "what kind of binding is it?" must be added on
top of this. The `readonly` work is the immediate driver, but G4/G5,
function-arg type-checking, and any later module/visibility system all
share this dependency.

### D3. Verification is structurally entwined with translation

`Translatable.verify` and `Translatable.translate` are on the same
interface. The driver in `Main.run` (lines 74-109) loops through top-level
nodes calling `verify` then `translate` per-module. Consequences:

- It's hard to add a *cross-cutting* pre-pass — say, "resolve all type
  names module-wide before checking any function body" — because the
  current design has no concept of a phase.
- Mutating the symbol table inside `verify` is the only mechanism for
  semantic state. A reader of the code can't tell at a glance whether a
  `verify()` is a pure check or a state mutator.
- The error pipeline returns a `VerificationResult` which is bubbled up
  one frame at a time; no batching, no error recovery.

### D4. Backend duplication

Across four backends, the same node-shape walking is repeated four times
with target-specific string interpolation. `emitIfBlock` has identical
shape (`emit(if-condition)` + body + each elif + optional else) in
every backend. There's no shared "block" or "statement-list" abstraction
that backends specialize on a per-token basis.

**Where to see it**: side-by-side at
`LegacyTextBackend.kt:55-71`, `JavaScriptBackend.kt:70-86`,
`PythonBackend.kt:83-96`, `CBackend.kt:107-123`. Same loop, four times.

Adding a new statement kind means modifying four files.

### D5. Error reporting is single-shot strings

`VerificationResult(isSuccessful: Boolean, errorMessage: String?)` — one
boolean and one string. There's no error code (so callers can't switch
on it), no severity (everything's fatal), no related-location pointers
(so "duplicate of X (originally declared at Y:Z)" isn't possible —
the only position is the one stored on the offending node, not the
one on the original decl), no batched accumulation (`Main.run` returns
on the first failure).

The `phase-1-outputs.txt` log + `notes/AUDIT-OPEN-QUESTIONS.md` shows
the kinds of issues a future stricter verifier will surface, and
single-shot strings don't scale to that volume.

### D6. No first-class function/value distinction in the symbol table

`FunctionImplementationData.verify` declares the function name with the
return type as info (`FunctionImplementationData.kt:35`). The symbol
table can't tell `add: "int"` (a variable) from `add: "int"` (a
function returning int). Any call resolution, signature checking, or
"is this thing callable" pass needs a kind discriminator.

### D7. Position information lives on statements but not on expressions

`TranslatableStatement` extends `Translatable` and adds `SourcePosition`
(`TranslatableStatement.kt:5-17`). But `ExpressionData`, `FunctionCallData`,
`LambdaFunctionData`, `BundleLiteralData`, `ArrayLiteralData`,
`ArrayIndexData` are *not* `TranslatableStatement`s — they're plain
classes. So a future "expression at $line:$col had type X, expected Y"
diagnostic cannot reference the exact subexpression. Today's "in $file at $line:$col"
suffix in `Main.run` is always the statement's start position.

### D8. Grammar's `type` rule is ambiguous about what's a type and what's an identifier

The `type` rule is `QUESTION_MARK? ID (L_BRACKET R_BRACKET)?`. So any ID
is a syntactically valid type. The verifier rejects unknown ones via
`PrimitiveTypes.isPrimitiveOrArray(type)`. That means: introducing
user-defined types requires no grammar change — but it also means
*every typo* in a type position currently gets the same generic
"not a recognized primitive" error.

Method calls on type literals (`int[].create(26)`) — README roadmap item
— can't be expressed because the `expression` rule's leaf alternatives
don't include `type` (only `ID`).

### D9. No separation between the source language and the four target dialects

Three artifacts conflate the source language and what each backend
emits:

- The "legacy" backend emits a *different output language* than the
  others (strings keep backticks, for-in is C++ `auto`, types are
  preserved). Its goldens don't pass `node --check` or any tool.
- The verifier's "primitive types are int/dec/bool/char" rule is the
  *source* language's rule, but it's enforced on a per-module basis as
  part of translation. Adding a backend that uses a different primitive
  set would mean re-coding the verifier per backend.
- The C backend's `cType(wfType)` (`CBackend.kt:71-83`) is the only
  module that maps source types to target types. If/when the source
  language grows a string type, every backend will need its own map.

### D10. Single-pass, single-module driver

`Main.run` loops over the `--files` argv list, parses each, emits each.
Cross-module visibility is not modeled — `Mod::fn(x)` works at codegen
only because the target language's name resolution does the job. C2 (per-module
headers) is explicitly the example of a feature that needs a multi-module pass.

---

## Section 6 — Surprises and worth-knowing facts

1. **The "legacy" backend is the default.** Running `./waterfall foo.wf`
   with no `--target` flag produces the C-like dump, not JS. New users
   following the README's first `Quick start` line see legacy output.
   `Backends.DEFAULT_TARGET = "legacy"` (`target/Backends.kt:9`).

2. **`for (auto x : c)` in legacy is C++, not C.** The legacy backend
   `emitForBlock` emits the C++11 range-for syntax
   (`LegacyTextBackend.kt:74`). The whole "legacy" output is technically
   C++-flavored, not C.

3. **Empty array literals are syntactically rejected.** The grammar at
   `WaterfallParser.g4:135-137` is `L_BRACKET positionalArgumentList R_BRACKET`,
   and `positionalArgumentList` (line 167) requires at least one
   expression. So `int[] empty = []` *cannot be written* today. Same for
   empty bundles. The C backend's "empty array default to int" branch
   (`CBackend.kt:220-223`) is therefore dead code at present.

4. **No comments in source.** The grammar has no comment token. Anything
   that looks like `//` or `/* */` in a `.wf` source file would be a
   lex error or tokenized as operators (`//` would lex as
   `DIVIDE DIVIDE`).

5. **The `?` nullable type prefix is parsed but rejected.** A declaration
   like `?int x = NULL` would be parsed by the grammar (because the
   `type` rule allows `QUESTION_MARK? ID`), then rejected by the
   verifier (because `PrimitiveTypes.isPrimitiveOrArray("?int")` is
   false — it doesn't strip the `?`). So nullable is reserved as syntax
   but unusable.

6. **`STRING_LITERAL` is inferred to `char`.** `s := \`hello\`` infers
   `s` as type `char`, not `string` or `char[]`
   (`UntypedVariableDeclarationAndAssignmentData.kt:40`). The comment
   admits this is "best-guess so the C backend can emit `char *`-ish
   things." But because the C backend's `cType("char")` is `char` (not
   `char *`), an untyped string assignment in C produces a *truncated*
   `char` variable holding the first byte of the pointer — almost
   certainly broken for anything non-trivial. No example exercises this
   in untyped form (all strings in examples are explicitly typed as
   `char[]`).

7. **The `Pair<K, V>` class is custom-built and pervasive.** It predates
   Kotlin's `kotlin.Pair`. `@JvmField val firstVal: K, @JvmField val secondVal: V`.
   `VerificationResult` extends it. `FunctionImplementationData.typedArguments`
   is a `List<Pair<String, String>>` (type, name). The Kotlin migration
   left it intact for source-compat with the prior Java callers.

8. **The C runtime check suppresses three classes of warnings** because
   the C backend's emitted code is genuinely incorrect for `Mod::fn`,
   `obj.method(x)`, and missing return statements:
   `-Wno-implicit-function-declaration` (every `Mod_fn` and `obj_fn` is
   undeclared), `-Wno-int-conversion` (array-literal type mismatches),
   `-Wno-return-type` (functions returning a value without `return` —
   present in some examples). Without the suppressions, the C goldens
   wouldn't pass.

9. **Function "self-declaration" stores the *return type* as the symbol info.**
   At `FunctionImplementationData.kt:35`, the function name is declared
   into the outer scope with `returnType ?: "void"` as its info value.
   So `func add(...) returns int` puts `("add", "int")` in the symbol
   table. A variable `int add = 0` puts `("add", "int")` too. They're
   indistinguishable by lookup. The duplicate-decl check works because
   `declare` doesn't read the info — only the existence — but anything
   that wants signature info later will need to fix this.

10. **`Main.run`'s verification-then-translation order is per-module, not
    global.** If you pass `./waterfall a.wf b.wf c.wf`, `a` is fully
    verified+translated before `b` starts. So `b` can't see anything `a`
    declared (no global symbol table). This is consistent with the
    `Mod::fn` story: cross-module references are not resolved at compile
    time, only at emit time (target language's job).

11. **`./gradlew build` already runs the full test suite.** The
    parameterized golden tests cover every example × every target.
    `UPDATE_GOLDEN=1 ./gradlew test --tests GoldenTests` regenerates
    them. `EndToEndSmokeTest` is a sanity net for the default backend.
    Tool-availability tests (`JsRuntimeCheckTest`, `PythonRuntimeCheckTest`,
    `CRuntimeCheckTest`) check that the emitted code is syntactically
    accepted by the corresponding host toolchain.

12. **There's a `DummyTest.kt` at `compiler/src/test/kotlin/DummyTest.kt`**
    (not in any package) that asserts `true == true`. Dead but harmless.

13. **`compiler/src/test/resources/golden/legacy/DuplicateDeclarationsModule.expected`
    is a single newline.** All three Duplicate*Module goldens are
    effectively empty across all four targets — they pin "compiler emits
    nothing on this input then fails."

14. **The `waterfall` shell script is two lines.** `#!/bin/bash` and
    `exec java -jar compiler/build/libs/compiler-0.0.1.jar "$@"`. The
    quoted `$@` was added in phase 1 (per `PHASE-1-decisions.md` row 12)
    to fix shell-arg handling. The script has no `JAVA_HOME` check or
    JVM-options story.

15. **The `Container.java` mentioned in `PHASE-2-decisions.md` row 10
    is gone**, replaced by `LegacyTextBackend.emitProgram` (same row).
    If you grep the codebase you'll find one reference to `Container` in
    the comment at `LegacyTextBackend.kt:33-37`.

16. **The build targets JVM 1.8 bytecode despite using Kotlin 2.0.21 and
    Java 17/21 to compile.** `build.gradle:16-22` pins `sourceCompatibility = VERSION_1_8`
    and `jvmTarget = JVM_1_8`. This makes the compiler jar runnable on
    older JVMs, with the downside of not using `Files.readString`
    (Java 11+) and similar conveniences. `FileUtils.readFile` uses
    `Scanner` instead.

17. **No `.gitignore` review needed**: there are no checked-in build
    artifacts in the working tree (verified via `git status`: clean).

18. **`build.gradle:33` pins `junit:junit:4.12`**, a JUnit 4 build from
    2014. The tests work fine but JUnit 4 is the floor here, not 5.
