# Waterfall

A single source language that transpiles to **JavaScript**, **Python**, or **C**.
Write Waterfall once; emit code in any supported target.

```
$ ./waterfall --target python examples/FibonacciModule.wf
# module FibonacciModule
def fib(n):
    if (n < 2):
        return n
    return (fib((n - 1)) + fib((n - 2)))
...
```

## Quick start

Requirements: a JDK (17 or 21 has been tested). Gradle and Kotlin are
fetched by the wrapper.

```bash
./gradlew build                                  # build the compiler jar
./waterfall examples/FunctionWithBodyModule.wf   # default: 'legacy' C-like emitter
./waterfall --target js examples/FibonacciModule.wf
./waterfall --target python examples/FibonacciModule.wf
./waterfall --target c examples/FibonacciModule.wf
```

The compiler exits non-zero on any compilation error so it composes cleanly with
shell pipelines.

## Supported targets

| Target  | Flag              | Runtime check                                                          |
|---------|-------------------|------------------------------------------------------------------------|
| Legacy  | `--target legacy` | The original C-like emitter. Default when `--target` is omitted.       |
| JavaScript | `--target js`  | Each example's output passes `node --check`.                           |
| Python 3 | `--target python` | Each example's output passes `python3 -c "import ast; ast.parse(...)"`. |
| C99     | `--target c`      | Each example's output passes `gcc -fsyntax-only` (warnings suppressed).|

The per-target byte-equal expected outputs live under
[`compiler/src/test/resources/golden/<target>/`](compiler/src/test/resources/golden)
and are exercised by `./gradlew test`.

## Language reference

### Modules

Every `.wf` file is one `module`. Top-level declarations are variables or
functions.

```
module MyModule {
    int x = 4
    func double(int n) returns int {
        return n + n
    }
}
```

### Primitive types

`int`, `dec`, `bool`, `char`. Arrays of any primitive: `int[]`, `dec[]`,
`bool[]`, `char[]`.

### Variable declarations

Typed (`type name = expr`), untyped (`name := expr` — type inferred from the
literal kind), and re-assignment (`name = expr` or any of `+= -= *= /= %=`).
Modifiers `const` and `imm` mark a binding immutable; the JS / Python / C
backends translate this to `const` / `typing.Final` / `const` respectively.

```
int x = 4
const dec pi = 3.14159
imm bool ready = true
y := 5            // inferred int
x += 1
```

### Functions

```
func add(int a, int b) returns int {
    return a + b
}

func sideEffect() {        // no return type = void
    doSomething()
}
```

Three call styles:

```
add(1, 2)                  // local
Math::sqrt(2)              // module-qualified
obj.field.method(arg)      // object / chained
```

Both positional and named arguments are supported: `fn(a = 1, b = 2)`.

### Control flow

```
if(cond) { ... } elif(otherCond) { ... } else { ... }
while(cond) { ... }
for(item in collection) { ... }
return                       // bare or `return <expr>`
```

### Expressions

Literals: `42`, `3.14`, `true`, `false`, `NULL`, `` `text` ``, `[1, 2, 3]`,
`|a, b|` (bundle).

Operators in precedence order:

| Operator                         | Notes                                  |
|----------------------------------|----------------------------------------|
| `^`                              | Exponentiation (lowered to `pow()` in C, `**` in JS/Python). |
| `*` `/` `%`                      | Multiplicative.                        |
| `+` `-`                          | Additive.                              |
| `<` `>` `<=` `>=`                | Comparison.                            |
| `equals`                         | Equality. Emits `===` in JS, `==` in C/Python. |
| `and`                            | Boolean and. Emits `&&` / `and`.       |
| `or`                             | Boolean or (loosest). Emits `\|\|` / `or`. |
| `arr[i]`                         | Array indexing.                        |
| `expr castas type`               | Type cast.                             |

