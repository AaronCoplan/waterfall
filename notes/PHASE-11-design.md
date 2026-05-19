# PHASE 11 — Verifier closes the P10 carry-forward gaps

Author: language-designer (Phase 11 design)
Date: 2026-05-19
Status: design spec, ready for pre-review skeptic + AI-augmented implementation
Predecessor exit tag: `phase-10-complete` (= `f2feab9`)
Inputs: `notes/PHASE-10-design.md` (load-bearing template + 13 PITFALLs), `IMPLEMENTATION-LOG.md` (Phase 10 retrospective + sub-task 5.5 silent behavior changes), `notes/team-output/04-strategy.md` §3 P11, `notes/team-output/00-EXECUTION-PLAYBOOK.md` §1+§3, the actual verifier/ + ir/ + symboltables/ source in `compiler/src/main/kotlin/`.

---

## Read this first

This document is the spec for Phase 11. P10 completed a foundation refactor with zero observable language changes. P11 is the **first phase since P9 to introduce behavioral surface changes**: the verifier starts catching programs that today compile silently. The P10 retrospective and the sub-task §5.3–§5.5 carry-forwards enumerate the exact gaps P11 closes; this spec sequences and disambiguates them.

P11 is **smaller and more focused than P10**. P10 was an interface migration; P11 is closing specific TODOs left behind. The strategy roadmap budgets P11 at **≤1 week calendar (AI-augmented)** and rates it "mechanical implementation against a clear spec — exactly the regime where AI compresses 10–30×" (`04-strategy.md` §3 P11). That budget holds only if this spec is precise. The §6 escalation list is where it can't be — surface those to Aaron.

If you are the AI implementer: read §5 PITFALLs before writing any code in the affected section. Read §6 ESCALATE items and **stop** if you hit one — do NOT silently resolve. The single most load-bearing decision in P11 is **whether golden tests change** (see §7.2). Get that wrong and you have made PITFALL #13 worse, not better.

---

## §0 Phase 11 kickoff

