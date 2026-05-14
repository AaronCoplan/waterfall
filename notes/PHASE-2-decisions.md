# Phase 2 — Best-Guess Decisions

Phase 2 introduced the `CodeGenerator` backend abstraction, the `--target` CLI flag,
and the golden-test harness. The legacy emitter produces byte-identical output to
phase 1 (`diff notes/phase-1-outputs.txt <(./waterfall …)` is empty for every
example).

| # | Decision | Best-guess taken | Alternatives | Where flagged |
|---|---|---|---|---|
| 1 | API shape: one `emit*` method per node vs. AST visitor pattern with `accept` | One `emit*` method per node on `CodeGenerator`; each `*Data` has `translate(backend)` that delegates to the matching `emitX(this)`. | True visitor pattern; pattern matching with instanceof; reflection. | `compiler/.../target/CodeGenerator.java` |
| 2 | Where does `emitProgram` live | On the backend, receives a `ModuleAst` aggregating top-level vars + functions. Each backend decides ordering and wrapping. | Walker in `Main`; backend exposes only fragment-level emits. | `compiler/.../statements/ModuleAst.java`, `LegacyTextBackend.emitProgram` |
| 3 | Where verify() runs | Stays as-is on each `*Data`; `Main` runs the verify pass before calling `backend.emitProgram(...)`. | Move verify onto backend; remove from data classes. | `compiler/.../Main.java` |
| 4 | Backend registry shape | Hash-map `LinkedHashMap<String, Supplier<CodeGenerator>>` in `Backends.java`. Phase 3+ adds one line per target. | ServiceLoader / SPI; reflection scan; enum. | `compiler/.../target/Backends.java` |
| 5 | Unknown `--target` behavior | Returns null from `forTarget`, ArgParser produces an error message, `Main` prints it to stderr and returns. | `argparse4j` `choices()`; exit code 2. | `compiler/.../argumentparsing/ArgParser.java` |
| 6 | Non-zero exit on error | Not implemented yet — `Main.main` returns normally regardless. `System.exit(1)` would break `EndToEndSmokeTest` which calls `Main.main` directly. | Throw a runtime exception in `Main` and have a thin wrapper that catches and exits; refactor the smoke test to spawn a subprocess. | `compiler/.../Main.java` (TODO note) |
| 7 | Routing of compiler diagnostic messages | Moved from `System.out.format` (which polluted the translation output) to `System.err.format`. | A diagnostic sink; structured JSON output. | `compiler/.../Main.java` |
| 8 | Where goldens live | `compiler/src/test/resources/golden/<target>/<example>.expected`. Working dir for the JUnit run is `compiler/`, so `../examples/` resolves correctly. | A top-level `tests/` directory; per-module split. | `GoldenTests.cases()` |
| 9 | Regenerating goldens | `UPDATE_GOLDEN=1 ./gradlew test --tests GoldenTests` writes whatever the compiler currently emits. Run after every backend change. | A separate `:updateGoldens` Gradle task. | `GoldenTests.matchesGolden` |
| 10 | Container.java | Deleted. Replaced by `LegacyTextBackend.emitProgram` which assembles the same headers/declarations/functions structure. | Keep as a backend-side helper. | (deletion) |
