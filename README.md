# Waterfall

### Setup

1. Ensure Java and Java Compiler are installed
2. Install Java dependencies and build the jar: `./gradlew build`

### Run

Once you've built the runnable jar, you can run the compiler in one of two ways:
1. Execute the jar directly: `java -jar compiler/build/libs/compiler-0.0.1.jar <arguments>`
2. Use the shortcut script: `./waterfall <args>`

You can pick the output language with `--target`:

```
./waterfall --target js  examples/FunctionWithBodyModule.wf
./waterfall --target python examples/FunctionWithBodyModule.wf
./waterfall --target c   examples/FunctionWithBodyModule.wf
```

### Supported Targets

| Target  | Flag              | Notes                                                                  |
|---------|-------------------|------------------------------------------------------------------------|
| Legacy  | `--target legacy` | Original C-like emitter. The default when `--target` is omitted.       |
| JavaScript | `--target js`  | Verified per example with `node --check`.                              |
| Python  | `--target python` | Verified per example with `python3 -c "import ast; ast.parse(...)"`.   |
| C       | `--target c`      | Verified per example with `gcc -fsyntax-only` (warnings suppressed).   |

### Syntax Design

- Assignments: `=`, `:=`, `+=`, `-=`, `*=`, `/=`, `%=`
- Operators: `/`, `*`, `+`, `-`, `%`, `^`
- Increment / Decrement: `++`, `--` (postfix, statement-level)
- Primitive Types: `int`, `dec`, `bool`, `char`
- Control: `if`, `elif`, `else`, `for`, `while`, `in`
- Conditionals / Comparators: `and`, `or`, `equals`, `<`, `>`, `<=`, `>=`
- Modifiers: `const`, `imm`
- Functions: `func`, `returns`
- Containers: `module`
- Casting: `castas`
- Return: `return`
- Indexing: `arr[i]`
- Literals: `[a, b]` (array), `|a, b|` (bundle), `` `text` `` (string)
- Lambdas: `(args) ==> body`
- Calls: `fn(x)`, `Module::fn(x)`, `obj.method(x)`; positional and named args

### Examples

The `examples/` directory contains `.wf` programs exercising each feature.
Per-target expected output is checked in under
`compiler/src/test/resources/golden/<target>/*.expected`.

**Find the sum of all the multiples of 3 or 5 below 1000.**
```
module SumOfMultiples {
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

**Determine whether a string can be formed from the characters in another.**
```
module CanBeFormed {
  func canBeFormed(char[] characters, char[] word) returns bool {
    const asciiOffset := 97
    const letterCounts := int[].create(26)

    for(char c in characters) {
      const charVal := c castas int
      letterCounts[charVal - asciiOffset]++
    }

    for(char c in word) {
      const charVal := c castas int
      letterCounts[charVal - asciiOffset]--
      if(letterCounts[charVal - asciiOffset] < 0) {
        return false
      }
    }

    return true
  }
}
```

### Roadmap

Next-steps work is grouped into four foundation-first phases. Each item links to
a labeled entry in [`notes/AUDIT-OPEN-QUESTIONS.md`](notes/AUDIT-OPEN-QUESTIONS.md),
which records the best-guess we took today and the cleanest fix path.

**Phase 8 — Foundational grammar gaps.** Three small additions that unblock most
downstream work. After these, the `canBeFormed` example above stops being
aspirational and starts to parse.

- ~~`G1`~~ — first-class `true` / `false` literals (deletes the Python identifier case-translation hack). _(closed in phase 8a)_
- ~~`G2`~~ — array types in the grammar (`int[]`, `char[]`); unblocks `C1`, `C4`, and `canBeFormed`. _(closed in phase 8f)_
- ~~`G3`~~ — function-body symbol-table scoping (declare inner vars into their scope). _(closed in phase 8e)_

**Phase 9 — Type system depth.** Builds on phase 8. The verifier graduates from
a primitive-name allowlist to something that actually tracks types through
expressions.

- `G4` — cross-expression type inference (calls, identifiers, arithmetic).
- `G5` — condition type-checking on `if` / `while` / `for` (depends on `G1` + `G4`).
- ~~`G6`~~ — `castas` with array target types (free once `G2` lands). _(closed in phase 8f)_

**Phase 10 — Cross-target semantic decisions.** Each item needs an explicit
design call before per-backend implementation can land — what is a bundle, how
does a C lambda lower, what's the named-arg ABI per target.

- `U1` — bundle semantics (tuple / struct / tagged record).
- `U2` — lambdas in C (lift to static functions in the same translation unit).
- `U3` — named-argument ABI per target.
- `U4` — string-literal escape handling.
- `C1` — C `for...in` lowering (requires `G2`).
- `C3` — C method dispatch (requires class/struct support — the biggest design call).

**Phase 11 — Tooling and polish.** Quality-of-life work that doesn't depend on
the deeper grammar/type lifting in phases 8–10.

- `C2` — per-module C headers so `Module::fn` actually links.
- ~~`C4`~~ — C array literal element-type inference. _(closed in phase 8g)_
- ~~`C5`~~ — demand-driven `#include` emission. _(closed in phase 8c)_
- `C6` — JS module wrapping (pick ESM / CJS / IIFE).
- ~~`C7`~~ — Python `typing.Final` for `const` / `imm`. _(closed in phase 8d)_
- ~~`T1`~~ — non-zero exit codes from `Main.main`. _(closed in phase 8b)_
- `T2` — Gradle 9 deprecation cleanup.

See [`notes/AUDIT-OPEN-QUESTIONS.md`](notes/AUDIT-OPEN-QUESTIONS.md) for the
per-item best-guess and the proposed fix path.

The `canBeFormed` example above is aspirational — it uses array types (`char[]`,
`int[]`) and method-style calls (`int[].create(26)`) that the grammar does not
yet support. Once phase 8 lands it should parse, which is part of why phase 8
comes first.
