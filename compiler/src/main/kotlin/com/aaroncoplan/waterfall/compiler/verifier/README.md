# verifier/

Verifier consumes `*Data` AST nodes, mutates the `SymbolTable`, and returns
a `VerifyResult`. No codegen here.

## Files

- `Verifier.kt` — top-level entry point: `Verifier.verifyModule(ModuleAst, SymbolTable)`
- `VerifyResult.kt` — result type: list of `VerifyError`s; empty = success
- `VerifyError.kt` — typed error sealed class (replaces `VerificationResult`)
- `ModuleVerifier.kt` — two-pass module walk (vars first, then functions)
- `StatementVerifier.kt` — per-statement dispatcher
- `ExpressionVerifier.kt` — expression walk (P11 stub in P10)
- `JoinAnalysis.kt` — branch-join body walker + readonly intersection stub (P12)
- `ErrorRenderer.kt` — renderer interface
- `HumanRenderer.kt` — human-readable renderer (byte-identical strings per §5.2 contract)
- `JsonRenderer.kt` — JSON renderer stub (deferred to §5.3.5 or P13)
