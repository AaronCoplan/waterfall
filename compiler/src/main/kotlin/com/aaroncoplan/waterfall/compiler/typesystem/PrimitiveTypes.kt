package com.aaroncoplan.waterfall.compiler.typesystem

/**
 * The Waterfall primitive type names, as advertised in README.md. The front-end
 * uses these for verification; back-ends map them to target-language equivalents.
 *
 * The `type` grammar rule produces names like `int` or `int[]`. Use [isPrimitive]
 * for a scalar primitive, [isArray] to recognize the array suffix, and
 * [elementType] to strip it.
 */
object PrimitiveTypes {

    const val INT  = "int"
    const val DEC  = "dec"
    const val BOOL = "bool"
    const val CHAR = "char"

    @JvmField
    val ALL: Set<String> = setOf(INT, DEC, BOOL, CHAR)

    /** Scalar primitive only (`int`, `dec`, `bool`, `char`). */
    @JvmStatic
    fun isPrimitive(type: String?): Boolean = type != null && ALL.contains(type)

    /** True for `int[]`, `dec[]`, `bool[]`, `char[]`. */
    @JvmStatic
    fun isArray(type: String?): Boolean {
        if (type == null || !type.endsWith("[]")) return false
        return ALL.contains(type.substring(0, type.length - 2))
    }

    /** Returns true for either a scalar primitive name or a primitive-array name. */
    @JvmStatic
    fun isPrimitiveOrArray(type: String?): Boolean = isPrimitive(type) || isArray(type)

    /** For an array type like `int[]`, returns `int`. For a scalar type, returns itself. */
    @JvmStatic
    fun elementType(type: String): String =
        if (isArray(type)) type.substring(0, type.length - 2) else type
}
