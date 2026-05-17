# PHASE 10 — Foundation Refactor

Author: language-designer (Task #12)
Date: 2026-05-15
Status: design spec, ready for AI-augmented implementation
Inputs: `notes/team-output/01-codebase-audit.md` (D1, D2, D3, D6), `notes/team-output/03-language-design.md` (§2c symbol-table model, §2d join algorithm), `notes/team-output/04-strategy.md` (P10 phase definition), `notes/team-output/07-ai-augmented-dev-research.md` (failure modes #1, #2, #4, #8, #9)

---

## Read this first

This document is the spec for Phase 10. It closes audit debts D1 (no IR), D2 (symbol-table info is `Any?`), D3 (verifier entwined with codegen), D6 (no kind discriminator for function vs variable).

It is written to be implemented by an AI agent working with a senior operator (Aaron) doing review. Every section is detailed enough that an implementer should not have to guess. Where ambiguity remains, look for the **PITFALL** callouts — those are the failure modes that hit AI implementers in the public case studies (Klabnik's hardcoded instruction sizes, Anthropic's verifier overfitting). Read those before writing any code in the affected section.

If you are the AI implementer: **escalate** the listed cases under "Section 7 — Escalation list" to human review instead of resolving silently.

---

## Section 0 — TL;DR

Phase 10 changes nothing observable about the source language. After P10, every existing example produces the same emitted output on every target. The work is internal:

1. Replace the stringly-typed symbol-table `Any?` info with a typed `SymbolInfo`. Closes D2 and D6.
2. Make `lookup` public, add `markReadonlyLocal` + `commitReadonly` for the §2c flow-sensitive `readonly` model, add explicit `enterScope`/`exitScope`. Closes D2 further.
3. Introduce a new `ir/` package: a typed AST distinct from the ANTLR-derived `*Data` classes. Backends consume the IR; the IR carries types at every node; lowering is a single pass that runs after verification. Closes D1.
4. Introduce a new `verifier/` package: top-level entry point `Verifier.verifyModule`, per-statement-kind verify functions. `verify()` is removed from `Translatable` and from every `*Data` class. Closes D3.
5. Migrate one piece at a time in a defined order; tests stay green at every step.

The reason this is one phase: each piece changes interfaces the others depend on. Doing them separately means three traumatic interface migrations; doing them together means one. The cost is one larger phase; the benefit is that everything downstream (type inference at P11, sum types and `match` at P12, generics at P14) sits on top of a stable foundation instead of layering on more debt.

---

## Section 1 — `SymbolInfo` (closes D2 and D6)

### 1.1 Why

Today's `SymbolTable.kt:5` stores `MutableMap<String, Any?>`. Every callsite passes the type as a string. So:

- A variable, a function, and an argument all store as a type string — there's no way to ask "is `add` a function or a variable named `add`?" (audit D6 + surprise #9).
- There's no `isReadonly` flag (audit Section 4, Form B blocking item).
- The declaration's source position isn't stored (needed for friendly errors at P11+, audit D5).
- The type itself is a `String` — `"int"`, `"int[]"`, `"void"` — so any future type-system work has to re-parse it (audit Section 3 on `PrimitiveTypes`).

P10 fixes all four in one struct.

### 1.2 `WaterfallType` — typed type representation

Before `SymbolInfo`, we need a real type representation. Today the type is a string. Replace it with a sealed class.

**File**: `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/typesystem/WaterfallType.kt` (new file).

```kotlin
package com.aaroncoplan.waterfall.compiler.typesystem

/**
 * A Waterfall type expression. Closed sum type — every variant must be matched
 * exhaustively. Adding a variant requires updating every `when (t)` site.
 *
 * Equality is structural (data classes / data objects). Pretty-printing via
 * [render] is the canonical user-facing form.
 */
sealed class WaterfallType {

    /** Render as the source-language form. Inverse of parsing a `type` rule. */
    abstract fun render(): String

    /** The four scalar primitives. */
    data object IntType : WaterfallType() { override fun render() = "int" }
    data object DecType : WaterfallType() { override fun render() = "dec" }
    data object BoolType : WaterfallType() { override fun render() = "bool" }
    data object CharType : WaterfallType() { override fun render() = "char" }

    /** Array of a primitive (today only; later phases may relax). */
    data class ArrayType(val element: WaterfallType) : WaterfallType() {
        override fun render() = "${element.render()}[]"
    }

    /**
     * The return type for a function with no `returns` clause. Distinct from
     * any value type. `VoidType.render()` returns `"void"` by convention.
     */
    data object VoidType : WaterfallType() { override fun render() = "void" }

    /**
     * A type we couldn't resolve. Carries the source text the parser saw so
     * the verifier can produce a friendly error. ErrorType never appears in
     * valid programs that pass verification.
     */
    data class ErrorType(val sourceText: String) : WaterfallType() {
        override fun render() = "<error:$sourceText>"
    }

    companion object {
        /**
         * Parse a `type` rule's text (e.g. "int", "int[]", "?int") into a
         * WaterfallType. Returns ErrorType when the text isn't recognized.
         *
         * For P10 we accept exactly what today's `PrimitiveTypes.isPrimitiveOrArray`
         * accepts. The leading `?` (nullable) is parsed but produces ErrorType —
         * matches today's behavior (audit surprise #5).
         */
        fun fromSourceText(text: String): WaterfallType {
            if (text.startsWith("?")) return ErrorType(text)
            val base = if (text.endsWith("[]")) text.dropLast(2) else text
            val baseType = when (base) {
                "int"  -> IntType
                "dec"  -> DecType
                "bool" -> BoolType
                "char" -> CharType
                "void" -> VoidType
                else   -> return ErrorType(text)
            }
            return if (text.endsWith("[]")) ArrayType(baseType) else baseType
        }
    }
}
```

**PITFALL #1** — The `void` case. Today `FunctionImplementationData.verify` at line 35 stores `returnType ?: "void"` in the symbol table. That string `"void"` is the same string the audit calls inconsistent (Section 3, `PrimitiveTypes`). Under the new scheme `VoidType` is a first-class variant of `WaterfallType`. `PrimitiveTypes.ALL` still does not contain `"void"`, which is correct because `void` is *not* a value type — it's only a return type. The verifier must reject `void x = ...` declarations (this was implicit before; make it explicit). Escalate if you discover any other site that conflated `"void"` with primitive types.

**PITFALL #2** — Don't add variants speculatively. The IR (Section 3) will need more type variants for records and sum types (P12), generics (P14). Those are *not* part of P10. Add them in their phases. Leaving them out now means `when (t)` matches are exhaustive at P10 and will get a compile error when new variants land — that's the desired error, it tells the implementer "you must handle this here too."

### 1.3 `SymbolKind` — the kind discriminator (closes D6)

**File**: `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/symboltables/SymbolKind.kt` (new file).

```kotlin
package com.aaroncoplan.waterfall.compiler.symboltables

/**
 * What kind of symbol this is. Distinguishes a function named `add` from a
 * variable named `add`, which today's symbol table cannot do (audit D6).
 *
 * P10 needs only Variable / Function / Argument — those are what the existing
 * compiler tracks. Records, sum types, and modules get their own variants when
 * the corresponding phases land (P12, P14).
 */
sealed class SymbolKind {
    /** A `int x = ...` or `x := ...` binding. */
    data object Variable : SymbolKind()

    /** A function parameter inside its function body. Distinct from Variable
     *  so the verifier can later print specialized error messages. */
    data object Argument : SymbolKind()

    /**
     * A top-level `func ... {}` declaration. The [returnType] is also stored
     * on the SymbolInfo, but storing the parameter signature here lets a later
     * phase implement call-site type-checking without re-walking the AST.
     *
     * P10 doesn't yet enforce call-arg types — that's P11. But the data is
     * available from P10 onward.
     */
    data class Function(
        val parameters: List<Pair<String, com.aaroncoplan.waterfall.compiler.typesystem.WaterfallType>>
    ) : SymbolKind()
}
```

**PITFALL #3** — Don't fold the function-signature info into `SymbolInfo.type`. The Audit Section 3 on `PrimitiveTypes` notes that today the function name is declared with its *return type* as the info value. That's stringly-typed and ambiguous (`add: "int"` is indistinguishable from a variable `int add = 0`). The fix is structural: store `kind = SymbolKind.Function(params)` and store the *return type* in `SymbolInfo.type`. This makes "what kind of thing is this?" answerable in one lookup.

### 1.4 `SymbolInfo` — the entry value

**File**: `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/symboltables/SymbolInfo.kt` (new file).

```kotlin
package com.aaroncoplan.waterfall.compiler.symboltables

import com.aaroncoplan.waterfall.compiler.statements.helpers.SourcePosition
import com.aaroncoplan.waterfall.compiler.typesystem.WaterfallType

/**
 * Per-binding state stored in the symbol table. Replaces the `Any?` value at
 * `SymbolTable.kt:5` (audit D2).
 *
 * Immutable. To "change" a binding's readonly state without a parent-scope
 * write (the §2c shadow model), the [SymbolTable] uses [SymbolTable.markReadonlyLocal];
 * to commit a flow-sensitive promotion at a branch join, it uses [SymbolTable.commitReadonly].
 * Both produce new SymbolInfo values via `.copy(isReadonly = true)` — the original
 * is never mutated in place.
 *
 * Equality and hashCode are structural (data class). Two SymbolInfos are equal
 * iff every field is equal, including source position.
 */
data class SymbolInfo(
    /** The declared (or inferred at P11+) type. For functions this is the
     *  return type; the parameter list lives on [kind]. */
    val type: WaterfallType,

    /**
     * Whether the binding is readonly. True iff the source declaration used
     * the `readonly` modifier (Piece 2 of this task), OR a flow-sensitive
     * `readonly x` statement has fired and the symbol table is exposing the
     * promoted state via [SymbolTable.lookup].
     */
    val isReadonly: Boolean,

    /** Variable / Argument / Function. Closes D6. */
    val kind: SymbolKind,

    /** Where this binding was declared. Used for friendly errors at P11+. */
    val sourcePosition: SourcePosition
)
```

**PITFALL #4** — `isReadonly` here is a *property of the entry as currently observed via lookup*, not a property of the *original declaration*. That distinction matters at branch joins: in a child scope's view of an ancestor-owned binding, `isReadonly` may be `true` because of `markReadonlyLocal` even though the parent scope's stored entry has `isReadonly = false`. See Section 2.4 for the resolution semantics. If you implement `lookup` such that it returns a different `SymbolInfo` value depending on shadow state, ensure you understand the test cases in §2.7.

---

## Section 2 — `SymbolTable` public API

### 2.1 Why

Today (`SymbolTable.kt:21`) `lookup` is `private`. No external caller can ask "is `x` readonly?" or "what's the type of `x`?". This blocks the type-inference work at P11, the call-site type-checking at P11, and the Form B `readonly` enforcement (audit Section 4).

The full new API is below. Stick to these signatures; don't add helpers speculatively.

### 2.2 The class itself

**File**: `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/symboltables/SymbolTable.kt` (replaces the existing file).

```kotlin
package com.aaroncoplan.waterfall.compiler.symboltables

import com.aaroncoplan.waterfall.compiler.statements.helpers.SourcePosition

/**
 * Lexically-scoped symbol table. Each `SymbolTable` represents one scope; child
 * scopes are constructed via [enterScope]. Lookups walk the parent chain.
 *
 * The transactional readonly model (per design doc §2c):
 *   - [declare] writes a binding to *this* scope's own storage. Throws if the
 *     name is in this scope or any ancestor scope (current rule, audit
 *     `DuplicateInnerDeclarationTest`).
 *   - [markReadonlyLocal] records a flow-sensitive promotion of `name` in
 *     *this* scope's local `readonlyShadow`. Parent scopes (and sibling branches)
 *     do NOT observe this change.
 *   - [commitReadonly] is invoked at a branch join (by [verifier.IfBlockVerifier]
 *     and friends) to persist a promotion to the scope that originally owns the
 *     binding.
 *   - [lookup] returns the *effective* SymbolInfo at the current scope, including
 *     any local shadow promotion stacked on top of the owning scope's stored info.
 *
 * NOT thread-safe. The compiler is single-threaded; if that changes, this becomes
 * a `ConcurrentHashMap`.
 */
class SymbolTable private constructor(private val parent: SymbolTable?) {

    /** Symbols this scope OWNS — i.e., declared in this scope via [declare]. */
    private val owned: MutableMap<String, SymbolInfo> = HashMap()

    /**
     * Names locally promoted via [markReadonlyLocal] in this scope. These are
     * NOT new declarations — every name here resolves to an entry in `owned`
     * here or in some ancestor. The shadow only flips `isReadonly` to true on
     * lookup; it never adds new symbols, never changes the type, never changes
     * the kind, never changes the declaration position.
     */
    private val readonlyShadow: MutableSet<String> = HashSet()

    /** Root-scope constructor for tests and the top-level driver. */
    constructor() : this(parent = null)

    // ---------------------------------------------------------------------- //
    // Scope management
    // ---------------------------------------------------------------------- //

    /**
     * Create a child scope rooted at `this`. The child sees `this` via lookup
     * but can't write to it directly.
     *
     * Semantics: the caller is responsible for not leaking the child past its
     * lifetime. Typical usage is:
     *
     *     val child = parent.enterScope()
     *     // ... verify statements against `child` ...
     *     parent.exitScope(child)  // see [exitScope] for what this does
     */
    fun enterScope(): SymbolTable = SymbolTable(parent = this)

    /**
     * Marker that a child scope is being abandoned. P10's symbol table is GC'd
     * (Kotlin object lifecycle), so this is a no-op in production code paths.
     * Provided so call sites read symmetrically with [enterScope]; future LSP
     * work may need a hook here for incremental compilation.
     *
     * Returns the snapshot of the child's local readonly shadow before abandonment.
     * Branch-join code (see §2.5) uses this snapshot to compute the intersection
     * across predecessors.
     */
    fun exitScope(child: SymbolTable): Set<String> {
        require(child.parent === this) {
            "exitScope: child's parent doesn't match. Did you mix up two scopes?"
        }
        return child.readonlyShadow.toSet()
    }

    // ---------------------------------------------------------------------- //
    // Core API
    // ---------------------------------------------------------------------- //

    /**
     * Declare a new binding in *this* scope.
     *
     * Errors:
     *   - DuplicateDeclarationError if `name` is already declared in this scope
     *     or any ancestor scope. (Shadowing is rejected — current behavior,
     *     preserved here.)
     */
    fun declare(name: String, info: SymbolInfo): DeclareResult {
        // Reject if any ancestor scope owns it.
        if (parent?.lookupOwning(name) != null) {
            return DeclareResult.Failure(
                DuplicateDeclarationError(
                    name = name,
                    attemptedAt = info.sourcePosition,
                    previouslyDeclaredAt = parent.lookupOwningPosition(name)
                )
            )
        }
        // Reject if this scope owns it.
        if (owned[name] != null) {
            return DeclareResult.Failure(
                DuplicateDeclarationError(
                    name = name,
                    attemptedAt = info.sourcePosition,
                    previouslyDeclaredAt = owned[name]!!.sourcePosition
                )
            )
        }
        owned[name] = info
        return DeclareResult.Success
    }

    /**
     * Look up `name` in this scope's chain. Returns null if not found.
     *
     * If any scope in the chain (this or an ancestor) has `name` in its
     * `readonlyShadow`, the returned SymbolInfo has `isReadonly = true`,
     * regardless of whether the owning scope's stored entry was readonly.
     *
     * Specifically:
     *   1. Walk the chain top-down (this -> parent -> ... -> root) looking
     *      for the *owning* scope (the one whose `owned` map contains `name`).
     *   2. Take that owning scope's stored SymbolInfo as the base.
     *   3. Walk the chain from `this` up to (and including) the owning scope,
     *      OR-ing in any `readonlyShadow` membership.
     *   4. If any scope in that range shadowed `name`, return `base.copy(isReadonly = true)`.
     *      Otherwise return `base` as-is.
     *
     * Returns null if no scope in the chain owns `name`.
     */
    fun lookup(name: String): SymbolInfo? {
        val owningScope = findOwningScope(name) ?: return null
        val base = owningScope.owned[name]!!

        // Walk this -> ... -> owningScope, OR-ing in shadow membership.
        var cursor: SymbolTable? = this
        while (cursor != null) {
            if (cursor.readonlyShadow.contains(name)) {
                return base.copy(isReadonly = true)
            }
            if (cursor === owningScope) break
            cursor = cursor.parent
        }
        return base
    }

    /**
     * Form B promotion. Records `name` in *this scope's* local shadow.
     *
     * Returns false (and is a no-op) if `name` is not visible in the scope
     * chain. The caller — the verifier — should have called [lookup] first
     * and emitted a friendly error.
     *
     * No parent-state mutation: a sibling scope verified after this call sees
     * the same parent state as if this call had never happened. This is the
     * property that makes the branch-join intersection rule correct (§2d of
     * the language design doc).
     */
    fun markReadonlyLocal(name: String): Boolean {
        if (lookup(name) == null) return false
        readonlyShadow.add(name)
        return true
    }

    /**
     * Commit a set of flow-sensitive readonly promotions to the *current* scope.
     * Invoked by branch-join code (see [verifier.JoinAnalysis]) after computing
     * the intersection of every reaching predecessor's local shadow.
     *
     * Walk-depth boundary (round-4 F8 fix): `commitReadonly` writes ONLY to
     * `this.readonlyShadow`. It does NOT walk up the parent chain. It does NOT
     * mutate the owning scope's `owned` map. This is the load-bearing property
     * for nested-if correctness.
     *
     * Why: when an inner if/else's join calls `commitReadonly` on its enclosing
     * scope (say, the outer if's then-branch scope T_outer), the commit must
     * stay inside T_outer. If it walked up to the scope that owns the binding
     * (typically the surrounding function), the binding would become readonly
     * for a sibling outer-if-else branch that never promoted it. The outer
     * if's own join, when it later runs, must do its OWN intersection — and
     * only then commit further. Each join level is independent.
     *
     * Names not in scope (no `lookup(name)` would resolve them) are silently
     * ignored. The caller should already have verified the name exists.
     *
     * Effect on lookup: because [lookup] walks the chain top-down OR-ing in
     * `readonlyShadow` membership, a name committed into `this.readonlyShadow`
     * is reported as `isReadonly = true` by [lookup] calls from this scope
     * and any deeper child scope, just as if the owning scope's entry had
     * been promoted. The semantic effect is the same; the durability scope
     * is the *invoking scope's lifetime*, not "forever in the owning scope."
     */
    fun commitReadonly(names: Set<String>) {
        for (name in names) {
            // Sanity: must be visible somewhere in the chain. If not, skip.
            if (lookup(name) == null) continue
            readonlyShadow.add(name)
        }
    }

    /** Snapshot the local shadow (for join-analysis). Defensive copy. */
    fun localReadonlyShadow(): Set<String> = readonlyShadow.toSet()

    // ---------------------------------------------------------------------- //
    // Internal helpers
    // ---------------------------------------------------------------------- //

    private fun findOwningScope(name: String): SymbolTable? {
        if (owned.containsKey(name)) return this
        return parent?.findOwningScope(name)
    }

    private fun lookupOwning(name: String): SymbolInfo? = findOwningScope(name)?.owned?.get(name)

    private fun lookupOwningPosition(name: String): SourcePosition? =
        findOwningScope(name)?.owned?.get(name)?.sourcePosition
}

/** Result of [SymbolTable.declare]. */
sealed class DeclareResult {
    data object Success : DeclareResult()
    data class Failure(val error: DuplicateDeclarationError) : DeclareResult()
}

/** Replaces the existing [DuplicateDeclarationException] for richer errors. */
data class DuplicateDeclarationError(
    val name: String,
    val attemptedAt: SourcePosition,
    val previouslyDeclaredAt: SourcePosition?
)
```

**PITFALL #5** — Don't widen `markReadonlyLocal` to a `markReadonly` method that walks to the owning scope and mutates there directly. That was the bug in the first draft of design doc §2c (skeptic F8). The destructive write to the parent breaks the intersection rule because sibling branches see each other's promotions. Use `commitReadonly` (only invoked from join code, never from `ReadonlyPromotionVerifier`) for the parent-scope commit.

**PITFALL #5b** (round-4 F8 follow-up) — Don't make `commitReadonly` walk further than the *current* scope. The function writes only to `this.readonlyShadow`; it never mutates `this.parent` or any ancestor. A common wrong instinct is "promote the binding where it was declared" — that's the destructive variant from PITFALL #5 again, just initiated from a different call site. Each control-flow join level is independent: an inner if/else's join commits to its immediate parent scope (where the join happens). When the surrounding outer if/else's join runs, it does its own intersection over its own predecessors' shadows. If the outer join's intersection includes the binding, it commits one level further; if not, the binding stays mutable past the outer join. The shadow-only commit is what makes this work — durable promotion of the owning scope's entry would skip the outer join's check entirely.

**PITFALL #6** — `lookup` walks both up the parent chain *and* applies shadows along the way. The temptation is to apply only the immediate scope's shadow. That's wrong: a `readonly x` in an outer scope must be visible to a nested inner scope's lookups. The walk-and-OR pattern in `lookup` above is the correct semantics. Test cases in §2.7 enforce this.

### 2.3 Behavior matrix

A reference table for the AI implementer to cross-check their implementation against:

| Scenario | `declare` | `lookup` returns | `markReadonlyLocal` | `commitReadonly` |
|---|---|---|---|---|
| Declare `x` in scope A, lookup `x` from A | Success | `SymbolInfo` from A's owned map | n/a | n/a |
| Declare `x` in A, lookup from A's child C | n/a | A's stored info, unmodified | n/a | n/a |
| Same as above, but C called `markReadonlyLocal("x")` | n/a | A's info `.copy(isReadonly = true)` | true (returned) | n/a |
| Same, then C exits and a sibling C' looks up `x` | n/a | A's info, *unmodified* (shadow died with C) | n/a | n/a |
| Both C and C' shadow `x`, then A `commitReadonly({"x"})` | n/a | A's info `.copy(isReadonly = true)` — the commit lands in A's own `readonlyShadow`, not in the owning scope. Lookups from A and from A's children return readonly; lookups from A's siblings or A's parent do *not*. | n/a | n/a |
| `declare` `x` in scope A, then in child C try `declare` `x` again | n/a | n/a | n/a | n/a; `declare` returns `Failure(DuplicateDeclarationError)` |
| `declare` `x` in A, look up `y` (not declared) | n/a | null | n/a | n/a |
| `markReadonlyLocal("undeclared")` | n/a | n/a | false (no-op) | n/a |
| `commitReadonly({"undeclared"})` | n/a | n/a | n/a | silent no-op |

### 2.4 Migration of existing callsites

The existing `declare(key, info)` calls pass a string. Each one must be rewritten:

- `TypedVariableDeclarationAndAssignmentData.verify` (line 30) → use `SymbolInfo(type = WaterfallType.fromSourceText(type), isReadonly = modifiers.contains("readonly"), kind = SymbolKind.Variable, sourcePosition = getSourcePosition())`. The existing `isImmutable()` helper (line 22) is removed during the migration — under the unified `readonly` keyword (language-design §2g), the modifier set has exactly one entry and the explicit `.contains("readonly")` check is clearer than a wrapper method.
- `UntypedVariableDeclarationAndAssignmentData.verify` (line 25) → same shape, with `WaterfallType.fromSourceText(inferredType)`.
- `FunctionImplementationData.verify` (line 35, self-decl for recursion) → `SymbolInfo(type = WaterfallType.fromSourceText(returnType ?: "void"), isReadonly = true, kind = SymbolKind.Function(parameters = [...]), sourcePosition = getSourcePosition())`. **PITFALL #7**: functions are *implicitly readonly*. You can't reassign a function's name. Today the verifier doesn't reject `fib = 5` after `func fib(...)` (audit Section 1, row "Function-arg type-checking") but P10 should — once functions store `isReadonly = true`, the assignment verifier (P11 work) rejects them automatically.
- `FunctionImplementationData.verify` (line 47, each typed argument) → `SymbolInfo(type = WaterfallType.fromSourceText(arg.firstVal), isReadonly = false, kind = SymbolKind.Argument, sourcePosition = ... /* position of the argument in the parameter list, not the function decl */)`. The position computation here is fiddly — see PITFALL #8.

**PITFALL #8** — Argument source position. Today, `FunctionImplementationData` stores `typedArguments: List<Pair<String, String>>` (type, name). There's no per-arg `SourcePosition`. The fix: change `FunctionImplementationData` to store a list of richer records that include the per-arg `SourcePosition` extracted from the ANTLR `typedArgument` rule context. Specifically, in the existing `FunctionImplementationData.kt:20-23` init block, walk `ctx.typedArgumentList()?.typedArgument()` and for each one capture `arg.name.symbol`'s line + column. Don't try to point at the function's `getSourcePosition()` for each arg — that produces useless "argument at line 5" errors when the function decl is on line 5 and the arg is column 30. Real per-arg positions are needed for friendly errors. **Escalate** if the existing `*Data` class hierarchy resists this change without a sweep.

### 2.5 Branch-join API contract

The branch-join logic itself lives in the verifier (Section 4), not on the symbol table. But the API the verifier uses is on the symbol table. Concretely:

The verifier, for each branch of an `if/elif/else`:

1. `val childScope = parentScope.enterScope()`
2. Verify the body statements against `childScope`. Each `readonly x` statement calls `childScope.markReadonlyLocal("x")`.
3. After the body is verified, `val shadowSnapshot = parentScope.exitScope(childScope)`.
4. Record `(shadowSnapshot, branchTerminates)` for this branch.

After all branches:

5. Filter out terminating branches (the ones that ended with `return`).
6. If at least one non-terminating branch remains, compute the intersection of their `shadowSnapshot` sets.
7. `parentScope.commitReadonly(intersection)`.

That's the only API surface needed. No `getReadonlyShadowFromAncestor` or similar — the verifier uses `localReadonlyShadow` on the child scope (which the `exitScope` snapshot returns), not on ancestors.

**PITFALL #9** — Loops use the same pattern with a structural twist. The loop body has at least two predecessors at the post-loop join: the "loop ran zero times" path (with empty shadow) and the "loop ran at least once" path (with body's shadow). The intersection of any set with the empty set is empty, so loops *never* propagate `readonly` past the loop body. This is the design (§2a.8); don't try to "fix" it. If you find yourself making loops propagate, you're implementing it wrong — re-read the design.

### 2.6 What goes away

After P10, the following are removed:

- `private fun lookup(...)` becomes the public `lookup` defined above. Anywhere else in the code that worked around `lookup` being private (none today, but watch for them in the migration) needs cleanup.
- `DuplicateDeclarationException` (the checked exception class) is removed; callers consume `DeclareResult.Failure` instead. The class file stays in source control for one release as a deprecated stub if needed for incremental migration, but the goal is to delete it.
- The stringly-typed `Any?` map is gone.

### 2.7 Required test cases

These are non-negotiable. Every one must pass.

```kotlin
package com.aaroncoplan.waterfall.compiler.symboltables

import com.aaroncoplan.waterfall.compiler.statements.helpers.SourcePosition
import com.aaroncoplan.waterfall.compiler.typesystem.WaterfallType
import org.junit.Test
import org.junit.Assert.*

class SymbolTableTest {

    private fun pos(line: Int = 1, col: Int = 0) =
        SourcePosition(fileName = "test.wf", line = line, column = col)

    private fun varInfo(type: WaterfallType, readonly: Boolean = false) =
        SymbolInfo(type, readonly, SymbolKind.Variable, pos())

    @Test fun lookupMissingReturnsNull() {
        val st = SymbolTable()
        assertNull(st.lookup("nope"))
    }

    @Test fun declareThenLookup() {
        val st = SymbolTable()
        st.declare("x", varInfo(WaterfallType.IntType))
        assertEquals(WaterfallType.IntType, st.lookup("x")?.type)
        assertFalse(st.lookup("x")!!.isReadonly)
    }

    @Test fun duplicateInSameScopeFails() {
        val st = SymbolTable()
        st.declare("x", varInfo(WaterfallType.IntType))
        val result = st.declare("x", varInfo(WaterfallType.DecType))
        assertTrue(result is DeclareResult.Failure)
    }

    @Test fun shadowingAncestorFails() {
        val parent = SymbolTable()
        parent.declare("x", varInfo(WaterfallType.IntType))
        val child = parent.enterScope()
        val result = child.declare("x", varInfo(WaterfallType.DecType))
        assertTrue(result is DeclareResult.Failure)
    }

    @Test fun childLookupSeesAncestor() {
        val parent = SymbolTable()
        parent.declare("x", varInfo(WaterfallType.IntType))
        val child = parent.enterScope()
        assertEquals(WaterfallType.IntType, child.lookup("x")?.type)
    }

    @Test fun markReadonlyLocalAffectsChild() {
        val parent = SymbolTable()
        parent.declare("x", varInfo(WaterfallType.IntType))
        val child = parent.enterScope()
        child.markReadonlyLocal("x")
        assertTrue(child.lookup("x")!!.isReadonly)
    }

    @Test fun markReadonlyLocalDoesNotAffectParent() {
        val parent = SymbolTable()
        parent.declare("x", varInfo(WaterfallType.IntType))
        val child = parent.enterScope()
        child.markReadonlyLocal("x")
        assertFalse(parent.lookup("x")!!.isReadonly)
    }

    @Test fun markReadonlyLocalDoesNotAffectSibling() {
        val parent = SymbolTable()
        parent.declare("x", varInfo(WaterfallType.IntType))
        val child1 = parent.enterScope()
        child1.markReadonlyLocal("x")
        parent.exitScope(child1)
        val child2 = parent.enterScope()
        assertFalse(child2.lookup("x")!!.isReadonly)
    }

    @Test fun commitReadonlyIsVisibleInInvokingScope() {
        // After commitReadonly on a scope, lookups from THAT scope (and its
        // descendants) return readonly. Round-4 F8 fix: the commit is local
        // to the invoking scope; siblings of the invoking scope are unaffected.
        // See `commitReadonlyDoesNotLeakToSibling` below for the sibling case.
        val parent = SymbolTable()
        parent.declare("x", varInfo(WaterfallType.IntType))
        parent.commitReadonly(setOf("x"))
        assertTrue(parent.lookup("x")!!.isReadonly)
    }

    @Test fun exitScopeReturnsLocalShadow() {
        val parent = SymbolTable()
        parent.declare("x", varInfo(WaterfallType.IntType))
        parent.declare("y", varInfo(WaterfallType.IntType))
        val child = parent.enterScope()
        child.markReadonlyLocal("x")
        val snap = parent.exitScope(child)
        assertEquals(setOf("x"), snap)
    }

    @Test fun lookupReturnsReadonlyWhenAncestorShadowed() {
        val grandparent = SymbolTable()
        grandparent.declare("x", varInfo(WaterfallType.IntType))
        val parent = grandparent.enterScope()
        parent.markReadonlyLocal("x")
        val child = parent.enterScope()
        // The shadow is on parent; child's lookup must see it.
        assertTrue(child.lookup("x")!!.isReadonly)
    }

    @Test fun functionKindIsDistinguishable() {
        val st = SymbolTable()
        val fnInfo = SymbolInfo(
            type = WaterfallType.IntType,
            isReadonly = true,
            kind = SymbolKind.Function(parameters = listOf("a" to WaterfallType.IntType)),
            sourcePosition = pos()
        )
        st.declare("add", fnInfo)
        val looked = st.lookup("add")
        assertTrue(looked!!.kind is SymbolKind.Function)
    }

    /**
     * Walk-depth boundary test (round-4 F8 fix). `commitReadonly` on an
     * intermediate scope must not leak the readonly state to siblings of
     * that scope (which would happen if commitReadonly walked up to the
     * owning scope and mutated `owned` there).
     */
    @Test fun commitReadonlyDoesNotLeakToSibling() {
        val grandparent = SymbolTable()
        grandparent.declare("x", varInfo(WaterfallType.IntType))
        val outerThen = grandparent.enterScope()
        outerThen.commitReadonly(setOf("x"))    // simulates an inner if/else join's commit
        // From outerThen and its descendants, x is readonly.
        assertTrue(outerThen.lookup("x")!!.isReadonly)
        val deeper = outerThen.enterScope()
        assertTrue(deeper.lookup("x")!!.isReadonly)
        // From a sibling of outerThen — what an outer-else branch would see — x is mutable.
        val outerElse = grandparent.enterScope()
        assertFalse(outerElse.lookup("x")!!.isReadonly)
        // From the grandparent itself, x is still mutable.
        assertFalse(grandparent.lookup("x")!!.isReadonly)
    }
}
```

If the implementer skips any of these, the symbol-table redesign is not complete.

---

## Section 3 — The IR (closes D1)

### 3.1 Why an IR

Today every backend walks `*Data` AST classes directly (audit D1). Three consequences:

1. **Lowering passes are impossible without IR mutation.** Audit U2 (C lambda lifting) is the canonical example — it needs to transform the tree before codegen. Nothing in today's pipeline supports that.
2. **Type information is not on the tree.** The `*Data` classes carry source-form types (`String`); the backends look them up imperatively. Type inference at P11 needs every expression node to carry its inferred type — that needs a new tree, not the existing `*Data`.
3. **The verify/codegen entanglement** (D3) is partly because `verify()` and `translate()` are both on the same `Translatable` interface, walking the same nodes. Splitting requires the codegen target to be a different tree shape than the input.

### 3.2 Package layout

**New package**: `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/ir/`.

Files in the new package, listed at the file level (each contains the noted sealed classes). Each file is small — sealed classes encourage one-file-per-hierarchy.

```
ir/
├── IrType.kt               -- Sealed class IrType, identical to WaterfallType for now.
├── IrModule.kt             -- IrModule (top-level), IrFunction, IrTopLevelVariable.
├── IrStatement.kt          -- Sealed class IrStatement and all variants.
├── IrExpression.kt         -- Sealed class IrExpression and all variants.
├── IrLowering.kt           -- Lowering pass: `Data` -> `Ir`. Takes a verified ModuleAst, returns IrModule.
├── README.md               -- One-paragraph doc: "This package is the IR. Backends consume it. Don't add verification logic here."
```

### 3.3 `IrType`

For P10, `IrType` is structurally identical to `WaterfallType` (Section 1.2). It exists as a separate sealed class for two reasons: (a) it lets the verifier and IR evolve independently when type inference grows the type lattice at P11; (b) it documents at the package boundary that the IR has its own type representation.

```kotlin
package com.aaroncoplan.waterfall.compiler.ir

import com.aaroncoplan.waterfall.compiler.typesystem.WaterfallType

/**
 * P10 minimal IR type. Same shape as WaterfallType — a wrapper that lets the
 * IR's representation evolve independently of the verifier's representation.
 *
 * After P11 grows type inference, IrType may carry richer info (e.g., source
 * expression position alongside the resolved type) that the verifier doesn't
 * need but the backends do.
 */
sealed class IrType {
    abstract fun render(): String
    abstract fun asWaterfallType(): WaterfallType

    data object Int : IrType() {
        override fun render() = "int"
        override fun asWaterfallType() = WaterfallType.IntType
    }
    data object Dec : IrType() {
        override fun render() = "dec"
        override fun asWaterfallType() = WaterfallType.DecType
    }
    data object Bool : IrType() {
        override fun render() = "bool"
        override fun asWaterfallType() = WaterfallType.BoolType
    }
    data object Char : IrType() {
        override fun render() = "char"
        override fun asWaterfallType() = WaterfallType.CharType
    }
    data class Array(val element: IrType) : IrType() {
        override fun render() = "${element.render()}[]"
        override fun asWaterfallType() = WaterfallType.ArrayType(element.asWaterfallType())
    }
    data object Void : IrType() {
        override fun render() = "void"
        override fun asWaterfallType() = WaterfallType.VoidType
    }

    companion object {
        fun fromWaterfallType(t: WaterfallType): IrType = when (t) {
            WaterfallType.IntType  -> Int
            WaterfallType.DecType  -> Dec
            WaterfallType.BoolType -> Bool
            WaterfallType.CharType -> Char
            WaterfallType.VoidType -> Void
            is WaterfallType.ArrayType -> Array(fromWaterfallType(t.element))
            is WaterfallType.ErrorType -> throw IllegalStateException(
                "Cannot lower error type to IR: ${t.sourceText}. " +
                "The verifier should have rejected this before lowering."
            )
        }
    }
}
```

### 3.4 `IrModule`, `IrFunction`, `IrTopLevelVariable`

```kotlin
package com.aaroncoplan.waterfall.compiler.ir

import com.aaroncoplan.waterfall.compiler.statements.helpers.SourcePosition

/**
 * The IR for a single Waterfall module. Produced by IrLowering, consumed by
 * each backend's emitProgram().
 */
data class IrModule(
    val name: String,
    val topLevelVariables: List<IrTopLevelVariable>,
    val functions: List<IrFunction>,
    val sourcePosition: SourcePosition
)

data class IrTopLevelVariable(
    val name: String,
    val type: IrType,
    val isReadonly: Boolean,        // true iff the source declaration used the `readonly`
                                    // modifier (per language-design §2g unification)
    val initializer: IrExpression,
    val sourcePosition: SourcePosition
)

data class IrFunction(
    val name: String,
    val parameters: List<IrParameter>,
    val returnType: IrType,         // IrType.Void for `void` functions
    val body: List<IrStatement>,
    val sourcePosition: SourcePosition
)

data class IrParameter(
    val name: String,
    val type: IrType,
    val sourcePosition: SourcePosition
)
```

### 3.5 `IrStatement`

```kotlin
package com.aaroncoplan.waterfall.compiler.ir

import com.aaroncoplan.waterfall.compiler.statements.helpers.SourcePosition

/**
 * Statement-level IR node. Every `*Data` class that subclasses TranslatableStatement
 * (`compiler/.../statements/helpers/TranslatableStatement.kt`) has an IR counterpart.
 *
 * Every variant carries its source position for error reporting from the codegen
 * layer (e.g., a backend can attribute an emit-time failure to the source line).
 *
 * No `verify()` method here. Verification lives in the verifier package
 * (Section 4); this IR is post-verification and post-lowering.
 *
 * No `translate()` method here either. Backends pattern-match on the sealed class
 * variants. The audit's D4 (backend duplication) doesn't go away in P10, but
 * sealed-class dispatch makes future backend deduplication tractable.
 */
sealed class IrStatement {
    abstract val sourcePosition: SourcePosition

    data class TypedVarDecl(
        val name: String,
        val type: IrType,
        val isReadonly: Boolean,
        val initializer: IrExpression,
        override val sourcePosition: SourcePosition
    ) : IrStatement()

    data class UntypedVarDecl(
        val name: String,
        val inferredType: IrType,       // populated by inference at P11; for P10 same as today's `inferType`
        val isReadonly: Boolean,
        val initializer: IrExpression,
        override val sourcePosition: SourcePosition
    ) : IrStatement()

    data class VarAssignment(
        val name: String,
        /** "=", "+=", "-=", "*=", "/=", "%=" — preserved from source. */
        val op: String,
        val value: IrExpression,
        override val sourcePosition: SourcePosition
    ) : IrStatement()

    data class IncrementStatement(
        val name: String,
        /** "++" or "--" */
        val op: String,
        override val sourcePosition: SourcePosition
    ) : IrStatement()

    /**
     * The Form B `readonly x` statement (per language-design §2 / Piece 2). After
     * P10 this is just a comment-level marker — emitted as nothing by every
     * backend. The IR carries it primarily for source-mapping completeness; a
     * later phase may drop it from the IR entirely.
     */
    data class ReadonlyPromotion(
        val name: String,
        override val sourcePosition: SourcePosition
    ) : IrStatement()

    data class IfBlock(
        val ifBranch: Branch,
        val elifBranches: List<Branch>,
        val elseBody: List<IrStatement>?,    // null if no else clause
        override val sourcePosition: SourcePosition
    ) : IrStatement() {
        data class Branch(val condition: IrExpression, val body: List<IrStatement>)
    }

    data class WhileBlock(
        val condition: IrExpression,
        val body: List<IrStatement>,
        override val sourcePosition: SourcePosition
    ) : IrStatement()

    data class ForBlock(
        val iteratorName: String,
        /** Today the grammar is `for (x in collectionId)` — collection is an identifier.
         *  When P11 grows real iterators this becomes an IrExpression. */
        val collectionName: String,
        val body: List<IrStatement>,
        override val sourcePosition: SourcePosition
    ) : IrStatement()

    data class ReturnStatement(
        val value: IrExpression?,        // null for bare `return`
        override val sourcePosition: SourcePosition
    ) : IrStatement()

    /**
     * A function-call used in statement position. Distinct from FunctionCall
     * the expression because backends emit a trailing `;` here in JS/C and
     * nothing in Python.
     */
    data class FunctionCallStatement(
        val call: IrExpression.FunctionCall,
        override val sourcePosition: SourcePosition
    ) : IrStatement()
}
```

### 3.6 `IrExpression`

```kotlin
package com.aaroncoplan.waterfall.compiler.ir

import com.aaroncoplan.waterfall.compiler.statements.helpers.SourcePosition

/**
 * Expression-level IR. Every variant carries:
 *   - its source position
 *   - its inferred result type (IrType)
 *
 * For P10, the inferred type is set from the source `type` (declared types
 * are honored verbatim) or from literal kind (today's inference). P11 grows
 * a real inference pass that fills these in for arithmetic, calls, etc.
 */
sealed class IrExpression {
    abstract val type: IrType
    abstract val sourcePosition: SourcePosition

    data class NullLiteral(
        override val type: IrType,        // P10: IrType.Void (today's "null" is untyped)
        override val sourcePosition: SourcePosition
    ) : IrExpression()

    data class BoolLiteral(
        val value: Boolean,
        override val sourcePosition: SourcePosition
    ) : IrExpression() {
        override val type: IrType = IrType.Bool
    }

    data class IntLiteral(
        val literalText: String,          // preserved verbatim so backends can emit "42" vs "0x2a" etc.
        override val sourcePosition: SourcePosition
    ) : IrExpression() {
        override val type: IrType = IrType.Int
    }

    data class DecLiteral(
        val literalText: String,
        override val sourcePosition: SourcePosition
    ) : IrExpression() {
        override val type: IrType = IrType.Dec
    }

    data class StringLiteral(
        /** Raw source text including the backticks. Backends call StringLiteralText.unescape. */
        val literalText: String,
        override val sourcePosition: SourcePosition
    ) : IrExpression() {
        override val type: IrType = IrType.Char   // matches today's untyped-string-infers-to-char behavior
    }

    data class Identifier(
        val name: String,
        override val type: IrType,
        override val sourcePosition: SourcePosition
    ) : IrExpression()

    data class BinaryOp(
        val op: String,                   // "+", "-", "and", "or", "equals", etc.
        val left: IrExpression,
        val right: IrExpression,
        override val type: IrType,        // P10: inferred by P11; populated with `left.type` as a placeholder
        override val sourcePosition: SourcePosition
    ) : IrExpression()

    data class Cast(
        val targetType: IrType,
        val operand: IrExpression,
        override val sourcePosition: SourcePosition
    ) : IrExpression() {
        override val type: IrType = targetType
    }

    data class ArrayIndex(
        val target: String,
        val index: IrExpression,
        override val type: IrType,
        override val sourcePosition: SourcePosition
    ) : IrExpression()

    data class ArrayLiteral(
        val elements: List<IrExpression>,
        override val type: IrType,        // IrType.Array(element) — element inferred from first element today
        override val sourcePosition: SourcePosition
    ) : IrExpression()

    data class BundleLiteral(
        val elements: List<IrExpression>,
        override val type: IrType,        // P10: placeholder — bundles still unspecified per audit U1
        override val sourcePosition: SourcePosition
    ) : IrExpression()

    data class Lambda(
        val parameters: List<IrParameter>,
        val body: FunctionCall?,           // null for empty `{}` body; today's grammar is one functionCall or empty
        override val type: IrType,        // P10: placeholder; lambdas don't have a first-class type until P11+
        override val sourcePosition: SourcePosition
    ) : IrExpression()

    /**
     * FunctionCall is BOTH an expression and (via [IrStatement.FunctionCallStatement])
     * a statement context. The IR class is shared.
     */
    data class FunctionCall(
        val kind: Kind,
        val moduleName: String?,           // non-null when kind = Module
        val receiverPath: List<String>,    // non-empty when kind = Object
        val functionName: String,
        val positionalArguments: List<IrExpression>,
        val namedArguments: List<Pair<String, IrExpression>>,
        override val type: IrType,        // the called function's return type, looked up at lowering time
        override val sourcePosition: SourcePosition
    ) : IrExpression() {
        enum class Kind { Local, Module, Object }
    }
}
```

### 3.7 `IrLowering` — the lowering pass

```kotlin
package com.aaroncoplan.waterfall.compiler.ir

import com.aaroncoplan.waterfall.compiler.statements.ModuleAst
// ... import each *Data class and SymbolTable, SymbolInfo

/**
 * Single pass converting a verified ModuleAst (with its `*Data` statements) to
 * an IrModule consumable by backends.
 *
 * Contract:
 *   - Input: a ModuleAst whose .verify() has completed successfully.
 *   - The symbol table from verification is also passed in — type info on
 *     expression nodes is looked up here.
 *   - Output: an IrModule. Throws if the input contains anything verification
 *     should have caught (e.g., an undeclared identifier in an expression).
 *
 * No verification logic in here. If you find yourself adding a check that
 * could fail validly, you're in the wrong package — move it to the verifier.
 */
object IrLowering {

    fun lowerModule(module: ModuleAst, symbolTable: SymbolTable): IrModule {
        // Maps each top-level var and function to its IR counterpart.
        // Implementation: walk module.topLevelVariables, walk module.functions.
        // ... (see Section 3.8 for the lowering algorithm per node type)
        TODO()
    }

    fun lowerStatement(stmt: TranslatableStatement, symbolTable: SymbolTable): IrStatement {
        // Walks the existing dispatcher pattern (StatementDispatcher.fromStatement)
        // but in reverse — `when (stmt)` over the known subclass list.
        TODO()
    }

    fun lowerExpression(expr: ExpressionData, symbolTable: SymbolTable): IrExpression {
        // Single big when over ExpressionData.Kind.
        TODO()
    }
}
```

### 3.8 Lowering algorithm per node type

The implementer should encode the mappings below as a `when` over the `*Data` subclasses (or `ExpressionData.Kind` for expressions). Every entry is a one-line specification of what to produce.

**Statements**:

| Input (`*Data` class) | Output (IrStatement) | Notes |
|---|---|---|
| `TypedVariableDeclarationAndAssignmentData` | `IrStatement.TypedVarDecl` | `type` from `WaterfallType.fromSourceText(.type)`; `isReadonly` from `.modifiers.contains("readonly")` (the only modifier in v1, per language-design §2g) |
| `UntypedVariableDeclarationAndAssignmentData` | `IrStatement.UntypedVarDecl` | `inferredType` from `WaterfallType.fromSourceText(.inferredType)` |
| `VariableAssignmentData` | `IrStatement.VarAssignment` | `op`, `value` mapped directly |
| `IncrementStatementData` | `IrStatement.IncrementStatement` | `op` directly |
| `ReadonlyPromotionData` (new, from Piece 2 design) | `IrStatement.ReadonlyPromotion` | `name` only |
| `IfBlockData` | `IrStatement.IfBlock` | recursively lower bodies and conditions |
| `WhileBlockData` | `IrStatement.WhileBlock` | recursively lower |
| `ForBlockData` | `IrStatement.ForBlock` | preserve `iteratorName` and `collectionName` |
| `ReturnStatementData` | `IrStatement.ReturnStatement` | recurse on `.value` if non-null |
| `FunctionCallStatementData` | `IrStatement.FunctionCallStatement` wrapping `lowerFunctionCall(.call)` | |

**Expressions** (by `ExpressionData.Kind`):

| Input kind | Output (IrExpression) | Notes |
|---|---|---|
| `NULL_LITERAL` | `IrExpression.NullLiteral` | type = `IrType.Void` for P10 |
| `BOOL_LITERAL` | `IrExpression.BoolLiteral` | value from `e.literalText == "true"` |
| `INT_LITERAL` | `IrExpression.IntLiteral` | literalText preserved |
| `DEC_LITERAL` | `IrExpression.DecLiteral` | literalText preserved |
| `STRING_LITERAL` | `IrExpression.StringLiteral` | literalText preserved (backticks and all); type = `IrType.Char` (audit gap) |
| `IDENTIFIER` | `IrExpression.Identifier` | `type` looked up via `symbolTable.lookup(name).type`; if null, **escalate** — verifier should have caught |
| `LAMBDA` | `IrExpression.Lambda` | parameters and body lowered |
| `BUNDLE` | `IrExpression.BundleLiteral` | placeholder type for P10 |
| `ARRAY` | `IrExpression.ArrayLiteral` | element type from first element's inferred type |
| `FUNCTION_CALL` | `IrExpression.FunctionCall` (also used in statement) | look up the function's return type via the symbol table for the `type` field |
| `BINARY_OP` | `IrExpression.BinaryOp` | type = `left.type` for P10 placeholder; P11 inference improves this |
| `ARRAY_INDEX` | `IrExpression.ArrayIndex` | type = element type of the target's array type via symbol lookup |
| `CAST` | `IrExpression.Cast` | targetType from `WaterfallType.fromSourceText(.castTargetType)` then `IrType.fromWaterfallType` |

### 3.9 Backend interface change

Today's `CodeGenerator` (file `target/CodeGenerator.kt`) takes `*Data` classes as inputs. After P10, it takes `Ir` classes.

```kotlin
package com.aaroncoplan.waterfall.compiler.target

import com.aaroncoplan.waterfall.compiler.ir.*

/**
 * Pluggable target-language code generator. Every method takes an IR node and
 * returns its rendered form in the target language. Backends never see the
 * source-form `*Data` classes anymore.
 */
interface CodeGenerator {
    fun name(): String
    fun emitProgram(module: IrModule): String

    fun emitTypedVarDecl(s: IrStatement.TypedVarDecl): String
    fun emitUntypedVarDecl(s: IrStatement.UntypedVarDecl): String
    fun emitVarAssignment(s: IrStatement.VarAssignment): String
    fun emitFunction(s: IrFunction): String
    fun emitIfBlock(s: IrStatement.IfBlock): String
    fun emitForBlock(s: IrStatement.ForBlock): String
    fun emitWhileBlock(s: IrStatement.WhileBlock): String
    fun emitFunctionCallStatement(s: IrStatement.FunctionCallStatement): String
    fun emitReturnStatement(s: IrStatement.ReturnStatement): String
    fun emitIncrementStatement(s: IrStatement.IncrementStatement): String
    fun emitReadonlyPromotion(s: IrStatement.ReadonlyPromotion): String

    fun emitExpression(e: IrExpression): String
    fun emitFunctionCall(c: IrExpression.FunctionCall): String
    fun emitLambda(l: IrExpression.Lambda): String
    fun emitArrayLiteral(a: IrExpression.ArrayLiteral): String
    fun emitBundleLiteral(b: IrExpression.BundleLiteral): String
}
```

**PITFALL #10** — Don't try to keep the old interface alive in parallel with the new one. The audit's D4 (backend duplication) is a real issue, but it's a *separate* problem from D1. P10 is just the IR migration. After P10, all four backends consume IR; addressing D4 (e.g., a shared "block-of-statements" helper) is post-P10 work.

---

## Section 4 — Verifier package (closes D3)

### 4.1 Why

Today `Translatable.verify(symbolTable)` is on every `*Data` class (audit `helpers/Translatable.kt:8`). The driver calls `verify` and `translate` on the same object. Three problems:

1. There's no place for a verifier to add a check that crosses multiple node types (e.g., "every `return` in a function returns the declared type") — every node verifies in isolation.
2. Mutating state during verify (e.g., `symbolTable.declare`) is the only side-channel for semantic info, and a reader can't tell at a glance whether `verify()` is a pure check or a state mutator.
3. Adding cross-cutting passes (`readonly` flow analysis from §2d, type inference from P11) requires either touching every `verify()` method or growing a parallel pass infrastructure.

P10 moves verification into its own package. The interface change is small but the architectural payoff is large.

### 4.2 Package layout

**New package**: `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/verifier/`.

```
verifier/
├── Verifier.kt             -- Top-level entry point. Takes ModuleAst + root SymbolTable, returns VerifyResult.
├── VerifyResult.kt         -- Replacement for VerificationResult. Carries an error list, not just one error.
├── VerifyError.kt          -- Sealed class for typed errors (each error is a data class with structured fields).
├── ModuleVerifier.kt       -- verifyModule(ModuleAst, SymbolTable): VerifyResult
├── StatementVerifier.kt    -- verifyStatement(s, symbolTable): List<VerifyError>
├── ExpressionVerifier.kt   -- verifyExpression(e, symbolTable, expectedType?): List<VerifyError>
├── JoinAnalysis.kt         -- The branch-join readonly intersection algorithm (§2d).
└── README.md               -- One-paragraph: "Verifier consumes *Data, mutates SymbolTable, returns VerifyResult. No codegen here."
```

### 4.3 The error types

```kotlin
package com.aaroncoplan.waterfall.compiler.verifier

import com.aaroncoplan.waterfall.compiler.statements.helpers.SourcePosition
import com.aaroncoplan.waterfall.compiler.symboltables.DuplicateDeclarationError
import com.aaroncoplan.waterfall.compiler.typesystem.WaterfallType

/**
 * Structured error data for the verifier. Replaces today's
 * `VerificationResult.errorMessage: String?` with typed variants so:
 *   - friendly-error rendering (P11+) can format each kind differently
 *   - LSP (P13) can serialize them to JSON
 *   - tests can match on kind, not on string prefixes
 *
 * Each variant carries a [primaryPosition] for the offending node, plus
 * structured fields the renderer can format. The string `message` is a
 * canonical short form for stderr output; renderers may produce richer text.
 */
sealed class VerifyError {
    abstract val primaryPosition: SourcePosition
    abstract val message: String

    data class UnknownType(
        val typeText: String,
        override val primaryPosition: SourcePosition
    ) : VerifyError() {
        override val message = "Type '$typeText' is not a recognized primitive or primitive array. " +
            "Known: int, dec, bool, char, and their array forms."
    }

    data class DuplicateDeclaration(
        val name: String,
        val previousPosition: SourcePosition?,
        override val primaryPosition: SourcePosition
    ) : VerifyError() {
        override val message: String = if (previousPosition != null) {
            "Duplicate declaration: '$name' was previously declared at ${previousPosition.generateMessage()}."
        } else {
            "Duplicate declaration: '$name'."
        }
    }

    /**
     * Form B promotion of a name not visible in scope.
     */
    data class ReadonlyOfUndeclared(
        val name: String,
        override val primaryPosition: SourcePosition
    ) : VerifyError() {
        override val message = "Cannot freeze undeclared binding '$name'. " +
            "A `readonly $name` statement requires `$name` to already be declared."
    }

    /**
     * Form A duplicate of a readonly declaration: this is the existing
     * "binding is already readonly" error, kept for double-`readonly x` cases.
     */
    data class AlreadyReadonly(
        val name: String,
        override val primaryPosition: SourcePosition
    ) : VerifyError() {
        override val message = "Binding '$name' is already readonly."
    }

    /**
     * Write to a readonly binding (assignment, compound assignment, increment).
     */
    data class AssignToReadonly(
        val name: String,
        val declarationPosition: SourcePosition,
        override val primaryPosition: SourcePosition
    ) : VerifyError() {
        override val message =
            "Cannot assign to readonly binding '$name' " +
            "(declared at ${declarationPosition.generateMessage()})."
    }

    data class IncrementOfReadonly(
        val name: String,
        val declarationPosition: SourcePosition,
        override val primaryPosition: SourcePosition
    ) : VerifyError() {
        override val message =
            "Cannot increment/decrement readonly binding '$name' " +
            "(declared at ${declarationPosition.generateMessage()})."
    }

    /**
     * Catch-all for converting a symbol-table-level error into a verifier
     * error. Used when [SymbolTable.declare] returns a Failure.
     */
    fun fromSymbolTable(e: DuplicateDeclarationError): DuplicateDeclaration =
        DuplicateDeclaration(
            name = e.name,
            previousPosition = e.previouslyDeclaredAt,
            primaryPosition = e.attemptedAt
        )
}

/**
 * Result of verifying a module. A non-empty `errors` list means the module
 * failed; an empty list means it succeeded.
 *
 * Unlike today's VerificationResult, errors accumulate. Verification of one
 * statement returning an error doesn't stop verification of subsequent
 * statements (the driver decides whether to bail). This is the P11+ friendly-
 * errors-with-multiple-issues precondition.
 *
 * NOTE: at the boundary, the existing `Main.run` aborts on the *first* error,
 * which is fine for P10 — but the verifier is structurally ready for the P11
 * "show all errors" upgrade. The driver change to accumulate-then-bail comes
 * with friendly errors at P11.
 */
data class VerifyResult(val errors: List<VerifyError>) {
    val isSuccessful: Boolean get() = errors.isEmpty()
}
```

### 4.4 Verifier API

```kotlin
package com.aaroncoplan.waterfall.compiler.verifier

import com.aaroncoplan.waterfall.compiler.statements.ModuleAst
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.compiler.target.TargetKeyword

object Verifier {
    /**
     * Verify a single module against a fresh SymbolTable. The symbol table is
     * mutated as declarations land.
     *
     * Returns a VerifyResult; the caller decides what to do on a non-empty
     * errors list (the current driver aborts on first; future drivers may
     * collect across modules).
     *
     * Target parameter (round-4 F12 fix):
     *
     *   - When `target == null`: verify the module as **target-agnostic**.
     *     Every `@external` annotation across every supported target keyword
     *     must be present for any function called via `@external`; any
     *     function called from Waterfall code with no Waterfall body AND a
     *     missing `@external` for any reserved target produces an error.
     *     This is the "library author wants to know it's callable everywhere"
     *     mode. Used by `wfpm publish` to validate before producing artifacts.
     *
     *   - When `target` is a specific `TargetKeyword` (Js, Python, C, Wasm):
     *     verify the module specifically for that target. A function call to
     *     a function with no `@external(<target>, ...)` and no Waterfall body
     *     produces an error; calls to functions with an `@external(<target>, ...)`
     *     are fine even if the function lacks support on OTHER targets. This
     *     is the "I'm building for this one target right now" mode.
     *
     * In P10, the only target-conditional check is `@external` partial-support
     * (per language-design §4.2 and §4.5). Beyond P10, more checks will use
     * this parameter — e.g., target-specific lints, target-conditional type
     * narrowing for FFI-bound types.
     *
     * AI-implementation note: `null` does NOT mean "default to some target."
     * It means "verify against ALL targets simultaneously." A common wrong
     * inference would be to treat `null` as "skip target-conditional checks
     * entirely" — that's silently permissive and exactly the failure mode
     * §4.2 was designed to prevent. The explicit semantics are: null = union;
     * specific = filter.
     */
    fun verifyModule(
        module: ModuleAst,
        symbolTable: SymbolTable,
        target: TargetKeyword? = null
    ): VerifyResult {
        return ModuleVerifier.verifyModule(module, symbolTable, target)
    }
}
```

The target is threaded through every level of verification that needs it:

- `ModuleVerifier.verifyModule(module, symbolTable, target)` — passes target down.
- `StatementVerifier.verifyStatement(stmt, scope, target)` — most statements don't care; pass-through.
- `ExpressionVerifier.verifyExpression(expr, scope, target, expectedType?)` — call-site checks use the target to decide whether a callee is reachable.

The `TargetKeyword` enum lives in `compiler/.../target/TargetKeyword.kt` (new file):

```kotlin
package com.aaroncoplan.waterfall.compiler.target

/** The set of valid first-argument values for `@external(...)`. */
enum class TargetKeyword {
    Js, Python, C, Wasm;

    companion object {
        /** Parse from the source text in an `@external` annotation. Null if unknown. */
        fun fromSourceText(text: String): TargetKeyword? = when (text) {
            "js" -> Js
            "python" -> Python
            "c" -> C
            "wasm" -> Wasm
            else -> null
        }
    }
}
```

`ModuleVerifier.verifyModule` (in its own file) does the existing two-pass walk: top-level vars first, then functions. The implementation mirrors today's loops in `Main.run` (lines 91-105) but uses `verifier/StatementVerifier.kt:verifyStatement` instead of `Translatable.verify`.

**Driver wiring** (`compiler/.../Main.kt`): the CLI's `--target` flag is parsed to a `TargetKeyword?` — when `--target` is passed, that's the specific value; when omitted, `null` (target-agnostic). The driver passes that value into `Verifier.verifyModule`. The legacy backend was dropped pre-P10 (Q5); `--target legacy` is rejected at CLI-parse time.

**PITFALL #14** — `target == null` is "verify against all targets," NOT "skip target-conditional checks." The wrong inference: "no target specified means I don't have to think about FFI." The right inference: "no target specified means EVERY target's `@external` set must be complete." When `wfpm publish` runs, it expects target-agnostic verification to fail loudly if any function is only callable on some targets — because that's exactly the situation the user wants flagged before pushing artifacts to npm/PyPI/headers. AI implementers must not collapse `null` into "permissive": that's a silently broken implementation that ships under-tested code to library consumers. Tests in §4.7 (`nestedIfDoubleInnerPromoteDoesNotLeak` and friends) use target=null; add explicit target-specific test cases when P12 lands the `@external` enforcement.

```kotlin
package com.aaroncoplan.waterfall.compiler.verifier

import com.aaroncoplan.waterfall.compiler.statements.*
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable

object StatementVerifier {

    /**
     * Verify a single statement and return any errors produced. Mutates the
     * given symbol table (declarations land here; Form B promotions go into
     * this scope's local shadow per §2c).
     */
    fun verifyStatement(stmt: TranslatableStatement, scope: SymbolTable): List<VerifyError> {
        return when (stmt) {
            is TypedVariableDeclarationAndAssignmentData -> verifyTypedVarDecl(stmt, scope)
            is UntypedVariableDeclarationAndAssignmentData -> verifyUntypedVarDecl(stmt, scope)
            is VariableAssignmentData -> verifyVarAssignment(stmt, scope)
            is IncrementStatementData -> verifyIncrement(stmt, scope)
            // Add ReadonlyPromotionData verifier after Piece 2 lands.
            // is ReadonlyPromotionData -> verifyReadonlyPromotion(stmt, scope)
            is IfBlockData -> verifyIfBlock(stmt, scope)
            is WhileBlockData -> verifyWhileBlock(stmt, scope)
            is ForBlockData -> verifyForBlock(stmt, scope)
            is ReturnStatementData -> verifyReturn(stmt, scope)
            is FunctionCallStatementData -> verifyFunctionCallStatement(stmt, scope)
            // FunctionImplementationData is verified at the top level by ModuleVerifier;
            // it's not a statement-position node despite extending TranslatableStatement
            // (audit Section 3, on the ModuleAst design). If you encounter one here,
            // the dispatcher is bugged.
            else -> error("Unexpected statement kind at verify-time: ${stmt::class.simpleName}")
        }
    }

    private fun verifyTypedVarDecl(s: TypedVariableDeclarationAndAssignmentData, scope: SymbolTable): List<VerifyError> {
        val errors = mutableListOf<VerifyError>()
        val type = WaterfallType.fromSourceText(s.type)
        if (type is WaterfallType.ErrorType) {
            errors += VerifyError.UnknownType(s.type, s.getSourcePosition())
        }
        val isReadonly = s.modifiers.contains("readonly")
        // Per language-design §2g, `readonly` is the only modifier in v1.
        // `const`/`imm` were removed; the parser rejects them with a friendly
        // migration error before this point, so they never reach the verifier.
        val info = SymbolInfo(
            type = type,
            isReadonly = isReadonly,
            kind = SymbolKind.Variable,
            sourcePosition = s.getSourcePosition()
        )
        val result = scope.declare(s.name, info)
        if (result is DeclareResult.Failure) {
            errors += VerifyError.fromSymbolTable(result.error)
        }
        // Verify the initializer expression. P10: no type-check (that's P11);
        // just walk for any nested errors (today's verify chain doesn't do this either).
        // Implementation: ExpressionVerifier.verifyExpression(s.value, scope, expectedType = null)
        return errors
    }

    private fun verifyVarAssignment(s: VariableAssignmentData, scope: SymbolTable): List<VerifyError> {
        val info = scope.lookup(s.name)
            ?: return emptyList()  // P10 doesn't error on unknown LHS — current behavior, audit's open gap.
                                   // P11 changes this.
        if (info.isReadonly) {
            return listOf(VerifyError.AssignToReadonly(
                name = s.name,
                declarationPosition = info.sourcePosition,
                primaryPosition = s.getSourcePosition()
            ))
        }
        return emptyList()
    }

    private fun verifyIncrement(s: IncrementStatementData, scope: SymbolTable): List<VerifyError> {
        val info = scope.lookup(s.name)
            ?: return emptyList()
        if (info.isReadonly) {
            return listOf(VerifyError.IncrementOfReadonly(
                name = s.name,
                declarationPosition = info.sourcePosition,
                primaryPosition = s.getSourcePosition()
            ))
        }
        return emptyList()
    }

    private fun verifyIfBlock(s: IfBlockData, scope: SymbolTable): List<VerifyError> {
        // Per JoinAnalysis: for each branch, enter a child scope, verify the body,
        // snapshot the shadow, check termination. Intersect at the join. Commit.
        return JoinAnalysis.verifyIfBlock(s, scope)
    }

    private fun verifyWhileBlock(s: WhileBlockData, scope: SymbolTable): List<VerifyError> {
        return JoinAnalysis.verifyWhileBlock(s, scope)
    }

    // ... etc.
}
```

### 4.5 The join algorithm (per design doc §2d)

The branch-intersection logic lives in `JoinAnalysis.kt`. The algorithm was specified in design doc §2d; here it's encoded as production Kotlin.

```kotlin
package com.aaroncoplan.waterfall.compiler.verifier

import com.aaroncoplan.waterfall.compiler.statements.IfBlockData
import com.aaroncoplan.waterfall.compiler.statements.WhileBlockData
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable

object JoinAnalysis {

    /**
     * Result of verifying one branch: the errors found and a structured summary
     * for the join.
     */
    private data class BranchSnapshot(
        val errors: List<VerifyError>,
        val localShadow: Set<String>,
        val terminates: Boolean
    )

    private fun verifyBranch(
        body: List<TranslatableStatement>,
        parent: SymbolTable
    ): BranchSnapshot {
        val child = parent.enterScope()
        val errors = mutableListOf<VerifyError>()
        for (s in body) {
            errors += StatementVerifier.verifyStatement(s, child)
        }
        val terminates = bodyEndsTerminating(body)
        val localShadow = parent.exitScope(child)
        return BranchSnapshot(errors, localShadow, terminates)
    }

    /**
     * True if the body's last statement is a `return` (which guarantees the
     * branch doesn't reach the join). P10 understands only `return`; later
     * phases that add `panic` or unreachable will extend this.
     */
    private fun bodyEndsTerminating(body: List<TranslatableStatement>): Boolean {
        val last = body.lastOrNull() ?: return false
        return last is com.aaroncoplan.waterfall.compiler.statements.ReturnStatementData
    }

    fun verifyIfBlock(s: IfBlockData, scope: SymbolTable): List<VerifyError> {
        val snapshots = mutableListOf<BranchSnapshot>()

        snapshots += verifyBranch(s.ifBranch.body, scope)
        for (elif in s.elifBranches) {
            snapshots += verifyBranch(elif.body, scope)
        }
        if (s.elseBody != null) {
            snapshots += verifyBranch(s.elseBody, scope)
        } else {
            // The implicit "skip the if entirely" path. Empty shadow,
            // non-terminating.
            snapshots += BranchSnapshot(emptyList(), emptySet(), terminates = false)
        }

        // Collect errors from every branch (don't bail on the first).
        val allErrors = snapshots.flatMap { it.errors }

        // Intersection over non-terminating predecessors only.
        val reaching = snapshots.filter { !it.terminates }
        if (reaching.isNotEmpty()) {
            val intersection = reaching
                .map { it.localShadow }
                .reduce { a, b -> a intersect b }
            if (intersection.isNotEmpty()) {
                scope.commitReadonly(intersection)
            }
        }

        return allErrors
    }

    fun verifyWhileBlock(s: WhileBlockData, scope: SymbolTable): List<VerifyError> {
        // Single-pass body verification — see design doc §2d for the proof
        // that one pass is sound. The body's promotions land in the body
        // scope's local shadow and die when the body scope dies. No
        // commitReadonly call — loops never propagate `readonly` past the
        // body. (PITFALL #9 in Section 2.5.)
        val snapshot = verifyBranch(s.body, scope)
        return snapshot.errors
    }
}
```

**PITFALL #11** — The "implicit else" branch in `verifyIfBlock` is critical. If you forget to add `BranchSnapshot(emptyList(), emptySet(), terminates = false)` when `s.elseBody == null`, the intersection becomes whatever the if-branch promoted (with no else branch to intersect with). That makes a one-sided promotion accidentally propagate, violating design doc §2a.5. Test cases in §4.7 enforce this.

#### Nested-if walk-depth example (round-4 F8 fix)

The intersection rule, applied recursively, has to be careful about *where* the commit lands. Below is a worked trace that motivates the walk-depth boundary spec'd in §2.2 on `commitReadonly`.

Source:

```
int x = 0
if (a) {
    if (b) { readonly x } else { /* x mutable */ }
    // inner join: x is NOT readonly here (intersection of {"x"} and {} is {})
    readonly x       // explicit promotion in outer if's then-branch
} else {
    /* x mutable */
}
// outer join: x is NOT readonly here (intersection of T_outer's {"x"} and E_outer's {} is {})
x = 5                // legal — x is still mutable at this point
```

Scope layout during verification:

```
F (function scope; owns x)
└── T_outer (outer if-then; child of F)
    ├── T_inner (inner if-then) — markReadonlyLocal("x") → readonlyShadow = {"x"}
    ├── E_inner (inner if-else) — readonlyShadow = {}
    └── inner join in T_outer:
          intersection({"x"}, {}) = {}
          → no commit (intersection is empty)
        then `readonly x` statement → T_outer.readonlyShadow gains "x"

└── E_outer (outer if-else; child of F) — readonlyShadow = {}

outer join in F:
  intersection(T_outer.readonlyShadow={"x"}, E_outer.readonlyShadow={}) = {}
  → no commit
```

Now consider the variant where both inner branches promote:

```
int x = 0
if (a) {
    if (b) { readonly x } else { readonly x }
    // inner join: x IS readonly here (intersection of {"x"} and {"x"} is {"x"})
    // → T_outer.commitReadonly({"x"}) — writes to T_outer.readonlyShadow ONLY.
    //   Critically, does NOT walk up to F.owned[x] and mutate it there.
} else {
    /* x mutable */
}
// outer join: T_outer.readonlyShadow={"x"} ∩ E_outer.readonlyShadow={} = {}
//   → no commit at F. x stays mutable past the outer if.
x = 5                // legal
```

If `commitReadonly` walked up to F (the owning scope) and mutated `F.owned[x].isReadonly = true`, then the lookup from inside E_outer (the outer-else branch, a sibling of T_outer) would see `x` as readonly, and a write `x = 5` inside E_outer would be incorrectly rejected. The shadow-only commit prevents this: the inner join's promotion lives in `T_outer.readonlyShadow`, which is invisible to E_outer's lookups (E_outer's lookup walks E_outer → F, never through T_outer).

The general rule, restated: **each `commitReadonly` is local to the scope it's invoked on. Outer joins re-do their own intersection over their own predecessors' shadows.** This composes cleanly with arbitrary nesting depth.

A test case in §2.7 (`commitReadonlyDoesNotLeakToSibling`) enforces this directly. A test case in §4.7 (`nestedIfDoubleInnerPromoteDoesNotLeak`) tests the end-to-end behavior at the module level.

### 4.6 Driver changes

`compiler/Main.kt:74-109` today calls `v.verify(symbolTable)` on each top-level `*Data`. After P10:

```kotlin
// In Main.run, replacing the loop at lines 91-105:
val symbolTable = SymbolTable()
val moduleAst = ModuleAst(parseResult.getFilePath(), module)
// The CLI's --target flag is parsed into a TargetKeyword?: null for "verify
// against all targets" (e.g., when `wfpm publish`-ing), or a specific value
// when building for one target. Pass-through to the verifier per §4.4.
val target: TargetKeyword? = arguments.targetKeyword()  // null if --target omitted
val verifyResult = Verifier.verifyModule(moduleAst, symbolTable, target)
if (!verifyResult.isSuccessful) {
    for (err in verifyResult.errors) {
        System.err.println("${err.message} in ${err.primaryPosition.generateMessage()}")
    }
    throw CompilerError("verification failed")
}

val ir = IrLowering.lowerModule(moduleAst, symbolTable)
println(backend.emitProgram(ir))
```

The driver gets shorter — verification and lowering are two function calls — and the symbol-table-aware lowering pass has access to the same symbol table the verifier used (for looking up call return types and identifier types when filling in IR `type` fields).

### 4.7 Required test cases (verifier)

```kotlin
// Verifier tests — full module level.
// These complement the SymbolTable tests in §2.7.

class VerifierTest {

    @Test fun emptyModule() {
        val module = parseAndAst("module Foo { }")  // helper that returns ModuleAst
        val result = Verifier.verifyModule(module, SymbolTable())
        assertTrue(result.isSuccessful)
    }

    @Test fun duplicateTopLevelVarFails() {
        val module = parseAndAst("""
            module Foo {
                int x = 1
                int x = 2
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0] is VerifyError.DuplicateDeclaration)
    }

    @Test fun readonlyAssignmentFails() {
        val module = parseAndAst("""
            module Foo {
                func f() {
                    int x = 1
                    readonly x
                    x = 2
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0] is VerifyError.AssignToReadonly)
    }

    @Test fun readonlyIncrementFails() {
        val module = parseAndAst("""
            module Foo {
                func f() {
                    int x = 1
                    readonly x
                    x++
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0] is VerifyError.IncrementOfReadonly)
    }

    @Test fun bothBranchesReadonlyPromotesAtJoin() {
        val module = parseAndAst("""
            module Foo {
                func f(bool b) {
                    int x = 1
                    if (b) { readonly x }
                    else { readonly x }
                    x = 2
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0] is VerifyError.AssignToReadonly)
    }

    @Test fun oneBranchReadonlyDoesNotPromote() {
        val module = parseAndAst("""
            module Foo {
                func f(bool b) {
                    int x = 1
                    if (b) { readonly x }
                    else { /* nothing */ }
                    x = 2
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        assertTrue(result.isSuccessful)  // x is still mutable after the join
    }

    @Test fun terminatingBranchIgnoredAtJoin() {
        val module = parseAndAst("""
            module Foo {
                func f(bool b) returns int {
                    int x = 1
                    if (b) { return 0 }
                    else { readonly x }
                    x = 2
                    return x
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        // The `return 0` branch is terminating, so the else-branch is the only
        // predecessor of the join; it promoted, so x is readonly at `x = 2`.
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0] is VerifyError.AssignToReadonly)
    }

    @Test fun loopBodyPromotionDoesNotEscape() {
        val module = parseAndAst("""
            module Foo {
                func f() {
                    int x = 1
                    while (true) {
                        readonly x
                    }
                    x = 2
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        // After the while, x is still mutable. (PITFALL #9.)
        assertTrue(result.isSuccessful)
    }

    /**
     * Walk-depth boundary test (round-4 F8 fix), end-to-end at the module
     * level. The inner if/else both branches promote, so the inner join
     * commits the promotion to the outer if's then-scope. The outer else
     * branch does NOT promote. The outer join intersects and finds the
     * promotion isn't durable past the outer if. The post-outer-if
     * `x = 5` must be legal.
     */
    @Test fun nestedIfDoubleInnerPromoteDoesNotLeak() {
        val module = parseAndAst("""
            module Foo {
                func f(bool a, bool b) {
                    int x = 1
                    if (a) {
                        if (b) { readonly x }
                        else { readonly x }
                        // inner join: x is readonly within T_outer
                    } else {
                        // outer-else: x is NOT promoted
                    }
                    // outer join: intersection is empty; x is mutable here
                    x = 5
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        assertTrue(result.isSuccessful)
    }

    /**
     * Companion case to the above: the inner if/else BOTH promote AND the
     * outer else ALSO promotes. The outer join's intersection then promotes
     * durably and the post-outer-if `x = 5` is rejected.
     */
    @Test fun nestedIfDoublePromoteThenOuterElsePromoteDoesLeak() {
        val module = parseAndAst("""
            module Foo {
                func f(bool a, bool b) {
                    int x = 1
                    if (a) {
                        if (b) { readonly x }
                        else { readonly x }
                    } else {
                        readonly x
                    }
                    x = 5
                }
            }
        """.trimIndent())
        val result = Verifier.verifyModule(module, SymbolTable())
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0] is VerifyError.AssignToReadonly)
    }
}
```

If the implementer skips any of these, the verifier separation is not complete.

### 4.8 Error format: JSON-first (Aaron's call, round-4 Q10)

The compiler's *primary* error output format is JSONL (one JSON object per line, stderr). Human-readable output is a *converter* — a separate module the compiler invokes by default when stderr is a TTY. LSP tooling, AI tooling, and any consumer that needs machine-readable diagnostics reads the JSONL stream directly.

#### Why JSON-first

- **LSP integration is trivial.** The LSP server consumes the JSON directly with no parsing of human text. This is the load-bearing reason — every shipped LSP for a small language has to map compiler errors into LSP `Diagnostic` objects, and parsing human-formatted text is brittle. Producing structured JSON eliminates the conversion step.
- **AI tooling** (Claude Code, Cursor, Codex) reads the JSON to summarize and act on errors. Per the AI-augmented research (`07-ai-augmented-dev-research.md` failure mode #4), spec-driven workflows depend on machine-readable feedback loops. Human-formatted strings break these loops.
- **Versioning is explicit.** The JSON schema has a version field. Consumers (LSPs, AI agents, third-party CI tools) can pin to a schema version; breaking changes bump the version, not the human text.
- **Human output is still beautiful** — it's just downstream of JSON. The converter renders the same errors in Elm/Roc/Gleam-style colorized format with source snippets, related-info pointers, and suggested-fix hints. The friendly errors per `04-strategy.md` §6 are delivered by the converter, not by the compiler core.

#### Schema

The JSON schema file lives at `notes/error-schema-v1.json` (a JSON Schema Draft 2020-12 document). The compiler ships with the schema in its resources for runtime introspection.

Each line of stderr is one JSON object matching this schema:

```json
{
  "schemaVersion": 1,
  "severity": "error",
  "code": "WF1001",
  "message": "Cannot assign to readonly binding 'x'.",
  "primaryLocation": {
    "file": "examples/Foo.wf",
    "line": 5,
    "column": 12,
    "endLine": 5,
    "endColumn": 13
  },
  "relatedInfo": [
    {
      "message": "Originally declared as readonly here",
      "location": {
        "file": "examples/Foo.wf",
        "line": 3,
        "column": 9,
        "endLine": 3,
        "endColumn": 10
      }
    }
  ],
  "suggestedFix": null,
  "tags": []
}
```

**Field semantics (each marked required / optional):**

| Field | Required | Type | Notes |
|---|---|---|---|
| `schemaVersion` | required | integer | `1` for the v1 schema. Bumped on any breaking change to this format. |
| `severity` | required | enum | `"error"` or `"warning"`. P10 ships only `"error"`. P11 friendly-errors work may add `"warning"`. `"info"` and `"hint"` are reserved for LSP-style suggestions; not in v1. |
| `code` | required | string | Stable error code, e.g. `"WF1001"`. Each `VerifyError` variant maps 1:1 to a code. Consumers switch on this, never on `message`. See "Code allocation" below. |
| `message` | required | string | One-sentence canonical form. Plain prose, no embedded formatting. The human converter may render this verbatim or substitute a richer text rendered from the structured fields. |
| `primaryLocation` | required | object | Where the error is anchored. Contains `file` (string), `line` (1-based int), `column` (0-based int — matching ANTLR's `charPositionInLine`), `endLine` (int), `endColumn` (int). `endLine`/`endColumn` are *optional within the object*; when omitted, the consumer treats the location as a single-character span. |
| `relatedInfo` | optional | array of `{message, location}` | Pointers to other source positions the error refers to (e.g., "previously declared at"). Empty array allowed; omit the field if no related info. |
| `suggestedFix` | optional | string or null | A one-line "what to try" hint, plain prose. The human converter renders this as the "what to try" line in friendly-error output (Elm/Roc style). `null` when the verifier has no suggestion. |
| `tags` | optional | array of strings | Reserved for LSP-style tags (`"unnecessary"`, `"deprecated"`). Empty in v1; the field exists so consumers can rely on its presence. |

**Required vs optional, summarized**: `schemaVersion`, `severity`, `code`, `message`, `primaryLocation` are always present. `relatedInfo`, `suggestedFix`, `tags` are optional and may be omitted entirely or set to `null` / `[]`.

**Code allocation.** Each `VerifyError` subclass gets a stable error code:

| Code | VerifyError variant |
|---|---|
| `WF1001` | `AssignToReadonly` |
| `WF1002` | `IncrementOfReadonly` |
| `WF1003` | `ReadonlyOfUndeclared` |
| `WF1004` | `AlreadyReadonly` |
| `WF1101` | `DuplicateDeclaration` |
| `WF1102` | `UnknownType` |
| `WF2001` | `MissingExternalForTarget` (P12+) |
| ... | (P12+ codes allocated in the WF2xxx range) |

`WF1xxx` is reserved for P10-era errors; `WF2xxx` for P12-era (sum types, generics, `@external`); etc. The code is stable across versions for back-compat with consumers that switch on it. Renaming a `VerifyError` class is fine; changing its code is a breaking change.

#### CLI flags

```
./waterfall examples/Foo.wf                # JSONL to stderr if non-TTY; pretty to stderr if TTY
./waterfall --errors json examples/Foo.wf  # force JSONL regardless of TTY
./waterfall --errors human examples/Foo.wf # force human-readable regardless of TTY
./waterfall --errors none examples/Foo.wf  # suppress error output entirely (for `wfpm` integration)
```

The default (no `--errors` flag) detects whether stderr is a TTY using `System.console() != null`. TTY → human; non-TTY (pipe, CI logs, IDE integration) → JSONL. This matches `gh`, `cargo`, and similar tools.

#### Human converter

A separate Kotlin module at `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/diagnostics/HumanRenderer.kt`. Input: a `VerifyError` (or the equivalent JSON). Output: a multi-line colorized string with:

1. Header: `error[WF1001]: Cannot assign to readonly binding 'x'.`
2. Source snippet (3 lines of context, with the offending line highlighted and column-marker carets).
3. Related-info pointers as additional snippets.
4. `help: <suggestedFix>` if present.

Color via ANSI escape codes; disabled with `--no-color` or when stderr is non-TTY. Model: Elm, Roc, Gleam outputs (per `04-strategy.md` §6 "friendly errors" deliverable). P11 friendly-errors work extends this renderer; P10 ships a basic version that's "readable" without yet being "delightful."

#### LSP consumption

The future LSP server (P13 deliverable per `04-strategy.md` §3) consumes the JSONL stream by spawning the compiler subprocess with `--errors json` and reading line-by-line from stderr. Each line is parsed as a `Diagnostic`; the LSP maps the `code` to an LSP code, `severity` to LSP severity, `primaryLocation` to LSP range, `relatedInfo` to LSP related information, `suggestedFix` to a `CodeAction`. This is the load-bearing reason the schema is structured this way.

P10 doesn't ship the LSP, but P10 must ship the JSON output that the P13 LSP can consume without retrofitting.

#### Schema file

The JSON Schema document lives at `notes/error-schema-v1.json` and is shipped in the compiler's resources at `compiler/src/main/resources/error-schema-v1.json`. Skeleton:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://waterfall-lang.dev/schemas/error-v1.json",
  "title": "Waterfall Compiler Error v1",
  "type": "object",
  "required": ["schemaVersion", "severity", "code", "message", "primaryLocation"],
  "properties": {
    "schemaVersion": { "const": 1 },
    "severity": { "enum": ["error", "warning"] },
    "code": { "type": "string", "pattern": "^WF[0-9]+$" },
    "message": { "type": "string" },
    "primaryLocation": { "$ref": "#/$defs/sourceLocation" },
    "relatedInfo": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["message", "location"],
        "properties": {
          "message": { "type": "string" },
          "location": { "$ref": "#/$defs/sourceLocation" }
        }
      }
    },
    "suggestedFix": { "type": ["string", "null"] },
    "tags": { "type": "array", "items": { "type": "string" } }
  },
  "$defs": {
    "sourceLocation": {
      "type": "object",
      "required": ["file", "line", "column"],
      "properties": {
        "file": { "type": "string" },
        "line": { "type": "integer", "minimum": 1 },
        "column": { "type": "integer", "minimum": 0 },
        "endLine": { "type": "integer", "minimum": 1 },
        "endColumn": { "type": "integer", "minimum": 0 }
      }
    }
  }
}
```

The implementer is responsible for keeping `VerifyError` Kotlin types and this schema in sync. v1.1+ may introduce automated schema generation from `VerifyError` via Kotlin reflection; v1 doesn't bother — the hand-maintained schema is fine for the P10-era error set.

#### Renderer responsibilities split

| Concern | Lives in | Notes |
|---|---|---|
| Building `VerifyError` from verification | `verifier/` package | P10 spec'd; uses the typed sealed class. |
| Serializing `VerifyError` → JSON | `diagnostics/JsonRenderer.kt` (new) | One function per `VerifyError` variant. Tested per-variant. |
| Rendering JSON → human text | `diagnostics/HumanRenderer.kt` (new) | Reads the canonical JSON form; produces colorized multi-line text. Can be invoked from the compiler OR by an external tool feeding it stdin. |
| LSP-side consumption | (P13) | Reads JSONL via subprocess stderr. Maps to LSP `Diagnostic`. Not part of P10. |

The split exists so the human converter is testable in isolation: feed it JSON, check the rendered output. No need to round-trip through full compilation.

**Acceptance criteria for this section (P10):**

- [ ] `notes/error-schema-v1.json` exists, validates as Draft 2020-12 JSON Schema.
- [ ] `diagnostics/JsonRenderer.kt` exists and produces output that validates against the schema for every `VerifyError` variant.
- [ ] `diagnostics/HumanRenderer.kt` exists and produces readable output for every variant. "Readable" at P10 means: error code visible, primary location formatted as `file:line:col`, message on the next line, related info as sub-bullets if present.
- [ ] The compiler's default behavior detects TTY and selects format accordingly.
- [ ] At least one round-trip test: produce JSON from a `VerifyError`, parse it back, assert the result matches the original.

### 4.9 Test framework: property-based testing with Kotest (round-4 Q11)

P10 lands the foundation that subsequent phases will exercise. The test suite needs to graduate from "a few hand-written cases per feature" to **property-based tests** that probe the verifier and lowering pass with generated inputs. Klabnik's "tests pass, code is wrong" failure mode (per `07-ai-augmented-dev-research.md` failure mode #1) is *exactly* what property tests catch when example-based tests don't.

**Recommendation: Kotest (kotest.io)**, specifically the `kotest-property` module.

#### Why Kotest

Three frameworks were considered:

| Framework | Verdict | Reason |
|---|---|---|
| **Kotest** (kotest.io) | **Pick** | Modern Kotlin; first-class sealed-class arbiters via `Arb.choice` and `Arb.of`; automatic shrinking; integrates with JUnit 4 runners (matches the project's existing JUnit 4 base per audit T1). Used in mainstream Kotlin projects; familiar to candidate contributors. |
| **Jqwik** (jqwik.net) | Reject | Java-native; the Kotlin DSL on top is awkward. Generators for sealed classes need hand-rolled adapters. Works, but more friction. |
| **Hand-rolled with `kotlin.random.Random`** | Reject for the verification triad | Tempting for dependency hygiene, but it forfeits shrinking and sealed-class arbiters — both load-bearing for IR-level property tests. Re-implementing those from scratch costs more than picking up Kotest. |

The decision aligns with the strategist's verification triad (per round-3 strategy): property tests are *one of three* load-bearing test categories (alongside golden tests and differential tests against a known-good oracle). Property tests need shrinking to produce useful failures; we don't get that from hand-rolled random generation.

**Gradle dependency** (added in `compiler/build.gradle`):

```groovy
testImplementation 'io.kotest:kotest-property:5.9.1'
testImplementation 'io.kotest:kotest-runner-junit4:5.9.1'
```

The runner module preserves the existing JUnit 4 invocation path (`./gradlew test`) — no separate command for property tests; they run alongside the golden tests.

#### What a Waterfall property test looks like

A representative test: "for any well-typed AST, lowering preserves the type at every expression node."

```kotlin
package com.aaroncoplan.waterfall.compiler.ir

import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.compiler.typesystem.WaterfallType
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import org.junit.Test

class IrLoweringPropertyTest {

    /** Arbitrary primitive type for property tests. */
    private val arbType: Arb<WaterfallType> = Arb.choice(
        Arb.of(WaterfallType.IntType),
        Arb.of(WaterfallType.DecType),
        Arb.of(WaterfallType.BoolType),
        Arb.of(WaterfallType.CharType)
    )

    /** Arbitrary literal expression matching a given type. */
    private fun arbLiteral(type: WaterfallType): Arb<ExpressionData> = when (type) {
        WaterfallType.IntType  -> Arb.int().map { ExpressionData.intLiteral(it.toString()) }
        WaterfallType.DecType  -> Arb.double().map { ExpressionData.decLiteral(it.toString()) }
        WaterfallType.BoolType -> Arb.boolean().map { ExpressionData.boolLiteral(it.toString()) }
        WaterfallType.CharType -> Arb.string(1..10).map { ExpressionData.stringLiteral("`$it`") }
        else -> error("not a literal-producing type")
    }

    @Test
    fun loweringPreservesPrimitiveLiteralType() {
        // For any primitive type and any literal of that type, lowering should
        // produce an IrExpression whose `type` field matches the source type.
        checkAll(100, arbType) { type ->
            val literal = arbLiteral(type).single()
            val ir = IrLowering.lowerExpression(literal, SymbolTable())
            assert(ir.type.asWaterfallType() == type) {
                "Lowering changed the type. Source: $type, IR: ${ir.type.asWaterfallType()}"
            }
        }
    }

    @Test
    fun readonlyShadowSurvivesArbitraryNesting() {
        // For any nested if/else structure, if EVERY branch promotes x to readonly,
        // x must be readonly at the outermost join. (The §2d intersection rule.)
        // This is a property test over nesting depth and branch composition.
        // ... (full implementation walks a randomly generated if-tree, asserts
        // intersection invariants at every join level)
    }
}
```

**Where property tests pay off most in P10:**

- **`SymbolTable` invariants**: for any sequence of `declare` / `enterScope` / `markReadonlyLocal` / `commitReadonly` / `exitScope` operations, the lookup result at every program point matches the invariants spec'd in §2.2 (especially the walk-depth boundary from round-4 F8).
- **`IrLowering` round-trips**: for any well-typed Waterfall AST, lowering produces an IR whose types match the verifier's assignments.
- **`JoinAnalysis` correctness**: for randomly generated control-flow trees, the intersection rule produces the same result as a naive O(n²) brute-force reference implementation. (The reference implementation is also written for the test, separately from the production code — this is differential testing, per `07-ai-augmented-dev-research.md` mitigation for failure mode #1.)
- **JSON renderer fidelity**: for any `VerifyError`, the JSON renderer's output validates against `error-schema-v1.json` and round-trips back to a structurally-equal `VerifyError`.

#### What's NOT a property test in P10

- **Golden tests** (`./gradlew test --tests GoldenTests`) stay as they are — exact byte-equal output comparison. Goldens catch "did anything change?" Property tests catch "are the invariants true?" Both are necessary; property tests don't replace goldens.
- **Differential testing against a known-good compiler** (Carlini's GCC-oracle pattern) is the third leg of the triad. P10 doesn't need this yet; P12+ (when generics + monomorphization land) will. The framework choice doesn't affect when this lands.

#### Acceptance criteria for this section (P10)

- [ ] Gradle dependency on `kotest-property` and `kotest-runner-junit4` added.
- [ ] At least one property test per major subsystem: SymbolTable (the walk-depth boundary), JoinAnalysis (intersection over generated branches), JsonRenderer (schema round-trip).
- [ ] Property tests run as part of `./gradlew test` — no separate command needed.
- [ ] Each property test has at least 100 iterations by default; failure produces a *shrunk* counterexample (Kotest does this automatically).

#### Strategy implication

The choice of Kotest affects round-3 strategy: the mutation-test-gate framework (which the strategist is sequencing) needs to coexist with the property-test framework. Kotest and Pitest (the standard JVM mutation-test tool) work together cleanly — Pitest mutates production code, Kotest property tests detect surviving mutants more reliably than fixed-example tests do. The combination is the strongest verification posture for P10's deliverables. **Flagging this to the team-lead, not the strategist directly, per lane discipline.**

P10's six sub-tasks land in order. Tests stay green at every step; if they don't, *stop and fix*. Don't proceed.

### Sub-task 5.1 — Introduce `WaterfallType`, `SymbolKind`, `SymbolInfo`

**Files added:**
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/typesystem/WaterfallType.kt`
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/symboltables/SymbolKind.kt`
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/symboltables/SymbolInfo.kt`

**Files changed:** none yet. `SymbolTable.kt` still stores `Any?` at this step.

**Expected test impact:** none — these are new files unused by anything yet.

**Sanity check:** `./gradlew build` passes.

### Sub-task 5.2 — Migrate `SymbolTable` to typed storage + public `lookup` + shadow API

**Files changed:**
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/symboltables/SymbolTable.kt` (full rewrite per Section 2.2).
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/symboltables/DuplicateDeclarationException.kt` (delete; replaced by `DuplicateDeclarationError` and `DeclareResult`).

**Callsites updated** (these are the only ones today):
- `compiler/.../statements/TypedVariableDeclarationAndAssignmentData.kt:30` — wrap declare in DeclareResult handling.
- `compiler/.../statements/UntypedVariableDeclarationAndAssignmentData.kt:25` — same.
- `compiler/.../statements/FunctionImplementationData.kt:35` — self-decl, wrap as Function.
- `compiler/.../statements/FunctionImplementationData.kt:47` — per-arg declare.

For each, the existing `verify` body now wraps the `declare` call in a check for `DeclareResult.Failure`. The existing string error messages are translated 1:1 to `VerificationResult(false, msg)` returns (the verify() method on Translatable still exists at this step — Section 4 separation lands in Sub-task 5.5).

**Expected test impact:** all existing tests still pass. `DuplicateInnerDeclarationTest` tests the same shape but with the new typed errors.

**Sanity check:** Run `./gradlew test`. All goldens unchanged. Run the SymbolTable unit tests from §2.7.

**PITFALL #12** — During this sub-task, the `Any?` -> `SymbolInfo` migration is the most error-prone step in the whole phase. Klabnik-style review: the operator should read every commit. Any place that does `info as String` or relies on the type being a `String` needs to switch to `info.type.render()` (returning a `String`) — but you can't blanket-replace `as String` with `.type.render()` because the bare reads of `info` should now be SymbolInfo, not String. Take it one callsite at a time and verify.

### Sub-task 5.3 — Add verifier package; move `verify()` out of `Translatable`

**Files added:**
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/verifier/VerifyResult.kt`
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/verifier/VerifyError.kt`
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/verifier/Verifier.kt`
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/verifier/ModuleVerifier.kt`
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/verifier/StatementVerifier.kt`
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/verifier/ExpressionVerifier.kt`
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/verifier/JoinAnalysis.kt`

**Files changed:**
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/statements/helpers/Translatable.kt` — `verify(symbolTable)` removed from the interface.
- Every `*Data` class — its `override fun verify` method is removed; the body migrates into the corresponding `verifyXxx` function in `StatementVerifier.kt` (or `ModuleVerifier.kt` for `FunctionImplementationData`).
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/statements/helpers/VerificationResult.kt` — delete. Replaced by `VerifyResult` + `VerifyError`.
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/Main.kt:91-105` — the verify loop becomes a single call to `Verifier.verifyModule`.

**Expected test impact:** all existing tests still pass. Error messages may shift slightly in formatting (now structured); update any test that asserts on exact error text.

**Sanity check:** Run `./gradlew test`. Add tests from §4.7. All goldens unchanged.

### Sub-task 5.4 — Add `ir/` package; write the lowering pass

**Files added:**
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/ir/IrType.kt`
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/ir/IrModule.kt`
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/ir/IrStatement.kt`
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/ir/IrExpression.kt`
- `compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/ir/IrLowering.kt`

**Files changed:** none yet. Backends still consume `*Data`. The IR is built but not used.

**Expected test impact:** Add `IrLoweringTest` that verifies the lowering produces structurally-correct IR for every example. No golden changes.

**Sanity check:** Manually inspect the IR output for `FibonacciModule.wf`. Confirm it has the expected shape.

### Sub-task 5.5 — Migrate backends to consume IR

This is the biggest single change. One backend at a time. The order to use:

1. `JavaScriptBackend` (cleanest).
2. `PythonBackend`.
3. `CBackend` (most complex; do last so the others' migration patterns are settled).

**Files changed per backend:**
- The backend's main file — every `emit*` method's parameter type changes from `*Data` to `Ir*`.
- The `Backends.kt` registry — no change.

**Expected test impact:** All goldens unchanged. The IR-driven backend produces the same output as the `*Data`-driven one. Any divergence is a bug.

**Sanity check after each backend:** Run `./gradlew test --tests GoldenTests` filtered to the relevant target. All pass.

After all four backends are migrated, `CodeGenerator.kt` itself updates (the interface signature change in Section 3.9).

**PITFALL #13** — The "produces the same output" check is *exactly* the verifier-overfitting failure mode the AI-research doc (07) warns about. The implementer will be tempted to write tests that the new IR-driven backend passes by construction, then declare victory. The honest test is: **goldens unchanged**. If the goldens have to change, something is wrong; if you find yourself updating a golden, **escalate**.

### Sub-task 5.6 — Remove old paths

**Files changed:**
- `Translatable.translate(backend)` — remove the method. `*Data` classes no longer have `translate`.
- `compiler/.../target/CodeGenerator.kt` — interface signature already updated in 5.5.

**Files NOT deleted (yet):**
- The `*Data` classes themselves. They're still produced by the parser and consumed by `IrLowering`. Deleting them is the long-term path but P10 is "introduce IR alongside, lowering bridges them."

**Expected test impact:** all tests pass. Some imports become unused; clean them up.

**Sanity check:** `./gradlew build` clean. `./gradlew test` green. `./waterfall examples/FibonacciModule.wf` produces the same output it did before P10 started.

### Migration order rationale

5.1 → 5.2 → 5.3 → 5.4 → 5.5 → 5.6 is forced by the dependency graph:

- 5.2 needs the types from 5.1.
- 5.3 needs the new SymbolTable from 5.2.
- 5.4 needs the structure of *Data classes to lower from, plus the symbol table to look up types.
- 5.5 needs the IR from 5.4.
- 5.6 needs all backends migrated.

There's no shortcut. Each step has a green-tests gate.

---

## Section 6 — AI-implementation guidance

This section is for the AI agent implementing P10 and the human reviewing the result. Read both subsections.

### 6.1 Common pitfalls (cross-referenced)

Repeated from the inline callouts for quick scanning:

| # | Location | Failure mode (AI-research doc) |
|---|---|---|
| 1 | §1.2 — `WaterfallType` `void` handling | #4 silent spec resolution |
| 2 | §1.2 — Don't add type variants speculatively | #1 verifier overfitting on absent variants |
| 3 | §1.3 — Don't store function info in `SymbolInfo.type` | #4 silent spec resolution |
| 4 | §1.4 — `isReadonly` is observed-via-lookup state, not the stored entry | #4 silent spec resolution |
| 5 | §2.2 — Don't widen `markReadonlyLocal` to destructive | #1, #4 (re-introduces the F8 bug from design §2c) |
| 6 | §2.2 — `lookup` walks up AND applies shadow stack | #4 silent spec resolution |
| 7 | §2.4 — Functions are implicitly readonly | #4 silent spec resolution |
| 8 | §2.4 — Per-argument source positions | #4 silent spec resolution |
| 9 | §2.5 — Loops never propagate readonly past the body | #1 (don't "fix" what looks like a missing feature) |
| 10 | §3.9 — Don't keep old + new CodeGenerator interfaces in parallel | scope creep |
| 11 | §4.5 — Implicit-else branch is critical | #4 silent spec resolution |
| 12 | §5.2 — `Any?` -> `SymbolInfo` is the highest-error-rate migration step | #2 latent semantic divergence |
| 13 | §5.5 — Goldens unchanged is the honest test | #1 verifier overfitting; #8 AI-writes-and-passes-its-own-tests |

### 6.2 Escalation list (places to stop and ask)

The AI agent should **escalate to human review** instead of resolving silently in these cases. Each is a place where the design intentionally leaves ambiguity that depends on something the AI can't see:

1. **Per-argument source positions** (PITFALL #8). If the existing `FunctionImplementationData` resists the change without a sweep — for example, if its `typedArguments: List<Pair<String, String>>` is used by callers that would also need updating — escalate. Don't try to retrofit; instead surface the scope expansion to the operator.

2. **Existing `verify()` methods doing more than declaration**. If any `verify()` method does something other than calling `symbolTable.declare` and walking children (e.g., it does a real type check), surface that to the operator before lifting it into the new verifier. The audit claims most verify methods are no-ops, but the auditor may have missed one.

3. **Any place where `*Data.verify` mutates state besides `SymbolTable.declare`**. The transactional symbol-table model assumes verify-time mutations are limited to declare and (post-Piece-2) `markReadonlyLocal`. Any other state mutation discovered during the migration needs human review — it might be a structural assumption the IR breaks.

4. **Inferring types for `BINARY_OP` IR nodes**. Section 3.6 says "type = left.type for P10 placeholder." That's intentionally weak — real type inference is P11. If the AI agent finds itself wanting to actually infer the result type (e.g., promoting int+dec to dec), escalate. P10 is not the time.

5. **`StringLiteral` type assignment**. The audit (surprise #6) notes that today untyped strings infer to `char`. The IR follows suit (Section 3.6). If during migration this feels wrong, escalate — fixing it is a real-string-type design call (language design doc §1.7) that belongs to a future phase.

6. **The driver's accumulate-vs-bail decision** (§4.6). Today's driver bails on the first error; the new VerifyResult supports accumulation. P10 keeps the bail-on-first behavior; P11 may flip it. *Don't speculatively switch* the driver to accumulate during P10 — escalate if doing so seems tempting.

### 6.3 Pre-merge checklist (for the human reviewer)

Before merging a P10 PR, the reviewer (Aaron) should verify:

- [ ] `./gradlew build` passes locally without warnings beyond the existing Gradle deprecations (audit T2).
- [ ] `./gradlew test` passes with all goldens unchanged.
- [ ] **No golden file is modified** by the P10 PR. If one is, that's a behavioral regression — find it.
- [ ] All test cases in §2.7 (SymbolTable) pass.
- [ ] All test cases in §4.7 (Verifier) pass.
- [ ] `Translatable.kt` no longer has `verify`. `*Data` classes no longer have `verify`.
- [ ] No `*Data` class is imported in `compiler/.../target/` (the backends consume only `ir.*`).
- [ ] `SymbolTable.lookup` is `public`.
- [ ] `IrLowering.lowerModule` produces an `IrModule` for every example in `examples/`. (Manual inspection: pick one example and read its IR — it should look obvious.)
- [ ] No new code in `compiler/.../verifier/` calls `backend.emit*` or imports anything from `target/`. (Verifier shouldn't know about backends.)
- [ ] No new code in `compiler/.../ir/` calls `symbolTable.declare`. (IR is post-verification.)
- [ ] No new code in `compiler/.../target/` calls `symbolTable.declare`, `Verifier.verifyModule`, or any `*Data` walker. (Backends consume only IR.)

If any item fails, that's a "stop and fix" before merge, not a "merge and follow up."

---

## Section 7 — Cross-cutting concerns discovered while writing this spec

Surfacing these for the team-lead per the task brief. None of them block P10; they affect later phases.

### 7.1 P11 (type inference) is more invasive than the strategy doc suggests

The strategy doc (`04-strategy.md` §3, P11) says "Hindley-Milner-lite inference pass over the IR." That's the right framing, but writing this P10 spec made clear that **identifier-type lookup in P10 lowering already requires the symbol table to know expression types**, and P10's IR has `IrType` on every expression. So P10 doesn't *do* inference, but it produces the *substrate* for inference: a typed IR.

The implication for P11: the inference pass is essentially "fill in the `type` field on every `IrExpression`, currently set to placeholders by P10's lowering." That's a smaller delta than "build a type system from scratch." P11 may be faster than the strategy doc budgeted. Surface for the strategist to consider.

### 7.2 P11 may need to run before lowering, not after

The above means there's a choice at P11: either (a) run inference over the `*Data` tree before lowering, and have lowering produce a fully-typed IR; or (b) lower with placeholder types and have inference walk the IR to fill them in. Option (b) is what P10's spec assumes (lowering happens, types are placeholder). If the implementer at P11 finds option (a) cleaner, that's a P11 design call — but it implies P10's lowering becomes simpler (it can punt all type fields) and P11 grows to cover the inference *and* the IR's type field population.

This is a phase-boundary question the strategist should weigh in on. P10 ships either way.

### 7.3 The audit's `Pair<K, V>` class is used pervasively (audit surprise #7)

The migration in §5.2 will touch every callsite of `SymbolTable.declare`. Three of those are inside `FunctionImplementationData.verify` which uses the custom `Pair<K, V>` class for `typedArguments`. If the per-argument source position fix (PITFALL #8) goes in, the `Pair<String, String>` for typedArguments becomes inadequate — it needs a third field for position. That's a small refactor of `FunctionImplementationData` that ripples into `LambdaFunctionData` (also stores `Pair<String, String>` for arg lists).

Mitigation: introduce a small `TypedArgument(type, name, position)` data class in the `statements/` package and migrate both `FunctionImplementationData` and `LambdaFunctionData` to use it. The `Pair<K, V>` class itself stays (audit surprise #7 makes clear it's still used elsewhere; full removal is post-P10 cleanup).

### 7.4 `VerifyError` will need rendering helpers at P11

The friendly-errors work at P11 (per `04-strategy.md` §3) needs each `VerifyError` to render as "source-snippet + what-went-wrong + suggested-fix." P10's `VerifyError` has the `message` field for stderr; P11 will add a renderer that takes a `VerifyError` and produces the multi-line friendly form. The structured error fields (`name`, `previousPosition`, `declarationPosition`) are exactly what the renderer needs — confirming the design.

No P10 action needed; just noting that the structured-error design pays off at P11.

### 7.5 The `WaterfallType.fromSourceText` mechanism is duck-tape

`WaterfallType.fromSourceText(text: String): WaterfallType` (Section 1.2) is the bridge from today's string-typed `type: String` field on `*Data` classes to the new sealed-class representation. It's fine for P10 — the alternative is rewriting how `*Data` classes parse types out of the ANTLR context, which is much more work.

P11+ should remove this bridge: the `*Data` classes (or their direct successors in the IR-only world) should parse the ANTLR type context directly into `WaterfallType` at construction time. That eliminates the round-trip string and the `ErrorType("?int")` case.

No P10 action needed; P11 should plan to remove `fromSourceText`.

---

## Appendix A — Summary of new files

For convenience, the complete set of new files P10 introduces:

```
compiler/src/main/kotlin/com/aaroncoplan/waterfall/compiler/
├── typesystem/
│   └── WaterfallType.kt              (NEW)
├── symboltables/
│   ├── SymbolKind.kt                 (NEW)
│   ├── SymbolInfo.kt                 (NEW)
│   └── SymbolTable.kt                (REWRITTEN — was 26 lines, now ~150)
├── verifier/
│   ├── VerifyResult.kt               (NEW)
│   ├── VerifyError.kt                (NEW)
│   ├── Verifier.kt                   (NEW)
│   ├── ModuleVerifier.kt             (NEW)
│   ├── StatementVerifier.kt          (NEW)
│   ├── ExpressionVerifier.kt         (NEW)
│   ├── JoinAnalysis.kt               (NEW)
│   └── README.md                     (NEW)
├── ir/
│   ├── IrType.kt                     (NEW)
│   ├── IrModule.kt                   (NEW)
│   ├── IrStatement.kt                (NEW)
│   ├── IrExpression.kt               (NEW)
│   ├── IrLowering.kt                 (NEW)
│   └── README.md                     (NEW)
├── statements/
│   └── helpers/
│       ├── Translatable.kt           (MODIFIED — verify() removed)
│       ├── VerificationResult.kt     (DELETED)
│   └── *Data.kt                       (MODIFIED — verify() removed from each)
├── symboltables/
│   └── DuplicateDeclarationException.kt    (DELETED)
├── target/
│   └── CodeGenerator.kt              (MODIFIED — methods take Ir*)
│   └── [JavaScript|Python|C]Backend.kt   (MODIFIED — consume Ir*)
└── Main.kt                           (MODIFIED — verifier call + lowering call)
```

Tests added:

```
compiler/src/test/kotlin/com/aaroncoplan/waterfall/compiler/
├── symboltables/
│   └── SymbolTableTest.kt            (NEW — §2.7 cases)
├── verifier/
│   └── VerifierTest.kt               (NEW — §4.7 cases)
└── ir/
    └── IrLoweringTest.kt             (NEW — §5.4 sanity)
```

---

## Appendix B — Why this is one phase, not three

A reasonable alternative is to do (a) the symbol-table redesign as P10a, (b) the verifier separation as P10b, (c) the IR introduction as P10c. The reasons against:

1. **Interface churn.** Each sub-phase changes a shared interface. P10a changes `SymbolTable.declare`; P10b changes `Translatable.verify`; P10c changes `CodeGenerator.emit*`. Doing them separately means three traumatic interface migrations spread across three CI cycles, each of which touches every backend. Bundling them means one migration.

2. **Test-suite coverage gaps.** Between P10a and P10b, the verifier still lives on `*Data`. Between P10b and P10c, backends still consume `*Data`. Each intermediate state is a place where bugs can accumulate undetected because the code structure is in flux.

3. **AI-implementation cohesion.** Per the AI-augmented research (failure mode #9, long-context drift), splitting the foundation work across three phases that interleave with other work risks the agent forgetting decisions made in P10a by the time P10c lands. One phase = one mental context.

The cost is that P10 is large. The strategy doc estimated it at a quarter (~3 months evenings); the spec's level of detail above is calibrated to support that estimate with AI-augmented implementation. If the strategist's revised AI-augmented timeline pulls P10 to weeks instead of months, this spec remains the right input — only the wall-clock changes.

---

End of P10 design.
