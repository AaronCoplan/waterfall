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

The `canBeFormed` example is aspirational — it uses array types (`char[]`,
`int[]`) and method-style calls (`int[].create(26)`) that the grammar does
not yet support. See `notes/AUDIT-OPEN-QUESTIONS.md` for the running list of
gaps and the order they should be filled.
