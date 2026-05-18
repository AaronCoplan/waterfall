package com.aaroncoplan.waterfall.compiler.symboltables

import com.aaroncoplan.waterfall.compiler.statements.helpers.SourcePosition
import com.aaroncoplan.waterfall.compiler.typesystem.WaterfallType
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Property-based tests for [SymbolTable] at N=10,000 per playbook §3 Leg 1.
 *
 * Style: JUnit-4-annotated (`@Test`) + Kotest `checkAll` inside `runBlocking`.
 * NOT StringSpec / BehaviorSpec / FunSpec — project runner is wired for JUnit 4
 * via `useJUnit()` in root build.gradle; mixing spec styles risks silent non-runs.
 *
 * TRIP-WIRE: after this file lands, run
 *   ./gradlew test --tests SymbolTablePropertyTest
 * If it reports zero tests, the runner integration is broken — escalate before
 * adding more properties.
 *
 * WaterfallType scope: [arbType] covers IntType, DecType, BoolType, CharType (four
 * scalar primitives). Excluded: ArrayType (requires recursive Arb — follow-on task),
 * ErrorType (sentinel type; could produce false-positive property failures).
 *
 * Name Arb: uses Arb.string(1, 10) for alphanumeric-safe strings of length 1–10.
 * Minimum length 1 avoids the empty-string edge case (not a valid identifier;
 * could mask HashMap key-comparison bugs). Where two distinct names are required,
 * prefix-based construction ("p1_$base" / "p2_$base") guarantees non-equality
 * without filtering, avoiding unnecessary Arb shrinking overhead.
 */
class SymbolTablePropertyTest {

    // ------------------------------------------------------------------ //
    // Arb definitions
    // ------------------------------------------------------------------ //

    /** Non-empty strings (1–10 chars). Used as symbol names. */
    private val arbName: Arb<String> = Arb.string(1, 10)

    /** The four scalar WaterfallType primitives. */
    private val arbType: Arb<WaterfallType> = Arb.of(
        WaterfallType.IntType,
        WaterfallType.DecType,
        WaterfallType.BoolType,
        WaterfallType.CharType
    )

    /**
     * Function parameter lists (0–4 entries). Zero-length = parameterless function.
     * [Arb.pair] is from io.kotest.property.arbitrary per §5.2 Q3 confirmation.
     * Fallback if unavailable: `arbName.flatMap { n -> arbType.map { t -> n to t } }`
     * composed with Arb.list.
     */
    private val arbFnParams: Arb<List<Pair<String, WaterfallType>>> =
        Arb.list(Arb.pair(arbName, arbType), 0..4)

    /** Variable and Argument kinds (the two non-Function simple kinds). */
    private val arbSimpleKind: Arb<SymbolKind> = Arb.of(SymbolKind.Variable, SymbolKind.Argument)

    private fun pos() = SourcePosition(fileName = "prop.wf", line = 1, column = 0)

    // ------------------------------------------------------------------ //
    // Declare / lookup round-trip (×4)
    // ------------------------------------------------------------------ //

    @Test fun `declare Variable kind then lookup returns the declared SymbolInfo`() {
        runBlocking {
            checkAll(10000, arbName, arbType, Arb.boolean()) { name, type, readonly ->
                val st = SymbolTable()
                val info = SymbolInfo(type, readonly, SymbolKind.Variable, pos())
                val result = st.declare(name, info)
                assertTrue(result is DeclareResult.Success)
                assertEquals(info, st.lookup(name))
            }
        }
    }

    @Test fun `declare Argument kind then lookup returns the declared SymbolInfo`() {
        runBlocking {
            checkAll(10000, arbName, arbType, Arb.boolean()) { name, type, readonly ->
                val st = SymbolTable()
                val info = SymbolInfo(type, readonly, SymbolKind.Argument, pos())
                val result = st.declare(name, info)
                assertTrue(result is DeclareResult.Success)
                assertEquals(info, st.lookup(name))
            }
        }
    }

    @Test fun `declare Function kind then lookup preserves parameter list`() {
        runBlocking {
            checkAll(10000, arbName, arbFnParams) { name, params ->
                val st = SymbolTable()
                val info = SymbolInfo(
                    type = WaterfallType.VoidType,
                    isReadonly = true,
                    kind = SymbolKind.Function(params),
                    sourcePosition = pos()
                )
                val result = st.declare(name, info)
                assertTrue(result is DeclareResult.Success)
                val fnKind = st.lookup(name)!!.kind as SymbolKind.Function
                assertEquals(params, fnKind.parameters)
            }
        }
    }

    @Test fun `declared symbol type and kind are preserved across Variable and Argument kinds`() {
        runBlocking {
            checkAll(10000, arbName, arbType, arbSimpleKind) { name, type, kind ->
                val st = SymbolTable()
                val info = SymbolInfo(type, false, kind, pos())
                st.declare(name, info)
                val looked = st.lookup(name)!!
                assertEquals(type, looked.type)
                assertEquals(kind, looked.kind)
            }
        }
    }

    // ------------------------------------------------------------------ //
    // Shadow isolation (×3)
    // ------------------------------------------------------------------ //

