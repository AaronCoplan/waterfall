# ir/

This package is the IR. Backends consume it (after §5.5 migrates them from `*Data`). Don't add verification logic here.

## Files

- `IrType.kt` — sealed class IrType (mirrors WaterfallType; evolves independently)
- `IrModule.kt` — IrModule, IrFunction, IrTopLevelVariable, IrParameter
- `IrStatement.kt` — sealed class IrStatement and all variants
- `IrExpression.kt` — sealed class IrExpression and all variants (13 kinds)
- `IrLowering.kt` — lowering pass: `IrLowering.lowerModule(ModuleAst, resolvedTypes)`

## F1=C side-table

`IrLowering.lowerModule` takes `resolvedTypes: Map<ExpressionData, WaterfallType>` from
`VerifyResult.resolvedTypes` (populated by `Elaboration` during the verify walk). This
solves the function-scope-lifetime gap: verifier function-body scopes are discarded after
verification; the side-table preserves the resolved types across the scope boundary.
