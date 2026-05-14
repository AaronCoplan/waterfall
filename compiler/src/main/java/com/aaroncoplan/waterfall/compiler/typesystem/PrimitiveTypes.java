package com.aaroncoplan.waterfall.compiler.typesystem;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The Waterfall primitive type names, as advertised in README.md. The front-end
 * uses these for verification; back-ends map them to target-language equivalents.
 *
 * The {@code type} grammar rule produces names like {@code int} or {@code int[]}.
 * Use {@link #isPrimitive(String)} for a scalar primitive, {@link #isArray(String)}
 * to recognize the array suffix, and {@link #elementType(String)} to strip it.
 */
public final class PrimitiveTypes {

    public static final String INT  = "int";
    public static final String DEC  = "dec";
    public static final String BOOL = "bool";
    public static final String CHAR = "char";

    public static final Set<String> ALL = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(INT, DEC, BOOL, CHAR)));

    private PrimitiveTypes() {}

    /** Scalar primitive only ({@code int}, {@code dec}, {@code bool}, {@code char}). */
    public static boolean isPrimitive(String type) {
        return type != null && ALL.contains(type);
    }

    /** True for {@code int[]}, {@code dec[]}, {@code bool[]}, {@code char[]}. */
    public static boolean isArray(String type) {
        if (type == null || !type.endsWith("[]")) return false;
        return ALL.contains(type.substring(0, type.length() - 2));
    }

    /** Returns either a scalar primitive name or a primitive-array name. */
    public static boolean isPrimitiveOrArray(String type) {
        return isPrimitive(type) || isArray(type);
    }

    /** For an array type like {@code int[]}, returns {@code int}. For a scalar type, returns itself. */
    public static String elementType(String type) {
        return isArray(type) ? type.substring(0, type.length() - 2) : type;
    }
}
