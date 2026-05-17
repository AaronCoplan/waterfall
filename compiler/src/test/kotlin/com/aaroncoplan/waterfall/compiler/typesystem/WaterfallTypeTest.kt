package com.aaroncoplan.waterfall.compiler.typesystem

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for WaterfallType.fromSourceText and WaterfallType.forReturnType.
 *
 * §1.2 specifies exactly which strings parse to which types and which produce
 * ErrorType. These tests catch silent-resolution bugs at the type-parsing layer
 * before the verifier even runs.
 *
 * Coverage: happy paths, all four primitives + their arrays, void, then every
 * explicitly-rejected case, then forReturnType contract, then render() round-trip.
 */
class WaterfallTypeTest {

    // --- fromSourceText: happy paths ---

    @Test fun fromSourceText_int() =
        assertEquals(WaterfallType.IntType, WaterfallType.fromSourceText("int"))

    @Test fun fromSourceText_dec() =
        assertEquals(WaterfallType.DecType, WaterfallType.fromSourceText("dec"))

    @Test fun fromSourceText_bool() =
        assertEquals(WaterfallType.BoolType, WaterfallType.fromSourceText("bool"))

    @Test fun fromSourceText_char() =
        assertEquals(WaterfallType.CharType, WaterfallType.fromSourceText("char"))

    @Test fun fromSourceText_intArray() =
        assertEquals(WaterfallType.ArrayType(WaterfallType.IntType), WaterfallType.fromSourceText("int[]"))

    @Test fun fromSourceText_decArray() =
        assertEquals(WaterfallType.ArrayType(WaterfallType.DecType), WaterfallType.fromSourceText("dec[]"))

    @Test fun fromSourceText_boolArray() =
        assertEquals(WaterfallType.ArrayType(WaterfallType.BoolType), WaterfallType.fromSourceText("bool[]"))

    @Test fun fromSourceText_charArray() =
        assertEquals(WaterfallType.ArrayType(WaterfallType.CharType), WaterfallType.fromSourceText("char[]"))

    @Test fun fromSourceText_void_givesVoidType() {
        assertEquals(WaterfallType.VoidType, WaterfallType.fromSourceText("void"))
    }

    // --- fromSourceText: rejected cases ---

    @Test fun fromSourceText_voidArray_givesErrorType() {
        val r = WaterfallType.fromSourceText("void[]")
        assertTrue("void[] must produce ErrorType, got $r", r is WaterfallType.ErrorType)
        assertEquals("void[]", (r as WaterfallType.ErrorType).sourceText)
    }

    @Test fun fromSourceText_doubleArray_givesErrorType() {
        assertTrue(WaterfallType.fromSourceText("int[][]") is WaterfallType.ErrorType)
    }

    @Test fun fromSourceText_nullable_givesErrorType() {
        val r = WaterfallType.fromSourceText("?int")
        assertTrue(r is WaterfallType.ErrorType)
        assertEquals("?int", (r as WaterfallType.ErrorType).sourceText)
    }

    @Test fun fromSourceText_nullableArray_givesErrorType() {
        assertTrue(WaterfallType.fromSourceText("?int[]") is WaterfallType.ErrorType)
    }

    @Test fun fromSourceText_uppercaseInt_givesErrorType() {
        assertTrue(WaterfallType.fromSourceText("INT") is WaterfallType.ErrorType)
        assertTrue(WaterfallType.fromSourceText("Int") is WaterfallType.ErrorType)
    }

    @Test fun fromSourceText_leadingSpace_givesErrorType() {
        assertTrue(WaterfallType.fromSourceText(" int") is WaterfallType.ErrorType)
    }

    @Test fun fromSourceText_trailingSpace_givesErrorType() {
        assertTrue(WaterfallType.fromSourceText("int ") is WaterfallType.ErrorType)
    }

    @Test fun fromSourceText_empty_givesErrorType() {
        val r = WaterfallType.fromSourceText("")
        assertTrue(r is WaterfallType.ErrorType)
    }

    @Test fun fromSourceText_whitespaceOnly_givesErrorType() {
        assertTrue(WaterfallType.fromSourceText("   ") is WaterfallType.ErrorType)
    }

    @Test fun fromSourceText_unknownIdentifier_givesErrorType() {
        assertTrue(WaterfallType.fromSourceText("String") is WaterfallType.ErrorType)
        assertTrue(WaterfallType.fromSourceText("VoidType") is WaterfallType.ErrorType)
        assertTrue(WaterfallType.fromSourceText("unknown") is WaterfallType.ErrorType)
    }

    @Test fun fromSourceText_errorTypePreservesSourceText() {
        listOf("void[]", "int[][]", "?int", "INT", "VoidType").forEach { input ->
            val r = WaterfallType.fromSourceText(input)
            assertTrue("Expected ErrorType for '$input'", r is WaterfallType.ErrorType)
            assertEquals(
                "ErrorType.sourceText must equal original input for '$input'",
                input, (r as WaterfallType.ErrorType).sourceText
            )
        }
    }

    // --- render() round-trip ---

    @Test fun render_roundTripForAllValidTypes() {
        val valid = listOf(
            WaterfallType.IntType,
            WaterfallType.DecType,
            WaterfallType.BoolType,
            WaterfallType.CharType,
            WaterfallType.VoidType,
            WaterfallType.ArrayType(WaterfallType.IntType),
            WaterfallType.ArrayType(WaterfallType.DecType),
            WaterfallType.ArrayType(WaterfallType.BoolType),
            WaterfallType.ArrayType(WaterfallType.CharType),
        )
        for (type in valid) {
            val rendered = type.render()
            val roundTripped = WaterfallType.fromSourceText(rendered)
            assertEquals("render→fromSourceText round-trip failed for $type", type, roundTripped)
        }
    }

    // --- forReturnType ---

    @Test fun forReturnType_null_givesVoidType() {
        assertEquals(WaterfallType.VoidType, WaterfallType.forReturnType(null))
    }

    @Test fun forReturnType_int_givesIntType() {
        assertEquals(WaterfallType.IntType, WaterfallType.forReturnType("int"))
    }

    @Test fun forReturnType_void_givesVoidType() {
        assertEquals(WaterfallType.VoidType, WaterfallType.forReturnType("void"))
    }

    @Test fun forReturnType_intArray_givesArrayType() {
        assertEquals(WaterfallType.ArrayType(WaterfallType.IntType), WaterfallType.forReturnType("int[]"))
    }

    @Test fun forReturnType_voidArray_givesErrorType() {
        assertTrue(WaterfallType.forReturnType("void[]") is WaterfallType.ErrorType)
    }

    @Test fun forReturnType_emptyString_givesErrorType() {
        assertTrue(WaterfallType.forReturnType("") is WaterfallType.ErrorType)
    }
}
