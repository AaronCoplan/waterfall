# Waterfall — Language Design (Task #3)

Author: language-designer
Inputs:
- `notes/team-output/01-codebase-audit.md` (capabilities, extension points, debt)
- `notes/team-output/02-pl-landscape.md` (prior art, novelty of `readonly` form B)
- `README.md`, `parser/src/main/antlr/WaterfallParser.g4`, `parser/src/main/antlr/WaterfallLexer.g4`
- Source under `compiler/src/main/kotlin/com/aaroncoplan/waterfall/`

This document is the design-side answer to *what is Waterfall missing as a language, and exactly how does the proposed `readonly` feature work?* Each section can be read independently. File:line references throughout cite the auditor's map.

---

## Section 0 — TL;DR

The headline design calls are:

- **Tier 1 (legitimacy bar)**: records, sum types + exhaustive `match`, modules with explicit `import` and visibility, `Result`-style error handling with a `?` propagator, a real `string` type, and a Gleam-style `@external` for FFI / target-conditional code. These are the prerequisites for being a serious language in 2026 (per Task #2 synthesis).
- **Tier 1 type system**: introduce monomorphized parametric generics (`func map<T, U>(...)`). Reify nothing at runtime. Type erasure is rejected because it produces inscrutable C output. Reified types are rejected because Waterfall doesn't have a real runtime.
- **Headline feature — `readonly`**: `readonly` is the single keyword for "this binding cannot be reassigned." It works as a declaration modifier (Form A: `readonly int x = 4`) and as a mid-scope statement (Form B: `readonly x` on its own line, freezing a previously-mutable binding). The legacy keywords `const` and `imm` are replaced — see §2g for the (small, mechanical) migration. Form B is best understood as **a one-line shorthand for Rust's shadowing-and-rebinding idiom, applied without a fresh binding** — Rust users write `let x = x;` for the same effect. Waterfall makes the idiom syntactic. Form B is verifier-only — emitted code in every backend is identical to the un-frozen version.
- **Branch joins**: a `readonly` promotion in *all* paths leading to a join point promotes the binding past the join (intersection rule). Otherwise the binding stays mutable. Justified at length in 2a.
- **Loops**: `readonly` inside a loop body errors at the second iteration's re-entry only if a write occurs first. Practical phrasing: `readonly` inside a loop body is interpreted as "no writes after this point in this iteration, and no writes in any later iteration." Branch-style intersection logic also applies to loop-back edges. See 2a-loops.
- **Aliasing**: `readonly` is a binding-level concept, not an object-level one. Promoting `a` does not freeze `b` when both point to the same record. We will not adopt deep / transitive freeze in v1. Pony-style reference capabilities are explicitly out of scope.
- **Cross-target divergence**: adopt Gleam's `@external` model. `@external(js, "Math", "sqrt")` etc. Per-target bodies live in one source. The auditor's `Mod_fn` mangling (CBackend.kt:191) and untyped FFI become formalized.

The rest of this document defends these calls.

---

## Section 1 — Feature catalog

### Conventions for this section

For each entry:
- **Why it matters / unlocks**: one paragraph.
- **Syntax**: a `.wf` sketch in current style.
- **AST sketch**: new `*Data` classes / fields.
- **Lowering**: one short paragraph per target (JS / Python / C / legacy). Tensions flagged.
- **Priority**: T1 = legitimacy gate, T2 = strong nice-to-have, T3 = post-legitimacy.

References to the codebase use the auditor's file:line cites.

---

### 1.1 Records / structs (T1 — load-bearing)

**Why it matters.** Waterfall has primitives (`int`, `dec`, `bool`, `char`) and primitive arrays. There is no way to bundle named fields. Every nontrivial program needs records. The audit's grammar gap (line 82) says: "No `struct`, `class`, `record`, `type`, `enum`, or `interface` keyword." Without records, Waterfall cannot express any domain model and cannot become a credible language.

This entry is deliberately one of the deepest in this catalog — records are the *substrate* on which sum types, pattern matching, and generics will be built. Land it badly and everything else gets harder.

**Syntax.**

```
record Point {
    dec x
    dec y
}

record Person {
    char[] name
    int age
    bool admin
}

// Construction — uses the existing `Mod::fn` mechanic shape, intentionally,
// so the front-end's existing module-qualified call resolution is the model:
Point p = Point::new(x = 3.0, y = 4.0)

// Or, when only positional is needed:
Point p = Point::new(3.0, 4.0)

// Field access uses the existing dot-path syntax from objectFunctionCall.
dec dx = p.x
```

The constructor function `Point::new` is the *only* way to materialize a record; literal `{x: 1, y: 2}` syntax is rejected because the existing grammar already has a `bundleLiteral` that competes for `|...|` and we do not want a third literal form on each backend that would need its own representation decision (audit U1 is still open).

**AST sketch.**

New top-level declaration alternative in `WaterfallParser.g4` (alongside `functionImplementation`):

```antlr
topLevelDeclaration
    : typedVariableDeclarationAndAssignment
    | functionImplementation
    | recordDeclaration         // NEW
    ;

recordDeclaration
    : RECORD name=ID NEWLINE? L_CURLY NEWLINE+ recordField+ R_CURLY NEWLINE+
    ;

recordField
    : type name=ID NEWLINE+
    ;
```

New `RECORD: 'record';` in the lexer (must be before `ID`, per the audit's lexer-ordering note at WaterfallLexer.g4:21).

New `RecordDeclarationData.kt` in `compiler/.../statements/`. Stores the record name and a `List<Pair<String, String>>` of (type, fieldName), mirroring `FunctionImplementationData.typedArguments` so the existing per-arg-walk style is preserved. Verification: each field type is a `PrimitiveTypes.isPrimitiveOrArray` *or* the name of another already-declared record in scope (which means PrimitiveTypes grows a registry — see 1.7).

`ModuleAst` grows a third list `records: List<RecordDeclarationData>` next to `topLevelVariables` and `functions` (audit notes the existing pair-of-lists structure at ModuleAst.kt:14-15 and that source-order isn't preserved — accept this for now, defer source-ordering to a separate Tier 3 task).

Constructor `RecordName::new(...)` is *not* a separate AST kind. It's a `moduleFunctionCall` whose moduleName is a record name. The verifier learns that constructors exist by walking `module.records` first and registering each `RecordName::new(...)` signature into the module-level symbol table. This avoids growing the call grammar.

Field access `p.x` is *not* a function call; we need a new expression alternative `fieldAccess`:

```antlr
fieldAccess
    : receiver=ID DOT (name=ID DOT)* finalField=ID
    ;

expression
    : ... | fieldAccess | ...
    ;
```

(The audit at FunctionCallData.kt:49 already documents the disambiguation pattern for objectFunctionCall — fieldAccess is the same pattern minus the parentheses. Reuse it.)

**Lowering — JS.** Records are plain object literals.

```js
const Point = {
    new: (x, y) => ({x: x, y: y})
};
const p = Point.new(3.0, 4.0);
const dx = p.x;
```

Module-level `Mod::fn(x)` already routes to `Mod.fn(x)` in JS (JavaScriptBackend.kt at the `MODULE` branch of emitFunctionCall). Adding constructors only means emitting a `Mod = { new: (...) => ({...}) }` factory at module-top.

**Lowering — Python.** `@dataclass`:

```python
from dataclasses import dataclass
@dataclass
class Point:
    x: float
    y: float
    @staticmethod
    def new(x, y):
        return Point(x, y)
```

Or, if we want to avoid a dataclass dependency, plain classes with `__init__`. Pick `@dataclass` — Python 3.7+ ships it, the audit's runtime check is `python3` and any installable Python ≥ 3.7 has dataclasses (`python3 ast.parse` doesn't run the code so import availability is irrelevant for the syntax check). PythonBackend grows a `usesDataclass: Boolean` flag in the same shape as the existing `usesFinal` at PythonBackend.kt:31.

**Lowering — C.** `struct` plus a constructor function:

```c
typedef struct Point {
    double x;
    double y;
} Point;

Point Point_new(double x, double y) {
    return (Point){x, y};
}
```

C's `obj.field` Just Works for direct field access. The `Mod::fn` mangling at CBackend.kt:191 is reused as `RecordName_new(...)`. The receiver-as-first-arg hack at CBackend.kt:194 doesn't apply to records — they have no methods (yet; see 1.13).

**Lowering — legacy.** Pass through the keyword. `record Point { dec x dec y }` and `Point::new(3.0, 4.0)` and `p.x`. The legacy backend's purpose is regression anchoring (audit's surprises #2 and #4); it does not need to compile.

**Tensions.**

- **No methods on records in v1.** Methods become a Tier 2 item after we choose between vtables / function pointers / receiver-as-first-arg uniformly (this is the open audit issue C3 mentioned in the README). Records-without-methods are still useful and let the type system land.
- **No record literal syntax.** Forcing `Record::new(...)` keeps the grammar simple. Subsequent grammar work can add `Record { x: 1, y: 2 }` literal syntax later without breaking constructor calls.
- **Records cannot reference themselves yet.** Recursive records (e.g., a singly-linked list `List { int head; List tail }`) require nullable types to terminate. Nullables are still gated by the `?T` syntax currently parsed-but-rejected (audit's surprise #5). This is fine — recursive records can wait for nullables to land.

**Priority.** T1 — no records, no language.

---

### 1.2 Sum types / tagged unions / enums (T1)

**Why it matters.** Per Task #2, sum types with exhaustive pattern matching are table stakes for any language claiming "modern" status in 2026. Rust, Swift, Kotlin (sealed), Gleam, Crystal, Zig, Roc, Idris all have them. They are the construct that pulls the most other features into being viable: `Result<T, E>` for error handling, `Option<T>` for nullability, `enum`-with-payload for state machines.

**Syntax.**

```
union Shape {
    Circle(dec radius)
    Rectangle(dec width, dec height)
    Triangle(dec a, dec b, dec c)
}

Shape s = Shape::Circle(5.0)
```

Variant constructors live in the union's namespace, matching the existing `Mod::fn(x)` shape (so the front-end's existing module-qualified call routing carries them with no grammar change to call sites).

Single-variant enums (C-style) are a special case where each variant has zero payload:

```
union Color {
    Red()
    Green()
    Blue()
}
```

We do *not* introduce a separate `enum` keyword. One construct (`union`) handles both. Justification: Kotlin chose `sealed class` + `class`; Rust chose `enum`. Both work. Picking one keyword (`union`) avoids two near-identical constructs in the grammar.

**AST sketch.**

```antlr
topLevelDeclaration
    : typedVariableDeclarationAndAssignment
    | functionImplementation
    | recordDeclaration
    | unionDeclaration                    // NEW
    ;

unionDeclaration
    : UNION name=ID NEWLINE? L_CURLY NEWLINE+ unionVariant+ R_CURLY NEWLINE+
    ;

unionVariant
    : name=ID L_PARENS typedArgumentList? R_PARENS NEWLINE+
    ;
```

New `UNION: 'union';` token in the lexer. `UnionDeclarationData` AST class with `name: String`, `variants: List<Variant>` where each variant is `(name: String, fields: List<Pair<String, String>>)`.

The verifier registers each variant constructor `Shape::Circle` into the module symbol table just like record constructors. Constructor return type is the union name.

**Pattern matching** is its own grammar entry — see 1.3 next. Pattern matching is what makes unions useful.

**Lowering — JS.** Tagged objects. Each variant gets a discriminator `_tag`:

```js
const Shape = {
    Circle: (radius) => ({_tag: "Circle", radius: radius}),
    Rectangle: (width, height) => ({_tag: "Rectangle", width: width, height: height}),
    Triangle: (a, b, c) => ({_tag: "Triangle", a: a, b: b, c: c}),
};
```

The `_tag` field is the discriminator that pattern matching reads.

**Lowering — Python.** Dataclasses with a discriminator field — or, equivalently, a `class` per variant inheriting from a common base. Pick a discriminator dataclass for simplicity and homogeneity with JS:

```python
@dataclass
class Shape:
    _tag: str
    # variant-specific fields stored as attributes; safe because Python is dynamic.
```

Better still: emit one dataclass per variant under a wrapper class, à la `Shape.Circle`. This is more Pythonic and matches `match` statement syntax (PEP 634) for the v1 lowering of pattern matching. Pick the *one dataclass per variant* approach.

**Lowering — C.** Tagged union, the classic C idiom:

```c
typedef enum ShapeTag { SHAPE_CIRCLE, SHAPE_RECTANGLE, SHAPE_TRIANGLE } ShapeTag;
typedef struct Shape {
    ShapeTag _tag;
    union {
        struct { double radius; } Circle;
        struct { double width; double height; } Rectangle;
        struct { double a; double b; double c; } Triangle;
    } _payload;
} Shape;

Shape Shape_Circle(double radius) {
    return (Shape){SHAPE_CIRCLE, ._payload.Circle = {radius}};
}
```

The audit's `cType` (CBackend.kt:71) needs to learn about union types — but since unions are declared (registered in a known table), this is mechanical.

**Lowering — legacy.** Pass through.

**Tensions.**

- **Variant constructor naming collides with `Mod::fn`.** A constructor `Shape::Circle(5.0)` looks identical at a call site to a module-qualified function call. This is fine because we'll arrange for the verifier to register both kinds of names into a per-module table and let the call resolution pick the right one. The audit's surprise #9 already notes that the symbol table can't distinguish a function from a variable — this is the *same* problem at a slightly different scale, and the fix (richer SymbolInfo, see Section 2c) is shared.
- **Empty variants need parentheses.** `Color::Red()` not `Color::Red`. This is for grammar consistency — every variant is a constructor function shape.
- **No payload-less unions emit warnings in C.** A union with all-empty variants degenerates to a C enum; we'll detect this in the verifier and emit a `typedef enum` instead of the tagged-struct shape.

**Priority.** T1. Cannot ship pattern matching without unions.

---

### 1.3 Pattern matching (T1)

**Why it matters.** Without `match`, sum types aren't useful — you have to read the discriminator manually with `if/elif`, which is boilerplate. Per Task #2 (Part 1, Section 1, observation 1): exhaustive matching is the 2026 baseline.

**Syntax.**

```
func describe(Shape s) returns char[] {
    match(s) {
        Circle(radius) => `circle r=${radius}`     // string interp is itself T1, see 1.7
        Rectangle(width, height) => `rect`
        Triangle(a, b, c) => `tri`
    }
}
```

`match` is an expression (it produces a value of the same type from each arm). Exhaustiveness is required — the verifier rejects a match that omits variants. A wildcard `_` is allowed for `_ => default`.

**AST sketch.**

```antlr
matchExpression
    : MATCH L_PARENS expression R_PARENS L_CURLY NEWLINE+ matchArm+ R_CURLY
    ;

matchArm
    : pattern LAMBDA expression NEWLINE+
    ;

pattern
    : ID L_PARENS patternBinder (COMMA patternBinder)* R_PARENS      // variant pattern
    | UNDERSCORE                                                       // wildcard
    | INT_LITERAL | BOOL_LITERAL | DEC_LITERAL                         // literal patterns (later)
    ;

patternBinder
    : ID                                                              // bind variant field to name
    ;
```

New lexer tokens: `MATCH: 'match';`, `UNDERSCORE: '_';`. `_` as a token requires care — the identifier rule at WaterfallLexer.g4:35 begins with `[a-zA-Z]` so a bare `_` is *not* a valid identifier today, which means adding `UNDERSCORE: '_';` is safe and won't shadow ID.

`MatchExpressionData` lives next to `IfBlockData`. Each arm holds (pattern, body). For v1, only variant patterns and the wildcard are supported. Nested patterns, guard clauses, and `or` patterns are explicitly Tier 3 — they bloat the verifier and aren't load-bearing for legitimacy.

The verifier:
- Walks the input expression's static type. If not a union, reject.
- For each arm, register the variant's fields as new bindings into a child symbol table (binding name -> field type).
- Check arms cover every variant of the union (compute the set of variant names from `module.unions` at verification time, subtract the arm pattern names, error if non-empty and no wildcard arm).
- Each arm's body must evaluate to the same type (or at least, today's no-op return-type check means we just register but don't compare — T1 type inference fills in later).

**Lowering — JS.** A chain of `if (x._tag === "Circle") { ... }`:

```js
function describe(s) {
    if (s._tag === "Circle") {
        const radius = s.radius;
        return `circle r=${radius}`;
    } else if (s._tag === "Rectangle") {
        // ...
    } else if (s._tag === "Triangle") {
        // ...
    } else {
        throw new Error("unreachable");  // exhaustiveness fallback
    }
}
```

The trailing `throw` is for safety (the verifier guarantees coverage, so this branch is unreachable; it's a runtime sanity check).

**Lowering — Python.** Python 3.10+ has `match`/`case` (PEP 634). The runtime check is `python3 -c "import ast; ast.parse(...)"` (audit row "--target python" in the README), which accepts `match` syntax on a sufficiently new Python parser. But: the audit's golden test machinery runs whatever python3 ships in the test environment. We must check that the project's test environment is on Python ≥ 3.10. **Action for the implementer**: gate `match` lowering on the Python version reported by the runtime check; for Python < 3.10, fall back to a chain of `isinstance(s, Shape.Circle)` checks. Either lowering is mechanical given the dataclass-per-variant representation chosen in 1.2.

```python
def describe(s):
    match s:
        case Shape.Circle(radius=radius):
            return f"circle r={radius}"
        case Shape.Rectangle(width=width, height=height):
            return "rect"
        case Shape.Triangle(a=a, b=b, c=c):
            return "tri"
```

**Lowering — C.** A `switch` on `_tag`:

```c
char *describe(Shape s) {
    switch (s._tag) {
        case SHAPE_CIRCLE: {
            double radius = s._payload.Circle.radius;
            return /* sprintf or fixed string */;
        }
        case SHAPE_RECTANGLE: { /* ... */ }
        case SHAPE_TRIANGLE: { /* ... */ }
        default: abort();
    }
}
```

`abort()` is the C analog of "unreachable." Header `<stdlib.h>` requested.

**Lowering — legacy.** Pass through.

**Tensions.**

- **`match` as expression vs statement.** Picking *expression* matches Rust/Swift/Kotlin. JS/Python/C all need a temporary variable to hold the arm's result when `match` is used in expression position (e.g., `int x = match(s) { ... }`). The lowering for that introduces a synthesized helper function or an IIFE in JS / inline lambda in Python / `({ ... })` GCC statement-expression in C (the audit notes C99 is the target — statement-expressions are a GCC extension, so for C we'd lower expression-position `match` via a temporary and prefixed if-chain instead). This is fiddly but mechanical.
- **Exhaustiveness diagnostics need named-variant info.** This is one of the first features that forces the symbol table to grow a kind discriminator (`Symbol.Kind.UNION`, `Symbol.Kind.VARIANT`, etc.) — but the same redesign serves records, generics, modules, and `readonly`. Bundle the work.

**Priority.** T1. Sum types without `match` are unusable.

---

### 1.4 Generics (T1 — load-bearing)

**Why it matters.** Without parametric polymorphism, every collection-shaped utility (`map`, `filter`, `reduce`, `Option`, `Result`, `List<T>`) is either written per element type or wrapped in unsafe casts. The audit at line 81 confirms no `<` / `>` reserved for type position. This is a load-bearing decision — choose the flavor poorly and the C backend explodes.

**Pick a flavor.**

Three options exist:
1. **Reified (Java-style at runtime, Rust-style at compile time only via dyn).** Types are values, available at runtime. Rust monomorphizes; Java erases.
2. **Monomorphized (Rust, C++ templates).** Each type instantiation is compiled separately; no runtime type info. Static dispatch.
3. **Type-erased (Java raw, Haskell-without-typeclasses).** Types are check-only; at runtime everything is `Object` / void pointer.

**Recommendation: monomorphization.** Justification, by target:

- **JS** has no type system at runtime. Erasure and reification both collapse to "do nothing." Monomorphization is wasteful (you'd emit `swap_int`, `swap_dec`, etc., all with identical bodies). For JS the *emitted* code should be a single untyped function — *but the front-end still tracks generics during verification*. So JS is type-erased at the codegen layer.
- **Python** same as JS — runtime is dynamic. Type-erase at codegen.
- **C** has no parametric types at all. Monomorphization is the only realistic option — emit `swap_int`, `swap_dec`, etc., one per usage. The C backend already mangles `Mod::fn` to `Mod_fn` at CBackend.kt:191; extending this to `swap_int` / `swap_dec` is the same shape.
- **Legacy** — pass through generic syntax verbatim.

So the answer is: **monomorphize for C; type-erase for JS, Python, legacy.** This means *the front-end maintains a list of all `(generic function, instantiation type args)` pairs encountered during verification* and the C backend emits one body per pair. The JS / Python backends emit just one body and ignore the type args.

This requires:
- The verifier walking call sites with their concrete type args.
- A per-module *instantiation set* tracked on `ModuleAst`.
- The C backend looking up the instantiation set and emitting one body per (`functionName`, `[T1, T2, ...]`).

This is real complexity. Decision: stage it. v1 generics are *parametric on functions only*. Generic records and generic unions are Tier 2.

**Syntax.**

```
func swap<T>(T a, T b) returns T {
    T temp = a
    a = b
    b = temp
    return a    // illustrative — Waterfall doesn't actually have pass-by-reference
}

func map<T, U>(T[] xs, (T) ==> U fn) returns U[] {
    // hand-wavy; the real implementation needs sized arrays which is its own story
}
```

Type parameters use angle brackets — yes, that requires the grammar to disambiguate `<` and `>` from comparison operators. ANTLR's left-recursive `expression` rule (WaterfallParser.g4:105-125) makes this hard. The standard workaround is:
- In *type* position, parse `<` as the start of a type-arg list.
- In *expression* position, parse `<` as comparison.

This is the design every modern language uses (Java, C#, Kotlin, Swift, Rust). The disambiguation lives in the grammar — we treat the `<T>` in `swap<T>(...)` as part of the function-name `IDENTIFIER<TYPE_ARGS>` lexeme via grammar rule `genericName: ID (LESS_THAN type (COMMA type)* GREATER_THAN)?`.

**Grammar delta.**

```antlr
functionImplementation
    : FUNCTION name=ID typeParams? L_PARENS typedArgumentList? R_PARENS (RETURNS returnType=type)? statementBlock NEWLINE+
    ;

typeParams
    : LESS_THAN ID (COMMA ID)* GREATER_THAN
    ;

type
    : QUESTION_MARK? ID typeArgs? (L_BRACKET R_BRACKET)?
    ;

typeArgs
    : LESS_THAN type (COMMA type)* GREATER_THAN
    ;

localFunctionCall
    : functionName=ID typeArgs? L_PARENS functionCallArguments? R_PARENS
    ;

// And similarly for moduleFunctionCall, objectFunctionCall.
```

**Tensions.**

- **Type inference is needed for ergonomic generics.** Audit G4 (cross-expression type inference) is currently unimplemented. Without inference, every generic call needs explicit type args: `swap<int>(a, b)`. This is acceptable for v1 — Java didn't have type inference for generics until Java 7. But G4 is on the critical path for generics to feel modern.
- **Generic methods on records/unions in v1?** No. Adding generic records means the C backend has to monomorphize records too — every `List<int>`, `List<dec>` is a different `struct`. We will defer generic records to Tier 2 once monomorphization is stable. (The same call applies to generic unions, which would mean `Option<T>` is Tier 2 — meaning v1 `Result` is hand-instantiated per error type, which is acceptable since v1 only needs `Result<T, char[]>` for the language to be useful.)
- **Constraints / type bounds.** Out of scope for v1. The Rust/Swift "where T: Iterator" pattern adds 30% to the type system complexity. Defer to Tier 2 or Tier 3.

**Priority.** T1 for generic *functions* only. Generic records / unions / type bounds are Tier 2.

---

### 1.5 Modules and namespaces (T1 — load-bearing)

**Why it matters.** The audit at line 134 admits the multi-file model is currently "pass several .wf paths to ./waterfall." There is no `import` keyword. `Mod::fn(x)` works only because the codegen-layer emits the same string and the *target language* resolves it. From the outside, Waterfall has no module system.

This is the single most blocking issue for the language being usable for non-toy code. Per Task #2, every credible language has a documented module/import story.

**Syntax — explicit imports.**

```
import Math
import std::collections::List as List

module FibonacciModule {
    func fib(int n) returns int {
        // ...
        return Math::sqrt(2.0) castas int
    }
}
```

`import Math` makes `Math::fn(x)` callable in this module. `import std::collections::List as List` is shorthand for the same with renaming.

**Visibility.** Add `pub` modifier on top-level declarations. Default visibility is `module` (visible only within the same module — i.e., the same `.wf` file).

```
module FibonacciModule {
    pub func fib(int n) returns int { ... }       // exported
    func helper(int x) returns int { ... }        // module-private
}
```

This solves three current problems:
- `Mod::fn(x)` from another module can be statically resolved at compile time, not punted to the target runtime.
- The C backend can emit per-module headers (the audit's C2 issue — README roadmap acknowledges).
- Programs can be organized into multiple files cleanly.

**Module hierarchy / packages.** Allow nested paths: `import std::collections::List`. The path elements are folder names, and the final name is the file name (without `.wf`). So `std/collections/List.wf` corresponds to `std::collections::List`.

This means the compiler driver needs to:
1. Accept a *root directory* or *source set* on the CLI, not just a flat list of `.wf` files (audit Main.kt:75-83).
2. Walk imports transitively, building a module dependency graph.
3. Verify and emit modules in topological order.

**AST sketch.**

```antlr
program
    : importStatement* module EOF
    ;

importStatement
    : IMPORT modulePath (AS aliasName=ID)? NEWLINE+
    ;

modulePath
    : ID (DOUBLE_COLON ID)*
    ;

topLevelDeclaration
    : PUB? typedVariableDeclarationAndAssignment
    | PUB? functionImplementation
    | PUB? recordDeclaration
    | PUB? unionDeclaration
    ;
```

New lexer tokens: `IMPORT: 'import';`, `AS: 'as';`, `PUB: 'pub';`.

`ImportData` AST class with `path: List<String>` and `alias: String?`. `ModuleAst` gains an `imports: List<ImportData>` field. Each top-level decl gains an `isPublic: Boolean` flag.

**Lowering — JS.**

```js
// module FibonacciModule
import * as Math from "./Math.js";
// or, with the alias form:
import { default as List } from "./std/collections/List.js";

export function fib(n) { /* ... */ }
function helper(x) { /* ... */ }
```

Use ES modules. The audit's C6 issue (JS module wrapping) is finally resolved — pick ESM. CJS users transpile via their own build pipeline.

**Lowering — Python.**

```python
# module FibonacciModule
import Math
from std.collections import List

def fib(n): ...
def _helper(x): ...    # leading underscore = module-private convention
```

Python's import maps cleanly. Module-private decls get a leading underscore (Python convention).

**Lowering — C.**

The hard target. Each module becomes a `.c` + `.h` pair:

```c
// FibonacciModule.h
#ifndef FIBONACCIMODULE_H
#define FIBONACCIMODULE_H
#include "Math.h"
int FibonacciModule_fib(int n);
#endif

// FibonacciModule.c
#include "FibonacciModule.h"
int FibonacciModule_fib(int n) { /* ... */ }
static int FibonacciModule_helper(int x) { /* ... */ }   // static = module-private
```

The audit's C2 issue is finally resolvable: only `pub` decls land in the header. Private decls are emitted with `static` linkage. `Mod::fn(x)` mangles to `Mod_fn(x)` (already done at CBackend.kt:191).

This forces the CLI to emit *multiple files* — today's CBackend emits a single TU to stdout. The audit at Main.kt:75-83 notes that the driver emits each module's code to stdout in turn; this needs to change to "emit each module's `.c` and `.h` to a target directory."

**Lowering — legacy.** Pass through `import` lines as comments. Don't enforce visibility.

**Tensions.**

- **Cyclic imports.** Forbid them in v1. The compiler reports the cycle and errors. Most module systems do this; the few that allow cycles (Go's package system, Python with care) end up with strange ordering bugs.
- **The CLI changes shape.** From `./waterfall foo.wf` to `./waterfall --source-dir ./src --out-dir ./out`. This is breaking. We can keep the old form for single-file legacy mode and add the new form as the recommended path. Justify breaking by the fact that single-file is not viable for the new module system.
- **What does the existing `Mod::fn` call resolve to when `Mod` was not imported?** A verification error. This is the first time the verifier rejects a previously-accepted program — but the program never *really* worked (audit notes #10: cross-module references aren't resolved at compile time). The "rejection" is making explicit what was always broken.

**Priority.** T1. Multi-file programs are essential.

---

### 1.6 Error handling (T1 — load-bearing)

**Why it matters.** The audit at line 157 says: "The compiler itself throws CompilerError but the language has no equivalent." Without an error handling mechanism, the language can express only happy-path code. Per Task #2, every modern language has a clear error story.

**Pick a model.** Three options:
1. **Exceptions.** Familiar (Java, Python, C#). But: C has no native exceptions (requires setjmp/longjmp tricks); JS exceptions are different in shape from Python exceptions. Cross-target lowering is *painful*.
2. **Result / Option.** Type-safe (Rust, Swift, Gleam, Haskell). Works on every target because it's just a tagged union (which we just designed in 1.2). Composes with the type system. The downside is that it requires every function to declare its error type or use a top-level `?` operator to propagate.
3. **Effect tracking.** Beautiful (Koka, Roc). Requires type-system maturity Waterfall does not have.

**Recommendation: Result.** The most direct argument is that *Waterfall already has a tagged union design (1.2)*. `Result<T, E>` is literally a built-in union:

```
union Result<T, E> {
    Ok(T value)
    Err(E error)
}
```

(This is a *generic* union — see the dependency on 1.4. Generic unions are Tier 2 per 1.4. So `Result<T, E>` is special-cased in v1: a built-in generic union the compiler treats specially. Once Tier 2 generic unions land, the special case dissolves.)

A function that may fail declares its error type:

```
func parseInt(char[] s) returns Result<int, char[]> {
    if(badInput(s)) {
        return Result::Err(`bad input`)
    }
    return Result::Ok(42)
}
```

**The `?` propagator.** Borrowed from Rust:

```
func consume(char[] s) returns Result<int, char[]> {
    int n = parseInt(s)?      // unwrap or propagate Err
    return Result::Ok(n + 1)
}
```

The `?` operator on a `Result<T, E>` either yields the `Ok` payload (if `Ok`) or returns the `Err` from the enclosing function (if `Err`). The enclosing function must itself return `Result<_, E>` for the propagation to type-check.

**AST sketch.**

The `?` is a new postfix operator on expressions:

```antlr
expression
    : ... | expression QUESTION_MARK
    ;
```

But `QUESTION_MARK` is already used as the prefix on the nullable type (currently parsed-but-rejected, audit's surprise #5). This is fine — the grammar disambiguates by position (prefix on `type`, postfix on `expression`). ANTLR's longest-match will handle it.

`TryPropagationData` AST class with the operand expression.

**Lowering — JS.**

```js
// res = parseInt(s);
// if (res._tag === "Err") return res;
// const n = res.value;
const _r = parseInt(s);
if (_r._tag === "Err") return _r;
const n = _r.value;
```

The `?` desugars into a synthesized `let _r = ...; if (_r._tag === "Err") return _r;` before the use site. Mechanical.

**Lowering — Python.** Same shape:

```python
_r = parseInt(s)
if isinstance(_r, Result.Err):
    return _r
n = _r.value
```

**Lowering — C.** Same:

```c
Result_int_charP _r = parseInt(s);
if (_r._tag == RESULT_ERR) return _r;
int n = _r._payload.Ok.value;
```

The monomorphized name `Result_int_charP` reflects the type args. (Generic monomorphization, see 1.4.)

**Lowering — legacy.** Pass through.

**Tensions.**

- **No exception ergonomics.** A developer who is used to `try/catch` will find `Result` annoying for prototyping. Counterargument: Gleam, Rust, Elm, PureScript all decline exceptions and survive. The Task #2 synthesis explicitly notes "ergonomic error handling without exceptions (Rust's `?`, Gleam's `Result`)" as a current frontier.
- **`Result<_, _>` requires generic unions, which we're deferring.** Compromise: `Result` is a built-in special-cased generic union for v1. The compiler hardcodes its representation. When generic unions land (Tier 2), `Result` becomes a regular library type and the special case dissolves.
- **No `panic` / `abort` / unrecoverable errors.** v1 does not have a `panic` keyword. For unreachable arms (e.g., the trailing `throw` in 1.3's JS lowering), we generate a target-specific abort that the user can't customize. Tier 2 can add `panic` later.

**Priority.** T1. The language is unusable without error handling.

---

### 1.7 Strings as a real type (T1)

**Why it matters.** Audit row "Char literal" (line 119): "No grammar rule. `char` type stores via STRING_LITERAL inference (untyped `:=` of a string infers `char`)." Audit surprise #6: "`STRING_LITERAL` is inferred to `char`," meaning `s := \`hello\`` infers `s` as type `char`, which in C produces a *truncated `char`*. This is broken.

**Design.**

Introduce `string` as a primitive type, alongside `int`, `dec`, `bool`, `char`. A `string` is a length + bytes; `char` becomes a single Unicode codepoint. The audit's existing `char` type is *renamed* to `string` and a new `char` type is introduced for codepoints. (Reverse migration: `char` keeps the legacy meaning and `string` is new — that's also fine. Pick one. Recommendation: **rename**, because every existing example uses `char[]` for what is morally `string`. Renaming `char[]` to `string` is the cleanest break.)

Concretely:

- Add `string` to `PrimitiveTypes.ALL` (typesystem/PrimitiveTypes.kt:19).
- The `STRING_LITERAL` token now infers to `string`, not `char`.
- `char` represents a single Unicode codepoint (lower to `int` in C with helper, `str[0]` in Python, `string` of length 1 in JS).
- Char literals `'a'` get a new grammar production. The single-quote token is currently unused — confirmed by reading WaterfallLexer.g4.

**AST and lowering.**

`PrimitiveTypes.STRING` added. `cType("string") = "char *"` in CBackend. JS/Python keep using the existing string-literal lowering (`StringLiteralText.escapeFor`).

**Tensions.**

- **String concatenation.** Today `+` between strings parses but no backend resolves it (audit row "String concatenation"). v1 says: `+` on `string` types is concatenation. JS uses `+`, Python uses `+`, C uses a runtime helper `Waterfall_str_concat(a, b)`. C needs a tiny runtime — see 1.13.
- **String interpolation `\`hello ${name}\``.** Tier 2 — used in this design's examples but not strictly necessary for v1. Tier 2 adds template-literal syntax. v1 says use concatenation.
- **Unicode handling.** v1 is byte-level for `string` in C (`char *` with no codepoint awareness); JS/Python use their native UTF-16/UTF-8. This is a known compromise and matches Haxe's approach (per Task #2 sources).

**Priority.** T1. The language is embarrassing without a real string type.

---

### 1.8 Collections (T2)

**Why it matters.** Today only arrays exist (`int[]`, `char[]`, etc.). Maps, sets, lists with append/prepend — none exist. Most real programs use maps and sets.

**Design.**

`List<T>`, `Map<K, V>`, `Set<T>` as built-in generic types. They require generics (1.4) to express, and they're best implemented as Tier 2 once generic records/unions land.

For v1, the existing primitive arrays remain. Tier 2 adds:
- `List<T>` — append/prepend, length, indexing. JS → `[]` with methods. Python → `list`. C → a `struct { T* data; int len; int cap; }` with helper functions.
- `Map<K, V>` — JS `Map`, Python `dict`, C → hash table from a tiny runtime.
- `Set<T>` — JS `Set`, Python `set`, C → hash table.

**Tensions.**

- **C runtime size.** A real `List/Map/Set` in C requires malloc, free, memcpy, hash functions. This is a real runtime, not just a header file. v1 ships none; Tier 2 adds the runtime.
- **Mutation semantics across targets.** JS `[].push(x)` mutates; the Python equivalent mutates; the C helper also mutates. So `List<T>` is conventionally a mutable type. This composes with `readonly` (see Section 2) — `readonly List<int> xs` means the binding is frozen, but the list it points to may still be mutated by aliases. This is consistent with the binding-vs-object distinction in Section 2.

**Priority.** T2.

---

### 1.9 FFI / target-conditional code (T1 — load-bearing per Task #2 synthesis)

**Why it matters.** Task #2 explicitly calls out that every credible multi-target language has a target-divergence story (Haxe `#if`, Gleam `@external`, Roc platforms, Nim pragmas) and "the strategy varies, but it must exist."

**Recommendation: Gleam-style `@external`.** Justified at length in Section 4. Summary here:

```
@external(js, `Math.sqrt`)
@external(python, `math.sqrt`)
@external(c, `sqrt`)
func sqrt(dec n) returns dec

// Or, with a fallback Waterfall body for unannotated targets:
@external(js, `Math.sqrt`)
@external(python, `math.sqrt`)
@external(c, `sqrt`)
func sqrt(dec n) returns dec {
    // Used by the legacy backend, which has no @external mapping.
    return n
}
```

The function has *no Waterfall body* when an `@external` exists for the current target; the call site lowers to the target identifier directly. When no `@external` exists for a target, the fallback body is used.

**Why this design over Haxe's `#if`.** Haxe's `#if js ... #else ... #endif` puts conditional code *inside* function bodies, fragmenting the source. Gleam's per-function `@external` keeps each function's behavior on each target visible at the function declaration site. Per Task #2, this is "more sophisticated than Haxe's macro-style `#if`."

**AST sketch.**

```antlr
externalAnnotation
    : AT EXTERNAL L_PARENS targetName=ID COMMA targetSymbol=STRING_LITERAL R_PARENS NEWLINE+
    ;

functionImplementation
    : externalAnnotation* FUNCTION name=ID ...
    ;
```

New lexer tokens: `AT: '@';`, `EXTERNAL: 'external';`.

`FunctionImplementationData` gains an `externals: Map<String, String>` field (target name -> target symbol).

**Lowering.** At each backend's `emitFunctionImpl`, check if an `@external(<this target>)` exists. If so, emit nothing (the function's body is the target's existing identifier) and rewrite all call sites to use the target symbol directly. Concretely, the JS backend turns `sqrt(2.0)` at the call site into `Math.sqrt(2.0)`; the Python backend turns it into `math.sqrt(2.0)`; the C backend turns it into `sqrt(2.0)` and adds `#include <math.h>` to required headers. (The same C backend already requests `<math.h>` for `^` lowering at CBackend.kt:165-169 — same machinery.)

**Tensions.**

- **Multi-arg target FFI.** What if `Math.sqrt` is a *static method* on `Math` in JS but a *free function* `math.sqrt` in Python? The `@external` syntax above handles this — each target gets its own qualified path. The compiler emits literally what the annotation says.
- **Target-only types.** If `Math.sqrt` takes a JS Number and returns a Number, but in Python takes/returns float, the Waterfall signature must be "compatible enough" with both. v1 says: trust the user; type-check the Waterfall signature against `dec` (or whatever they declared) and let the target deal with mismatches at runtime. Tier 3 adds *per-target FFI signatures* à la Haxe externs.
- **What about functions that can only exist on some targets?** Like `setTimeout` (JS-only)? Today, define it with `@external(js, ...)` only. Calls on other targets fail at *verification time* with "no `@external` for this target and no Waterfall body." This matches Gleam's expression-level target tracking (Task #2 Part 2.15) — a *correctness* property.

**Priority.** T1. Without this, every target-specific helper is a compile-time hack.

---

### 1.10 Package management (T2)

**Why it matters.** Per Task #2: "Every successful modern language has a recognized package manager." Without one, Waterfall reads as research.

**Design.**

A `waterfall.toml` (or `Waterfall.toml`) manifest at the project root:

```toml
[package]
name = "myapp"
version = "0.1.0"

[dependencies]
http = "1.0"
stdlib = { git = "https://github.com/aaroncoplan/waterfall-stdlib", tag = "v0.3" }
```

A registry — initially a GitHub-org hosting `waterfall-stdlib`, `waterfall-http`, etc. — and a CLI `waterfall add http` that updates the manifest.

**File layout.**

```
my-project/
├── waterfall.toml
├── waterfall.lock
├── src/
│   ├── Main.wf
│   ├── http/
│   │   └── Client.wf       # accessible as http::Client
│   └── ...
└── target/
    ├── js/
    ├── python/
    └── c/
```

`./waterfall build` reads the manifest and emits to `target/<target>/`. Old `./waterfall foo.wf` single-file mode remains for ad-hoc usage.

**Tensions.**

- **Registry is a real artifact.** A package registry needs hosting, a search interface, and a security model (signed releases, trusted publishers). For v1, lean on Git URLs and tags — every dependency is `{git = "...", tag = "..."}`. No separate registry until adoption justifies it. Cargo did this informally for years before crates.io existed.
- **Versioning.** SemVer. Lock file pins exact resolved versions.
- **No cross-language deps.** A Waterfall package depends only on other Waterfall packages. Target-language deps (npm, PyPI, C libraries) are handled by the user at the *target* build step — Waterfall does not try to be a meta-package-manager.

**Priority.** T2. The language design can land without this; the *legitimacy story* needs it.

---

### 1.11 Tooling story — LSP, formatter, debugger (T2)

**Why it matters.** Per Task #2: every credible language has LSP / editor support; "without this, modern developers won't try the language seriously."

**Design — Language Server (LSP).**

Build a Kotlin LSP implementation as a separate gradle subproject (`lsp/`). Initial scope:
- Diagnostics (re-use existing `VerificationResult` infrastructure).
- Go-to-definition for module imports and function names.
- Hover types for identifiers (requires the symbol table to expose `lookup`, which is currently private at SymbolTable.kt:21).
- Autocomplete for variants of unions, fields of records, and imported modules.

Audit's debt D5 (single-shot string errors) is on the critical path — the LSP needs structured diagnostics (error codes, severity, related-info pointers). Resolve D5 *before* building the LSP. Specifically: replace `VerificationResult(Boolean, String?)` with a `VerificationResult(severity: Severity, code: ErrorCode, message: String, primaryLocation: SourcePosition, relatedLocations: List<RelatedInfo>)` data class.

**Formatter.**

A `waterfall fmt` subcommand. Apply consistent indentation, newline rules, modifier order. Reads the AST and pretty-prints. The audit's D4 issue (no shared pretty-printer) is partially answered here — the formatter becomes the shared pretty-printer for the source language.

**Debugger.**

Source maps for JS (the audit's gap mentioned at line 148; Gleam 1.16 added source maps for JS per Task #2). Python debugging via `pdb` works on emitted Python with no extra work if line numbers are preserved. C debugging via `gdb` works on emitted C if `#line` directives are emitted.

**Tensions.**

- **Building an LSP is a substantial undertaking.** ~2-4 person-months of focused work, by typical small-language LSP estimates.
- **Source maps are nontrivial.** Gleam took years to ship them. v1 says: emit `#line` directives in C (cheap), emit JS source maps (medium), Python preserves line numbers natively if we emit the body at the same source lines (easy).

**Priority.** T2.

---

### 1.12 Standard library (T2 — depends on FFI)

**Why it matters.** A language with no stdlib is forced to FFI every common operation. Task #2: every credible language has at minimum a prelude.

**Design.**

Minimum v1 stdlib (post-FFI landing):

- `std::math` — `sqrt`, `pow`, `abs`, `floor`, `ceil` (each `@external` to the target's math library).
- `std::io` — `print`, `println`, `readLine` (each `@external`).
- `std::result` — `Result<T, E>`, `Option<T>` (built-in, but stdlib types).
- `std::string` — `length`, `concat`, `split`, `contains` (mix of `@external` to target string methods).
- `std::list` — `List<T>` as designed in 1.8 (Tier 2 anyway).

The stdlib ships as a separate `.wf` package under `stdlib/`. Imported via `import std::math`.

**Tensions.**

- **Per-target divergence in the stdlib is the unit test for `@external`.** If `@external` works well, the stdlib falls out for free. If not, the stdlib reveals the gaps. Schedule `@external` first.
- **`println` on C needs `<stdio.h>` and a `printf` wrapper.** This is the C runtime budding — see 1.13.

**Priority.** T2.

---

### 1.13 A small C runtime (T2 — emerged from 1.7, 1.8, 1.12)

**Why it matters.** Lots of the design decisions above (strings as a real type, collections, `println`) require *something* on the C side to exist. JS/Python have built-in runtime support; C doesn't. Either Waterfall provides a tiny C runtime (a single `waterfall_runtime.c` + `.h`) or every C-target program has unresolved references.

**Design.**

A single small C library:

```c
// waterfall_runtime.h
#ifndef WATERFALL_RUNTIME_H
#define WATERFALL_RUNTIME_H

#include <stdbool.h>
#include <stdlib.h>
#include <string.h>

typedef char* Waterfall_string;

Waterfall_string Waterfall_str_concat(const char *a, const char *b);
void Waterfall_println(const char *s);
void Waterfall_print(const char *s);

// ... (List, Map, etc., when Tier 2 collections land)

#endif
```

Shipped alongside the compiler jar. The C backend's `emitProgram` adds `#include "waterfall_runtime.h"` and the build pipeline links `-lwaterfall_runtime`.

**Tensions.**

- **The runtime needs to be built and linked.** This is the first time Waterfall has a separate build step downstream of code emission. The existing pipeline ends at `gcc -fsyntax-only` (audit's runtime check). The new pipeline ends at `gcc -o out *.c -lwaterfall_runtime`.
- **Memory management.** The runtime allocates strings. v1 leaks. Tier 3 adds arenas / reference counting / GC. Per Task #2: every language has some answer here, but for a v1 transpiler, leaking is acceptable for short-running programs and CLIs.

**Priority.** T2.

---

### Section 1 summary

| Feature | Priority | Depends on |
|---|---|---|
| 1.1 Records | T1 | nothing |
| 1.2 Unions | T1 | nothing |
| 1.3 Pattern matching | T1 | 1.2 |
| 1.4 Generic functions | T1 | nothing |
| 1.5 Modules + import + pub | T1 | nothing |
| 1.6 Result + `?` | T1 | 1.2, partially-special-cased 1.4 |
| 1.7 String as real type | T1 | nothing |
| 1.8 Collections (List, Map, Set) | T2 | 1.4 + generic records |
| 1.9 FFI `@external` | T1 | nothing |
| 1.10 Package management | T2 | 1.5 |
| 1.11 LSP / formatter / debug | T2 | symbol table redesign (audit D2/D5) |
| 1.12 Stdlib | T2 | 1.9 |
| 1.13 C runtime | T2 | 1.7, 1.8, 1.12 |

T1 items together get Waterfall to "credible small transpiled language" per Task #2's bar. T2 items get it onto a competitive footing with Gleam / Crystal / Nim. T3 (out of this design's scope) — concurrency, effect tracking, advanced type-system features.

Plus the headline feature, which is its own section:

---

## Section 2 — The `readonly` design

This is the load-bearing feature. The strategist will sequence it; I describe how it actually works.

The user's three asks (per the team-lead brief):

- **Form A**: `readonly x = 4` at declaration time (similar to existing `const` / `imm`).
- **Form B**: `readonly x` as a standalone statement mid-function, freezing a previously-mutable binding.
- **Compile-time enforced** (no runtime checks).
- **Potentially**: subfield freezing `readonly x.field`.

This design implements all three under a single keyword. `readonly` is both a modifier (Form A: `readonly int x = 4`) and a statement (Form B: `readonly x`); §2g handles the small migration that removes the legacy `const`/`imm` keywords.

The researcher (Task #2 Part 3) confirmed Form B is approximately novel as a syntactic surface. Pony's `recover` is block-scoped; Java's "effectively final" is whole-scope and rule-based, not statement-driven; Rust uses shadowing as an idiom. Nobody has *this exact construct*. We don't lean on the novelty as the headline pitch (see §0): the value-add is making Rust's shadowing idiom directly expressible without a fresh binding, while staying verifier-only across all targets.

This is also a design opportunity: Form B can become the headline differentiator for Waterfall — "the language that lets you freeze a variable mid-function, statically."

### 2a — Semantics: precise spec

#### 2a.1 What is `readonly`?

`readonly` is a **binding modifier** that says: *no write to this name can succeed from this program point forward in this lexical scope.* It is not a type modifier. It applies to the *binding* (the name in the symbol table), not to the *object* the binding points to.

This distinction matters: if `xs` is a `readonly List<int>`, the binding `xs` cannot be reassigned, but mutations to the list *via aliases* are still legal. See 2a.6 (Aliasing) for why we make this call.

#### 2a.2 Form A: declaration-time

```
readonly int x = 4
```

The binding `x` is frozen from the moment of declaration to the end of its lexical scope. Any subsequent `x = ...`, `x += ...`, `x++`, `x--`, or compound assignment is a compile error.

Form A is the `readonly` modifier on the declaration. It sits in the `modifier` grammar slot at `WaterfallParser.g4:179` (the rule becomes `: READONLY` — see §2b). The verifier sets `SymbolInfo.isReadonly = true` at declare time when a `readonly` modifier is present; downstream lookups see the immutable state. The legacy `const`/`imm` keywords are removed in v1 (see §2g for migration).

#### 2a.3 Form B: mid-scope promotion

```
int x = 0
x = compute()
readonly x          // <-- the standalone statement
// x = 5            // ERROR: cannot write to readonly binding `x`
```

Form B is a **statement**, not an expression. Its sole effect is to record a freeze on `x` in the *current scope's local shadow* of the symbol table (see §2c). It produces no runtime effect — the emitted code in every backend at this statement is empty (see §2e for the firm "emit nothing" rule).

Form B is best understood as **a one-line shorthand for Rust's shadowing-and-rebinding idiom**:

```rust
// In Rust, the common pattern:
let mut x = build();
mutate(&mut x);
let x = x;   // shadows; from here, x is immutable.
```

Waterfall's Form B is the same semantic motion written without a fresh binding and without a copy:

```
int x = 0
x = compute()
readonly x   // from here, x is immutable.
```

The novelty is **syntactic**, not semantic — Waterfall is making an idiom that already exists in popular languages directly expressible. The researcher (Task #2) noted Pony's `recover` is the closest analog and operates on whole blocks; Form B operates on a single binding at a single program point. We don't lean on the novelty as the headline value; the value is *ergonomics* relative to the Rust idiom, and *cross-target portability* (the rule is verifier-only, so the JS/Python/C output is unchanged).

#### 2a.4 Scope of the freeze: lexical scope, persisting through all paths

The defining question: **how far does the freeze reach?**

**Rule**: the freeze applies from the program point of the `readonly x` statement to the end of the *lexical scope in which `x` was originally declared*.

This is the strongest semantic the construct can have without becoming confusing. Consider this:

```
func f() {
    int x = 0
    {
        readonly x
        x = 5         // ERROR
    }
    x = 6             // What about here?
}
```

Two options:
1. **Block-only freeze**: `readonly x` inside an inner block freezes `x` only for that block. After the block ends, `x` is mutable again.
2. **Scope-of-declaration freeze**: `readonly x` freezes `x` for the rest of the scope in which `x` was declared (here, the function body). The line `x = 6` after the inner block is also rejected.

**Recommendation: scope-of-declaration freeze (option 2).** Justification:

- Block-only freeze makes the construct *almost useless* for reasoning. The reader at `x = 6` cannot tell whether `x` is readonly without scanning every preceding block.
- Scope-of-declaration freeze matches the *intent* — "from this point, this name is constant." The reader sees `readonly x` once and knows the rest of the scope is frozen.
- It composes cleanly with control flow (see 2a.5).
- This makes Form B *not* equivalent to writing `readonly` at declaration time *delayed* — which would be option 1's semantics. Form B is genuinely different and more useful.

The rule's mechanical formulation, refined to match the transactional symbol-table model in §2c: when the verifier sees `readonly x`, it records the freeze in the *current* scope's local `readonlyShadow` via `markReadonlyLocal(x)`. Subsequent `lookup("x")` from this scope or any deeper child scope returns `isReadonly = true`. The parent's binding entry is *not* mutated at the statement — that only happens at branch-join time via `commitReadonly` (§2d). The end result, for code that doesn't branch, is the same: from the `readonly x` statement to the end of the original-declaration scope, `x` is rejected on writes.

#### 2a.5 Control-flow joins: branches

The thorniest question. What does this mean?

```
int x = 0
if (cond) {
    readonly x
} else {
    // x is still mutable here
}
x = 5      // What now?
```

Three options:
1. **Pessimistic** — after the if, `x` is readonly if *either* branch promoted it.
2. **Optimistic** — after the if, `x` is readonly only if *both* branches promoted it.
3. **Confused** — error: the join state is ambiguous.

**Recommendation: optimistic (intersection of branch states), with one caveat.**

After the if, the binding is `readonly` if-and-only-if *every path* leading to this join point promoted the binding. Otherwise, the binding remains mutable.

So in the snippet above:
- After the `if` block: `x` is readonly inside.
- After the `else` block: `x` is mutable inside.
- At the join after the `if/else`: only the if-branch promoted; the else-branch did not; the intersection is "not all promoted," so **`x` is mutable** at the join. `x = 5` is accepted.

Compare:

```
int x = 0
if (cond) {
    readonly x
} else {
    readonly x
}
x = 5      // ERROR — both branches promoted; intersection says readonly.
```

**Justification.** The intersection rule is the only rule that satisfies both:
- *Soundness*: a program that reads `x` after the join is guaranteed that `x`'s value was set on every path leading here. If we promoted optimistically only when *one* branch promoted, then a write after the join on the other branch's path could surprise us — but wait, that path didn't promote, so it didn't write either. So actually, the intersection rule and the pessimistic rule are *both* sound for reads. The difference is what writes we reject.
- *Programmer expectation*: "I added `readonly x` to this branch; I expect `x` to stay mutable on the other branch's path." The pessimistic rule would surprise the programmer by rejecting writes after the if even when only one branch promoted.

This matches the rule for "definite assignment" in Java/C# (a variable is definitely assigned at a join only if it's assigned on every path). Same rule, dual concept: we promote-to-readonly only if every path promotes.

**Caveat**: if any branch has an unreachable / always-throws / always-returns ending, that branch's state is effectively "irrelevant" for the join. So:

```
int x = 0
if (cond) {
    readonly x
} else {
    return 0
}
// At this join, only the if-branch reaches here. So x is readonly here.
x = 5      // ERROR
```

This requires a simple control-flow analysis ("does this branch end with a return / throw?"). v1 doesn't *have* exceptions, but `return` is detectable. So v1 implements this for return-ending branches. (When we add `panic`/`unreachable` in Tier 2, extend the analysis.)

#### 2a.6 Aliasing

```
record Box { int v }
Box a = Box::new(1)
Box b = a            // a and b alias the same record
readonly a
b.v = 2              // is this an error?
```

Three positions:
1. **Deep / transitive freeze**: freezing `a` recursively freezes everything `a` points to. So `b.v = 2` is an error because the underlying record is frozen.
2. **Aliased-binding freeze**: freezing `a` also freezes any other binding that aliases the same object. So `b = Box::new(3)` is an error (rebinding `b`)? Or any mutation through `b` is an error? This is incoherent unless we have a real aliasing analysis.
3. **Binding-only freeze**: freezing `a` only freezes the *name* `a`. The underlying record is still mutable, and `b.v = 2` is fine.

**Recommendation: binding-only freeze (option 3).**

Justification:
- A real aliasing-aware analysis requires either reference capabilities (Pony) or borrow checking (Rust). Both are major language redesigns. Waterfall is not aiming to be Rust.
- Option 1 is also feasible *only if records are deeply traced at compile time*, which means the type system has to know "what does `a` point to" — that's escape analysis territory, expensive and complex.
- Option 3 is *honest*: it freezes what we can statically guarantee (the binding) and admits that aliasing is something the language doesn't try to control. The error message can be explicit: "`readonly` freezes the binding `a`, not the underlying record. Use `imm record` (Tier 2) for transitive immutability."

This means `readonly` has a clear, simple specification that the user can hold in their head. The cost is honesty about what `readonly` does *not* do.

**Future work**: a Tier 2 `imm record` declaration that marks a record as deeply immutable from the type level. This is a *type modifier*, not a binding modifier — distinct from `readonly`, composable with it. Out of scope for v1 of `readonly`.

#### 2a.7 Subfield promotion: `readonly x.field`

The user mentioned this as a possible extension. Two interpretations:

1. **Freeze the field**: from this point, `x.field = ...` is rejected, but `x.otherField = ...` is fine.
2. **Freeze through the field**: from this point, neither `x.field` nor any sub-path can be written.

**Recommendation: support interpretation 1 only, and only for records. Defer to Tier 2.**

Justification:
- The semantics are clear and limited.
- Implementation requires the symbol-table info to track per-field readonly state for record-typed bindings. That's a non-trivial extension.
- v1 ships without this. The user can achieve a comparable result by introducing a new binding: `readonly int frozenField = x.field` — though that copies the value rather than freezing the location.
- Tier 2 adds it once the record type system is more mature.

For v1: the grammar rule for the `readonly` statement accepts only a plain `ID`, not a dotted path. So `readonly x.field` is a *syntax error* in v1, with a diagnostic that says "subfield `readonly` is planned but not yet supported."

#### 2a.8 Loops

```
while (cond) {
    readonly x
    // body
}
// What's x's state here?
```

The promotion happens once per iteration. The freeze applies *within* the loop body from that statement forward. The question is: does the freeze persist to the *next* iteration?

**Recommendation: scope-of-declaration freeze (per 2a.4) plus this rule for loop-bodies**: the freeze applies for the rest of the current iteration's body, and at the loop's join (top of the next iteration), the binding's state is the intersection of "the state at loop entry" and "the state at the bottom of the body." This is the same intersection rule from 2a.5 applied to the loop-back edge.

Concretely:

```
int x = 0
while (cond) {
    readonly x      // <-- promotes x to readonly for the rest of this iteration's body
    // x = 5        // <-- ERROR: readonly in this iteration
}
// What's x's state here?
```

At the loop's bottom-of-body, `x` is readonly. At the loop's top, `x` is... we have two predecessors: the initial entry (where `x` is mutable) and the loop-back (where `x` is readonly). Intersection: *mutable*. So the loop body's *re-entry* sees `x` as mutable, and then encounters the `readonly x` statement and freezes again.

After the loop, the state is also the intersection: the loop may have run zero times (`x` is mutable from before the loop) or one+ times (`x` is readonly at the bottom). Intersection: *mutable*. So after the loop, `x = 5` is fine.

This matches the intuition: a `readonly` inside a loop body is a per-iteration freeze. To freeze a variable for the *whole* duration of the loop *and* afterwards, write `readonly x` *before* the loop:

```
int x = 0
readonly x
while (cond) {
    // x is readonly here
}
// x is still readonly here
```

**Justification.** The per-iteration freeze is what the user wants when they write `readonly x` inside a loop — they're saying "after this point in this iteration, x is locked." Persisting across iterations would also be a defensible choice, but it makes `readonly x` inside a loop almost always wrong (because the second iteration would hit the same statement on a frozen binding and... what? Re-freeze? No-op?). The per-iteration interpretation makes the construct useful.

The intersection rule for the loop-back edge is the *same rule as for if-branches* (2a.5). One rule, one mental model. Clean.

**For `for` loops**: same rule. The iteration variable itself is a fresh binding each iteration (it's a `name=ID IN collection=ID` per WaterfallParser.g4:57); promoting other bindings inside the loop body follows the same rule as `while`.

#### 2a.9 Function-call arguments

If a readonly binding (either Form A declaration or Form B–frozen) is passed to a function, is the callee's parameter also readonly?

**Rule: parameters are mutable bindings in the callee, independent of caller state. The callee may use Form B (`readonly x` inside the function body) to freeze its parameter, but that is a callee-side decision.**

Note: the v1 grammar does not allow modifiers on function parameters (the `typedArgument` rule at WaterfallParser.g4:97-99 is `type name=ID`, no `modifier*` slot). Parameters are always declared mutable; the callee freezes them via Form B if desired. Adding a `readonly` modifier to parameter syntax is a separate small grammar extension (not in this design's v1 scope but easy to add later).

```
func consume(int x) {
    readonly x          // freeze the parameter here; rest of body sees x as immutable.
    // x = 5            // ERROR
}

func mutate(int x) {
    x = 5      // legal; this x is a fresh binding (parameters are bindings, not aliases)
}

readonly int n = 10     // Form A: n is readonly at declaration
consume(n)     // fine — value is copied to callee's parameter binding; callee's binding is independent.
mutate(n)      // fine — same reason; mutating callee's local copy doesn't affect caller's n.
```

Justification:
- Waterfall doesn't have pass-by-reference. The callee gets a copy of the value (for scalars) or the pointer (for arrays/records — but the pointer is itself a value).
- So mutating the callee's parameter has no effect on the caller's binding. There's no need to "propagate readonly across the call boundary."
- A callee that wants the *self-documenting contract* "this function does not modify its parameter" can use Form B as the first statement of its body. This is documentation, not a constraint on callers.

**Tension with aliasing.** If `n` is a record and the callee mutates `n.field`, the caller's `n.field` is mutated too (since records are passed by reference / shared pointer). This is the same aliasing problem as 2a.6 — `readonly` is binding-only, not transitively object-level. Calling `consume(myRecord)` where `consume` mutates a field of its parameter *does* affect the caller's record. This is consistent with the binding-only freeze and documented as such.

#### 2a.10 Lambda captures

This subsection had a longer, more confident first draft that proposed a Rust-style "all captures implicitly readonly" rule. The skeptic (F1) pointed out two problems with that framing:

1. The rule is **vacuous in v1** — the current grammar's `lambdaFunction` body is exactly one `functionCall` (or empty); see `WaterfallParser.g4:139-142`. A function call cannot, syntactically, perform a write. So in v1 there is no case where the rule rejects a program.
2. The rule **contradicts default mental models** — Kotlin closures freely mutate captured `var`s, JS closures freely mutate captured `let`s, Rust closures default to borrowing (the implicit-readonly framing applies *only* when the body never writes — `FnMut` and `FnOnce` exist precisely because mutating captures is common). Picking "implicitly readonly" as the default would be a surprise to every audience.

The correct framing splits the question into "what's needed now" and "what to decide later."

##### 2a.10.A — v1 lambda capture semantics

In v1, lambda bodies are syntactically incapable of writing to any binding, captured or not. The grammar at `WaterfallParser.g4:139-142` requires the body to be a `functionCall` or `L_CURLY R_CURLY` (empty). Neither contains an assignment statement, an increment statement, or a `readonly x` statement.

Therefore the question "what does `readonly` do to a captured binding?" has no answer in v1 because no v1 program can construct the scenario. The verifier needs **no special rule** for lambdas in v1. Lambda definition does not snapshot, copy, or freeze any state; calling a lambda does not interact with `readonly`.

Concretely:

```
func f() {
    int x = 0
    const fn := () ==> doSomething(x)    // lambda reads x; no write possible.
    readonly x
    fn()                                  // legal: calling a lambda doesn't write.
}
```

Both forms (defining before the freeze; defining after) verify identically. The lambda body is just a function call, which only reads its operands. v1 ships with no lambda-specific `readonly` rule; the verifier treats free variables in a lambda body the same way it treats any other read.

##### 2a.10.B — deferred decision for multi-statement bodies (P12)

The strategist's roadmap places multi-statement lambda bodies in Phase 12 (P12). When that phase lands, the grammar grows to allow a `statementBlock` body, which can contain assignments and `readonly` statements. *Then* the question "what does mutating a captured binding mean?" becomes real, and a design call is needed.

At that point, three candidate models exist (carried forward from the earlier draft):

1. **Implicit-readonly captures.** Captured bindings cannot be written by the lambda body. Rust's `Fn` default.
2. **Implicit-mutable captures, with `readonly` opt-in.** Captured bindings can be written; the user marks a capture readonly if desired. Kotlin / JS default.
3. **Capture-time snapshot.** Lambda definition copies the current readonly-state of each free variable; the lambda's view of that variable's mutability is fixed at definition time.

**This is a P12 decision, not a v1 decision.** The earlier draft's confident pick of option 1 was premature given the F1 critique. The phase-12 designer should weigh the three candidates against the *actual* Waterfall ergonomics that will exist by then, with the benefit of a year's worth of user feedback on the v1 `readonly` shape. The skeptic flagged option 1 as contradicting Kotlin/Rust/JS expectations, which is a valid concern for Waterfall's positioning — but the right resolution is "decide later with evidence," not "lock in now."

For the strategist: when sequencing P12, allocate design time for this specific subproblem. It does not need to be on the critical path for `readonly`'s value.

#### 2a.11 Shadowing

```
int x = 0
readonly x
{
    int x = 10     // is this an error?
    x = 11         // mutation of the inner x
}
```

The audit at SymbolTable.kt:8-19 already rejects ancestor-shadowing — `declare` walks parents and throws on any name collision. So this is *already* an error today (the inner `int x = 10` would fail verification before `readonly` enters the picture).

So shadowing across nested scopes is not allowed. *Within the same scope*, shadowing is also not allowed (current rule, same code path). The interaction with `readonly` is therefore: it doesn't matter what `readonly` does to `x` because you can't redeclare `x` anyway.

Future direction: if Waterfall ever relaxes the shadowing rule (Rust-style rebinding), `readonly` interacts cleanly because the new binding is a fresh symbol-table entry. We can defer that decision; for v1, the existing strict no-shadowing rule continues.

#### 2a.12 What replaces `const` and `imm`

Today (per audit row "Modifier `imm`" line 34): `imm` is "treated identically to `const`." So they're already aliases of each other — and now both are aliases of `readonly`. v1 unifies all three under `readonly` and removes `const`/`imm` from the language.

Concretely:
- `readonly` is a `modifier` alternative for declaration-time use (Form A).
- `readonly` introduces the `readonlyPromotion` statement for mid-scope use (Form B).
- `const` and `imm` are no longer accepted as modifiers in v1. The parser emits a friendly error ("use `readonly` instead") and points the user at the migration in §2g.

Justification (the trade is small and the gain is consistent):
- **One keyword carries one meaning.** Form A and Form B both result in "from this point, this name is immutable." They share a *state*, even if they differ in *action*. The library-author + Gleam-vibe niche identifies the language by its aesthetic clarity (per `04-strategy.md` §2 Candidate 5); three keywords for the same end state is exactly the noise the niche penalizes.
- **`readonly` is the clearest name.** TypeScript uses it for properties; C# uses it for fields. Both audiences read "readonly" as "you can't write to this." `const` is overloaded with C++'s compile-time-constant meaning; `imm` is Waterfall-only and carries no signal externally.
- **Migration is small.** Three `.wf` files in the repo use `const`/`imm` today; the strategy doc explicitly accepted the migration cost.
- **The novel-statement story doesn't get muddied.** `readonly x` and `readonly int x = 4` are visibly the same word doing the same conceptual thing in two grammatical positions — which is what we want. Readers don't need to learn two keywords with overlapping meaning.

See §2g for the actual migration plan.

### 2b — Grammar deltas

The actual `.g4` changes, citing the audit's section 4 inventory.

**Lexer (`WaterfallLexer.g4:10-11`):**

```antlr
// modifiers
READONLY: 'readonly';   // NEW — must come before ID at line 35.

// Retained for one release for friendly migration errors. Removed in v1.1.
CONST: 'const';
IMM: 'imm';
```

The lexer-ordering note at line 21 of the audit applies: `READONLY` must be declared before `ID` so the lexer picks the keyword on ties. `CONST` and `IMM` stay as tokens in v1 so the parser can emit a clear "use `readonly` instead" error rather than a generic syntax error; they're removed entirely in v1.1.

**Parser (`WaterfallParser.g4:179-182`) — modifier rule replaced:**

```antlr
modifier
    : READONLY
    ;
```

`CONST` and `IMM` are *not* alternatives of `modifier`. If they appear in modifier position, parsing fails — the parser-level error message handles this case specifically (see §2g).

**Parser (`WaterfallParser.g4:19-29` and a new production) — Form B statement:**

```antlr
statement
    : typedVariableDeclarationAndAssignment
    | untypedVariableDeclarationAndAssignment
    | variableAssignment
    | functionCall NEWLINE+
    | ifBlock
    | forBlock
    | whileBlock
    | returnStatement
    | incrementStatement
    | readonlyPromotion             // NEW
    ;

readonlyPromotion
    : READONLY name=ID NEWLINE+
    ;
```

Modeled after `incrementStatement` at WaterfallParser.g4:31-33, exactly as the audit's section 4 recommended.

**Disambiguation note**: the lexer/parser must not mistake the Form B statement `readonly x` for a Form A declaration. In the existing grammar, a declaration begins with `modifier* type name=ID EQUALS expression NEWLINE+`. After this change, both Form A and Form B start with `READONLY`. ANTLR resolves this with one-token-of-lookahead: Form A needs a `type` (a `?`-prefixed or bare `ID` followed optionally by `[]`) after the `READONLY`, then another `ID` for the variable name, then `EQUALS`. Form B is just `READONLY name=ID NEWLINE+`. The difference shows up at the second token after `READONLY` — either it's followed by another `ID` and `EQUALS` (declaration) or by `NEWLINE` (statement). ANTLR's standard LL(*) prediction handles this without manual disambiguation; verify with the regenerated parser that `readonly x = 4` and `readonly x` both parse correctly.

**Net additions**: one new token (`READONLY`), one new statement alternative (`readonlyPromotion`), and a simplified `modifier` rule (one alternative instead of two). Subfield promotion (`readonly x.field`) is *not* added in v1 (see 2a.7).

### 2c — AST shape and symbol-table changes

**New AST class.** `ReadonlyPromotionData.kt`, sibling of `IncrementStatementData.kt`:

```kotlin
package com.aaroncoplan.waterfall.compiler.statements

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator

class ReadonlyPromotionData(filePath: String, ctx: WaterfallParser.ReadonlyPromotionContext)
    : TranslatableStatement(filePath, ctx) {

    @JvmField val name: String = ctx.name.text

    override fun verify(symbolTable: SymbolTable): VerificationResult {
        // 1. Look up `name`. Error if not found in this scope chain.
        // 2. Error if already readonly (per the lookup, which respects any local shadow).
        // 3. Record the freeze in *this scope's local shadow only*. Do not mutate
        //    parent-owned entries — that would break the intersection rule at §2d
        //    by leaking child-branch state to sibling branches.
        val info = symbolTable.lookup(name)
            ?: return VerificationResult(false,
                "Cannot freeze undeclared binding '$name'")
        if (info.isReadonly) {
            return VerificationResult(false,
                "Binding '$name' is already readonly")
        }
        symbolTable.markReadonlyLocal(name)
        return VerificationResult(true, null)
    }

    override fun translate(backend: CodeGenerator): String = backend.emitReadonlyPromotion(this)
}
```

**Dispatcher.** Add one line to `StatementDispatcher.fromStatement` at StatementDispatcher.kt:17-28:

```kotlin
stmt.readonlyPromotion()?.let { return ReadonlyPromotionData(filePath, it) }
```

**Symbol table — the bigger change.** Per audit D2 ("Symbol table info is `Any?`"), the symbol table needs a richer info type. This is the single most important refactor in this design, and it serves *every* T1 feature (records, unions, generics, modules, readonly — all need richer info than a type string).

The design below addresses a concrete bug that an earlier sketch of this section had (flagged by skeptic F8): a child branch destructively mutating its parent scope's binding makes it impossible for a sibling branch to observe the binding's pre-promotion state, which breaks the intersection rule at §2d. The fix is to **never mutate parent-owned entries from a child scope**. Promotions inside a child scope are recorded in a *local shadow record* attached to the child; the verifier reads the shadow on lookup, and only at the end of a branch — after the intersection — does the parent's entry get promoted.

```kotlin
package com.aaroncoplan.waterfall.compiler.symboltables

data class SymbolInfo(
    val type: String,                  // current "string" of the type, until type system maturity
    val kind: Kind,                    // distinguishes variables / functions / arguments / records / unions / variants
    val isReadonly: Boolean,           // for variables and arguments
    val declarationPosition: SourcePosition? = null
) {
    enum class Kind {
        VARIABLE, ARGUMENT, FUNCTION, RECORD, UNION, VARIANT, IMPORTED_MODULE
    }
}

class SymbolTable(private val parentSymbolTable: SymbolTable?) {

    /** Bindings *owned* by this scope (declared here). */
    private val nameToInfoMap: MutableMap<String, SymbolInfo> = HashMap()

    /**
     * Local overrides for bindings owned by an ancestor scope. Form B writes go here
     * when this scope is below the binding's owning scope. Treated as a *shadow*
     * overlay: lookup walks `nameToInfoMap` → `readonlyShadow` → parent. The shadow
     * is only ever a (name -> isReadonly=true) record; it never adds new symbols
     * and never changes types.
     */
    private val readonlyShadow: MutableSet<String> = HashSet()

    @Throws(DuplicateDeclarationException::class)
    fun declare(key: String, info: SymbolInfo) {
        if (parentSymbolTable != null) {
            val parentInfo = parentSymbolTable.lookup(key)
            if (parentInfo != null) throw DuplicateDeclarationException()
        }
        if (nameToInfoMap[key] != null) throw DuplicateDeclarationException()
        nameToInfoMap[key] = info
    }

    /** PUBLIC — needed by T1 features (readonly, type inference, generics, ...). */
    fun lookup(key: String): SymbolInfo? {
        // 1. Owned in this scope? Apply any local-shadow promotion on top.
        nameToInfoMap[key]?.let { owned ->
            return if (key in readonlyShadow) owned.copy(isReadonly = true) else owned
        }
        // 2. Owned by an ancestor? Walk the chain, then apply our local shadow.
        val ancestorInfo = parentSymbolTable?.lookup(key) ?: return null
        return if (key in readonlyShadow) ancestorInfo.copy(isReadonly = true) else ancestorInfo
    }

    /**
     * Form B promotion. Records the freeze in *this scope's local shadow only*.
     * Parent scopes (and sibling branches that share a parent) do NOT observe this
     * change. This is the property that makes the intersection rule at §2d work.
     *
     * Returns false if `name` is not visible in the scope chain.
     */
    fun markReadonlyLocal(key: String): Boolean {
        // Must exist somewhere in the chain. (The verifier checks this first;
        // we double-check here as a safety net.)
        if (lookup(key) == null) return false
        readonlyShadow.add(key)
        return true
    }

    /**
     * Apply a finalized set of promotions to *this scope's owned entries*. Used by
     * branch-join code (§2d) after computing the intersection of the predecessors'
     * shadows. The caller has already verified that every relevant predecessor
     * promoted each name in `names`.
     *
     * If a name is not owned here, walks to the parent (for the case where the
     * intersection commits a promotion of an ancestor-owned binding from inside
     * this scope's level — the result should land on the owning scope).
     */
    fun commitReadonly(names: Set<String>) {
        for (name in names) {
            val owned = nameToInfoMap[name]
            if (owned != null) {
                nameToInfoMap[name] = owned.copy(isReadonly = true)
            } else {
                parentSymbolTable?.commitReadonly(setOf(name))
            }
        }
    }

    /** Snapshot which names are locally-shadowed-readonly in this scope (for §2d). */
    fun localReadonlyShadow(): Set<String> = readonlyShadow.toSet()

    // ... iteration / introspection methods to support LSP and richer diagnostics
}
```

**The key property** of this design: `markReadonlyLocal` is *non-destructive with respect to parent state*. A child branch that promotes `x` only affects its own `readonlyShadow`. When the child's verification ends, the parent's `nameToInfoMap[x]` is unchanged, so a sibling branch (an `else`-branch, say) verifies starting from the same pre-promotion state.

`commitReadonly` is the *only* path by which a parent-owned binding becomes durably readonly. It is invoked exclusively at branch joins (§2d), after the intersection over predecessors is computed.

Migration: every callsite of `symbolTable.declare(name, typeString)` (audit Section 4 lists them) updates to `symbolTable.declare(name, SymbolInfo(type, Kind.VARIABLE, isReadonly=..., ...))`. The audit identifies the callsites:
- `TypedVariableDeclarationAndAssignmentData.verify` line 30
- `UntypedVariableDeclarationAndAssignmentData.verify` line 25
- `FunctionImplementationData.verify` lines 35 (self) and 47 (args)

This refactor is *not* a `readonly`-only concern. It's the symbol-table redesign called for by audit D2 / D6 / G4. We just bundle it with `readonly` work because that's the first feature that needs it.

### 2d — Verifier algorithm

Three new checks, all powered by the new `SymbolInfo`:

**Check 1: write to a readonly binding (Form A and Form B both produce this rejection).**

Where: `VariableAssignmentData.verify` (currently a no-op at VariableAssignmentData.kt:17).

```kotlin
override fun verify(symbolTable: SymbolTable): VerificationResult {
    val info = symbolTable.lookup(name)
        ?: return VerificationResult(false, "Assigned to undeclared name '$name'")
    if (info.isReadonly) {
        return VerificationResult(false,
            "Cannot assign to readonly binding '$name'.")
    }
    return VerificationResult(true, null)
}
```

**Check 2: increment/decrement of a readonly binding.**

Where: `IncrementStatementData.verify` (currently a no-op at IncrementStatementData.kt:16).

```kotlin
override fun verify(symbolTable: SymbolTable): VerificationResult {
    val info = symbolTable.lookup(name)
        ?: return VerificationResult(false, "Incremented undeclared name '$name'")
    if (info.isReadonly) {
        return VerificationResult(false,
            "Cannot increment/decrement readonly binding '$name'.")
    }
    return VerificationResult(true, null)
}
```

**Check 3: the join semantics for branches / loops (2a.5, 2a.8).**

This is the algorithmically interesting part. The transactional symbol-table API from §2c makes it tractable: child scopes record promotions in their *local shadow*, never mutating the parent. Branch-join code reads each predecessor's shadow, computes the intersection, and only then calls `commitReadonly` on the parent.

This addresses skeptic F8 directly: an earlier (destructive) version of `markReadonly` made the intersection rule unimplementable because by the time the else-branch ran, the parent's binding was already frozen by the then-branch and the else-branch couldn't observe the pre-promotion state. The shadow model avoids that bug.

**The general pattern.** For any control-flow construct with multiple predecessors at a join point — `if/elif/else`, `match` arms, loop-back edges — the verification shape is:

```
enterBranch(parent)        // creates a child SymbolTable(parent)
  verify the branch body, which may call markReadonlyLocal(...) one or more times
  on the *child*; the child's readonlyShadow accumulates these.
exitBranch(child)          // returns localReadonlyShadow() + a "terminates?" flag.

[... repeat for every branch / predecessor ...]

joinPredecessors(parent, predecessorSnapshots):
  reachingPredecessors = predecessorSnapshots.filter { !it.terminates }
  if reachingPredecessors.isEmpty(): return    // all branches terminate; no join.
  intersection = reachingPredecessors.map { it.shadow }.reduce { a, b -> a intersect b }
  parent.commitReadonly(intersection)
```

The intersection is over *promotion sets* — exactly the bindings that every non-terminating predecessor froze. Only those names get committed to the parent.

**Concrete `IfBlockData.verify`.**

```kotlin
class IfBlockData {
    override fun verify(symbolTable: SymbolTable): VerificationResult {
        data class BranchSnapshot(val shadow: Set<String>, val terminates: Boolean)
        val snapshots = mutableListOf<BranchSnapshot>()

        // if-branch
        val ifScope = SymbolTable(symbolTable)
        for (s in ifBranch.body) {
            val r = s.verify(ifScope)
            if (!r.isSuccessful()) return r
        }
        snapshots += BranchSnapshot(ifScope.localReadonlyShadow(), bodyEndsTerminating(ifBranch.body))

        // each elif
        for (elif in elifBranches) {
            val elifScope = SymbolTable(symbolTable)
            for (s in elif.body) {
                val r = s.verify(elifScope)
                if (!r.isSuccessful()) return r
            }
            snapshots += BranchSnapshot(elifScope.localReadonlyShadow(), bodyEndsTerminating(elif.body))
        }

        // else-branch — if absent, treat as an empty non-terminating predecessor
        // (the "skip the if entirely" path) with an empty shadow.
        if (elseBody != null) {
            val elseScope = SymbolTable(symbolTable)
            for (s in elseBody) {
                val r = s.verify(elseScope)
                if (!r.isSuccessful()) return r
            }
            snapshots += BranchSnapshot(elseScope.localReadonlyShadow(), bodyEndsTerminating(elseBody))
        } else {
            snapshots += BranchSnapshot(emptySet(), terminates = false)
        }

        // Join: intersect the shadows of all reaching (non-terminating) predecessors.
        val reaching = snapshots.filter { !it.terminates }
        if (reaching.isNotEmpty()) {
            val intersection = reaching.map { it.shadow }.reduce { a, b -> a intersect b }
            if (intersection.isNotEmpty()) {
                symbolTable.commitReadonly(intersection)
            }
        }
        return VerificationResult(true, null)
    }
}
```

The key call is `symbolTable.commitReadonly(intersection)`. The *parent* scope (the one passed into `verify`) is mutated only here, only after the intersection. No child branch can leak its promotion to a sibling.

**Worked example for clarity** (the same logic explained as a snippet trace):

```
func f(bool cond) {
    int x = 0
    if (cond) {
        readonly x        // Records x in if-scope's readonlyShadow.
                          // Parent symbolTable['x'].isReadonly is STILL false.
    } else {
        // (nothing)       // else-scope's readonlyShadow is empty.
    }
    x = 1                 // Verifier looks up x in parent: isReadonly=false. LEGAL.
}
```

Trace through the algorithm:
1. Enter `if (cond)`. Create `ifScope = SymbolTable(parent)`.
2. Verify the if-body. The `readonly x` statement calls `ifScope.markReadonlyLocal("x")`. Now `ifScope.readonlyShadow = {"x"}`. The parent's `nameToInfoMap["x"]` is unchanged.
3. Exit if-branch. Snapshot: `(shadow = {"x"}, terminates = false)`.
4. Enter the implicit else-branch (or the explicit empty body). Create `elseScope`. Verify nothing happens. Snapshot: `(shadow = {}, terminates = false)`.
5. Intersection of `{"x"}` and `{}` is `{}`. Nothing to commit.
6. Continue with the next statement after the `if`. The parent scope has `x.isReadonly = false`. So `x = 1` is accepted.

Compare to the both-promoted case (Snippet 3 in §2f): both branches' shadows are `{"x"}`, intersection is `{"x"}`, `commitReadonly({"x"})` is called on the parent, and the subsequent `x = 1` is rejected. The two cases differ only in the intersection step.

This is the **flow-sensitive analysis** the audit's D1 (no IR) and D3 (verification entwined with translation) make harder than it should be. We accept this complexity for `readonly` because it's the value-add. For other T1 features (modules, records, etc.), no flow-sensitive analysis is needed.

**Loops** use the same pattern, applied to the loop-back edge:

```kotlin
class WhileBlockData {
    override fun verify(symbolTable: SymbolTable): VerificationResult {
        // The body runs in a child scope. The body's promotions only ever land in
        // the child's local shadow.
        val bodyScope = SymbolTable(symbolTable)
        for (s in body) {
            val r = s.verify(bodyScope)
            if (!r.isSuccessful()) return r
        }
        val bodyShadow = bodyScope.localReadonlyShadow()
        val bodyTerminates = bodyEndsTerminating(body)

        // Two predecessors at the post-loop join: the zero-iteration path (loop
        // didn't run; nothing promoted) and the some-iteration path (body ran,
        // promotions in bodyShadow at the bottom). Intersection with the empty
        // zero-iteration set is always empty — so a loop body's promotions
        // NEVER propagate past the loop. This is the §2a.8 design call.
        //
        // No commitReadonly call. The parent scope is unchanged by the loop body.
        return VerificationResult(true, null)
    }
}
```

The simplification is the punchline: **loops never promote past the loop body**, because one of the predecessors of the post-loop join is always the zero-iteration path with an empty shadow. Intersection with the empty set is empty. This falls out of the rule from §2a.8 without special-casing.

The "for the rest of the iteration's body" semantics (§2a.8) is realized by the fact that the body scope's local shadow is in effect during body verification — so a `readonly x` early in the body causes subsequent body statements to see `x` as readonly. But the shadow is dropped at the body's end.

For `for-in` loops, the iteration variable is declared into `bodyScope` at entry; otherwise the verification shape is identical.

### 2e — Backend lowering

The defining point: **Form B is verifier-only**. The emitted code in each target is byte-identical to the un-frozen version. No comment, no marker, no debug breadcrumb — nothing. Skeptic F7 caught an earlier draft that contradicted itself within this very section; this version picks one rule and sticks to it: **emit nothing**.

Each backend gets a new `emitReadonlyPromotion` method on `CodeGenerator` (audit Section 4, line 5 of the Summary). Every implementation returns the empty string.

**JavaScript.**

Form A: `readonly int x = 4` → `const x = 4;` (the JS backend already emits `const` for `isImmutable()` at JavaScriptBackend.kt:51).

Form B: `readonly x` → emit nothing. The audit at JavaScriptBackend.kt:51-52 notes that the `const`-vs-`let` choice in *emitted JS* is made at declaration time and is unchangeable thereafter. So the JS variable was emitted as `let` (because the Waterfall source wasn't `readonly` at declaration) and stays `let`. The Waterfall verifier rejects subsequent writes; the JS runtime never sees them.

**Python.**

Form A: `readonly int x = 4` → `x: Final = 4` (PythonBackend.kt:57-71 already emits `Final` for `isImmutable()`).

Form B: emit nothing. The Python variable was emitted without `Final` (because the Waterfall source wasn't `readonly` at declaration). The verifier rejects subsequent writes; the emitted Python is unchanged.

**C.**

Form A: `readonly int x = 4` → `const int x = 4;` (CBackend.kt:85-93).

Form B: emit nothing. Same logic — the C variable was declared without a `const` qualifier (because the Waterfall source wasn't `readonly` at declaration), and that declaration is unchangeable in C. Form B is verifier-only.

**Legacy.**

The audit notes (Section 4, Backend `Legacy`) that legacy doesn't read modifiers at all. Form A is a no-op in legacy and Form B also is. Leave it as-is — legacy's purpose is regression anchoring, not correctness.

**Summary.** Form B emits nothing in every backend. The user gets a compile-time guarantee with zero runtime overhead, zero target-language artifacts, and zero divergence between Waterfall and each target.

Code generator interface adds:

```kotlin
interface CodeGenerator {
    // ... existing methods ...
    fun emitReadonlyPromotion(s: ReadonlyPromotionData): String
}
```

Every backend implements the same trivial body:

```kotlin
override fun emitReadonlyPromotion(s: ReadonlyPromotionData): String = ""
```

That is the entire codegen story for Form B. No debug-comment variant, no `--emit-comments` flag, no per-target option. The rule is one line. If traceability ever becomes a felt need, source maps (Tier 2 per 1.11) are the right tool.

### 2f — Edge cases (worked .wf snippets)

Below: 12 concrete `.wf` snippets and what each does. The user reads these and confirms the spec matches intuition.

#### Snippet 1: Basic Form B

```
func f() {
    int x = 0
    x = 1
    readonly x
    // x = 2   // ERROR: cannot assign to readonly binding 'x'
}
```

Verifier: at `readonly x`, the verifier calls `symbolTable.markReadonlyLocal("x")` (§2c). Then the next `x = 2` (if uncommented) fails Check 1, because `lookup("x")` returns `isReadonly = true` once the local shadow is consulted.

Emit (JS): `let x = 0; x = 1;` (no third line emitted for `readonly x`, per §2e).

#### Snippet 2: Form A — the `readonly` modifier on declarations

```
func f() {
    readonly int x = 5
    // x = 6   // ERROR
}
```

The assignment is rejected at verification time: `SymbolInfo.isReadonly` is set to `true` at declare time (because the `readonly` modifier is present), so `VarAssignmentVerifier` produces `VerifyError.AssignToReadonly`. Form A is the modifier form of `readonly`; Form B in Snippet 1 is the statement form. Both use the same keyword.

#### Snippet 3: Promotion in both branches → readonly after the join

```
func f(bool cond) {
    int x = 0
    if (cond) {
        readonly x
    } else {
        readonly x
    }
    // x = 1   // ERROR: both branches promoted; intersection says readonly.
}
```

Verifier: each branch records `x` in its own `readonlyShadow` via `markReadonlyLocal`. At the join, the intersection of `{"x"}` and `{"x"}` is `{"x"}`, so `symbolTable.commitReadonly({"x"})` is called on the parent scope (§2c, §2d). Subsequent `x = 1` fails Check 1 because `lookup("x")` now returns `isReadonly = true`.

#### Snippet 4: Promotion in only one branch → mutable after the join

```
func f(bool cond) {
    int x = 0
    if (cond) {
        readonly x
    } else {
        // nothing
    }
    x = 1     // OK: only one branch promoted; intersection says mutable.
}
```

Inside the `if` branch, `x = 5` after the freeze would have been rejected. After the join, `x` is mutable.

#### Snippet 5: Caveat — terminating branch is ignored at the join

```
func f(bool cond) returns int {
    int x = 0
    if (cond) {
        return 0    // terminating — not a predecessor of the join
    } else {
        readonly x
    }
    // x = 1     // ERROR: only the else branch reaches here; it promoted.
    return x
}
```

The terminating-branch caveat from 2a.5. The else-branch is the only predecessor of the join, and it promoted, so `x` is readonly after the join.

#### Snippet 6: Loops — per-iteration freeze, mutable after

```
func f() {
    int x = 0
    while (someCondition()) {
        readonly x
        // x = 1   // ERROR inside this iteration's body
    }
    x = 2          // OK: loop-back intersection says mutable; post-loop intersection also mutable.
}
```

The loop-body's effect on `x`'s state is "becomes readonly mid-body." The loop-back intersection: entry-state is `mutable`, end-of-body is `readonly`, intersection is `mutable`. So at the top of every iteration, `x` is mutable again, then the body re-encounters `readonly x` and re-freezes. Post-loop, intersection is also `mutable`.

#### Snippet 7: Loops — promote *before* the loop for whole-loop freeze

```
func f() {
    int x = 0
    readonly x
    while (someCondition()) {
        // x = 1   // ERROR: x is readonly here.
    }
    // x = 2   // ERROR: x is still readonly here.
}
```

Promoting before the loop puts `x` into readonly state at loop entry. The body sees readonly. Post-loop, still readonly.

#### Snippet 8: Aliasing — binding-only freeze, not deep

```
record Box { int v }

func f() {
    Box a = Box::new(1)
    Box b = a               // a and b alias
    readonly a
    // a = Box::new(2)      // ERROR: rebinding `a` is rejected
    b.v = 5                 // OK: `readonly` only froze the binding `a`, not the underlying record.
    a.v                     // OK: read returns 5 (because b's mutation aliased a)
}
```

This is the binding-vs-object distinction from 2a.6. The user must accept this; the error messages should hint at it.

#### Snippet 9: Function arguments — freeze is local to the callee

```
func consume(int x) {
    readonly x          // callee freezes its parameter via Form B.
    // x = 5            // ERROR inside this function
}

func f() {
    int n = 10
    consume(n)    // legal
    n = 11        // legal — caller's `n` is unaffected by callee's frozen parameter.
}
```

Per 2a.9, the callee's freeze is independent of the caller's binding state. (Parameters are always declared mutable in v1; the callee uses Form B if it wants the freeze.)

#### Snippet 10: Lambda captures — no v1 interaction with readonly

```
func f() {
    int x = 0
    const fn := () ==> doSomethingWith(x)    // lambda body is just a function call; reads x.
    readonly x
    fn()           // legal: calling the lambda doesn't write anything.
}
```

Per 2a.10.A, v1 lambda bodies are syntactically a single function call and cannot write to any binding. The verifier needs no lambda-specific `readonly` rule. The multi-statement body question is deferred to P12 (2a.10.B).

#### Snippet 11: Shadowing is still rejected (no change from today)

```
func f() {
    int x = 0
    readonly x
    {
        // int x = 10    // ERROR: shadows ancestor `x` (existing rule, audit DuplicateInnerDeclarationTest).
    }
}
```

Audit confirms this is already an error. `readonly` doesn't change the rule.

#### Snippet 12: Subfield promotion — syntax error in v1

```
func f() {
    Box a = Box::new(1)
    // readonly a.v    // SYNTAX ERROR in v1: subfield readonly is planned, not yet supported.
}
```

The grammar's `readonlyPromotion` rule accepts only `ID`, not a dotted path. v1 rejects this at parse time with the diagnostic listed in 2a.7.

### 2g — Migration: replace `const` and `imm` with `readonly`

`readonly` is the single keyword for "this binding cannot be reassigned." It covers both forms — as a declaration modifier (Form A: `readonly int x = 4`) and as a mid-scope statement (Form B: `readonly x`). The legacy keywords `const` and `imm` are removed.

Rationale:

- **Library-author + Gleam-vibe niche punishes inconsistency.** Per the strategy doc, the language's aesthetic identity is part of the value prop. "Three keywords for one concept" (`const`, `imm`, plus a hypothetical `readonly`) is the kind of small-but-loud noise that signals "this language wasn't designed carefully" to a candidate user evaluating Waterfall against Gleam or Crystal. One keyword carries one meaning.
- **`readonly` is the clearest name.** TypeScript chose it for property modifiers; C# chose it for fields. Both audiences read "readonly" as "you can't write to this," and that's exactly Waterfall's semantics. `const` overloads with compile-time-constant in C++/C; `imm` is unique to Waterfall and carries no meaning to outsiders.
- **Form A and Form B share a property.** Both result in "from this point, this name is immutable." Different *actions* lead to the same *state*, and conflating them under one keyword foregrounds the state. The pitch becomes one sentence: "`readonly` — at declaration or mid-scope, both compile-checked."
- **Migration cost is three `.wf` files.** The strategy doc explicitly accepted this trade. The 3-file migration is the entire visible cost; the gain is a one-keyword story for the language's lifetime.

#### v1 grammar shape

- `readonly` is a valid `modifier` (Form A) AND introduces the new `readonlyPromotion` statement (Form B).
- `const` and `imm` are flagged for removal during P10's symbol-table refactor (the same phase that lands `readonly` enforcement). They are not part of v1's user-visible surface.
- The `modifier` grammar rule becomes `: READONLY` (single alternative). The lexer keeps `CONST` and `IMM` tokens around for one compiler version's worth of clear error messages — see "Migration plan" below.

#### Migration plan: the three `.wf` files

These are the only Waterfall source files in the repo that use `const` or `imm` today (verified by `grep -rE '\b(const|imm)\b' examples/`):

1. `examples/VariablesAndFunctionsModule.wf` — `const int x = 4` → `readonly int x = 4`.
2. `examples/DuplicateDeclarationsModule.wf` — `const int add = 55` → `readonly int add = 55`.
3. `examples/DuplicateVariableDeclarationsModule.wf` — `const int x = 55` → `readonly int x = 55`.

Plus the language reference in `README.md` (the "Variable declarations" section under "Language reference") — search for "const" and "imm" and rewrite the section to use only `readonly`.

The mechanical migration is a single AST-aware rewrite. **Preferred approach**: extend `compiler/.../Main.kt` with a temporary `--migrate-modifiers` flag during the P10 work that reads a `.wf` file, parses it, rewrites every `const`/`imm` token in the `modifier` rule to `readonly`, and writes the file back. Then run it against `examples/*.wf` once and commit.

**Fallback approach** (if the AST-aware path is too involved for what's being migrated): a per-file `sed` is acceptable because the keywords `const` and `imm` are *only* used as modifiers in the existing example set (they don't appear as identifiers or substrings of identifiers, verified by the same grep). The sed:

```sh
sed -i.bak -E 's/\b(const|imm)\b/readonly/g' examples/VariablesAndFunctionsModule.wf
sed -i.bak -E 's/\b(const|imm)\b/readonly/g' examples/DuplicateDeclarationsModule.wf
sed -i.bak -E 's/\b(const|imm)\b/readonly/g' examples/DuplicateVariableDeclarationsModule.wf
```

The `.bak` files are deleted after a manual diff confirms the change. The README is migrated by hand (it contains prose discussing the keywords, not just modifier-position uses; a regex would mangle the surrounding text).

Tests: the golden files at `compiler/src/test/resources/golden/<target>/*.expected` contain *target-language* `const` keywords (e.g., JS's `const x = 4;` for `readonly int x = 4`). Those are NOT migrated — they reflect the emitted output, which still uses the host language's keyword. Verify after the migration that the relevant goldens (for `VariablesAndFunctionsModule.wf` etc.) remain byte-identical when re-emitted; the JS backend continues to map `isReadonly = true` → JS `const`, etc.

#### Backward-compat for one release: friendly error on `const`/`imm`

To soften the migration for downstream users we don't know about (anyone with `.wf` files outside the repo), v1 ships a friendly error when `const` or `imm` is encountered in modifier position:

> Error: keyword 'const' is no longer supported. Use 'readonly' instead. (The same change applies to 'imm', which was an alias of 'const'.)

**Implementation cost (round-4 F16 note).** This is not free. ANTLR's default behavior when an unexpected token appears in a non-matching rule is to produce "no viable alternative at input 'const'" — useful only to someone who already knows the grammar. To produce the friendly message above, the compiler ships a custom `ANTLRErrorListener` that recognizes the `CONST` and `IMM` tokens at the offending position and substitutes the helpful text. Concretely, this is ~50–80 lines of Kotlin, added to `parser/src/main/kotlin/.../FileParser.kt` (or a new sibling file).

The `CONST` and `IMM` tokens remain in the lexer (so they tokenize as keywords, not identifiers — without that, the listener can't recognize them) but they're not alternatives of the `modifier` rule. The listener intercepts the parser's error event when it occurs at a position where a `CONST` or `IMM` token is being parsed in modifier position, and substitutes the friendly text on stderr.

Sketch of the listener:

```kotlin
package com.aaroncoplan.waterfall.parser

import com.aaroncoplan.waterfall.generated.WaterfallLexer
import org.antlr.v4.runtime.*

/**
 * Substitutes a friendly message when the parser encounters `const` or `imm`
 * in modifier position. Removed in v1.1 along with the CONST/IMM lexer tokens.
 */
class LegacyModifierErrorListener(private val filePath: String) : BaseErrorListener() {
    override fun syntaxError(
        recognizer: Recognizer<*, *>,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String,
        e: RecognitionException?
    ) {
        val token = offendingSymbol as? Token
        if (token != null && (token.type == WaterfallLexer.CONST || token.type == WaterfallLexer.IMM)) {
            val keyword = token.text
            val friendly = "keyword '$keyword' is no longer supported. " +
                "Use 'readonly' instead. " +
                "(The same change applies to 'imm', which was an alias of 'const'. See " +
                "https://waterfall-lang.dev/migrate/const-imm for the one-line migration.)"
            System.err.println("$filePath at $line:$charPositionInLine $friendly")
            return
        }
        // Fall through to the existing SyntaxErrorListener formatting.
        defaultDelegate.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e)
    }
}
```

Registered alongside the existing `SyntaxErrorListener` in `FileParser.parseFile`. The listener is removed in v1.1 when the `CONST` and `IMM` lexer tokens themselves go.

The implementation cost is small but non-zero — call it half a Friday for the implementer including tests that the friendly text appears for at least three migration-trigger inputs (`const int x = 4`, `imm bool b = true`, and a multi-modifier case if `pub` lands in P14's import work). Don't drop the listener idea on the grounds of "it's just one extra error message" — without it, the migration UX is "no viable alternative at input 'const'" which actively confuses the very users we want to help.

This is the standard "deprecate-warn-then-remove" path applied at the smallest possible scale.

#### Why this is the right call

This recommendation is opinionated — it imposes a (small) migration cost on every existing Waterfall program in exchange for a cleaner language identity. The library-author niche explicitly tolerates this trade: per the strategy doc, the language's aesthetic clarity is part of why a library author would consider Waterfall over WASM-via-Rust. Inconsistency in the keyword set undermines exactly the value-prop the niche depends on.

A future change of mind is *easy*: re-add `const` as an alias of `readonly` by adding `CONST: 'const';` back to the lexer and a `CONST` alternative to the `modifier` rule, treating it as identical to `READONLY` in the verifier. This is an additive grammar change, not a breaking one — at which point Aaron can revisit if real-world usage suggests `const` was the better keyword. v1 commits to `readonly` only; the door isn't locked.

---

## Section 3 — Cross-cutting design principles

Five principles that justify all decisions above:

### Principle 1 — Compile-time over runtime

`readonly` Form B is compile-time only. Generics monomorphize for C and erase elsewhere — no runtime type machinery. Pattern matching exhaustiveness is checked statically. The Waterfall philosophy: catch what you can at compile time; emit code that has no Waterfall-specific runtime baggage.

This makes Waterfall's emitted code *unidentifiable* as Waterfall-emitted from the target side (modulo comments and source maps). The user gets the type-safety benefits and pays no runtime cost.

### Principle 2 — Cross-target divergence is explicit, never implicit

The `@external` annotation (Section 1.9) is the only mechanism for target-specific code. Everything else is target-uniform. The audit's surprise #1 about legacy being default and outputting different content per target is exactly the problem this principle solves: when a feature can't be uniform across targets, it must say so at the declaration site.

This means:
- No silent fallbacks. If `Mod::fn(x)` is called on a target where `Mod` is not externally defined and not a Waterfall module, the compiler errors. Today's behavior of "emit and hope the target runtime resolves it" (audit's surprises #10) is rejected by this principle.
- The `@external` is the *only* lever for divergence. No `#if` directives, no preprocessor macros, no platform conditionals scattered through function bodies.

### Principle 3 — One construct per concept

One keyword per concept. `readonly` covers both forms of immutability — declaration-time (Form A modifier) and mid-scope (Form B statement) — because they produce the same state. `union` covers enums and tagged unions. `import` covers all forms of cross-module reference (no `use`, `include`, `require` synonyms).

The Task #2 case study of CoffeeScript's death is the cautionary tale: many small constructs that overlap with the target language are a fragile moat. Better to have one construct per concept and make each one carry weight. The `const`/`imm`/`readonly` migration in §2g is this principle applied in the small.

### Principle 4 — The symbol table is the source of truth

Audit D2 calls out that today's `Any?` symbol-table info is the load-bearing debt blocking growth. The new `SymbolInfo` data class with a `kind` discriminator, full type information, position info, and the `isReadonly` flag is *the* source of truth for: variable lookup, function resolution, type checking, immutability checking, and LSP hover.

The `lookup` function (§2c) is the canonical answer to "what is the current state of binding `x` at this verification point?" It walks the scope chain, applies any local Form B shadow, and returns a `SymbolInfo` reflecting the current effective state.

The `readonlyShadow` overlay introduced in §2c is *inside* the symbol table, not parallel to it — `lookup` is the only API users go through. The shadow is an implementation detail of the symbol table's flow-sensitive state machinery, hidden behind the same `lookup` call. There is still one place to look.

### Principle 5 — Errors are first-class

Audit D5 calls out single-shot string errors as a growth blocker. v1 of Waterfall replaces `VerificationResult(Boolean, String?)` with a structured `Diagnostic` type: severity, code, primary location, related locations, suggested fix. This enables:
- Friendly error messages (Task #2 synthesis: a marketed feature).
- LSP integration.
- Batched diagnostics (audit Main.kt:74-109 issue: one error stops all translation).

This is the *legitimacy bar* per Task #2: "Friendly errors are a marketed feature now. ... low-cost differentiator for a small language — small effort, but high signal of 'we care about the developer.'"

---

## Section 4 — Cross-target divergence model: `@external`

Picking up the Section 1.9 design with worked examples.

### Why `@external` over `#if`

From Task #2 Part 2.15: Gleam's expression-level target tracking is "more sophisticated than Haxe's macro-style #if." Specifically:

- **Function-level granularity** (Gleam): each function's per-target implementation is visible at the function declaration. The reader sees the function once and learns "this is `sqrt`, here's its JS body, here's its Python body, here's its C body."
- **Statement-level granularity** (Haxe): a function body has `#if js ... #else ... #endif` slices. The function appears to be one thing but is actually four, fragmented.

Function-level granularity composes better with imports, generics, and pattern matching because everything inside a function body is target-uniform.

### Worked example: `sqrt`

```
@external(js, `Math.sqrt`)
@external(python, `math.sqrt`)
@external(c, `sqrt`)
pub func sqrt(dec n) returns dec
```

Note: when *all* targets have an `@external`, the function declaration has no Waterfall body. The grammar already requires a body (`emptyBlock` or `statementBlock`), so we relax that rule:

```antlr
functionImplementation
    : externalAnnotation* PUB? FUNCTION name=ID typeParams? L_PARENS typedArgumentList? R_PARENS
      (RETURNS returnType=type)?
      (emptyBlock | statementBlock | externalDeclaration)
      NEWLINE+
    ;

externalDeclaration
    : // explicit no-body marker; signals "this function has only @external implementations"
    ;
```

A simpler grammar move: allow the `statementBlock` to be empty *and* allow no block at all when at least one `@external` is present. The verifier checks: if no body provided, at least one `@external` per emitted target must exist (each backend checks at codegen).

Codegen at the call site:

- JS: `Math.sqrt(arg)`. The `@external` syntax `"Math.sqrt"` is emitted verbatim with arguments substituted in. (This means `@external(js, "console.log")` for `print` works the same way — the syntax `"console.log"` is just the target identifier.)
- Python: `math.sqrt(arg)`. Plus, the prelude includes `import math`.
- C: `sqrt(arg)`. Plus, `#include <math.h>` is added to required headers. (The compiler maps known external symbols to their header — for v1, this is a small built-in map; later, the `@external` syntax could be extended to specify the header explicitly: `@external(c, "sqrt", header="<math.h>")`.)

### Worked example: `setTimeout` (JS-only)

```
@external(js, `setTimeout`)
pub func setTimeout((Unit) ==> Unit callback, int millis)
```

No Waterfall body. Calling `setTimeout(fn, 100)`:
- JS: `setTimeout(fn, 100)`. Works.
- Python: **verification error** at the call site: "no `@external(python)` for `setTimeout` and no Waterfall body. Cannot lower for target=python."
- C: same error.

This matches Gleam's expression-level target tracking. Programs that use `setTimeout` on the Python target are *correctness errors*, not runtime errors.

### Worked example: cross-target compatible function with Waterfall body

```
@external(js, `myFastSqrt`)         // optional override: use a native implementation on JS
pub func mySqrt(dec n) returns dec {
    // Waterfall body — used by Python, C, legacy targets.
    // ... a naive implementation
}
```

On JS, calls lower to `myFastSqrt(n)`. On every other target, the Waterfall body is emitted as usual and calls lower to `mySqrt(n)` (or its mangled form on C).

The annotation reads as "override on the named target; fall through to the body otherwise." The strategist can sequence this once `@external` itself lands; the override case is a small extension of the base case.

### Grammar location

`externalAnnotation` lives in the grammar at the top of `functionImplementation`. `AT: '@';` and `EXTERNAL: 'external';` are new lexer tokens. The current grammar's `@JvmField` annotations on Kotlin AST classes are *Kotlin* `@`, not Waterfall `@`; introducing `@` to Waterfall's grammar conflicts with nothing existing.

### 4.1 Target keywords — reserved name list

The first argument to `@external` is a *target keyword*. The set of valid keywords is part of the language, not an open string namespace. v1 reserves exactly the following:

| Keyword | Meaning | Notes |
|---|---|---|
| `js` | The JavaScript backend (ESM-emitting; see 4.3). | Live in v1. |
| `python` | The Python backend (Python 3.x). | Live in v1. |
| `c` | The C backend (C99). | Live in v1. |
| `wasm` | WASM backend. | **Reserved, not live in v1.** Strategy doc Q6 keeps WASM optional (P17). Using `@external(wasm, ...)` parses without error and verifies as "no implementation available" if the user is compiling to WASM, otherwise the annotation is ignored. Pre-allocating the keyword now means programs written today don't need to be rewritten when WASM lands. |

**The legacy target keyword is NOT reserved.** Per strategy doc Q5 (Aaron dropping the legacy backend), `@external(legacy, ...)` is rejected at parse time as an unknown target. Programs needing legacy-target FFI use the Waterfall body fallback (the "cross-target compatible function with Waterfall body" pattern in Section 4 — the body is what runs on legacy, since legacy has no `@external` mapping).

Any unrecognized target keyword is a *verification error* at the annotation site, not silently ignored. The error message lists the valid keywords. This protects against typos (`@external(javascript, ...)` instead of `@external(js, ...)`) that would otherwise produce hard-to-trace "Cannot lower for target=js" errors.

The reserved list is owned by the compiler; adding a new target (e.g., reactivating `legacy` or activating `wasm`) is a compiler-level change, not a per-program override.

### 4.2 Partial-target-support semantics

A function with an `@external(target, ...)` annotation for only a subset of targets is **partially target-supported**. The semantics for a call to such a function:

1. **At compile time, when emitting for target T**:
   - If the function has an `@external(T, ...)` annotation → emit the target symbol as the call.
   - Else if the function has a Waterfall body → emit the lowered body as usual (the call goes to the mangled Waterfall name).
   - Else → **verification error**. The call cannot be compiled for target T because no implementation exists. Message: `Cannot call <fn> when compiling for target=<T>: no @external(<T>, ...) annotation and no Waterfall body.`

2. **The verifier is target-aware**. Today's compiler doesn't pass the chosen target through the verifier — verification produces one result regardless of `--target`. P10's verifier separation makes adding this trivial: `Verifier.verifyModule` takes the target as an additional parameter, threads it through to where call sites are checked.

3. **No "polyglot fallback" beyond what's stated.** The compiler does *not* try to compile a Python `@external` symbol as if it were a JS one. The annotation is target-specific and non-transferable.

This is the Gleam model: a program that uses `setTimeout` is a *typed* JS-only program. Compiling it for Python is a *correctness* error, surfaced at compile time, not a runtime crash.

**Concrete example**:

```
@external(js, `setTimeout`)
pub func setTimeout((Unit) ==> Unit callback, int millis)

pub func scheduleWork() {
    setTimeout(() ==> doWork(), 100)
}
```

- `./waterfall --target js example.wf` → succeeds; emits `setTimeout((() => doWork()), 100);`.
- `./waterfall --target python example.wf` → fails verification with: `Cannot call setTimeout when compiling for target=python: no @external(python, ...) annotation and no Waterfall body.` at the `scheduleWork` source position.
- `./waterfall --target c example.wf` → same error for target=c.

The user's responsibility is to scope target-specific functions behind their own typed boundary (don't have your portable algorithm call `setTimeout` directly).

### 4.3 Per-target lowering details

**JS — ESM imports.**

`@external(js, "Math.sqrt")` does *not* generate an `import` — `Math` is a JS global, and dotted-path string-form annotations emit the path verbatim at call sites. For *named imports from a module*, the annotation form is:

```
@external(js, `crypto-js::sha256`)        // module name :: export name
```

The `::` separator inside the string is the convention. At codegen, the JS backend emits:

```js
import { sha256 } from "crypto-js";
// ...
sha256(input)
```

Imports are accumulated per-module and emitted at the top of the JS output. ESM is the chosen module format (per strategy doc §3 P14 deliverable; CJS users transpile via their own pipeline).

If the JS string contains no `::`, it's treated as a *global path* (e.g., `Math.sqrt`, `console.log`) and no import is emitted.

**Python — `from` imports.**

`@external(python, "math.sqrt")` is interpreted as a dotted-path; the dotted-path's first segment is the module, the rest is the symbol. The Python backend emits:

```python
from math import sqrt
# ...
sqrt(arg)
```

For symbols inside a sub-module (e.g., `numpy.linalg.inv`), the annotation is `@external(python, "numpy.linalg.inv")`; the import becomes `from numpy.linalg import inv`. Symbols that are themselves modules (rare) use `import` form via a special convention: `@external(python, "json::*")` emits `import json` and references the symbol as `json.<call>` — but this is an edge case for v1.

**C — `#include` + forward declarations.**

`@external(c, "sqrt")` requires a header. v1 has two paths:

1. **Built-in mapping for stdlib symbols.** The compiler ships a small table mapping known libc symbols to their headers:

   ```
   sqrt, pow, abs, fabs    -> <math.h>
   strlen, strcmp, strcpy  -> <string.h>
   malloc, free            -> <stdlib.h>
   printf, fprintf         -> <stdio.h>
   ```

   For symbols in this table, the C backend adds the header to `requiredHeaders` (the existing TreeSet at CBackend.kt:37) and the call site emits the bare name.

2. **Explicit header annotation for non-stdlib symbols.** Vendor / third-party headers must be declared explicitly using an extended annotation form:

   ```
   @external(c, `crc32`, header=`"libcrc32.h"`)
   pub func crc32(char[] data, int len) returns int
   ```

   The `header=` is a named argument inside the `@external` annotation; its value is a STRING_LITERAL containing the include-form (either `<system.h>` or `"local.h"`). The C backend adds it to `requiredHeaders` and emits the bare name as the call.

For unknown symbols with no header annotation, the C backend produces a *codegen-time error* with the specific message: `@external(c, "<sym>") used but no header is known. Add header="<...>" to the annotation, or this symbol must be in the stdlib mapping.` The error includes the position of the annotation.

This mirrors the strategy doc's R6 mitigation (idiomatic outputs): a C consumer of a Waterfall-built library expects clean `#include` directives, not implicit globals.

### 4.4 The `@external` × record/sum-type boundary

**Decision: `@external` functions can accept and return primitives, primitive arrays, records, and sum types, but not lambdas. Records and sum types must be defined in the same module as the `@external` declaration, OR be drawn from the standard library (so the compiler knows their layout on each target).**

Reasoning:

- **Primitives + primitive arrays**: trivially OK. JS/Python/C all have natural representations (number/list, etc.).
- **Records**: lower to JS plain objects, Python dataclasses, C structs (per Section 1.1). The cross-target representation is documented. An `@external` function that takes a `Point` parameter receives the target-language equivalent — a JS object with `{x, y}`, a Python `Point(x, y)`, a C `Point` struct. The user wiring up the external implementation knows what they're receiving from the docs.
- **Sum types**: lower to tagged objects in JS, dataclasses-per-variant in Python, tagged unions in C (per Section 1.2). The discriminator is `_tag` (JS), variant subclass (Python), `_tag` field (C). An `@external` function that takes a `Shape` parameter receives the target's representation. The same documentation applies.
- **Lambdas**: NO. The current grammar's lambda body is one functionCall (audit §1) and the cross-target lowering (audit U2) for C is still an open problem. Allowing `@external` functions to accept lambdas exposes the C-lambda-lowering problem at every FFI boundary. Defer to post-v1 once U2 is resolved.

**The "must be defined locally or in stdlib" constraint** prevents this:

```
// In package A:
record Foo { int x }

// In package B, imports A:
@external(js, "useFoo")
pub func useFoo(Foo f) returns int
```

The package B author may not control package A's record layout, and a change to A's `Foo` would silently break the JS-side `useFoo` consumer. v1 simply rejects this: an `@external` function can only mention types defined in its own module or in `std::*`. Cross-package types in `@external` signatures are a v1.x extension once we have a clear "ABI stability" story for cross-package records.

### 4.5 Symbol-table representation

An `@external` function is represented in the symbol table as a regular `SymbolKind.Function` with one structural addition: the list of `(target, targetSymbol)` pairs is attached to the `SymbolInfo` (or to a side-table keyed by name; the implementer can choose).

**Recommended approach**: extend `SymbolKind.Function` to carry an optional `externals: Map<TargetKeyword, TargetSymbol>` field (where `TargetKeyword` is an enum: `Js, Python, C, Wasm`, and `TargetSymbol` is a parsed structure — for JS it's `JsExternal.Global(path: String)` or `JsExternal.Module(module: String, name: String)`; for Python it's `PythonExternal(modulePath: List<String>, symbol: String)`; for C it's `CExternal(name: String, header: String?)`).

The verifier checks at function-declaration time:
- Every `@external` target keyword is in the reserved list (§4.1).
- No two `@external` annotations share the same target keyword.
- If the function has no body, at least one `@external` exists; conversely, all non-`@external` targets receive the body.

The verifier also checks at call-site time (the new target-aware verifier from §4.2):
- If the chosen target has no implementation (no `@external` for it and no body), produce the error from §4.2 verbatim.

This wiring is what closes the loop from §4.2 ("the verifier is target-aware"): the symbol table knows what's implemented where, and the verifier passes the target through to call-site checks.

### 4.6 Worked examples — the three case studies

Each of these is a representative call shape the strategy doc identifies as a candidate first case study. Showing the `@external` line that would appear:

**CRC32** (Aaron-authored P14 seed case study). The library implements CRC32 in pure Waterfall. The only external dependencies are I/O primitives for reading bytes — e.g., reading a file in JS uses `fs.readFileSync`, in Python `open().read()`, in C `fopen`/`fread`. The CRC32 algorithm itself is pure Waterfall (`for` loop over bytes, XOR table, etc.), no `@external` needed. But the *demo program* that exercises it uses I/O:

```
@external(js, `fs::readFileSync`)
@external(python, `pathlib.Path.read_bytes`)
@external(c, `read_file_bytes`, header=`"file_util.h"`)
pub func readFileBytes(char[] path) returns int[]
```

The C-side requires a small wrapper header `file_util.h` shipped with the example because C doesn't have a one-liner for "read whole file as bytes." This shape — "the algorithm is Waterfall, the I/O bindings are `@external`" — is the prototype for every library in this niche.

**Small parser** (external case study, P13/14 candidate — TOML subset, CSV, expression evaluator). Parsers are typically zero-FFI: the algorithm reads characters, advances indices, returns an AST. The only `@external` is for string handling:

```
@external(js, `String.prototype.charCodeAt::call`)
@external(python, `ord`)
@external(c, `charcode_at`, header=`"strutil.h"`)
pub func charCodeAt(char[] s, int i) returns int
```

The JS form here uses the `::call` form because `String.prototype.charCodeAt` is an instance method and needs an explicit receiver. This is the gnarly case the v1 syntax has to handle — see §4.7 PITFALLS below.

**Crypto / hashing primitive** (external case study P15+, e.g., Blake3 variant, small signature). The primitive itself is pure Waterfall — bit twiddling, modular arithmetic. Some primitives need a constant-time-comparison helper, which is target-specific:

```
@external(js, `crypto::timingSafeEqual`)
@external(python, `hmac.compare_digest`)
@external(c, `constant_time_eq`, header=`"sodium.h"`)
pub func constantTimeEqual(int[] a, int[] b) returns bool
```

This is the case where `@external` shines: the algorithm is portable, but one helper is target-specific *and security-critical* (constant-time comparison), and the user gets to write the algorithm once and bind to the right helper per target.

### 4.7 PITFALLS for the implementer

1. **The `::` separator in JS module strings is a Waterfall convention, not JS syntax.** Don't try to emit it. At codegen, split on `::` to recover `(moduleName, symbolName)` and emit an `import` separately from the call site.

2. **The C `header=` named argument parses as a special form**. The grammar of the `@external` annotation needs to support both positional (`@external(c, "name")`) and named-arg (`@external(c, "name", header="...")`) forms. Reuse the existing `functionCallArguments` grammar for this — it already supports both positional and named args.

3. **Verify target-keyword spelling at parse time, not just verify time.** A typo like `@external(javacript, ...)` should fail fast with a clear message: "Unknown target keyword 'javacript'. Did you mean 'javascript'? Valid: js, python, c, wasm." Levenshtein-1 suggestions are nice-to-have; for v1 the literal list of valid keywords suffices.

4. **The "no `@external` for target T" verification error must show ALL the targets that lack support**, not just the one currently being compiled. A typical user wants to know "is this function callable everywhere?" at the call site, not just "is this function callable in JS?". The error message should list:
   > `setTimeout` has @external for: js. Missing for: python, c.

   This lets the user fix all missing implementations in one pass rather than discovering them one target at a time.

5. **Symbol-table population happens during verification, not lowering.** The list of `@external` annotations on a function must be in the `SymbolInfo` before any call to that function is verified. This means the verifier walks all function declarations first (collecting their `@external` lists), then walks call sites. Today's verifier does a similar two-pass walk; the addition is parsing and attaching the `@external` lists in the first pass.

### Tensions (preserved from earlier)

- **Per-target signature.** What if `setTimeout` on JS takes `(callback, millis)` but on some hypothetical Python equivalent takes `(callback, callback_args, delay)`? Different arity. v1 says: the Waterfall signature is the *source of truth*; targets must match it. If they don't, the user wraps the target's API in a thin Waterfall function. This is consistent with Gleam's approach and avoids the per-target-signature complexity Haxe externs deal with.
- **String literals for target symbols.** The target symbol is a STRING_LITERAL in the annotation, not an ID. This is so we can reference dotted paths (`Math.sqrt`) or Python's `math.sqrt` or C's `__builtin_sqrt` without confusing the parser.

---

## Appendix A — Summary of the proposed grammar deltas

For the implementer, here is the consolidated diff against the current grammar.

### Lexer additions (`WaterfallLexer.g4`)

```antlr
// New keywords
RECORD: 'record';
UNION: 'union';
MATCH: 'match';
IMPORT: 'import';
PUB: 'pub';
AS: 'as';
EXTERNAL: 'external';
READONLY: 'readonly';

// New tokens
AT: '@';
UNDERSCORE: '_';
```

All keyword tokens must be declared *before* `ID` (audit's lexer-ordering note).

### Parser changes (`WaterfallParser.g4`)

- `program` accepts a leading `importStatement*`.
- `topLevelDeclaration` adds `recordDeclaration`, `unionDeclaration`. Each existing top-level decl alternative accepts an optional `PUB?` prefix.
- `statement` adds `readonlyPromotion`, and (independently) `matchExpression` as a new expression alternative.
- `modifier` is replaced: the rule becomes `: READONLY` (single alternative). `CONST` and `IMM` are removed; the parser produces a friendly migration error if they appear (see §2g).
- `functionImplementation` adds optional `externalAnnotation*` prefix and optional `typeParams`.
- `type` adds optional `typeArgs`.
- `expression` adds `matchExpression`, `fieldAccess`, and a postfix `QUESTION_MARK` (the `?` propagator from 1.6).

### Total file count of the grammar after v1

Today: 2 grammar files (Lexer + Parser), ~180 lines.
After v1: same 2 files, ~280-320 lines. A small grammar by any standard, even with all T1 features added.

---

## Appendix B — Critical-path summary for the strategist

This appendix is for Task #4 (the strategist), who will sequence implementation.

**Phase 0 (refactor — pre-T1):**
- Symbol-table redesign (audit D2, D6). Required by every T1 feature.
- Structured diagnostics (audit D5). Required for LSP later, and for friendly errors now.

**Phase 1 (T1 — the legitimacy bar):**
- Records (1.1) and Unions (1.2) together. Pattern matching (1.3) immediately after.
- `readonly` Form A (modifier) + Form B (statement) (Section 2). Both are new in v1; the `const`/`imm` migration (§2g) is part of this work. Both lower onto the same `SymbolInfo.isReadonly` flag introduced by the symbol-table redesign.
- Modules with `import` and `pub` (1.5). Most disruptive — touches the CLI and the C backend's emission story.
- Generic functions (1.4). Monomorphization for C.
- `Result` + `?` propagator (1.6). Built on unions, used everywhere.
- `string` as a primitive (1.7). Cleanest small change.
- `@external` FFI (1.9). Built on the import system.

**Phase 2 (T2 — competitive footing):**
- Collections (1.8), package management (1.10), tooling/LSP (1.11), stdlib (1.12), C runtime (1.13).

**Phase 3 (T3 — post-legitimacy):**
- Generic records/unions, subfield `readonly`, transitive immutability (`imm record`), concurrency primitives, effect tracking, advanced pattern features (guards, nested patterns).

Both Form A and Form B are new in v1 (the legacy `const`/`imm` modifiers are migrated away in §2g). The implementation rides on the symbol-table refactor that anchors the whole T1 phase: Form A sets `SymbolInfo.isReadonly = true` at declare time, Form B records the freeze in the local readonly shadow at the statement, both feed the same downstream `isReadonly` check on assignment. The strategist can sequence the whole `readonly` package alongside Records/Unions or after; both are reasonable.

---

End of document.