- **Strategist roadmap entry:** `notes/team-output/04-strategy.md` §3, P11.
- **Predecessor phase exit tag:** `phase-10-complete` @ `f2feab9`.
- **Spec at implementation start:** `notes/PHASE-11-design.md` at the commit produced by this design session, plus any pre-review skeptic edits that land before sub-task §4.1 begins.
- **Phase budget:** ≤1 week calendar (AI-augmented). Sub-tasks below total ~4 commits worth of production code + ~3 commits of tests + ~1 commit of process trial. Plan-mode iterations expected to average 1.5 per sub-task (per P10 retrospective baseline).
- **Pre-review skeptic session:** REQUIRED before §4.1 begins. Demand ≥3 material findings — the spec is denser than P10's §5.2 by design (more behavioral consequences per line). Fold findings into spec edits, then commit + tag a checkpoint before implementation.
- **Open ambiguities entering implementation:** see §6. None of them should be silently resolved.
- **Verification-design commitment (§7 triad):**
  - **Leg 1 — Property invariants** at N=10000: identifier-resolution soundness (verifier rejects iff Elaboration's side-table maps to VoidType for an identifier name); BinaryOp.type comparison invariant (for all left/right type pairs and ops in the comparison family, result type is Bool); per-expression source-position threading invariant (every IR expression node's `sourcePosition` equals the parent statement's position OR a source-derived position — but never a placeholder).
  - **Leg 2 — Differential oracle:** 22 example programs × 3 backends = 66 parameterized golden test cases. **In P11 specifically, the gate is conditional**: some goldens are *expected* to change (those whose examples relied on the silent-pass-through-as-Void gap; see §7.2 for the enumerated set). All other goldens must remain byte-identical. New `scripts/check-goldens-unchanged-except-p11.sh` enforces the conditional gate, listing the exact allowed-to-change files. Per-commit golden gate strategy: see §7.2.
  - **Leg 3 — Adversarial fixtures** at `compiler/src/test/resources/adversarial/phase-11/sub-task-N.M/`. Target ≥20 entries per sub-task that introduces a new VerifyError variant (§4.1, §4.2, §4.3). Mix of positive (must verify) + negative (must reject with the new variant). §4.4 (process trial + relocations) gets ≥10 entries.

### Phase-budget impact of behavioral surface changes

P10 PITFALL #13 was "goldens unchanged is the honest test." That rule **inverts** for P11 because closing OQ-3=C and OQ-5.4-1 means the verifier rejects programs the goldens previously accepted. The §7.2 design accepts this inversion DELIBERATELY and lists which goldens change, why, and what their new shape is. This is **not** a PITFALL #13 violation — it is the explicit closure of a P10 documented gap. The phase-exit checklist (§7.6) requires that every golden-diff in P11 corresponds to a documented gap closure.

---

## §1 Executive summary — Why P11, why now, why this scope

### 1.1 What P11 solves

P11 closes the six P10 carry-forward gaps + two skeptic findings + one process improvement:

| # | Carry-forward | Closes | Source |
|---|---|---|---|
| 1 | `VerifyError.UnknownIdentifier` variant | OQ-3=C identifier-resolution gap | P10 retrospective; sub-task §5.3 outcome |
| 2 | `Elaboration` no longer stores `VoidType` for undeclared names; the verifier rejects them earlier | OQ-5.4-1 silent-pass-through | sub-task §5.4 OQ-5.4-1 resolution; cross-reference: `IrLowering.kt:187-192` M7 fix |
| 3 | `BinaryOp.type` becomes `IrType.Bool` for comparison/equality ops; otherwise inferred (int+int=int, int+dec=dec) | BinaryOp.type placeholder | P10 retrospective; cross-reference: `IrExpression.kt:60` KDoc + `IrLowering.kt:209` |
| 4 | `verifyExpression` walks lambda bodies | Lambda body not walked | P10 retrospective; cross-reference: `ExpressionVerifier.kt:29` TODO |
| 5 | `forBlock` iterator type inferred from collection | forBlock iterator hard-coded IntType | P10 retrospective; cross-reference: `StatementVerifier.kt:140-146` R2 + `Elaboration.kt:113-118` |
| 6 | `ExpressionData.sourcePosition` field; threaded through every node | PITFALL #8 (per-expression positions) | P10 retrospective; cross-reference: `IrLowering.kt:88-91` R2 TODO |
| 7 | `ExpressionVerifier` validates `castas T` target resolves to a known type | Skeptic Finding 2 (RISK): Main.kt ISE escape | P10 retrospective; cross-reference: `IrType.kt:50-53` ISE + `Main.kt` no catch |
| 8 | `StringLiteralText` relocates from `statements/` to `ir/util/` | Skeptic Finding 3 (MINOR): package boundary | P10 retrospective |
| 9 | Skeptic seed cross-check process trial | Skeptic Finding 5: distinguish template-anchoring from genuine calibration | P10 retrospective |

### 1.2 Why not P12 (sum types + match) first?

The strategist roadmap (`04-strategy.md` §3 P11) places P11 before P12 for three reasons that the P10 retrospective amplifies:

1. **P12 grammar lands the `readonly x` statement and `match` expression.** Both depend on a verifier that can resolve identifiers and check types — exactly what P11 ships. Starting P12 first means the new `match` exhaustiveness checker would be built on top of the OQ-3=C gap, propagating the silent-pass-through pattern into a much larger surface area.
2. **P10 left specific TODOs.** Every one of the nine items above is a TODO marker in code (search: `TODO(P11)` finds 4; `TODO(audit)` finds 3 more in `UntypedVariableDeclarationAndAssignmentData`; PITFALL #8 surfaces twice in `IrLowering.kt`). The discipline says: close the TODOs you have before opening new ones. Starting P12 with these still open compounds debt.
3. **The skeptic seed cross-check (§4.4) needs a real sub-task to trial.** Running the trial on the §5.6 deletion-only sub-task of P10 would have produced a trivial finding set — not enough surface area to distinguish template-anchoring from genuine calibration. P11's §4.1 (new VerifyError variants + walker skeleton) is the right size to trial against.

### 1.3 Success criteria for P11

- Every sub-task in §4 lands; the production-code commits show the expected closures.
- The verifier rejects programs it previously accepted in **exactly the documented set** (see §7.2 enumeration). It does not over-reject (no programs that should compile now fail).
- The 60 unchanged golden test cases pass (22 programs × 3 backends = 66 total, minus 6 documented-to-change); the documented-to-change goldens land in the same commit as their causing code change (no "fix forward later" — the F10 cross-tree drift rule applies).
- The skeptic seed cross-check ran for §4.1; the per-seed finding overlap was logged in `IMPLEMENTATION-LOG.md`; the process determination (template-anchoring vs. genuine) was made.
- `Main.kt` no longer leaks `IllegalStateException` from `IrLowering` for any verified-as-successful module (cast-target validation closes the only known path; skeptic Finding 2 closed).
- `StringLiteralText` no longer imports from `statements/` in any backend file (skeptic Finding 3 closed).

### 1.4 What P11 explicitly does NOT do

To prevent scope creep — Aaron's gate at plan-mode time:

- **No type inference for arithmetic.** `int + dec → dec` lands in P11's BinaryOp.type table (§4.3) but the verifier does not yet *reject* `bool + int`. Rejection is full type-checking, which is P11 in the strategy doc but the §4.3 scope below holds it to "infer the type for IR consumers; do not yet reject mismatches." P11's verifier rejects identifier errors and known structural errors (cast target unknown, void-in-value-position). Arithmetic-mismatch rejection is deferred — see §6 ESCALATE OQ-11.4.
- **No condition `bool`-check.** `if(n)` where `n: int` continues to verify in P11. Rejecting non-bool conditions is the next half of `04-strategy.md` §3 P11's deliverable list ("Reject non-bool conditions"). **Aaron decides at plan-mode time** whether condition-bool checking lands in §4.3 or in a follow-up P11b. See §6 ESCALATE OQ-11.5.
- **No call-site argument type checking.** Same reasoning — `04-strategy.md` says P11 closes this, but the scope is large (variadic args, named args, lambda args) and a half-implementation is worse than none. See §6 ESCALATE OQ-11.5.
- **No friendly-error rendering changes.** P10's HumanRenderer covers the new VerifyError variants by extending the `when` block (mechanical). The full Elm/Roc-style friendly errors are P13.
- **No P12-deferred work creeps in.** `readonly x` statement, `match`, sum types — none of these. If you find yourself wanting to add any of them, stop. P12.
- **No removing `WaterfallType.fromSourceText`.** That bridge stays. §7.5 of P10 notes its removal is "P11+" work; in P11 we keep it because it's load-bearing for `UntypedVariableDeclarationAndAssignmentData.inferType()` which P11 does not refactor.

The narrow scope is the value. Mike-Test-#2 in `04-strategy.md` (idiomatic-output polish) absorbed +2–3 weeks into P14 because the niche demands it; P11 is the *opposite* — every feature pulled out of P11 is a feature P12 inherits cleanly.

---

## §2 Changes in the verifier/ package

### 2.1 New `VerifyError` variants

P10's `VerifyError.kt` ships 7 variants in WF1xxx range. P11 adds 4 in the WF11xx and WF12xx range:

| Code | New variant | Carry-forward | Section |
|---|---|---|---|
| `WF1201` | `UnknownIdentifier` | #1 OQ-3=C | §2.2 |
| `WF1202` | `CastTargetUnknown` | #7 skeptic Finding 2 | §2.3 |
| `WF1203` | `LambdaParameterShadowing` | #4 lambda body verification (subset of duplicate-decl pattern, but specialized so the renderer can mention the lambda context) | §2.4 |
| `WF1204` | `ForIteratorShadowing` | #5 (when the iterator name collides with an enclosing scope's binding) | §2.5 |

**OQ-11.0** (resolved-in-spec): The decision to introduce `LambdaParameterShadowing` and `ForIteratorShadowing` as distinct variants rather than re-using `DuplicateDeclaration` is intentional: the renderer must mention "lambda parameter" / "for-loop iterator" specifically. The §5.2 `DuplicateDeclaration` message ("Duplicate declaration: $name") is correct but unhelpful when the source is a one-character `x` in `for(x in xs)`. **Skeptic note**: this resolution is the kind of finding the §4.4 cross-check should surface independently. If it doesn't, the process is template-anchored.

#### Code allocation update

Update `notes/PHASE-10-design.md` §4.8 "Code allocation" table (or this spec's appendix references it) to include the WF12xx range. The WF11xx range remains: `WF1101` UnknownType, `WF1102` DuplicateDeclaration, `WF1103` VoidNotAValueType. Use WF12xx for P11-era *identifier-resolution* and *cast-target* errors; reserve WF13xx for P11's type-checking work (currently deferred per §1.4). When P12 lands, its codes go into WF2xxx as already documented.

### 2.2 `UnknownIdentifier` — closes OQ-3=C

The current `verifyVarAssignment` and `verifyIncrement` silently no-op on unknown LHS (`StatementVerifier.kt:97-98, 112-113`). Elaboration walks expression sub-trees but stores `VoidType` rather than absent for unknown identifier references (`Elaboration.kt:149-153`). `IrLowering` reads the side-table and lowers undeclared identifiers to `IrExpression.Identifier(name, IrType.Void)` (`IrLowering.kt:184-193`). P11 closes the gap at the verifier level so the side-table never stores VoidType for an identifier; lowering never lowers `IrType.Void` for an identifier; backends never emit broken code from an unverified program.

#### `VerifyError.UnknownIdentifier`

```kotlin
/**
 * An identifier reference (in an expression or as the LHS of an assignment/increment)
 * could not be resolved against the current scope chain. Closes OQ-3=C (P10 carry-forward).
 *
 * Error code WF1201.
 *
 * **Field semantics**:
 * - [name]: the identifier text as it appeared in source.
 * - [context]: a short string naming the syntactic context — see [Context] below.
 * - [primaryPosition]: the source position of the identifier (NOT the enclosing statement).
 *   In P11, this is the statement's position unless §2.6 per-expression-position work
 *   has landed; see §3.2 for the precise position-threading rule.
 *
 * **OQ-11.1**: when an UnknownIdentifier is encountered in a function call's argument
 * list, this variant is emitted for the offending argument. Whether the rest of the
 * arguments are still verified (best-effort) or short-circuited (first-fail) is an
 * ESCALATE decision — see §6 OQ-11.1.
 */
data class UnknownIdentifier(
    val name: String,
    val context: Context,
    override val primaryPosition: SourcePosition
) : VerifyError() {
    override val code = "WF1201"
    override val message: String = when (context) {
        Context.EXPRESSION         -> "Unknown identifier '$name'"
        Context.ASSIGNMENT_LHS     -> "Cannot assign to undeclared identifier '$name'"
        Context.INCREMENT_TARGET   -> "Cannot increment undeclared identifier '$name'"
        Context.FOR_COLLECTION     -> "Unknown collection identifier '$name' in for-loop"
        Context.ARRAY_INDEX_TARGET -> "Unknown array identifier '$name'"
    }

    enum class Context {
        /** Identifier appearing in any expression position (right-hand side of `=`, condition, arg, etc.). */
        EXPRESSION,
        /** Identifier appearing as the LHS of `x = ...` / `x += ...`. */
        ASSIGNMENT_LHS,
        /** Identifier appearing as the target of `x++` / `x--`. */
        INCREMENT_TARGET,
        /** Identifier appearing as the collection in `for(x in collection)`. */
        FOR_COLLECTION,
        /** Identifier appearing as the array target in `arr[index]`. */
        ARRAY_INDEX_TARGET,
    }
}
```

#### Emission sites

The new variant is emitted from:

1. **`StatementVerifier.verifyVarAssignment`** — replace the silent `?: return emptyList()` at line 97 with `?: return listOf(VerifyError.UnknownIdentifier(s.name, Context.ASSIGNMENT_LHS, s.getSourcePosition()))`.
2. **`StatementVerifier.verifyIncrement`** — same pattern at line 112.
3. **`ExpressionVerifier.verifyExpression`** — new walker (§2.6) emits this for IDENTIFIER / FUNCTION_CALL.LOCAL / ARRAY_INDEX / FOR_COLLECTION sub-expressions whose name doesn't resolve via `scope.lookup(name)`.
4. **`StatementVerifier.verifyForBlock`** — when the collection name doesn't resolve, emit `Context.FOR_COLLECTION`. **OQ-11.6** (ESCALATE): see §6 — should `verifyForBlock` reject undeclared collections, or accept them as P10 does? Today the `for(item in things)` example compiles; closing this would break `ControlFlowModule.wf`.

#### Elaboration interaction

`Elaboration.elaborateExpression` for IDENTIFIER (`Elaboration.kt:149-153`) currently writes `WaterfallType.VoidType` when `scope.lookup(name)` returns null. P11 must change this. The post-P11 contract:

- The verifier walks expressions FIRST (in `verifyStatement` / `verifyExpression`), emits `UnknownIdentifier` for any name that fails to resolve. The verifier's error list accumulates.
- Elaboration walks expressions SECOND, with the now-validated tree. When an identifier still fails to resolve at Elaboration time (which should never happen if the verifier rejected it earlier), Elaboration **throws `IllegalStateException`** rather than writing VoidType. The exception message: `"Elaboration: unresolved identifier '$name' after verifier should have rejected it; verifier+elaboration drift"`.
- `IrLowering.kt:187-192` (the M7 fallback that reads VoidType) becomes UNREACHABLE for identifier nodes. **P11 implementation must verify this**: add an assertion in `IrLowering.lowerExpression` for `Kind.IDENTIFIER` that throws if `resolvedTypes[expr]` is `IrType.Void` (use Elaboration's identity-keyed map; the previous "fallback to Void" was tolerating the gap). **OQ-11.2** (resolved-in-spec): the assertion fires only for IDENTIFIER nodes — VoidType continues to be valid for `NullLiteral`, lambda return type, etc. The assertion message names the gap: `"IrLowering: identifier '$name' resolved to Void after P11; ModuleVerifier failed to emit UnknownIdentifier"`.

#### Ordering: verifier first, elaborator second — and the "single-walk" alternative

P10's `ModuleVerifier.verifyFunctionDeclaration` (`ModuleVerifier.kt:104-107`) interleaves verifier and elaboration **per statement**:
```kotlin
for (stmt in f.statements) {
    errors += StatementVerifier.verifyStatement(stmt, functionScope)
    Elaboration.elaborateStatement(stmt, functionScope, resolvedTypes)
}
```

P11 has TWO options:

- **Option A (recommended) — Keep interleaved walk; add identifier validation INTO Elaboration.** The verifier walks declarations + statement-level checks; elaboration walks expressions and emits `UnknownIdentifier` into the same error list (passed as `MutableList<VerifyError>`). The verifier's own error list and elaboration's error list merge into the final `VerifyResult.errors`. This requires a minor `Elaboration` API change: `elaborateStatement` now takes `MutableList<VerifyError>` and writes into it.
- **Option B — Add a third pass.** Verifier walks, then a new `ExpressionWalker` walks, then Elaboration walks. Three passes is over-engineering for the current shape.

**Recommendation: Option A.** The two passes are already co-walking; Elaboration already has the scope context to do `scope.lookup`. Promoting elaboration to also emit verifier errors is a natural extension. This avoids a third pass AND lets the elaboration's existing scope walks (which are correct — they replay the verifier's scope shape) drive the identifier validation. The cost is a small API change to Elaboration.

**OQ-11.3** (ESCALATE — see §6): is "promote Elaboration to emit VerifyError" the right boundary? An alternative is to give `ExpressionVerifier` real teeth (it's currently a stub at `ExpressionVerifier.kt:24-29`) and route Elaboration's identifier checks through it. Both are defensible. Aaron decides at plan-mode time.

### 2.3 `CastTargetUnknown` — closes skeptic Finding 2 RISK

P10's `IrType.fromWaterfallType` throws `IllegalStateException` when given an `ErrorType` (`IrType.kt:50-53`). `Main.kt:39-46` does NOT catch `IllegalStateException`, so a `castas <unknown-type>` expression that passes verification today escapes as a raw JVM stack trace to the user. Sub-task §5.5 documented this as an intentional silent behavior change ("Cast to unknown user type now crashes (was: soft TODO render)"). Skeptic Finding 2 flagged it as RISK: the ISE leaks JVM internals to a user error.

#### `VerifyError.CastTargetUnknown`

```kotlin
/**
 * A `castas T` expression names a type T that doesn't resolve. Closes skeptic
 * Finding 2 from the P10 retrospective. P10 path: WaterfallType.fromSourceText
 * returned ErrorType; IrType.fromWaterfallType threw IllegalStateException at
 * lowering time; Main.kt let the ISE escape as a raw stack trace.
 *
 * Error code WF1202.
 *
 * **Field semantics**:
 * - [targetText]: the source text of the cast target type, byte-identical to ErrorType.sourceText.
 * - [primaryPosition]: the source position of the CAST expression (NOT the operand).
 *   In P11, this is the enclosing statement's position until §2.6 per-expression positions land.
 */
data class CastTargetUnknown(
    val targetText: String,
    override val primaryPosition: SourcePosition
) : VerifyError() {
    override val code = "WF1202"
    override val message = "Cast target '$targetText' is not a recognized type"
}
```

#### Emission site

`ExpressionVerifier.verifyExpression` — for `ExpressionData.Kind.CAST`, check that `WaterfallType.fromSourceText(expr.castTargetType ?: "void")` does NOT return an `ErrorType`. If it does, emit `CastTargetUnknown`. Recurse into the operand. Also emit `CastTargetUnknown` when `fromSourceText(...)` returns `VoidType` (the `castas void` case — per OQ-11.14, void is not a castable value type). The message is `"Cast target 'void' is not a value type"`.

#### Lowering interaction

Once the verifier emits `CastTargetUnknown`, `Main.kt` short-circuits via the existing `if (!verifyResult.isSuccessful) throw CompilerError(...)`. `IrLowering` is never called for verified-as-failed modules. The ISE in `IrType.fromWaterfallType` becomes structurally unreachable for the cast-target case. The ISE itself stays in place — it remains the right behavior if something else regresses (e.g., a future variant forgets to call the verifier first). **R1 precondition** documented at `IrLowering.kt:32-34` already says this.

**MINOR sub-decision** (resolved-in-spec): the ISE itself becomes UNREACHABLE in production code paths *only* once cast targets are checked. Other code paths that could reach the ISE — `WaterfallType.fromSourceText` returning ErrorType from a leading `?` (nullable syntax) or other syntactically-rejected types — must also be checked. P11 audits the call sites:

| Call site | What it parses | P11 status |
|---|---|---|
| `WaterfallType.fromSourceText(v.type)` in `IrLowering.lowerModule` (line 46) — top-level vars | The declared type | **already verifier-checked** via `verifyTypedVarDecl` UnknownType emission |
| `WaterfallType.fromSourceText(arg.firstVal)` in `IrLowering.lowerModule` (line 58) — function parameters | The parameter type | **already verifier-checked** via `verifyFunctionDeclaration` UnknownType emission at `ModuleVerifier.kt:90-92` |
| `WaterfallType.fromSourceText(stmt.type)` in `IrLowering.lowerStatement` (line 95) — typed var decls | The declared type | **already verifier-checked** |
| `WaterfallType.fromSourceText(stmt.inferredType)` in `IrLowering.lowerStatement` (line 102) — untyped var decls | The inferred type ("int", "dec", "char", or "int" fallback) | Never ErrorType in practice (the inferType() helper at `UntypedVariableDeclarationAndAssignmentData.kt:20-29` produces only valid primitive names). Documented invariant. |
| `WaterfallType.fromSourceText(expr.castTargetType ?: "void")` in `IrLowering.lowerExpression` line 216 — cast targets | The cast target type | **P11 closes via CastTargetUnknown** |
| `WaterfallType.fromSourceText(arg.firstVal)` in `IrLowering.lowerExpression` line 259 — lambda parameters | The parameter type | **P11 closes via lambda body verification** (§2.4 below also adds parameter-type validation) |
| `WaterfallType.forReturnType(f.returnType)` in `IrLowering.lowerModule` line 65 — function return type | The return type | **already verifier-checked** via `verifyFunctionDeclaration` at `ModuleVerifier.kt:81-83` |

All call sites either are already checked or are closed by §2.3 (cast) + §2.4 (lambda). **PITFALL note**: if a P12 work item adds a new `WaterfallType.fromSourceText` call site in IR lowering without adding a corresponding verifier check, the ISE becomes reachable again. The §7 phase-exit checklist requires a grep for `fromSourceText` in `target/` and `ir/` to confirm zero new call sites.

### 2.4 Lambda body verification — closes carry-forward #4

`ExpressionVerifier.kt:24-29` is a P10 stub (`emptyList()` for all expressions). Lambda bodies' sub-expressions are walked by Elaboration (`Elaboration.kt:213-229`) but Elaboration does not produce verifier errors for them. P11 wires the verifier to walk lambda bodies properly.

#### What "lambda body verification" means

A `LambdaFunctionData` has:
- `typedArguments: List<parser.Pair<String, String>>` — type/name pairs.
- `body: FunctionCallData?` — single function call expression OR null for empty body `{}`.

The verifier checks:
1. **Lambda parameter types are known.** Each `typedArgument.firstVal` (the type text) must satisfy `PrimitiveTypes.isPrimitiveOrArray`. If not, emit `VerifyError.UnknownType` with `primaryPosition = LAMBDA_POS` from `Elaboration.kt:45` — until §2.6 per-expression positions land, use a synthesized `SourcePosition("lambda-expr", 0, 0)` (same as existing Elaboration). **OQ-11.7** (ESCALATE): is `LAMBDA_POS` acceptable, or must P11 thread the parent statement's position? See §6.
2. **Lambda parameter names don't shadow enclosing-scope names.** This is already enforced by `SymbolTable.declare`'s anti-shadowing rule (`SymbolTable.kt:91-114`). When the lambda's parameter declaration fails, emit `LambdaParameterShadowing` with the parameter name + a position field. **Sub-decision** (resolved-in-spec): re-use `DuplicateDeclaration` rather than a new variant? **NO** — the renderer needs to say "lambda parameter '$name' shadows enclosing scope". A dedicated variant is small + clean.
3. **Lambda body sub-expressions are walked.** The body is a `FunctionCallData`. Each `positionalArgument` and `namedArgument.secondVal` is an `ExpressionData` that gets `verifyExpression` called on it against the lambda's local scope (params declared, parent scope visible).
4. **Lambda body's called function name resolves OR is acceptably-undeclared per OQ-11.6 policy.** See §6 — this is the same FOR_COLLECTION question for FUNCTION_CALL.LOCAL.

#### `VerifyError.LambdaParameterShadowing`

```kotlin
/**
 * A lambda parameter name shadows a name already declared in an enclosing scope.
 * Closes carry-forward #4 — lambda body now walked by verifier. Distinct from
 * [DuplicateDeclaration] so [HumanRenderer] can render the lambda context.
 *
 * Error code WF1203.
 */
data class LambdaParameterShadowing(
    val name: String,
    val previousPosition: SourcePosition?,
    override val primaryPosition: SourcePosition
) : VerifyError() {
    override val code = "WF1203"
    override val message = "Lambda parameter '$name' shadows enclosing scope binding"
}
```

#### Emission site

`ExpressionVerifier.verifyExpression` — for `ExpressionData.Kind.LAMBDA`:
- Open a child scope (`scope.enterScope()`).
- For each typed argument: validate type (UnknownType if not primitive/array); declare into child scope (LambdaParameterShadowing if `DeclareResult.Failure`).
- Walk the body (`expr.lambda?.body`) — its positional + named arguments via `verifyExpression(..., lambdaScope)`.
- Exit child scope.

### 2.5 `ForIteratorShadowing` + iterator type inference — closes carry-forward #5

P10's `verifyForBlock` (`StatementVerifier.kt:138-151`) declares the iterator as `WaterfallType.IntType` (R2 fix per skeptic). P11 infers the iterator's type from the collection:

| Collection's resolved type | Iterator's type |
|---|---|
| `IntType` (not an array — e.g., source has `for(x in n)` where `n: int`) | `IntType` (P10 behavior preserved — implicit-int loops) |
| `ArrayType(IntType)` | `IntType` |
| `ArrayType(DecType)` | `DecType` |
| `ArrayType(BoolType)` | `BoolType` |
| `ArrayType(CharType)` | `CharType` |
| `ArrayType(ArrayType(_))` | `ArrayType(_)` (the inner element type — P10 grammar doesn't support nested arrays explicitly but the type system can represent it) |
| `VoidType` (collection name not in scope per OQ-11.6 policy) | `VoidType` (defer to OQ-11.6 — collection unresolved is the issue, not iterator) |
| Anything else | `VoidType` placeholder; emit no error (the issue is the collection, not the iterator) |

The iterator variable is declared into the body scope per existing R2 fix. Its `SymbolInfo.type` is now the inferred type rather than always `IntType`.

#### `VerifyError.ForIteratorShadowing`

When the iterator name collides with an enclosing scope binding, the existing `SymbolTable.declare` returns `DuplicateDeclaration.Failure`. P11 catches this and converts to `ForIteratorShadowing` with the iterator's source position:

```kotlin
data class ForIteratorShadowing(
    val name: String,
    val previousPosition: SourcePosition?,
    override val primaryPosition: SourcePosition
) : VerifyError() {
    override val code = "WF1204"
    override val message = "For-loop iterator '$name' shadows enclosing scope binding"
}
```

**OQ-11.8** (resolved-in-spec): the choice to introduce a dedicated variant rather than `DuplicateDeclaration` mirrors §2.4's reasoning — the renderer needs context. The skeptic seed cross-check (§4.4) should validate this is the right call.

### 2.6 Per-expression source positions — closes PITFALL #8 partially

Today, every `IrExpression` sub-tree under a statement carries the *statement's* source position (per `IrLowering.kt:87-91` — the R2 comment names this). P11 adds `ExpressionData.sourcePosition: SourcePosition` and threads it through.

#### `ExpressionData` change

```kotlin
class ExpressionData(filePath: String, ctx: WaterfallParser.ExpressionContext) {
    // ... existing fields ...
    @JvmField val sourcePosition: SourcePosition  // NEW: per-expression position
    
    init {
        // NEW: derive sourcePosition from ctx.start
        sourcePosition = SourcePosition(
            filePath,
            ctx.start.line,
            ctx.start.charPositionInLine
        )
        // ... existing init body ...
    }
}
```

The constructor stays a single ANTLR-context arg + filePath; the new field is derived from `ctx.start`. No new constructor parameters; no breaking change to the `TranslatableStatement.kt` parent-class pattern.

#### Threading through IrLowering

`IrLowering.lowerExpression` currently takes a `fallbackPos: SourcePosition` parameter; that parameter goes AWAY in P11 because the position now lives on `ExpressionData` itself:

```kotlin
// BEFORE (P10):
private fun lowerExpression(
    expr: ExpressionData,
    resolvedTypes: Map<ExpressionData, WaterfallType>,
    fallbackPos: SourcePosition  // statement-level fallback
): IrExpression { ... }

// AFTER (P11):
private fun lowerExpression(
    expr: ExpressionData,
    resolvedTypes: Map<ExpressionData, WaterfallType>
): IrExpression { 
    val pos = expr.sourcePosition  // per-expression position from ANTLR context
    // ... use pos in every IrExpression constructor ...
}
```

All call sites in `lowerStatement` drop the `pos` argument. All call sites in `lowerFunctionCall` drop the `pos` argument. All sub-expression `lowerExpression` calls drop the `fallbackPos` argument.

#### Threading through ExpressionVerifier

Same pattern. `ExpressionVerifier.verifyExpression` reads `expr.sourcePosition` for the `primaryPosition` field of any emitted error (replacing the statement-level fallback). This is what makes `UnknownIdentifier`, `CastTargetUnknown`, etc., actually report the correct location.

#### What "partial" means

This closes PITFALL #8 for **expression nodes**, but NOT for **function argument positions** (which are a separate ripple: `FunctionImplementationData.typedArguments` is `List<parser.Pair<String, String>>` with no positions). The function-arg case was deferred in P10 §5.2 ("TODO(P10): per-arg source positions blocked on typedArguments record migration"). P11 closes the expression case, but **deliberately defers the function-arg case**: the typedArguments record migration ripples through three backends + LambdaFunctionData (per §7.3 of P10 spec) and would explode P11's scope. The `TypedArgument(name, type, sourcePosition)` data class proposed in P10 §7.3 stays unbuilt in P11. **OQ-11.9** (resolved-in-spec): function-arg positions remain `f.getSourcePosition()` per P10 § 5.2 + `LAMBDA_POS` per Elaboration; closing this is P12 or later.

### 2.7 Migration of `Elaboration` and `verifyResult.resolvedTypes`

`Elaboration` continues to populate `resolvedTypes` for use by `IrLowering`. Three changes:

1. **Identifier resolution writes the resolved type, never VoidType-for-undeclared.** Per §2.2, the verifier emits `UnknownIdentifier` before Elaboration encounters the undeclared name. When Elaboration encounters one anyway (drift bug), it throws ISE (§2.2 contract).
2. **BinaryOp gets a side-table entry** (it didn't in P10 per `Elaboration.kt:191-194` R4 note). P11 needs the entry because the inferred type (Bool for comparisons; numerical promotion otherwise) is computed at verify time and read by lowering. The R4 deferral was the right call in P10 (placeholder type meant the entry was dead code); in P11 it isn't.
3. **forBlock writes the iterator's inferred type.** Today, Elaboration declares the iterator as IntType (`Elaboration.kt:113-118`). P11 inspects `scope.lookup(s.collectionName)?.type` and derives the iterator type per §2.5.

**OQ-11.10** (resolved-in-spec): the `resolvedTypes` map is now a write-target for the verifier (via Elaboration), and a read-source for IR lowering. The contract — keyed by `ExpressionData` identity — is unchanged. The key set grows: P10's R4 said "BinaryOp's sub-expressions get entries but BinaryOp itself does not"; P11 says BinaryOp itself gets an entry. **Defensive coding**: `IrLowering.lowerExpression` for `BinaryOp` should read `resolvedTypes[expr]` and fall back to `lIr.type` only when the entry is absent (Elaboration drift). The fallback is the P10 behavior; the new behavior reads from the side-table when available. The assertion at IDENTIFIER for VoidType (§2.2) does NOT apply to BinaryOp — Bool is a valid result type.

---

## §3 Changes in IR + IrLowering

### 3.1 BinaryOp.type inference table

The full table:

| Operator | Operator family | Left type | Right type | Result type |
|---|---|---|---|---|
| `equals` | comparison | any | any | `Bool` |
| `<`, `<=`, `>`, `>=` | comparison | numeric (Int/Dec) | numeric | `Bool` |
| `and`, `or` | logical | `Bool` | `Bool` | `Bool` (P11 does NOT yet reject non-Bool operands — see §1.4) |
| `+`, `-`, `*`, `/`, `%` | arithmetic | `Int` | `Int` | `Int` |
| `+`, `-`, `*`, `/`, `%` | arithmetic | `Int` | `Dec` | `Dec` (numerical promotion) |
| `+`, `-`, `*`, `/`, `%` | arithmetic | `Dec` | `Int` | `Dec` (numerical promotion) |
| `+`, `-`, `*`, `/`, `%` | arithmetic | `Dec` | `Dec` | `Dec` |
| `^` | arithmetic | numeric | numeric | matches left (preserves P10 placeholder for the power case) |
| anything else | unknown | any | any | left's type (P10 fallback preserved) |

**OQ-11.11** (resolved-in-spec): why does `^` preserve P10 placeholder behavior while other arithmetic does numerical promotion? Because `^` (power) emits to backends as:
- JS: `**` (returns same type as left)
- Python: `**` (returns int from `int**int`, float from int/float**float — Python's own promotion rules apply)
- C: `pow(...)` (always returns `double`, regardless of left)

The cross-target behavior here is already inconsistent at the backend layer. Inferring `Dec` for `int^int` would produce IR that suggests it's a float, but JS emits an int — making the IR's `type` field a lie. The honest answer is "the inference doesn't help here, P11 leaves the placeholder, P12+ may add cross-target normalization." **Skeptic note**: this resolution is on the boundary of "silent resolution" vs "principled deferral." A skeptic finding that disagrees is good signal.

**OQ-11.12** (resolved-in-spec): the unification rule for unknown / Error / Void on either side: the result is the left operand's type. This preserves P10's behavior exactly (`IrLowering.kt:209` `type = lIr.type`) and avoids cascading errors. The verifier should already have rejected the offending operand earlier (via UnknownIdentifier or UnknownType); reaching BinaryOp.type computation with Void on one side means the verifier missed something — but the IR shouldn't cascade. P11's inference table is **forgiving** in this regard.

### 3.2 Where the BinaryOp.type computation lives

Two options:

- **Option A — Elaboration computes the type at verify time; IR reads from side-table.** Consistent with the OQ-5.4-1 pattern (verifier-elaborates, IR consumes). The side-table grows one entry per BinaryOp.
- **Option B — IrLowering computes the type at lowering time.** Simpler local logic; no side-table change.

**Recommendation: Option A.** Consistency with the existing F1=C pattern (IDENTIFIER, ARRAY_INDEX, FUNCTION_CALL all read from side-table) is more valuable than the saved per-BinaryOp entry. Also: P12 may want to surface the inferred type for `match` exhaustiveness checking or sum-type unification — having the type available on `resolvedTypes` is cheaper than re-walking.

#### Side-table entry shape (Option A)

```kotlin
// In Elaboration.elaborateExpression, replace the R4-NOTE block (line 186-195) with:
ExpressionData.Kind.BINARY_OP -> {
    val l = expr.left ?: return
    val r = expr.right ?: return
    elaborateExpression(l, scope, table)
    elaborateExpression(r, scope, table)
    val lType = table[l] ?: WaterfallType.VoidType  // sub-expr should already be in table
    val rType = table[r] ?: WaterfallType.VoidType
    val resultType = inferBinaryOpType(expr.op ?: "?", lType, rType)
    // Note: we store the result type for BinaryOp itself, departing from P10's R4 deferral.
    resultType
}
```

The `inferBinaryOpType` helper lives in Elaboration (or a new `verifier/TypeInference.kt`):

```kotlin
internal fun inferBinaryOpType(op: String, lType: WaterfallType, rType: WaterfallType): WaterfallType {
    return when {
        // Comparison operators always produce Bool
        op == "equals" || op == "<" || op == "<=" || op == ">" || op == ">=" -> 
            WaterfallType.BoolType
        // Logical operators always produce Bool (P11 doesn't yet reject non-Bool operands)
        op == "and" || op == "or" -> 
            WaterfallType.BoolType
        // Power preserves P10 placeholder (cross-target divergence; see OQ-11.11)
        op == "^" -> lType
        // Arithmetic: numerical promotion
        op == "+" || op == "-" || op == "*" || op == "/" || op == "%" -> {
            if (lType == WaterfallType.DecType || rType == WaterfallType.DecType) 
                WaterfallType.DecType
            else 
                lType  // Int + Int = Int; everything else = left's type (forgiving fallback)
        }
        // Unknown operator: forgiving fallback (P10 behavior preserved)
        else -> lType
    }
}
```

#### IrLowering reads from side-table

```kotlin
// In IrLowering.lowerExpression for BINARY_OP:
ExpressionData.Kind.BINARY_OP -> {
    val l = expr.left ?: throw IllegalStateException("BINARY_OP missing left at ${expr.sourcePosition.generateMessage()}")
    val r = expr.right ?: throw IllegalStateException("BINARY_OP missing right at ${expr.sourcePosition.generateMessage()}")
    val lIr = lowerExpression(l, resolvedTypes)
    val rIr = lowerExpression(r, resolvedTypes)
    val resolvedType = resolvedTypes[expr]?.let { IrType.fromWaterfallType(it) } ?: lIr.type  // fallback to P10 behavior
    IrExpression.BinaryOp(
        op = expr.op ?: "?",
        left = lIr,
        right = rIr,
        type = resolvedType,
        sourcePosition = expr.sourcePosition  // §2.6: per-expression position
    )
}
```

### 3.3 `IrType.fromWaterfallType` — verify ErrorType becomes unreachable

P10 documents (`IrType.kt:50-53`) that the ISE for ErrorType "should be unreachable post-verification." P11 closes the cast-target case (§2.3). After P11, the only way ErrorType can reach `fromWaterfallType` is via a future regression. The ISE stays — it remains correct defensive behavior. **No P11 action required** for the ISE itself.

### 3.4 Position-threading audit

P11's §2.6 introduces `ExpressionData.sourcePosition`. The audit checks:

| File:line in P10 | P10 source position used | P11 source position used |
|---|---|---|
| `IrLowering.kt:48` (top-level var initializer) | `v.getSourcePosition()` (statement-level) | `v.value.sourcePosition` (expression-level) |
| `IrLowering.kt:59` (function parameter) | `f.getSourcePosition()` | **STAYS** `f.getSourcePosition()` (OQ-11.9 — deferred) |
| `IrLowering.kt:97` (typed var decl initializer) | `pos = stmt.getSourcePosition()` | `stmt.value.sourcePosition` |
| `IrLowering.kt:104` (untyped var decl initializer) | same | `stmt.value.sourcePosition` |
| `IrLowering.kt:110` (var assignment value) | same | `stmt.value.sourcePosition` |
| `IrLowering.kt:120,125` (if/elif condition) | same | `stmt.ifBranch.condition.sourcePosition` / `elif.condition.sourcePosition` |
| `IrLowering.kt:133` (while condition) | same | `stmt.condition.sourcePosition` |
| `IrLowering.kt:144` (return value) | same | `stmt.value?.sourcePosition` |
| `IrLowering.kt:148` (function call statement) | same | `stmt.call`'s inner ExpressionData if available — but FunctionCallData is NOT an ExpressionData. See OQ-11.13 below. |

**OQ-11.13** (ESCALATE — see §6, decision recorded): `FunctionCallStatementData.call` is `FunctionCallData`, NOT `ExpressionData`. Per OQ-11.13=(b), P11 DOES add `sourcePosition: SourcePosition` to `FunctionCallData` from `ctx.start`. The diff is small (1 field + constructor wiring); justifies the per-call source position for diagnostic quality and symmetry with `ExpressionData.sourcePosition`. The FunctionCallStatementData's IR statement then carries this per-call position rather than the enclosing statement's position.

### 3.5 No new IR types or variants

P11 does not add IR types or variants. The IR shape is stable from P10. The only IR-facing changes are:

- `IrExpression.BinaryOp.type` is populated correctly (no shape change).
- `IrExpression.*.sourcePosition` is the per-expression position (no shape change).
- `IrExpression.Identifier.type` is never `IrType.Void` for a resolved identifier (no shape change; just an invariant tightening).

This is by design: PITFALL #2 in P10 warned against speculative IR variants. P11 honors this — sum types, function-type variants, etc. all stay P12+.

---

## §4 Sub-task breakdown

P11's four sub-tasks, in dependency order. Total budget ≤1 week calendar.

| # | Sub-task | Scope | Carry-forwards closed |
|---|---|---|---|
| §4.1 | New VerifyError variants + ExpressionVerifier walker skeleton + UnknownIdentifier | 1 commit + tests | #1, #2 (OQ-3=C, OQ-5.4-1) |
| §4.2 | Cast-target validation + lambda body verification + per-expression source positions | 1 commit + tests | #4, #6, #7 (lambda, source positions, cast) |
| §4.3 | BinaryOp.type inference + forBlock iterator inference + IrLowering integration | 1 commit + tests | #3, #5 (BinaryOp.type, for-iterator) |
| §4.4 | StringLiteralText relocation + skeptic seed cross-check process trial + phase-exit ritual | 1 commit (mechanical) + log entries | #8, #9 (relocation, process improvement) |

### §4.1 — Sub-task: New VerifyError variants + ExpressionVerifier walker + UnknownIdentifier

**Files added:**
- (none — extend existing files)

**Files changed (production):**
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/verifier/VerifyError.kt` — add `UnknownIdentifier` variant (§2.2); add the Context enum.
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/verifier/ExpressionVerifier.kt` — promote from stub to real walker (§2.6). Body added in §4.2 (per-expression positions are needed first), but skeleton lands here.
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/verifier/StatementVerifier.kt` — four emission sites:
  1. `verifyVarAssignment` line 97: replace `?: return emptyList()` with `?: return listOf(UnknownIdentifier(s.name, Context.ASSIGNMENT_LHS, s.getSourcePosition()))`.
  2. `verifyIncrement` line 112: same pattern with `Context.INCREMENT_TARGET`.
  3. `verifyFunctionCallStatement`: check `scope.lookup(s.call.functionName)` when `s.call.kind == FunctionCallData.Kind.LOCAL`; emit `UnknownIdentifier(s.call.functionName, Context.EXPRESSION, s.getSourcePosition())` on null. (OQ-11.6=strict implementation note: spec line ~952.)
  4. `verifyForBlock`: before body walk, check `scope.lookup(s.collectionName)`; emit `UnknownIdentifier(s.collectionName, Context.FOR_COLLECTION, s.getSourcePosition())` on null; continue body walk regardless (§2.2 emission site #4; best-effort per OQ-11.1=a spirit).
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/verifier/Elaboration.kt` — **OQ-11.3=(a)** (ACCEPTED, spec line ~915): Elaboration takes `MutableList<VerifyError>` and emits `UnknownIdentifier` directly into it for expression-context unresolved names. Changes:
  1. `elaborateStatement(stmt, scope, table)` gains a fourth parameter `errors: MutableList<VerifyError>`; threads it to all `elaborateExpression` calls.
  2. `elaborateExpression(expr, scope, table)` gains two new parameters: `errors: MutableList<VerifyError>` + `primaryPos: SourcePosition` (the enclosing statement's position — per-expression position comes in §4.2; for §4.1, pass `stmt.getSourcePosition()` from `elaborateStatement`).
  3. `Kind.IDENTIFIER`: if `scope.lookup(name)` returns null → emit `UnknownIdentifier(name, Context.EXPRESSION, primaryPos)` into `errors` + write `WaterfallType.VoidType` to `table` (keeps existing table invariant; IrLowering's Void assertion is the drift catch if IrLowering is ever called on an unverified module).
  4. `Kind.ARRAY_INDEX`: if `scope.lookup(ai.target)` returns null → emit `UnknownIdentifier(ai.target, Context.ARRAY_INDEX_TARGET, primaryPos)`.
  5. `Kind.FUNCTION_CALL` for LOCAL kind: if `scope.lookup(fc.functionName)` returns null → emit `UnknownIdentifier(fc.functionName, Context.EXPRESSION, primaryPos)`.
  6. `elaborateFunctionCall` similarly gains `errors` + `primaryPos` and threads them through.
  - `ModuleVerifier` — passes the same `errors` list (used by `StatementVerifier.verifyStatement`) to `Elaboration.elaborateStatement` in both the top-level-variable loop and the function-body loop, so verifier + elaboration errors accumulate in one place.
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/verifier/HumanRenderer.kt` — extend the `when` block to render `VerifyError.UnknownIdentifier`. The byte-identical-string contract: emit `"${msg} in $pos"` where `msg` is the variant's `message` field. No legacy strings to preserve for this new variant.

**Files changed (IrLowering — defensive drift assertions):**
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/ir/IrLowering.kt` — for `Kind.IDENTIFIER`, add two defensive assertions. **These assertions are drift catches only — they never fire under normal operation** because `Main.kt` short-circuits at `!verifyResult.isSuccessful` before IrLowering is invoked, so any program with an unresolved identifier is rejected at verify time and never reaches lowering.
  1. Replace the `?: WaterfallType.VoidType` fallback at line 191 with: `?: error("IrLowering: identifier '$name' missing from resolvedTypes at ${fallbackPos.generateMessage()}; verifier or Elaboration drift")`. Fires only if an ExpressionData node was never elaborated (Elaboration bug).
  2. Add a post-fetch assertion: `if (waterfallType is WaterfallType.VoidType) error("IrLowering: identifier '$name' resolved to Void at ${fallbackPos.generateMessage()}; ModuleVerifier should have emitted UnknownIdentifier")`. Fires only if Elaboration wrote VoidType but the verifier somehow passed the module (drift bug). **The two assertions are different: the first catches missing entries; the second catches Void-poisoned entries.** (`fallbackPos` is the statement-level position available in §4.1; `expr.sourcePosition` replaces it in §4.2 once per-expression positions land.)

**Files added (tests):**
- `compiler/src/test/kotlin/com/aaroncoplan/waterfall/compiler/verifier/UnknownIdentifierTest.kt` — 6 mandatory + 1 emission-site coverage + 2 regression-coverage cases (expression-context cases land with §4.2):
  - `assignmentLhsUnknownIdentifierFails` — `undeclared = 5`.
  - `incrementUnknownIdentifierFails` — `undeclared++`.
  - `forCollectionUnknownIdentifierFails` — `for(item in undeclared) { ... }`. OQ-11.6 = strict (Aaron, 2026-05-19): test passes with FOR_COLLECTION error.
  - `undeclaredLocalFunctionCallStatementFails` — `func main() { doSomething() }` where `doSomething` is not declared anywhere. Asserts `UnknownIdentifier` with name `"doSomething"`, context = `EXPRESSION` (OQ-11.6=strict emission site 4; LOCAL function-call-statement).
  - `decrementUnknownIdentifierFails` — `undeclared--`. Same emission path as increment (INCREMENT_TARGET). Cheap one-liner regression guard.
  - `moduleCallDoesNotEmitUnknownIdentifier` — `Other::method()` (MODULE-kind call). Guards PITFALL #17: only LOCAL kind is checked; MODULE/OBJECT calls must NOT emit UnknownIdentifier.
  - `declaredIdentifierStillResolves` — guards no over-rejection: `int x = 5; x = 6` continues to verify.
  - `topLevelVarReferencedInFunctionStillResolves` — guards module-scope visibility into function bodies.
  - `forwardFunctionReferenceStillResolves` — guards the §5.4 Pass 1.5 forward-reference fix.
- `compiler/src/test/kotlin/com/aaroncoplan/waterfall/compiler/verifier/ExpressionVerifierWalkerSkeletonTest.kt` — 2 cases verifying §4.1 skeleton shape:
  - `walkerDispatchesOnKindWithoutCrashing` — exercises multiple ExpressionData.Kind values with declared identifiers; asserts no UnknownIdentifier + verifies clean.
  - `walkerReturnsEmptyListForIdentifierInSkeleton` — asserts that undeclared expression-context IDENTIFIER produces UnknownIdentifier from Elaboration (OQ-11.3=(a)), NOT from ExpressionVerifier (which is emptyList() for IDENTIFIER in §4.1).
- `compiler/src/test/kotlin/com/aaroncoplan/waterfall/compiler/verifier/UnknownIdentifierPropertyTest.kt` (Kotest style per existing `SymbolTablePropertyTest.kt` template) — at N=10000:
  - **Property 1**: for any program where every identifier reference is declared in scope, no UnknownIdentifier error is emitted.
  - **Property 2**: for any program with at least one identifier reference where the name does NOT appear in any scope, at least one UnknownIdentifier is emitted with that name and the correct Context.
  - **Property 3**: shadowing-soundness — if `x` is declared in outer scope and re-declared (rejected) in inner scope, the inner-scope error is `DuplicateDeclaration`, not `UnknownIdentifier`.

**Files added (Leg 3 adversarial fixture):**
- `compiler/src/test/resources/adversarial/phase-11/sub-task-4.1/programs.json` + corresponding `.wf` programs. Target: ≥20 entries (mix positive + negative).
- `compiler/src/test/kotlin/com/aaroncoplan/waterfall/compiler/tests/Sub41AdversarialTest.kt` (parameterized test runner per `Sub54AdversarialTest.kt` template).

**Expected behavior changes:**
1. `ControlFlowModule.wf` and `WhileModule.wf` reference undeclared identifiers (`doSomething`, `things`). The verifier will now reject them. **§7.2 enumerates the golden-change set.**
2. The `Main.kt` flow short-circuits at `verifyResult.isSuccessful` — no IR lowering; no backend emission; `CompilerError` thrown. `GoldenTests` swallows `CompilerError`; the golden becomes the empty string. **§7.2 covers this.**

**Expected test impact:**
- All P10 surviving tests pass.
- New `UnknownIdentifierTest` + `UnknownIdentifierPropertyTest` + adversarial fixture pass.
- `VerifierTest.assignToUndeclaredLhsIsP10NoOp` (the existing test at line 186-198 that asserts the P10 silent no-op) **FAILS** intentionally. P11 updates it to `assignToUndeclaredLhsNowEmitsUnknownIdentifier` and asserts the new variant. **Mandatory test rewrite** — list at the test's old name in the §4.1 commit message.
- `GoldenTests` parameterized cases for `ControlFlowModule.{c,python,js}` and `WhileModule.{c,python,js}` **CHANGE** per §7.2. Each golden file is updated atomically with the §4.1 commit.

**Sub-task §4.1 acceptance:** §4.1 commit lands as a single atomic commit. Per-commit golden gate strategy (§7.2): `scripts/check-goldens-unchanged-except-p11.sh` (new in this commit) passes — i.e., only the documented-to-change goldens differ.

**Plan-mode discipline:** ≥2 plan-mode iterations expected (per the P10 P10 average). Trip-wire: plan-mode iteration 1 with no clarification request = spec is too thin; spawn fresh-session skeptic per playbook §1.

### §4.2 — Sub-task: Cast-target validation + lambda body verification + per-expression source positions

**Files added:**
- (none — extend existing files)

**Files changed (production):**
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/statements/ExpressionData.kt` — add `@JvmField val sourcePosition: SourcePosition` derived from `ctx.start`. Update the init block to populate it.
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/verifier/VerifyError.kt` — add `CastTargetUnknown` (§2.3) and `LambdaParameterShadowing` (§2.4).
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/verifier/ExpressionVerifier.kt` — fully implement (no longer skeleton from §4.1). **Inherited from §4.1 deferral**: the IDENTIFIER emission responsibility remains with Elaboration per OQ-11.3=(a); §4.2 adds real bodies to the other cases below:
  - For `Kind.CAST`: validate `expr.castTargetType` parses to non-ErrorType; recurse into operand. Emit `CastTargetUnknown` on ErrorType.
  - For `Kind.LAMBDA`: enter child scope, declare params (with type validation), walk body via `verifyExpression(arg, lambdaScope)`. Emit `LambdaParameterShadowing` on shadowing; emit `UnknownType` on bad parameter type.
  - For `Kind.BINARY_OP`: recurse into left + right.
  - For `Kind.FUNCTION_CALL`: recurse into positional + named arguments. Per OQ-11.1 (escalate), decide first-fail vs best-effort for argument verification.
  - For `Kind.IDENTIFIER`: emit `UnknownIdentifier(name, Context.EXPRESSION, expr.sourcePosition)` on null lookup.
  - For `Kind.ARRAY_INDEX`: emit `UnknownIdentifier(name, Context.ARRAY_INDEX_TARGET, expr.sourcePosition)` if `expr.arrayIndex?.target` doesn't resolve; recurse into index.
  - For `Kind.ARRAY`: recurse into each element.
  - For `Kind.BUNDLE`: recurse into each element.
  - For `Kind.NULL_LITERAL`, `BOOL_LITERAL`, `INT_LITERAL`, `DEC_LITERAL`, `STRING_LITERAL`: no-op (no sub-tree).
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/verifier/StatementVerifier.kt` — wire `verifyExpression` calls into:
  - `verifyTypedVarDecl` — verify the initializer expression.
  - `verifyUntypedVarDecl` — verify the initializer expression.
  - `verifyVarAssignment` — verify the value expression (in addition to the existing LHS check).
  - `verifyForBlock` — verify the collection identifier (per OQ-11.6); verify each body statement (existing logic).
  - `verifyReturn` — verify the value expression (replacing the existing no-op at line 156-160).
  - `verifyFunctionCallStatement` — verify the call's sub-expressions.
  - `verifyIfBlock` / `verifyWhileBlock` — the existing `JoinAnalysis.verifyBranch` walks bodies; condition expressions are walked via `verifyExpression` on the parent scope (NOT body scope, matching `Elaboration.elaborateStatement`'s scope semantics at lines 87, 105).
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/ir/IrLowering.kt` — drop the `fallbackPos: SourcePosition` parameter from `lowerExpression`. All call sites use `expr.sourcePosition`. The `lowerFunctionCall` helper now takes `pos: SourcePosition` for the statement-level position (used for top-level FunctionCallData not nested in an ExpressionData).
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/verifier/Elaboration.kt` — **Inherited from §4.1 per OQ-11.3=(a)**: the `errors: MutableList<VerifyError>` + `primaryPos: SourcePosition` parameters already landed in §4.1 along with UnknownIdentifier emission for IDENTIFIER / ARRAY_INDEX / FUNCTION_CALL.LOCAL. No further structural change to Elaboration here. §4.2 adds: update `LAMBDA_POS` references where applicable to use `expr.sourcePosition` instead once that field exists (§2.6). **OQ-11.7 escalation point**: see §6.
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/verifier/HumanRenderer.kt` — extend `when` block for `CastTargetUnknown`, `LambdaParameterShadowing`.

**Files added (tests):**
- `compiler/src/test/kotlin/com/aaroncoplan/waterfall/compiler/verifier/CastTargetValidationTest.kt` — ≥6 cases:
  - `expressionContextUnknownIdentifierFails` — `int y = x + 1` where `x` undeclared. (Moved from §4.1 per skeptic Finding 1: requires `verifyExpression` called from `verifyTypedVarDecl`, which lands in §4.2.)
  - `arrayIndexUnknownArrayFails` — `int x = undeclared[0]`. (Moved from §4.1: requires `verifyExpression` for ARRAY_INDEX, §4.2.)
  - `lambdaArgUnknownIdentifierFails` — `[(int x) -> { sub(x, y) }]` where `y` undeclared in enclosing scope. (Moved from §4.1: requires lambda body walk, §4.2.)
  - `callArgUnknownIdentifierFails` — `add(x, y)` where `y` undeclared. OQ-11.1 = best-effort (Aaron): all args verified; accumulated errors returned. (Moved from §4.1: requires `verifyExpression` for FUNCTION_CALL args, §4.2.)
  - `castToKnownPrimitiveSucceeds` — `n castas int`.
  - `castToKnownArraySucceeds` — `xs castas dec[]`.
  - `castToUnknownTypeFails` — `n castas foo` emits CastTargetUnknown.
  - `castToVoidFails` — `n castas void` emits CastTargetUnknown (void is not a value type at parse layer — fromSourceText returns VoidType not ErrorType; this case requires extra care — see OQ-11.14 below).
  - `castToNullableSyntaxFails` — `n castas ?int` emits CastTargetUnknown (per the existing `?` rejection in fromSourceText).
  - `castOperandAlsoVerified` — `unknown castas int` emits UnknownIdentifier (the operand verified, not just the target).

**OQ-11.14** (resolved-in-spec): `castas void` — `fromSourceText("void")` returns `WaterfallType.VoidType`, not `ErrorType`. So §2.3's CastTargetUnknown emission rule ("emit on ErrorType") does NOT fire for `castas void`. Today, IrLowering lowers `castas void` to `IrExpression.Cast(targetType = IrType.Void, ...)` and backends emit `(void)operand` (C) / no-op (JS/Python). This is structurally wrong — you cannot cast to void in any of the three targets. P11 extends §2.3 to ALSO emit CastTargetUnknown for VoidType targets, with message: `"Cast target 'void' is not a value type"`. The variant gets a new `targetText = "void"` field. **Skeptic note**: this resolution silently bridges the cast-target-unknown variant with the void-not-a-value-type concept; an alternative is a dedicated `CastTargetVoid` variant. Single variant is cleaner; skeptic seed cross-check should validate.

- `compiler/src/test/kotlin/com/aaroncoplan/waterfall/compiler/verifier/LambdaBodyVerificationTest.kt` — ≥6 cases:
  - `lambdaParamsDeclaredIntoChildScope` — `[(int x) -> { print(x) }]` verifies x in body.
  - `lambdaParamShadowsEnclosingScope` — outer `int x = 5; ... [(int x) -> ...]` emits LambdaParameterShadowing.
  - `lambdaBodyReferencesEnclosingScope` — `int y = 10; ... [(int x) -> { print(y) }]` verifies clean.
  - `lambdaBodyUnknownIdentifier` — `[(int x) -> { print(undeclared) }]` emits UnknownIdentifier.
  - `lambdaParamUnknownType` — `[(foo x) -> ...]` emits UnknownType.
  - `lambdaWithEmptyBody` — `[(int x) -> {}]` verifies clean.
- `compiler/src/test/kotlin/com/aaroncoplan/waterfall/compiler/verifier/PerExpressionPositionTest.kt` — ≥4 cases:
  - `intLiteralCarriesOwnPosition` — `int x = 42` — the IntLiteral's position is NOT the statement's position (the literal is at a later column).
  - `binaryOpSubExpressionsCarryOwnPositions` — `int x = a + b` — `a`, `b`, and `+` carry distinct positions.
  - `nestedExpressionPositions` — `int x = ((a + b) + (c + d))` — five distinct sub-positions.
  - `unknownIdentifierReportsExpressionPosition` — `int x = a + b` with `b` undeclared reports the column of `b`, not the column of `int`.

**Files added (Leg 3 adversarial fixture):**
- `compiler/src/test/resources/adversarial/phase-11/sub-task-4.2/programs.json` — ≥20 entries focused on cast targets + lambda bodies + position edge cases.
- `compiler/src/test/kotlin/com/aaroncoplan/waterfall/compiler/tests/Sub42AdversarialTest.kt`.

**Expected behavior changes:**
1. `castas <unknown>` rejected at verify time (was: lowering ISE).
2. `castas void` rejected at verify time (was: emitted garbage in backends — see OQ-11.14).
3. Per-expression positions thread through; HumanRenderer messages now point at expression locations.
4. Lambda body errors surface — but no existing example in the corpus uses a lambda with an undeclared identifier in its body, so no goldens change for this case alone. **PITFALL**: the corpus may not exercise this fully; the adversarial fixture in `Sub42AdversarialTest.kt` is the primary coverage.

**Expected test impact:**
- All §4.1 tests + P10 surviving tests pass.
- New §4.2 tests pass.
- No new golden changes beyond those documented in §7.2 from §4.1.

### §4.3 — Sub-task: BinaryOp.type inference + forBlock iterator inference + IrLowering integration

**Files added:**
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/verifier/TypeInference.kt` (new) — hosts `inferBinaryOpType` (§3.2) + `inferIteratorType` helpers. Pure helper module; no state.

**Files changed (production):**
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/verifier/Elaboration.kt`:
  - `Kind.BINARY_OP`: instead of returning early per R4, compute the result type via `inferBinaryOpType(op, lType, rType)` and write into the side-table (§3.2).
  - `Kind.FOR_BLOCK` (in `elaborateStatement`): compute the iterator type from `scope.lookup(collectionName)?.type` via `inferIteratorType(...)`. Declare the iterator into the body scope with the inferred type (§2.5). Today this is hard-coded IntType at `Elaboration.kt:115`.
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/verifier/StatementVerifier.kt`:
  - `verifyForBlock` (line 138): similarly compute the iterator type via `inferIteratorType` and declare into the body scope with the inferred type.
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/ir/IrLowering.kt`:
  - For `Kind.BINARY_OP`: read the result type from `resolvedTypes[expr]` instead of falling back to `lIr.type` (§3.2). Keep the `lIr.type` fallback for defensive coding when the entry is absent.

**Files added (tests):**
- `compiler/src/test/kotlin/com/aaroncoplan/waterfall/compiler/verifier/BinaryOpInferenceTest.kt` — ≥8 cases covering every row in the §3.1 table:
  - `equalsReturnsBool` — `a equals b` → IrType.Bool regardless of operand types.
  - `lessThanReturnsBool`, `greaterThanReturnsBool`, etc. for each comparison operator.
  - `andReturnsBool`, `orReturnsBool` — including non-Bool operands (P11 doesn't reject; the result is still Bool per the inference table).
  - `intPlusIntReturnsInt` — `1 + 2` → IrType.Int.
  - `intPlusDecReturnsDec`, `decPlusIntReturnsDec`, `decPlusDecReturnsDec` — promotion cases.
  - `powerPreservesLeftType` — `a ^ 3` → matches left's type.
  - `unknownOpPreservesLeftType` — defensive fallback.
- `compiler/src/test/kotlin/com/aaroncoplan/waterfall/compiler/verifier/BinaryOpInferencePropertyTest.kt` — at N=10000:
  - **Property 1**: for all (op ∈ comparison family) and all left/right type pairs, the inferred type is Bool.
  - **Property 2**: for all (op ∈ arithmetic family except `^`) and (Dec on either side), the inferred type is Dec.
- `compiler/src/test/kotlin/com/aaroncoplan/waterfall/compiler/verifier/ForIteratorInferenceTest.kt` — ≥5 cases:
  - `forIteratorOverIntArrayInfersInt` — `for(x in xs)` where `xs: int[]`.
  - `forIteratorOverDecArrayInfersDec` — `for(x in xs)` where `xs: dec[]`.
  - `forIteratorOverBoolArrayInfersBool` — `for(x in xs)` where `xs: bool[]`.
  - `forIteratorOverScalarIntStaysInt` — `for(x in n)` where `n: int` (P10 implicit-int preserved).
  - `forIteratorShadowingFails` — outer `int item = 5; for(item in xs) { ... }` emits ForIteratorShadowing.

**Expected behavior changes:**
1. `IrExpression.BinaryOp.type` is correct for comparison and arithmetic-promotion cases. **Goldens unchanged**: no backend uses the `.type` field in BinaryOp emission today (verified via code inspection — all three backends emit `(left op right)` regardless of type).
2. `IrExpression.UntypedVarDecl.inferredType` for `x := a equals b` — **today** the syntactic `inferType()` returns `"int"` (it inspects only `expr.kind`, not BinaryOp's resolved type — see `UntypedVariableDeclarationAndAssignmentData.kt:20-29`). **P11 does NOT change `inferType()`** in this sub-task. So `x := a equals b` continues to declare `x` as `int` in the IR and backends. The BinaryOp.type fix is internal IR consistency; it doesn't ripple to UntypedVarDecl. **OQ-11.15** (resolved-in-spec): refactoring `inferType()` to use the resolved type from the side-table is OUT OF SCOPE for P11 — see §1.4. P12+ may close it.
3. `forBlock` iterator type infers correctly. **Goldens unchanged**: no example uses `for(x in xs)` where `xs` is non-int (the existing examples are `for(item in things)` where `things` is undeclared, OR scalar-iteration which preserves IntType behavior).

**Expected test impact:**
- All §4.1 + §4.2 + P10 surviving tests pass.
- New §4.3 tests pass.
- Goldens unchanged in this sub-task (§7.2 confirms).

### §4.4 — Sub-task: StringLiteralText relocation + skeptic seed cross-check + phase-exit ritual

**Files moved:**
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/statements/StringLiteralText.kt` → `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/ir/util/StringLiteralText.kt`. Package declaration updated.

**Files changed (imports):**
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/target/JavaScriptBackend.kt:4` — `import com.aaroncoplan.waterfall.compiler.statements.StringLiteralText` → `import com.aaroncoplan.waterfall.compiler.ir.util.StringLiteralText`.
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/target/PythonBackend.kt:4` — same.
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/target/CBackend.kt:4` — same.
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/ir/IrExpression.kt:43` — KDoc comment `"Backends call StringLiteralText.unescape"` updated to reference the new location.

**OQ-11.16** (resolved-in-spec): `ir/util/` is a new sub-package. Alternative locations considered:
- `ir/StringLiteralText.kt` — too prominent at the IR package level; the helper is utility code not an IR type.
- `target/util/StringLiteralText.kt` — only the backends use it, but it would couple the helper to `target/` which is a backend-specific package; the helper is target-agnostic.
- `util/StringLiteralText.kt` — a sibling-level utility package; too generic. Skeptic Finding 3 suggested either `ir/` or new `util/`.

**Pick: `ir/util/`.** Reasoning: backends consume IR; backends consume StringLiteralText; the helper is co-located with IR. The `util` sub-package keeps the IR root package clean. **Skeptic seed cross-check (§4.4) should validate this** — if two independent skeptic runs both suggest the same location, it's the right call.

**Files changed (process improvement — log entries):**
- `IMPLEMENTATION-LOG.md` — append a sub-task §4.1 outcome that includes the skeptic seed cross-check results (see protocol below).
- `notes/VERIFICATION-DISCIPLINE.md` — add a new P11 entry documenting the cross-check finding overlap.

#### Skeptic seed cross-check protocol — closes carry-forward #9

The P10 retrospective notes: "post-review skeptic produced identical finding counts (0F+5R+6M) for §5.1–§5.4." This is suspiciously regular and may indicate template-anchoring. P11 trials a cross-check protocol:

1. **Choose the sub-task** — §4.1 (UnknownIdentifier closure). It's the largest behavioral surface; richest finding space.
2. **Seed A skeptic run**: spawn a fresh-context Claude session. Prompt: "Read `notes/PHASE-11-design.md` §4.1 and the diff for sub-task §4.1. Find FATAL / RISK / MINOR findings." Standard prompt — what P10 used.
3. **Seed B skeptic run** (independent — run after Seed A's results are logged):
   - Different fresh-context Claude session (no shared history).
   - Different framing in the prompt: "Read `notes/PHASE-11-design.md` §4.1 and the diff for sub-task §4.1. You are a hostile reviewer who wants this PR to fail. Find the worst flaws."
   - Different temperature setting: bump from default 1.0 to 1.2 (more variation in finding generation).
4. **Compare**:
   - Count: how many findings did each produce?
   - Overlap: how many findings appear in both? Use a simple matching rule: two findings overlap if they reference the same file:line range OR describe the same root cause.
   - Determine: if overlap < 50%, the process is working — the seed sensitivity is real. If overlap >= 80% (structurally identical), the skeptic is template-anchoring; the result is noise; the process needs redesign.
5. **Log**: append the count + overlap + determination to `IMPLEMENTATION-LOG.md` sub-task §4.1 outcome AND `notes/VERIFICATION-DISCIPLINE.md` P11 entry. Include a one-sentence interpretation.

**OQ-11.17** (resolved-in-spec): the 50%/80% thresholds are heuristic. The first cross-check trial in P11 is a calibration run; the thresholds may be revised after the data lands. The protocol itself is the artifact; the thresholds are a starting point.

**OQ-11.18** (ESCALATE — see §6): the cross-check protocol introduces +1 skeptic session per sub-task (= +1 hour of human review per sub-task per `00-EXECUTION-PLAYBOOK.md` §3 budget). Aaron decides at plan-mode time whether the calibration value is worth the budget cost. If declined, the cross-check happens for §4.1 ONLY and the protocol is documented but not run for §4.2 and §4.3.

#### Phase-exit ritual (per `04-strategy.md` §3 P10 template + `00-EXECUTION-PLAYBOOK.md` §2)

After §4.3 lands and tests are green:

1. Run full triad at production scale: Leg 1 properties at N=10000, Leg 2 conditional golden gate, Leg 3 adversarial fixtures.
2. Walk the §7.6 phase-exit checklist line-by-line.
3. Spawn fresh-context skeptic on the full P11 diff (master..phase-11-complete). Resolve or accept findings.
4. Tag commit `phase-11-complete`.
5. Write Phase 11 retrospective in `IMPLEMENTATION-LOG.md` per the §2 phase-exit template.

**Files added (tests):**
- (no new tests beyond mechanical updates to existing imports)

**Expected behavior changes:**
- None — StringLiteralText relocation is pure refactor; goldens unchanged.

**Expected test impact:**
- All previous P11 tests + P10 surviving tests pass.
- Goldens unchanged in this sub-task (§7.2 confirms).
- New IMPLEMENTATION-LOG + VERIFICATION-DISCIPLINE entries land.

---

## §5 AI-implementation guidance (PITFALLs)

Per the P10 spec's pattern, these are the failure modes that hit AI implementers. Read them before writing code in the affected section.

### PITFALL #14 — Do not silently resolve `OQ-11.*` items

Every `OQ-11.N` in this document is either resolved-in-spec (with rationale) or escalated to §6. Both forms are explicit. If you find yourself reading the spec and thinking "the spec is ambiguous here, I'll just pick one," **stop**. Edit the spec or surface to Aaron. The cross-tree drift rule (`CLAUDE.md` F10) is non-negotiable.

Cross-references to research failure modes:
- failure mode #4 (silent spec resolution) — the dominant one.
- failure mode #1 ("tests pass, code is wrong") — silent resolutions often produce code that passes the existing tests but means something different from what the spec said.

### PITFALL #15 — Comparison operators return Bool, NOT LeftOperandType

The §3.1 inference table is exhaustive for the comparison family: `equals`, `<`, `<=`, `>`, `>=` all return `Bool`. **Do NOT** infer them as the left operand's type. The common mistake (because P10's placeholder was `lIr.type`) is to forget the comparison case and let `<` return `Int` (because both operands are Int). The §3.2 `inferBinaryOpType` table is the authority; the test `BinaryOpInferenceTest.lessThanReturnsBool` catches the mistake at unit-test time.

Cross-reference to failure mode: #1 verifier overfitting. The implementer who only tests `int x = a < b` and sees backends emit `(a < b)` (which works at JS/Python/C runtime regardless of IR type) will think they got it right; the IR-level invariant is broken.

### PITFALL #16 — `ExpressionData.sourcePosition` must thread through ALL expression sub-types

§2.6 names every site that previously used the statement-level fallback. Missing one (e.g., forgetting to update `lowerLambda` to use `lam.body?.sourcePosition` instead of the function's position) silently inherits the parent's position — making the §4.2 `PerExpressionPositionTest.nestedExpressionPositions` test fail.

When in doubt: grep `fallbackPos` in `IrLowering.kt` post-implementation. **Zero matches expected** — the parameter is removed.

Cross-reference to failure mode: #4 silent spec resolution + #1 verifier overfitting on existing position tests.

### PITFALL #17 — Elaboration's Void-for-undeclared exit point is load-bearing

`Elaboration.kt:151` today: `?: WaterfallType.VoidType`. P11 changes this to `?: error("...")`. The change is small but high-stakes: if any non-IDENTIFIER expression path (e.g., FUNCTION_CALL.LOCAL at line 159; ARRAY_INDEX at line 170; BUNDLE at line 211) returns `WaterfallType.VoidType` for legitimately-Void cases, the new ISE in `IrLowering.lowerExpression IDENTIFIER` would fire on the wrong path.

**Mitigation**: the ISE in `IrLowering.lowerExpression` for IDENTIFIER fires ONLY when `resolvedTypes[expr]` is Void (specifically for IDENTIFIER nodes). It does NOT fire for FUNCTION_CALL (where Void is a legitimate MODULE/OBJECT call's placeholder), LAMBDA (where Void is the lambda's placeholder), or NULL_LITERAL (where Void is the literal's type).

The PITFALL: an implementer who reads "Elaboration no longer writes Void" and applies it across all expression kinds breaks FUNCTION_CALL.MODULE / LAMBDA / NULL_LITERAL. The fix is precise: ONLY the IDENTIFIER path of Elaboration drops VoidType-for-undeclared; the others keep their Void placeholders.

Cross-reference to failure mode: #1 (verifier overfitting). The §4.1 tests should include a `moduleCallWithUndeclaredCalleeReturnsVoidNotError` regression case to lock this in.

### PITFALL #18 — Cast-target validation must NOT fire on `void[]`

`fromSourceText("void[]")` returns `ErrorType("void[]")` (per the existing `void[]` guard at `WaterfallType.kt:79`). The CastTargetUnknown emission rule per §2.3 says "emit on ErrorType" — but `void[]` is more naturally rendered as "void is not a value type for an array element" rather than a generic "Cast target 'void[]' is not a recognized type."

**Resolution**: P11 emits CastTargetUnknown for `void[]` with the generic message. This is acceptable because:
- The error message says "Cast target 'void[]' is not a recognized type" — technically true.
- The user gets pointed at the right line.
- The "more friendly" message is a P13 friendly-errors deliverable.

**Implementer note**: do NOT add a special case for `void[]` in the verifier. The ErrorType path handles it uniformly. If you find yourself wanting a special case, that's PITFALL #14 — surface to Aaron.

Cross-reference to failure mode: #4 silent spec resolution.

### PITFALL #19 — `forBlock` iterator type inference: scalar-collection case preserves IntType

`ControlFlowModule.wf` has `for(item in things)` where `things` is undeclared. Today this emits `for (int item = 0; item < 0; item++)` in C (per the `/* TODO(audit): for-in over things */` comment in `ControlFlowModule.expected`). Per §2.5 + §2.2, P11 rejects this program at the verifier (the collection is undeclared → UnknownIdentifier with `Context.FOR_COLLECTION`).

`WhileModule.wf` is a separate case — it uses `for(...)` with no collection issue but does call `doSomething()` which is undeclared.

**The PITFALL**: implementer reads "infer iterator type from collection" and thinks the iterator-inference work is for ALL for-loops. It is — but for `for(x in n)` where `n: int` (scalar, not array), the iterator's inferred type is `IntType` (per §2.5 table). The P10 implicit-int convention is preserved when the collection is scalar Int.

**Implementer note**: the §2.5 table has TWO Int-result rows — `IntType` (scalar) AND `ArrayType(IntType)`. Don't conflate them. Test: a `for(x in n)` where `n: int` is a syntactic edge case the corpus doesn't exercise; the adversarial fixture in `Sub43AdversarialTest.kt` should include it as a positive case.

Cross-reference to failure mode: #1 verifier overfitting.

### PITFALL #20 — The conditional golden gate is per-commit AND per-file

Per §7.2, P11's golden gate changes from P10's "any diff = fail" to "diffs allowed only on the enumerated set." The enforcement script must check **both**:
- That the listed-as-changing goldens MAY differ.
- That every other golden is byte-identical.

A naive implementation that whitelists the *files* but allows ANY diff in them (e.g., not asserting the exact post-P11 content) is wrong. The script should diff each whitelisted file against its expected-post-P11 content (committed alongside the change), and diff all other files against HEAD.

**Implementer note**: see §7.2 — the script `scripts/check-goldens-unchanged-except-p11.sh` is part of the §4.1 commit (alongside the production code). Don't ship a §4.1 commit without the new script.

Cross-reference to failure mode: #13 (gold-standard test discipline) + #4 silent resolution.

### PITFALL #21 — Skeptic seed cross-check is NOT a permission to skip the standard skeptic

§4.4's seed cross-check is an **addition** to the standard pre-review + post-review skeptic ritual. It is not a replacement. Sub-task §4.1 gets:
- Pre-review skeptic on the §4.1 spec section.
- Post-review skeptic on the §4.1 PR diff.
- **PLUS** Seed B skeptic on the same PR diff (the cross-check).

Three total. The cross-check's purpose is calibration of the standard process, not replacement of it.

**Implementer note**: do not interpret "seed cross-check" as "we have two skeptic runs, we don't need the original one." That would be PITFALL #4 (silent resolution of "what does cross-check mean").

Cross-reference to failure mode: #4 silent spec resolution + #8 AI-writes-and-passes-its-own-tests.

### PITFALL #22 — Updating goldens IS allowed in P11, but it MUST be commit-atomic

P10 PITFALL #13 was "goldens unchanged is the honest test" — the gate for P10's foundation refactor with zero behavioral changes. P11 deliberately introduces behavioral changes; goldens DO change. **But the discipline is the same shape, with a different criterion**:

- P10: any golden change is a stop-the-line event.
- P11: any UNDOCUMENTED golden change is a stop-the-line event. **A golden change without a corresponding §7.2 entry is a regression.**
- P11: the golden update commit MUST be atomic with the production-code commit that causes the change. **Not "fix in next commit."** This is the F10 cross-tree drift rule applied to goldens.

The trip-wire: if you find yourself thinking "I'll commit the code change first and fix the golden in the next commit," **stop**. That's PITFALL #22. Update goldens in the same commit; verify with `scripts/check-goldens-unchanged-except-p11.sh`; iterate.

Cross-reference to failure mode: #1 verifier overfitting + cross-tree drift.

### PITFALL summary table

| # | Location | Failure mode |
|---|---|---|
| 14 | All — silent OQ-11.* resolution | #4 silent spec resolution |
| 15 | §3.1 — comparison ops return Bool, NOT left.type | #1 verifier overfitting (because backends don't expose the bug) |
| 16 | §2.6 — `ExpressionData.sourcePosition` must thread to ALL nodes | #4 + #1 |
| 17 | §2.2 + §4.1 — Elaboration's Void-for-undeclared change is IDENTIFIER-only | #1 verifier overfitting |
| 18 | §2.3 — Cast-target validation handles `void[]` uniformly via ErrorType | #4 silent spec resolution |
| 19 | §2.5 — forBlock iterator inference: scalar-collection preserves IntType | #1 verifier overfitting |
| 20 | §7.2 — conditional golden gate is per-commit AND per-file | #13 + #4 |
| 21 | §4.4 — seed cross-check is addition, not replacement | #4 + #8 |
| 22 | §7.2 — golden updates are commit-atomic with code | cross-tree drift |

---

## §6 Escalation list (ESCALATE items for AI implementer)

These are the cases where the spec intentionally leaves the decision to Aaron. **Surface them to Aaron at plan-mode time. Do NOT silently resolve.** Each item names the question, the candidate answers, the spec's recommended default if applicable, and the cost of getting it wrong.

### ESCALATE OQ-11.1 — Best-effort vs first-fail in function-call argument verification

When `verifyExpression` walks a `FUNCTION_CALL` and one of the positional arguments emits `UnknownIdentifier`, does the walker continue verifying the remaining arguments (best-effort), or short-circuit at the first failure?

**Candidates:**
- **(a) Best-effort.** Walk all args. Accumulate all errors. User gets one diagnostic listing every undeclared arg. **Recommended default**: this is P11+ "show all errors" precondition per `04-strategy.md` §6 friendly errors.
- **(b) First-fail.** Stop at the first UnknownIdentifier. User gets one error per re-run of the compiler.

**Cost of wrong choice:**
- If best-effort but spec says first-fail: minor — slightly more verbose error output; behavioral surface unchanged for verification-success cases.
- If first-fail but spec says best-effort: meaningful — users with multiple undeclared identifiers get a worse experience; the P13 friendly-errors work has to retrofit.

**Aaron decides.** Recommended: best-effort (a).

**ACCEPTED (Aaron, 2026-05-19): (a) best-effort — walk all args, accumulate all errors.**

### ESCALATE OQ-11.3 — Boundary between ExpressionVerifier and Elaboration for identifier validation

§2.2 recommends Option A (promote Elaboration to emit `VerifyError.UnknownIdentifier` directly into a shared error list). Alternative: Option C — give `ExpressionVerifier` real teeth and have Elaboration emit only via ExpressionVerifier callbacks.

**Candidates:**
- **(a) Recommended: Option A.** Elaboration takes `MutableList<VerifyError>` parameter; writes UnknownIdentifier into it for IDENTIFIER / ARRAY_INDEX / FUNCTION_CALL.LOCAL.
- **(c) Alternative: Option C.** Elaboration delegates identifier checks to `ExpressionVerifier.verifyExpression`; ExpressionVerifier owns all expression-level error production; Elaboration is purely type-population.

**Cost of wrong choice:**
- Both work. (a) is smaller API change; (c) is cleaner separation of concerns but more code.

**Aaron decides at plan-mode time.** Recommended: (a) for the smaller diff.

**ACCEPTED (Aaron, 2026-05-19): (a) — Elaboration takes `MutableList<VerifyError>` and emits `UnknownIdentifier` directly into it.**

### ESCALATE OQ-11.5 — Should P11 close non-bool condition rejection + call-site arg type checking, or defer to P11b?

`04-strategy.md` §3 P11 lists both as deliverables: "Reject non-bool conditions" and "Function-arg and return-type checking at call sites (closes the no-op `verify` methods flagged in the audit)." Section §1.4 of THIS spec defers them.

**Candidates:**
- **(a) Recommended: keep deferred.** P11 closes 6 carry-forwards + 2 skeptic findings + 1 process improvement. Adding the strategy doc's two extra items inflates scope by ~50%. Per `00-EXECUTION-PLAYBOOK.md` §6 calendar discipline, slipping the ≤1 week budget is the primary risk.
- **(b) Pull condition-bool rejection in.** It's a small surface (any `if`/`while`/`for` condition with non-Bool resolved type emits a new variant). It depends on §4.2 already landed (per-expression source positions) + §4.3 (BinaryOp.type so conditions like `a equals b` resolve to Bool).
- **(c) Pull both in.** Full strategy-doc scope. ≥2 weeks instead of 1.

**Cost of wrong choice:**
- (a) means P12 inherits the "non-bool condition" deliverable. P12 also has sum types + match + readonly. Crowded.
- (b) is feasible if §4.3 finishes in 2 days. Tight.
- (c) blows the budget.

**Aaron decides at plan-mode time.** Recommended: (a). If §4.3 finishes early, (b) is a stretch goal.

**ACCEPTED (Aaron, 2026-05-19): (a) defer to P12.** This defers items the strategist listed as P11 success criteria. P12 already carries sum types + match + readonly + module system + cast-target validation (P11 §4.2); adding non-bool condition rejection and function-arg type checking to P12 creates a crowded scope. Aaron's decision (2026-05-19): defer to P12; if P12 strains its ≤1 week budget, split into P12a (existing) + P12b (these two).

### ESCALATE OQ-11.6 — Should the verifier reject `for(x in undeclared_collection)`?

`ControlFlowModule.wf` has `for(item in things)` where `things` is undeclared. Today this emits broken (TODO-annotated) C output but compiles. P11's UnknownIdentifier with `Context.FOR_COLLECTION` would reject it.

Same question for `doSomething()` — an undeclared local function call (FUNCTION_CALL.LOCAL with undeclared name).

**Candidates:**
- **(a) Strict — reject.** `for(x in undeclared)` and `undeclared()` both rejected. **§7.2 enumerates the golden changes**: 6 golden files become empty (3 backends × 2 examples). The C/JS/Python output that today shows broken-but-syntactically-valid code disappears.
- **(b) Lenient — accept.** Mirror today's behavior. Undeclared LOCAL function calls and undeclared FOR_COLLECTION names continue to lower with `IrType.Void` and backends emit their TODO placeholders. **OQ-11.6=lenient** means re-introducing `WaterfallType.VoidType` for these specific cases — not via the IDENTIFIER path of Elaboration (which §4.1 closes) but via the FUNCTION_CALL and FOR_COLLECTION paths. The Void in `IrType` becomes a "this is a deliberate gap" signal rather than a "verifier missed it" signal.
- **(c) Strict with whitelist.** Add a per-example escape hatch (e.g., `@external` annotation — P12 work) or a per-source-file directive. **OUT OF SCOPE for P11** but worth naming as a P12 direction.

**Cost of wrong choice:**
- (a): goldens change for `ControlFlowModule.{c,python,js}` and `WhileModule.{c,python,js}`. The "examples that today compile to broken output" become "examples that today fail to compile." Behaviorally this is a STRICTER compiler — programs the old compiler accepted are rejected. The corpus shifts. Skeptic finding likely: "this is a behavioral surface change that may affect downstream consumers." Recommended response: accept; the examples were broken anyway; the new behavior is more honest.
- (b): goldens unchanged. The honest "OQ-3=C closed" claim is partial — identifiers are checked except in two specific syntactic positions. Skeptic finding likely: "you said you closed the gap; you actually closed the IDENTIFIER path but not the FUNCTION_CALL or FOR_COLLECTION paths." The gap is still there for two specific cases.

**Aaron decides.** Recommended: **(a) strict** — the goldens are wrong today and updating them to empty (or to a deliberate `// compilation failed: <reason>` placeholder) is the honest closure. §7.2 documents the change.

**ACCEPTED (Aaron, 2026-05-19): (a) strict — accept 6 golden changes (ControlFlowModule + WhileModule × 3 backends).** Implementation note for strict path: `verifyFunctionCallStatement` must also check `scope.lookup(fc.functionName)` for `fc.kind == LOCAL` and emit `UnknownIdentifier(fc.functionName, Context.EXPRESSION, stmt.getSourcePosition())` on null. This is distinct from `verifyExpression` for expression-context calls — the function-call-statement path routes through `StatementVerifier`, not through `ExpressionVerifier`.

### ESCALATE OQ-11.7 — Lambda parameter source positions

§2.4 emits `UnknownType` for bad lambda parameter types with `primaryPosition = LAMBDA_POS` (the existing `SourcePosition("lambda-expr", 0, 0)` from `Elaboration.kt:45`). This is structurally wrong — it points at nothing.

**Candidates:**
- **(a) Keep LAMBDA_POS.** Mirrors existing Elaboration behavior; doesn't expand scope.
- **(b) Use the lambda expression's source position.** P11's §2.6 adds `ExpressionData.sourcePosition`; the lambda expression itself has a position; use it.
- **(c) Per-parameter position.** Requires `LambdaFunctionData` migration (the `typedArguments: List<parser.Pair<String, String>>` doesn't carry positions). **Same ripple as PITFALL #8 — out of P11 scope.**

**Cost of wrong choice:**
- (a): error message says "at lambda-expr:0:0" which is meaningless. Friendly-error work at P13 has to retrofit.
- (b): error message points at the lambda's open-bracket position; close enough for P11; P12+ may improve.
- (c): scope blow-up.

**Aaron decides.** Recommended: (b). The lambda's expression-level position is available via §2.6's `ExpressionData.sourcePosition` and is the right level of granularity for P11.

**ACCEPTED (Aaron, 2026-05-19): (b) — use the lambda expression's source position from §2.6.**

### ESCALATE OQ-11.13 — FunctionCallData source position

§3.4's audit notes that `FunctionCallStatementData.call` is a `FunctionCallData`, not an `ExpressionData`. Prior to this OQ's resolution, `FunctionCallData` did not carry a `sourcePosition` field. The candidates below were whether to add one.

**Candidates:**
- **(a) FunctionCallData inherits the statement's position.** Today's behavior; preserved in P11.
- **(b) Add `sourcePosition` to FunctionCallData.** The class constructor takes `WaterfallParser.FunctionCallContext` — readable via `ctx.start`. Small change, but ripples through `IrLowering.lowerFunctionCall` and the FunctionCallStatementData IR statement.

**Cost of wrong choice:**
- (a): function-call errors in statement position (e.g., MODULE-call to a nonexistent module) point at the statement's position, not the call's position. P13 friendly errors may want finer.
- (b): one more field; small surface.

**Aaron decides.** Recommended: (b) for symmetry with `ExpressionData.sourcePosition`. The diff is small.

**ACCEPTED (Aaron, 2026-05-19): (b) — add `sourcePosition` to `FunctionCallData` from `ctx.start`.**

### ESCALATE OQ-11.18 — Skeptic seed cross-check budget

§4.4's protocol introduces +1 skeptic run per sub-task if applied to all four. At `00-EXECUTION-PLAYBOOK.md` §3 budget of ~2h/week of Aaron-time on fresh-session adversarial reviews, four extra runs = ~4h. Per-sub-task it's a single ~1h block.

**Candidates:**
- **(a) Recommended: apply to §4.1 ONLY.** Trial the protocol once; log results; decide protocol fate after. ~1h cost.
- **(b) Apply to all four sub-tasks.** ~4h cost. Yields a per-sub-task overlap statistic instead of a single point estimate.
- **(c) Skip entirely.** Carry-forward #9 stays open; document the protocol design without trialing. Zero cost; zero learning.

**Cost of wrong choice:**
- (a): single trial result may not generalize; calibration is weaker than (b).
- (b): if the protocol is fundamentally broken, four runs reveal that; but at 4h cost it's not free.
- (c): the original carry-forward stays open into P12 and may bite later.

**Aaron decides.** Recommended: (a). One trial gives enough signal to decide whether to invest more.

**ACCEPTED (Aaron, 2026-05-19): (a) — apply skeptic seed cross-check to §4.1 only.**

### ESCALATE OQ-11.4 — Mismatched arithmetic operand types (declared-not-promoted)

§3.1's table says `int + dec → dec` (numerical promotion). But should `bool + int` emit a verifier error? In real type systems it would. P11's §1.4 said "no rejection in P11."

**Candidates:**
- **(a) Recommended: defer.** P11 infers; P11 does NOT reject. The forgiving fallback (`else -> lType`) covers `bool + int → bool` — wrong but doesn't break IR consumers.
- **(b) Add `BinaryOpTypeMismatch` variant.** Reject `bool + int` and similar. Small additional surface; large adversarial-fixture impact.

**Cost of wrong choice:**
- (a): the type-mismatch gap stays open into P12. Same as OQ-11.5 — crowds P12.
- (b): adds a verifier variant; goldens unchanged (no current example mixes bool + int arithmetic); ~20 additional adversarial fixtures needed.

**Aaron decides.** Recommended: (a). Tied to OQ-11.5 as a package.

**ACCEPTED (Aaron, 2026-05-19): (a) defer — no `BinaryOpTypeMismatch` variant in P11; tied to OQ-11.5 deferral.**

### ESCALATE OQ-11.19 — Where does `inferIteratorType` live?

§4.3 places `inferBinaryOpType` and `inferIteratorType` in a new file `verifier/TypeInference.kt`. Alternative: place them as Elaboration helpers (private to Elaboration.kt), since that's the only caller.

**Candidates:**
- **(a) Recommended: separate file `verifier/TypeInference.kt`.** Makes the inference logic discoverable; sets up the P12+ inference-pass scaffold.
- **(b) Private helper in Elaboration.kt.** Smaller surface; less surface area for P12 to refactor.

**Cost of wrong choice:**
- (a): file gets created; P12 work moves into it cleanly.
- (b): Elaboration.kt grows; P12 inference work either lives inside or has to extract.

**Aaron decides.** Recommended: (a).

**ACCEPTED (Aaron, 2026-05-19): (a) — new file `verifier/TypeInference.kt`.**

### Summary of ESCALATE items

| # | Topic | Recommended | Risk if wrong |
|---|---|---|---|
| OQ-11.1 | Best-effort vs first-fail in arg verification | best-effort | medium (UX) |
| OQ-11.3 | ExpressionVerifier vs Elaboration boundary | Elaboration writes errors (a) | low (both work) |
| OQ-11.5 | Pull in condition-bool + call-arg checks? | defer (a) | medium (P12 crowding) |
| OQ-11.6 | Reject for/local-call with undeclared name? | strict (a); update goldens | high (corpus shift) |
| OQ-11.7 | Lambda parameter source position | use lambda expr position (b) | low (UX) |
| OQ-11.13 | FunctionCallData source position | add field (b) | low (UX) |
| OQ-11.18 | Skeptic cross-check budget | §4.1 only (a) | low |
| OQ-11.4 | Reject bool+int arithmetic? | defer (a) | medium (P12 crowding) |
| OQ-11.19 | Location of inference helpers | new TypeInference.kt (a) | low |

**Aaron decides all of these at plan-mode time for §4.1 (the first sub-task).** Decisions are recorded in the §4.1 commit's plan-back diff per the playbook §1 spec-first loop.

---

## §7 Cross-cutting concerns

### §7.1 Verification triad design

Per `00-EXECUTION-PLAYBOOK.md` §3, P11's verification triad:

**Leg 1 — Property tests at N=10000**:
- `UnknownIdentifierPropertyTest` (§4.1) — 3 properties.
- `BinaryOpInferencePropertyTest` (§4.3) — 2 properties.
- Per-expression source-position invariant — 1 property (every node's position is derived from `ctx.start`, never a placeholder constant).
- ForBlock iterator-type inference — 1 property (for all `WaterfallType`s as collection types, the inferred iterator type matches the §2.5 table).

Total: 7 new properties. Plus 15 existing P10 properties at N=10000 continue to pass.

**Leg 2 — Differential oracle (conditional)**:
- 22 example programs × 3 backends = 66 parameterized golden test cases.
- Documented-to-change set (§7.2): up to 6 specific golden files may change (3 backends × 2 examples: `ControlFlowModule`, `WhileModule`).
- All other 192 byte-equivalence checks must remain green throughout P11.
- New script `scripts/check-goldens-unchanged-except-p11.sh` enforces. Required at every commit.

**Leg 3 — Adversarial inputs**: per sub-task, ≥20 entries except §4.4 (mechanical, ≥10 OK).

| Sub-task | Target | Location |
|---|---|---|
| §4.1 | ≥20 | `compiler/src/test/resources/adversarial/phase-11/sub-task-4.1/` |
| §4.2 | ≥20 | `…/sub-task-4.2/` |
| §4.3 | ≥20 | `…/sub-task-4.3/` |
| §4.4 | ≥10 (light — relocation + process trial) | `…/sub-task-4.4/` |

Each adversarial fixture has positive (must verify) + negative (must reject with the expected variant) entries. Per-sub-task runners (`Sub4NAdversarialTest.kt`) follow the P10 `Sub54AdversarialTest.kt` template.

### §7.2 Per-commit golden gate strategy (the conditional gate)

**P10 rule (PITFALL #13)**: any golden change is stop-the-line.

**P11 rule (this section)**: any UNDOCUMENTED golden change is stop-the-line. The documented set is enumerated below; goldens NOT in this set must remain byte-identical at every P11 commit.

#### Documented-to-change set

OQ-11.6 = strict (recommended default). The following goldens change in the §4.1 commit:

| File | Pre-P11 content | Post-P11 content | Cause |
|---|---|---|---|
| `compiler/src/test/resources/golden/c/ControlFlowModule.expected` | Existing content with `/* TODO(audit): for-in over things */` placeholder and `doSomething();` | Empty string | OQ-11.6=strict: `for(item in things)` rejected (undeclared `things`); `doSomething()` rejected (undeclared) |
| `compiler/src/test/resources/golden/python/ControlFlowModule.expected` | Existing content | Empty string | same |
| `compiler/src/test/resources/golden/js/ControlFlowModule.expected` | Existing content | Empty string | same |
| `compiler/src/test/resources/golden/c/WhileModule.expected` | Existing content with `doSomething();` | Empty string | OQ-11.6=strict: `doSomething()` rejected (undeclared) |
| `compiler/src/test/resources/golden/python/WhileModule.expected` | Existing content | Empty string | same |
| `compiler/src/test/resources/golden/js/WhileModule.expected` | Existing content | Empty string | same |

**Why empty string**: `GoldenTests` swallows `CompilerError` (`GoldenTests.kt:80-83`) and asserts captured stdout. When the verifier rejects, `CompilerError` is thrown after error rendering to stderr; stdout is empty. The golden becomes the empty string.

**Sub-decision (resolved-in-spec)**: NO — we are NOT updating these examples to add valid implementations of `things` and `doSomething`. The examples were broken-by-design; pretending they work is dishonest. They become "negative test cases" — modules that exercise the OQ-11.6 path. **Skeptic note**: an alternative is to remove these examples entirely. We KEEP them and update goldens to empty because: (a) the example files demonstrate the source-language syntax for for-in and while-with-call; (b) they're useful regression coverage that the syntax still parses; (c) when P11.5/P12 lands cross-module visibility, `doSomething` could become an `@external` declaration and the goldens come back to life.

**Alternative if OQ-11.6 = lenient**: zero goldens change in §4.1. The §7.2 set is empty. The conditional script equals the P10 strict script.

#### The conditional script

```bash
#!/usr/bin/env bash
# §7.2: P11 conditional golden gate. Allows the documented set to differ;
# enforces byte-identical everywhere else.
# Run at every commit. Exits non-zero if any UNDOCUMENTED golden has changed.

# Documented-to-change set (per §7.2 of PHASE-11-design.md). EMPTY for OQ-11.6=lenient.
DOCUMENTED_CHANGES=(
  "compiler/src/test/resources/golden/c/ControlFlowModule.expected"
  "compiler/src/test/resources/golden/python/ControlFlowModule.expected"
  "compiler/src/test/resources/golden/js/ControlFlowModule.expected"
  "compiler/src/test/resources/golden/c/WhileModule.expected"
  "compiler/src/test/resources/golden/python/WhileModule.expected"
  "compiler/src/test/resources/golden/js/WhileModule.expected"
)

# git diff against HEAD (pre-commit) or master (CI):
TARGET="${1:-HEAD}"

# All goldens NOT in the documented set: require byte-identical.
DIFF_ARGS=(--exit-code -- compiler/src/test/resources/golden/)
for f in "${DOCUMENTED_CHANGES[@]}"; do
  DIFF_ARGS+=(":(exclude)$f")
done

if ! git diff "$TARGET" "${DIFF_ARGS[@]}" >/dev/null 2>&1; then
  echo "ERROR: Undocumented golden change detected." >&2
  echo "Diff (excluding documented-to-change set):" >&2
  git diff "$TARGET" "${DIFF_ARGS[@]}"
  exit 1
fi

# Content verification of documented-to-change goldens is GoldenTests.kt's job
# (parameterized snapshot tests that compare against the .expected files).
# This script only enforces that UNDOCUMENTED goldens are unchanged.
echo "Golden gate passed: undocumented goldens are byte-identical to $TARGET."
```

The script lives at `scripts/check-goldens-unchanged-except-p11.sh`. Lands in the §4.1 commit alongside the production code.

#### Why a conditional gate is acceptable (and not PITFALL #13 backsliding)

The P10 PITFALL #13 rule was correct **for P10's scope** — P10 explicitly committed to zero behavioral changes. P11 explicitly commits to closing documented gaps; the gaps' closure changes behavior; the goldens reflect behavior. A rigid PITFALL #13 in P11 would force one of:

- Skip the OQ-3=C closure (leaving the carry-forward open) — PITFALL #14 (silent resolution of "do we actually close the gap").
- Update goldens silently and call it acceptable — F10 cross-tree drift violation.
- Re-author the examples to remove undeclared identifiers — re-engineers the example corpus to fit the test gate rather than the language design.

The conditional gate is the honest path: declare exactly what changes, enforce strictness everywhere else, attach the documented set to the spec so future sessions can audit it.

The discipline trip-wire: **any P11 commit that touches a non-documented golden = STOP**. The script is what makes the discipline mechanical.

### §7.3 Backwards-compatibility framing

Programs compiling under P10's silent-pass-through-as-Void may fail under P11. **There are no real users of the language**, so backwards compatibility is not a hard constraint — but the migration story matters for the spec's honesty.

**The story** (one paragraph for SPEC.md / BUS-FACTOR.md / future README): "Phase 11 of the Waterfall compiler closed identifier-resolution gaps that previously caused programs with undeclared names to compile to broken target output. Programs in the project's `examples/` directory that referenced `things` (in for-in collections) or `doSomething` (in local function calls) were updated to fail verification with a clear error message rather than emit broken backend code. If you have Waterfall source from before Phase 11 that referenced undeclared names in these positions, it will now produce a verifier error. The fix is to declare the names (`int things = 0; func doSomething() { ... }`) or — when sub-module visibility lands in P11.5 — to import them from an external module."

**Per the strategist's roadmap (`04-strategy.md` §3 P11.5):** the `import` mechanism + `@external` annotation arrive at P11.5/P12, which is when the "declare or import" guidance becomes real. P11 itself does not ship `import`; the migration story for P10→P11 is "declare or remove."

### §7.4 §3.9 R6 (private dispatcher) — P10's decision holds

P10 sub-task §5.6 declined to promote `emitStatement` to a default interface method (R6 decision logged in `IMPLEMENTATION-LOG.md` sub-task §5.6 outcome + `CodeGenerator.kt` KDoc). P11 does NOT re-litigate. No fourth backend is planned for P11; the three existing backends keep their private dispatcher pattern. Re-open only when a fourth backend is being added (P12+ may consider).

**No P11 action.**

### §7.5 The `WaterfallType.fromSourceText` mechanism stays

P10 §7.5 noted that `fromSourceText` is duck-tape — bridging the `*Data` classes' string-typed `type` fields to the new sealed-class representation. P10 noted "P11+ should remove this bridge."

**P11 does NOT remove it.** The reason: removing it requires `*Data` classes to parse the ANTLR `type` context directly into `WaterfallType` at construction time. That's a sweep across `TypedVariableDeclarationAndAssignmentData`, `UntypedVariableDeclarationAndAssignmentData`, `FunctionImplementationData`, `LambdaFunctionData`, plus the `WaterfallParser` type-rule glue. Out of P11 scope.

The bridge stays. The §2.3 audit (cast-target validation) closes one of its known weak points (ErrorType returned from cast targets); the other call sites are already verifier-checked per the §2.3 table.

**P11 action**: the §7 phase-exit checklist requires a grep for `fromSourceText` in `target/` and `ir/` to confirm zero NEW call sites land in P11. (Existing call sites stay; new ones — if any — must be audited per the §2.3 table.)

### §7.6 Phase-exit checklist for P11

Before merging the `phase-11-complete` tag commit, the reviewer (Aaron) verifies:

- [ ] `./gradlew build` passes locally without new warnings.
- [ ] `./gradlew test` passes; the conditional golden gate per §7.2 is green.
- [ ] **Documented-to-change goldens** (per §7.2) have their expected post-P11 content; **no other goldens** have changed.
- [ ] All §4.1 + §4.2 + §4.3 test cases pass.
- [ ] All P11 property tests pass at N=10000.
- [ ] All P11 adversarial fixtures' "compiler-broke" rate is zero.
- [ ] `VerifyError` now has 11 variants (7 from P10 + 4 from P11): `AssignToReadonly`, `IncrementOfReadonly`, `ReadonlyOfUndeclared`, `AlreadyReadonly`, `DuplicateDeclaration`, `UnknownType`, `VoidNotAValueType`, **`UnknownIdentifier`**, **`CastTargetUnknown`**, **`LambdaParameterShadowing`**, **`ForIteratorShadowing`**.
- [ ] `HumanRenderer` renders all 11 variants in its `when` block.
- [ ] `IrLowering.lowerExpression` for IDENTIFIER no longer falls back to `WaterfallType.VoidType` (PITFALL #17); the new assertion fires only when ModuleVerifier missed an unresolved identifier.
- [ ] `ExpressionData` has a `sourcePosition` field; every IR expression node uses it.
- [ ] `lowerExpression` no longer takes a `fallbackPos` parameter (§2.6 + PITFALL #16).
- [ ] `StringLiteralText` now lives in `ir/util/`; backends import from the new path.
- [ ] `scripts/check-goldens-unchanged-except-p11.sh` exists and is wired into the test pipeline.
- [ ] `IMPLEMENTATION-LOG.md` has the P11 retrospective entry.
- [ ] `notes/VERIFICATION-DISCIPLINE.md` has the P11 triad entry + the skeptic seed cross-check determination.
- [ ] `Main.kt` no longer leaks `IllegalStateException` for any verified-as-successful module — verified via fresh-context skeptic on `Main.kt` + `IrLowering.kt` for ISE escape paths.
- [ ] **§4.4 cross-check trial logged** per §4.4 protocol; determination recorded.
- [ ] No new `WaterfallType.fromSourceText` call sites in `target/` or `ir/` beyond those audited in §2.3.

If any item fails: stop and fix before tagging.

### §7.7 Spec edits expected during implementation

P10's average was ~10 spec edits per sub-task (per `IMPLEMENTATION-LOG.md` retrospective). P11 should aim lower (less ambiguity by design per the OQ-11.* inventory). Trip-wire: **>15 spec edits in a single sub-task indicates the spec was under-specified for that sub-task** — stop and re-write the affected section before more code lands.

Expected spec edits during P11:
- §4.1 ramp-up: ~5–8 (from pre-review skeptic on this spec + plan-mode iterations).
- §4.2 ramp-up: ~3–5 (smaller surface).
- §4.3 ramp-up: ~3–5 (similar).
- §4.4 ramp-up: ~1–3 (mechanical).

Total estimated: ~15–25 spec edits across the phase.

---

## §8 AI-implementation guidance — additional notes

### §8.1 The interleaved verifier/elaboration walk is load-bearing

`ModuleVerifier.verifyFunctionDeclaration` interleaves `StatementVerifier.verifyStatement` and `Elaboration.elaborateStatement` per statement (per `ModuleVerifier.kt:104-107`). P11 changes do NOT alter this ordering. The verifier walks first; elaboration follows. Per §2.7, Elaboration begins writing UnknownIdentifier into the verifier's error list (Option A from OQ-11.3 recommendation).

Specifically: when ExpressionVerifier (called from StatementVerifier) walks an expression and emits UnknownIdentifier, the same expression sub-tree is then walked by Elaboration. If the verifier already emitted UnknownIdentifier for a name, Elaboration sees the same name as still-unresolved (scope state hasn't changed). The two-pass shape is *not* dispatching on "is the error already there"; both passes walk independently. This is wasteful (each name's lookup is done twice — once by ExpressionVerifier, once by Elaboration) but correct.

**OQ-11.20** (resolved-in-spec): the redundant lookup is acceptable for P11. P12+ may consolidate into a single walk; not P11's job.

### §8.2 Don't break the F1=C side-table contract

P10's F1=C decision (side-table elaboration; `VerifyResult.resolvedTypes` keyed by ExpressionData identity) is load-bearing. P11 extends the contract by:
- Adding entries for BinaryOp (§3.2).
- Tightening the IDENTIFIER entries to never be VoidType (§2.2).

**Do NOT** change the contract shape:
- Keys stay `ExpressionData` (identity-keyed via `IdentityHashMap`).
- Values stay `WaterfallType` (not `IrType` — that's a lowering concern).
- The map is read-only after `verifyResult.isSuccessful` is confirmed.

**OQ-11.21** (resolved-in-spec): the identity-keyed map means re-parsing the same source produces a different map (different ExpressionData instances). For incremental compilation (P13 LSP), this becomes a structural change. **Out of P11 scope** — P11 keeps the identity-keyed pattern; LSP work will revisit.

### §8.3 The four new VerifyError variants must all extend the existing pattern

The new variants follow the P10 conventions:
- Sealed class extension.
- `primaryPosition: SourcePosition`.
- `message: String` (canonical short form for HumanRenderer).
- `code: String` (stable WF12xx code).
- `data class` (structural equality for test fixtures).
- Documented in KDoc with the "byte-identical string contract" pattern where applicable.

**PITFALL note**: do NOT add a new pattern (e.g., a `severity` field or a `relatedInfo` field). The P10 spec already specifies that LSP-style severity + relatedInfo come via JsonRenderer's schema, NOT as fields on VerifyError. The §4.8 schema in P10's spec is the authority.

### §8.4 Atomic commits per sub-task

Per the playbook §1 spec-first loop, each sub-task is ONE atomic commit on master (modulo a small post-skeptic fixup commit). Specifically:

| Commit | Files | Tests | Goldens |
|---|---|---|---|
| §4.1 atomic | VerifyError, ExpressionVerifier skeleton, StatementVerifier, Elaboration, HumanRenderer, IrLowering | UnknownIdentifierTest, UnknownIdentifierPropertyTest, Sub41AdversarialTest + fixture | 6 golden files (per §7.2) + check script |
| §4.1 post-skeptic | any fixups | any | none |
| §4.2 atomic | ExpressionData, VerifyError, ExpressionVerifier full, StatementVerifier expansion, IrLowering fallbackPos drop, HumanRenderer | CastTargetValidationTest, LambdaBodyVerificationTest, PerExpressionPositionTest, Sub42AdversarialTest + fixture | none |
| §4.2 post-skeptic | any | any | none |
| §4.3 atomic | Elaboration BinaryOp, StatementVerifier forBlock, IrLowering BinaryOp side-table read, TypeInference.kt | BinaryOpInferenceTest, BinaryOpInferencePropertyTest, ForIteratorInferenceTest, Sub43AdversarialTest + fixture | none |
| §4.3 post-skeptic | any | any | none |
| §4.4 atomic | StringLiteralText move + 3 import updates + KDoc fix + IMPLEMENTATION-LOG + VERIFICATION-DISCIPLINE | Sub44AdversarialTest + fixture (light) | none |

Don't combine §4.1 and §4.2 into one commit. Don't split §4.1's spec changes from the production code change (F10 cross-tree drift).

---

## Appendix A — Summary of new files

```
compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/
├── verifier/
│   ├── VerifyError.kt          (MODIFIED — adds 4 variants)
│   ├── ExpressionVerifier.kt   (MODIFIED — promoted from stub to real walker)
│   ├── StatementVerifier.kt    (MODIFIED — wires verifyExpression calls)
│   ├── Elaboration.kt          (MODIFIED — drop Void-for-undeclared IDENTIFIER; add BinaryOp entry; iterator type)
│   ├── ModuleVerifier.kt       (MINOR — no structural change; existing interleave preserved)
│   ├── HumanRenderer.kt        (MODIFIED — extend when block for 4 new variants)
│   └── TypeInference.kt        (NEW — inferBinaryOpType, inferIteratorType helpers)
├── ir/
│   ├── IrLowering.kt           (MODIFIED — drop fallbackPos; use expr.sourcePosition; BinaryOp side-table read)
│   ├── IrExpression.kt         (KDoc only — type field semantics post-P11)
│   └── util/
│       └── StringLiteralText.kt (MOVED from statements/; package changes from .statements to .ir.util)
├── statements/
│   ├── ExpressionData.kt       (MODIFIED — adds sourcePosition: SourcePosition field)
│   ├── FunctionCallData.kt     (MODIFIED — adds sourcePosition: SourcePosition field from ctx.start per OQ-11.13)
│   └── StringLiteralText.kt    (DELETED — moved to ir/util/)
├── target/
│   ├── JavaScriptBackend.kt    (MODIFIED — StringLiteralText import path)
│   ├── PythonBackend.kt        (MODIFIED — StringLiteralText import path)
│   └── CBackend.kt             (MODIFIED — StringLiteralText import path)
└── Main.kt                     (NO CHANGE)
```

Tests added:

```
compiler/src/test/kotlin/com/aaroncoplan/waterfall/compiler/
├── verifier/
│   ├── UnknownIdentifierTest.kt              (NEW — §4.1)
│   ├── UnknownIdentifierPropertyTest.kt      (NEW — §4.1)
│   ├── CastTargetValidationTest.kt           (NEW — §4.2)
│   ├── LambdaBodyVerificationTest.kt         (NEW — §4.2)
│   ├── PerExpressionPositionTest.kt          (NEW — §4.2)
│   ├── BinaryOpInferenceTest.kt              (NEW — §4.3)
│   ├── BinaryOpInferencePropertyTest.kt      (NEW — §4.3)
│   ├── ForIteratorInferenceTest.kt           (NEW — §4.3)
│   └── VerifierTest.kt                       (MODIFIED — `assignToUndeclaredLhsIsP10NoOp` rewritten as `assignToUndeclaredLhsNowEmitsUnknownIdentifier`)
└── tests/
    ├── Sub41AdversarialTest.kt               (NEW)
    ├── Sub42AdversarialTest.kt               (NEW)
    ├── Sub43AdversarialTest.kt               (NEW)
    └── Sub44AdversarialTest.kt               (NEW)
```

Adversarial fixtures:

```
compiler/src/test/resources/adversarial/phase-11/
├── sub-task-4.1/
│   ├── programs.json
│   ├── positive/*.wf  (≥10 entries)
│   └── negative/*.wf  (≥10 entries with .expected-error siblings)
├── sub-task-4.2/      (≥20 entries)
├── sub-task-4.3/      (≥20 entries)
└── sub-task-4.4/      (≥10 entries — mechanical)
```

Scripts:

```
scripts/
├── check-goldens-unchanged.sh                (P10 — preserved for reference, NOT called in P11)
└── check-goldens-unchanged-except-p11.sh     (NEW — P11 conditional gate)
```

Doc updates:

```
IMPLEMENTATION-LOG.md                         (MODIFIED — add Phase 11 kickoff + per-sub-task outcomes + retrospective)
notes/VERIFICATION-DISCIPLINE.md              (MODIFIED — add P11 triad entry + skeptic cross-check finding)
BUS-FACTOR.md                                 (MINOR — verifier surface description update if needed)
```

Goldens (per §7.2, OQ-11.6=strict):

```
compiler/src/test/resources/golden/
├── c/ControlFlowModule.expected       (UPDATED — empty)
├── c/WhileModule.expected             (UPDATED — empty)
├── python/ControlFlowModule.expected  (UPDATED — empty)
├── python/WhileModule.expected        (UPDATED — empty)
├── js/ControlFlowModule.expected      (UPDATED — empty)
└── js/WhileModule.expected            (UPDATED — empty)
```

---

## Appendix B — Why P11 is smaller than P10 (and the strategy of restraint)

P10 was a 6-sub-task foundation refactor (~5 days actual; ~63 spec edits per `IMPLEMENTATION-LOG.md`). P11 is 4 sub-tasks; the goal is ≤1 week with materially fewer spec edits.

The strategy of restraint: each item in §1.4 ("what P11 does NOT do") could plausibly land in P11 — but each one moves the boundary. The phase landed on its current shape because:

1. **The strategy doc's literal P11 deliverables** are "Hindley-Milner-lite inference" + "Reject non-bool conditions" + "Function-arg and return-type checking at call sites." The first one is half-done by §4.3 BinaryOp inference. The second and third are deferred (§1.4).
2. **The retrospective's carry-forwards** are the items in §1.1's table. They are the floor for what P11 must close.
3. **The intersection** is what shipped in this spec. Items that appear in (a) but not (b) are deferred; items that appear in (b) are mandatory.

This is the "spec-first" discipline applied at the phase level: the strategist roadmap is one input; the retrospective carry-forwards are another; the spec sequences them coherently. Items that don't appear in either input do NOT make it into the phase.

**If the implementer finds themselves wanting to add a feature not in §1.1**: stop. Surface to Aaron. The discipline is more valuable than the feature.

---

## Appendix C — Cross-references to source files (load-bearing line numbers)

For the AI implementer's plan-mode loop, these are the exact code locations the spec touches. Verify these line numbers match HEAD (master @ `f2feab9` per `phase-10-complete`) before quoting them in plans.

| Section | File | Line | What |
|---|---|---|---|
| §2.2 | `compiler/.../verifier/StatementVerifier.kt` | 97-98 | `verifyVarAssignment` silent no-op on null lookup |
| §2.2 | `compiler/.../verifier/StatementVerifier.kt` | 112-113 | `verifyIncrement` silent no-op |
| §2.2 | `compiler/.../verifier/Elaboration.kt` | 149-153 | IDENTIFIER → VoidType-for-undeclared |
| §2.2 | `compiler/.../ir/IrLowering.kt` | 184-193 | OQ-5.4-1 M7 fallback for IDENTIFIER → IrType.Void |
| §2.3 | `compiler/.../ir/IrType.kt` | 50-53 | ISE for ErrorType (defensive) |
| §2.3 | `compiler/.../Main.kt` | 26-35 | `main()` does NOT catch IllegalStateException |
| §2.3 | `compiler/.../ir/IrLowering.kt` | 213-222 | CAST lowering — fromSourceText with potential ErrorType |
| §2.4 | `compiler/.../verifier/ExpressionVerifier.kt` | 24-29 | P10 stub (no-op) |
| §2.4 | `compiler/.../verifier/Elaboration.kt` | 213-229 | LAMBDA elaboration |
| §2.4 | `compiler/.../verifier/Elaboration.kt` | 45 | LAMBDA_POS constant |
| §2.5 | `compiler/.../verifier/StatementVerifier.kt` | 138-151 | verifyForBlock; R2 IntType decl |
| §2.5 | `compiler/.../verifier/Elaboration.kt` | 111-122 | for-block elaboration; IntType decl |
| §2.6 | `compiler/.../statements/ExpressionData.kt` | 47-107 | ExpressionData init; no sourcePosition today |
| §2.6 | `compiler/.../ir/IrLowering.kt` | 87-91 | Statement-level position TODO comment |
| §2.7 | `compiler/.../verifier/VerifyResult.kt` | 27-29 | resolvedTypes side-table contract |
| §3.1 | `compiler/.../ir/IrExpression.kt` | 60 | BinaryOp.type KDoc: "P10: left.type placeholder" |
| §3.1 | `compiler/.../ir/IrLowering.kt` | 209 | BinaryOp.type = lIr.type |
| §3.2 | `compiler/.../verifier/Elaboration.kt` | 186-195 | BinaryOp R4 (no side-table entry) |
| §3.4 | `compiler/.../ir/IrLowering.kt` | 163-167 | lowerExpression signature with fallbackPos |
| §4.1 | `compiler/.../tests/VerifierTest.kt` | 186-198 | `assignToUndeclaredLhsIsP10NoOp` — must be rewritten in §4.1 |
| §4.4 | `compiler/.../statements/StringLiteralText.kt` | 1-74 | The file to move |
| §4.4 | `compiler/.../target/JavaScriptBackend.kt` | 4 | First import to update |
| §4.4 | `compiler/.../target/PythonBackend.kt` | 4 | Second import to update |
| §4.4 | `compiler/.../target/CBackend.kt` | 4 | Third import to update |
| §4.4 | `compiler/.../ir/IrExpression.kt` | 43 | KDoc reference to StringLiteralText |
| §7.2 | `compiler/.../tests/GoldenTests.kt` | 79-84 | CompilerError swallow — confirms empty-stdout becomes the golden |

---

End of Phase 11 design.
