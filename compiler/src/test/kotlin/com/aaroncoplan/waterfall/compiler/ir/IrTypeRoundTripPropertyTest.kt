package com.aaroncoplan.waterfall.compiler.ir

import com.aaroncoplan.waterfall.compiler.typesystem.WaterfallType
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

/**
 * Property-based round-trip tests for [IrType] ↔ [WaterfallType] conversion.
 *
 * Core invariant: [IrType.fromWaterfallType] + [IrType.asWaterfallType] form a
 * lossless round-trip for all valid (non-error) WaterfallType values.
 * This is the P10 Leg 1 IrType invariant family per playbook §3.
 *
 * TRIP-WIRE: after this file lands, run
 *   ./gradlew test --tests IrTypeRoundTripPropertyTest
 * If zero tests are reported, the runner integration is broken — pause before proceeding.
 *
 * Style: JUnit-4 block-body + Kotest checkAll (block body avoids the
 * `= runBlocking { checkAll(...) }` void-return trap discovered in §5.2 — Kotest 5.x
 * `checkAll` returns `PropertyContext`, making expression-body methods non-void).
 *
 * Coverage partitions:
 *   - Scalar types (Int, Dec, Bool, Char, Void): 5 variants
 *   - Array types: ArrayType(scalar) — depth-1 only; depth-2+ is structurally
 *     allowed but not source-producible in P10 (grammar restricts to primitive[]).
 *   - ErrorType: EXCLUDED from the round-trip Arb; covered by a dedicated unit
 *     test asserting that fromWaterfallType(ErrorType) throws IllegalStateException.
 *
 * Known gaps (documented, not silently accepted):
 *   - Depth-2+ arrays (e.g. int[][]) excluded — not source-producible in P10 grammar.
 *   - WaterfallType.ArrayType(VoidType) excluded as array element — void[] cannot be
 *     produced from source text (WaterfallType.fromSourceText("void[]") returns
 *     ErrorType per the §1.2 guard). Structurally constructible but semantically invalid;
 *     excluding keeps the Arb within the reachable program state.
 */
class IrTypeRoundTripPropertyTest {

    // ------------------------------------------------------------------ //
    // Arb definitions
    // ------------------------------------------------------------------ //

    /** The 5 valid scalar WaterfallType variants. ErrorType excluded. */
    private val arbScalarType: Arb<WaterfallType> = Arb.of(
        WaterfallType.IntType,
        WaterfallType.DecType,
        WaterfallType.BoolType,
        WaterfallType.CharType,
        WaterfallType.VoidType
    )

    /**
     * Valid array element types (scalars minus Void).
     * void[] is source-invalid — excluded to keep the Arb within source-producible programs.
     */
    private val arbArrayElementType: Arb<WaterfallType> = Arb.of(
        WaterfallType.IntType,
        WaterfallType.DecType,
        WaterfallType.BoolType,
        WaterfallType.CharType
    )

    /** Depth-1 array types over valid element types. */
    private val arbArrayType: Arb<WaterfallType> =
        arbArrayElementType.map { WaterfallType.ArrayType(it) }

    // ------------------------------------------------------------------ //
    // Round-trip properties (×2)
    // ------------------------------------------------------------------ //

    @Test fun `scalar WaterfallType round-trips through IrType`() {
        runBlocking {
            checkAll(10000, arbScalarType) { wt ->
                val irType = IrType.fromWaterfallType(wt)
                assertEquals(wt, irType.asWaterfallType())
            }
        }
    }

    @Test fun `array WaterfallType round-trips through IrType`() {
        runBlocking {
            checkAll(10000, arbArrayType) { wt ->
                val irType = IrType.fromWaterfallType(wt)
                assertEquals(wt, irType.asWaterfallType())
            }
        }
    }

    // ------------------------------------------------------------------ //
    // Render consistency (×1)
    // ------------------------------------------------------------------ //

    @Test fun `IrType render matches WaterfallType render for all scalar types`() {
        runBlocking {
            checkAll(10000, arbScalarType) { wt ->
                val irType = IrType.fromWaterfallType(wt)
                assertEquals(wt.render(), irType.render())
            }
        }
    }

    // ------------------------------------------------------------------ //
    // ErrorType guard — unit test, not property (×1)
    // ------------------------------------------------------------------ //

    @Test fun `fromWaterfallType on ErrorType throws IllegalStateException`() {
        // ErrorType is the "verifier should have caught this" sentinel.
        // IrLowering must never be called on unverified (error-typed) programs.
        try {
            IrType.fromWaterfallType(WaterfallType.ErrorType("bogus"))
            fail("Expected IllegalStateException — lowering must not receive ErrorType inputs")
        } catch (e: IllegalStateException) {
            // Expected. Message content is an implementation detail; not asserted.
        }
    }
}