Lambdas: `(int x, int y) ==> body` where `body` is a function call. (Statement
bodies are planned — see roadmap.)

### Increment / decrement

Postfix `++` and `--` are statement-level on plain identifiers:

```
i++
counter--
```

### Casting

```
const charVal := c castas int
const asPointer := xs castas dec[]
```

## Examples

Every file under [`examples/`](examples/) compiles on every target. Pick one
and try each backend:

```bash
./waterfall --target js     examples/ArithmeticModule.wf
./waterfall --target python examples/FibonacciModule.wf
./waterfall --target c      examples/ArrayParamsModule.wf
```

A small Fibonacci that exercises functions, recursion, `while`, comparison,
return values, compound assignment, and `++`:

```
module FibonacciModule {
    func fib(int n) returns int {
        if(n < 2) {
            return n
        }
        return fib(n - 1) + fib(n - 2)
    }

    func sumOfFibs(int upTo) returns int {
        int total = 0
        int i = 0
        while(i < upTo) {
            total += fib(i)
            i++
        }
        return total
    }
}
```

The same Euler-style sum-of-multiples that appears in many beginner exercises:

```
module ArithmeticModule {
    func sumOfMultiples(int limit) returns int {
        int total = 0
        int n = 0
        while(n < limit) {
            if(n % 3 equals 0 or n % 5 equals 0) {
                total += n
            }
            n++
        }
        return total
    }
}
```

## Roadmap

What's left to build, ordered foundation-first. The companion file
[`notes/AUDIT-OPEN-QUESTIONS.md`](notes/AUDIT-OPEN-QUESTIONS.md) has the
per-item "best-guess we took today" plus the cleanest fix path.

### Type-system depth

- **`G4`** — Cross-expression type inference. Today `:=` infers from literal
  kind only (INT_LITERAL → `int`, etc.); calls / identifiers / arithmetic fall
  back to `int`. A real inference pass would propagate types through the symbol
  table.
- **`G5`** — Type-check `if` / `while` / `for` conditions as `bool`. Depends on
  `G4`.

### Grammar extensions

Three small additions, useful enough that this README's earlier draft showed
them in a `canBeFormed` example before they were caught as unparseable:

- Typed for-in iterator (`for(char c in chars)`).
- Array-element increment (`arr[i]++` / `arr[i]--`).
- Method calls on type literals (`int[].create(26)`).

### Cross-target semantic decisions

- **`U1`** — Bundle literals `|a, b|`: pick a representation (tuple? struct?
  tagged record?) before each backend can stop emitting list placeholders.
- **`U2`** — C lambdas. Lift the body to a static function in the same TU and
  reference it by name. Requires an AST transform pass.
- **`U3`** — Named arguments: pick an ABI per target. Today JS uses a single
  object literal, Python uses native named args, C drops them.
- **`C1`** — C `for...in` lowering. Today emits a zero-iteration stub with a
  TODO comment. Requires a collection representation decision.
- **`C3`** — C method dispatch (`obj.fn(x)`). Biggest design call: vtables vs.
  function pointers in structs vs. some hybrid.
- **`C6`** — JS module wrapping: pick ESM / CJS / IIFE.

### Tooling

- **`C2`** — Per-module C headers so `Module::fn(x)` actually links across TUs.
- **`T2`** — Sweep the Gradle 9 deprecation warnings.

## Project layout

```
parser/    ANTLR 4 grammar + a small Kotlin frontend that wraps the generated
           lexer/parser. Builds an AST.
compiler/  Kotlin — the verifier, the CodeGenerator interface, and the four
           backend implementations (legacy / js / python / c).
examples/  Working .wf programs, one per feature area.
notes/     Audit decisions and the running list of open questions.
```

The compiler is written in Kotlin 2.0 targeting the JVM 1.8 bytecode. ANTLR
emits Java for the lexer/parser; everything else (front-end, backends, tests)
is Kotlin. `./gradlew build` handles the whole pipeline.