    @Test fun `markReadonlyLocal on child does not affect parent lookup`() {
        runBlocking {
            checkAll(10000, arbName, arbType) { name, type ->
                val parent = SymbolTable()
                parent.declare(name, SymbolInfo(type, false, SymbolKind.Variable, pos()))
                val child = parent.enterScope()
                child.markReadonlyLocal(name)
                assertFalse(parent.lookup(name)!!.isReadonly)
            }
        }
    }

    @Test fun `markReadonlyLocal on child does not affect sibling after exitScope`() {
        runBlocking {
            checkAll(10000, arbName, arbType) { name, type ->
                val parent = SymbolTable()
                parent.declare(name, SymbolInfo(type, false, SymbolKind.Variable, pos()))
                val child1 = parent.enterScope()
                child1.markReadonlyLocal(name)
                parent.exitScope(child1)
                val child2 = parent.enterScope()
                assertFalse(child2.lookup(name)!!.isReadonly)
            }
        }
    }

    @Test fun `markReadonlyLocal in deep child does not propagate to grandparent or parent`() {
        runBlocking {
            checkAll(10000, arbName, arbType) { name, type ->
                val grandparent = SymbolTable()
                grandparent.declare(name, SymbolInfo(type, false, SymbolKind.Variable, pos()))
                val parent = grandparent.enterScope()
                val child = parent.enterScope()
                child.markReadonlyLocal(name)
                // Neither the immediate parent nor the grandparent should see the shadow.
                assertFalse(parent.lookup(name)!!.isReadonly)
                assertFalse(grandparent.lookup(name)!!.isReadonly)
            }
        }
    }

    // ------------------------------------------------------------------ //
    // Walk-depth boundary — PITFALL #5b (×3)
    // ------------------------------------------------------------------ //

    @Test fun `commitReadonly in intermediate scope is visible to child of that scope`() {
        runBlocking {
            checkAll(10000, arbName, arbType) { name, type ->
                val parent = SymbolTable()
                parent.declare(name, SymbolInfo(type, false, SymbolKind.Variable, pos()))
                val intermediate = parent.enterScope()
                intermediate.commitReadonly(setOf(name))
                val child = intermediate.enterScope()
                assertTrue(child.lookup(name)!!.isReadonly)
            }
        }
    }

    @Test fun `commitReadonly in intermediate scope does not affect sibling scope`() {
        runBlocking {
            checkAll(10000, arbName, arbType) { name, type ->
                val parent = SymbolTable()
                parent.declare(name, SymbolInfo(type, false, SymbolKind.Variable, pos()))
                val intermediate = parent.enterScope()
                intermediate.commitReadonly(setOf(name))
                val sibling = parent.enterScope()
                assertFalse(sibling.lookup(name)!!.isReadonly)
            }
        }
    }

    @Test fun `commitReadonly in intermediate scope does not affect grandparent lookup`() {
        runBlocking {
            checkAll(10000, arbName, arbType) { name, type ->
                val parent = SymbolTable()
                parent.declare(name, SymbolInfo(type, false, SymbolKind.Variable, pos()))
                val intermediate = parent.enterScope()
                intermediate.commitReadonly(setOf(name))
                // The owning scope must remain mutable regardless of what the
                // intermediate scope committed.
                assertFalse(parent.lookup(name)!!.isReadonly)
            }
        }
    }

    // ------------------------------------------------------------------ //
    // exitScope snapshot contract (×1)
    // ------------------------------------------------------------------ //

    @Test fun `exitScope returns exactly the names marked readonly in that child scope`() {
        runBlocking {
            checkAll(10000, arbName) { baseName ->
                // Prefix-based distinct names — no filtering needed.
                val n1 = "p1_$baseName"
                val n2 = "p2_$baseName"
                val parent = SymbolTable()
                parent.declare(n1, SymbolInfo(WaterfallType.IntType, false, SymbolKind.Variable, pos()))
                parent.declare(n2, SymbolInfo(WaterfallType.DecType, false, SymbolKind.Variable, pos()))
                val child = parent.enterScope()
                child.markReadonlyLocal(n1) // mark only n1; n2 stays mutable
                val snap = parent.exitScope(child)
                assertEquals(setOf(n1), snap)
            }
        }
    }

    // ------------------------------------------------------------------ //
    // markReadonlyLocal on undeclared — no-op (×1)
    // ------------------------------------------------------------------ //

    @Test fun `markReadonlyLocal on undeclared name returns false and shadow remains empty`() {
        runBlocking {
            checkAll(10000, arbName) { baseName ->
                // "d_$baseName" is declared; "u_$baseName" is never declared.
                // Different prefixes guarantee they cannot be equal.
                val declared = "d_$baseName"
                val undeclared = "u_$baseName"
                val st = SymbolTable()
                st.declare(declared, SymbolInfo(WaterfallType.IntType, false, SymbolKind.Variable, pos()))
                assertFalse(st.markReadonlyLocal(undeclared))
                assertTrue(st.localReadonlyShadow().isEmpty())
            }
        }
    }
}
