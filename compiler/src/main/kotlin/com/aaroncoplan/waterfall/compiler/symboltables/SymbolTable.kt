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
     * Names not in scope (no `lookup(name)` would resolve them) are silently
     * ignored. The caller should already have verified the name exists.
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
