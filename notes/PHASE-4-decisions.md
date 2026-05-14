# Phase 4 — Best-Guess Decisions (Python backend)

`./waterfall --target python examples/<X>.wf` now emits Python 3. Every output is
parsed by `python3 -c "import ast; ast.parse(...)"` in `PythonRuntimeCheckTest`.

| # | Decision | Best-guess | Alternatives | Where flagged |
|---|---|---|---|---|
| 1 | Module wrapping | Emit `# module <name>` as the first line; declarations at module scope. No `__all__`, no class wrapping. | Wrap in a class; build a package. | `PythonBackend.emitProgram` |
| 2 | Indentation handling | Each `emit*` returns its node at "indent level 0"; an `indent(text, level)` helper prefixes every non-empty line when placing inside a parent block. | Track an `indent` field that mutates during emission. | `PythonBackend.indent` |
| 3 | Types | Dropped. `int x = 4` → `x = 4`. | Emit type hints (`x: int = 4` and `def fn(a: int) -> int:`). Easy win for phase 7. | `emitTypedVarDecl`, `emitFunctionImpl` |
| 4 | `const`/`imm` | Not enforced — Python lacks compile-time const. Emitted as plain assignments. | Use `typing.Final`; freeze with `types.MappingProxyType`; emit `# const` comment. | `emitTypedVarDecl` (TODO) |
| 5 | Empty function body | Emit `pass`. | Emit `...`. | `emitFunctionImpl` |
| 6 | `for (n in coll)` | `for n in coll:` followed by indented body (or `pass`). | None really — direct mapping. | `emitForBlock` |
| 7 | `if`/`elif`/`else` | Direct `if`/`elif`/`else` mapping. Empty bodies use `pass`. | None — direct mapping. | `emitIfBlock` |
| 8 | Lambdas `(args) ==> body` | `(lambda args: body)` with body = function call. Empty `{}` body → `(lambda args: None)`. | `def __anon(args): return body` then return name; multi-line lambda not possible in Python. | `emitLambda` |
| 9 | String literals | Strip outer backticks, wrap in double quotes. No escape resolution. | Use `repr()` semantics; triple-quote multi-line. | `emitExpression` (STRING_LITERAL branch, TODO) |
| 10 | `null` | Emit `None`. | None. | `emitExpression` (NULL_LITERAL branch) |
| 11 | Bundle literals | Python list `[a, b, ...]`. | tuple `(a, b)`; namedtuple; dataclass. | `emitBundleLiteral` (TODO) |
| 12 | `Module::fn(x)` | `Module.fn(x)`. Assumes `Module` is in scope as a Python module or class. | `from Module import fn`; flat mangling. | `emitFunctionCall` (MODULE branch) |
| 13 | Named arguments `fn(a=1)` | Direct Python `fn(a=1)` — native syntax. | None. | `emitFunctionCall` (named-args branch) |
| 14 | Function separator | Two blank lines between top-level `def`s (PEP-8). | One blank line; no separator. | `emitProgram` |
