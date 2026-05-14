package com.aaroncoplan.waterfall.compiler.typesystem;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The Waterfall primitive type names, as advertised in README.md. The front-end
 * uses these for verification; back-ends map them to target-language equivalents.
 */
public final class PrimitiveTypes {

    public static final String INT  = "int";
    public static final String DEC  = "dec";
    public static final String BOOL = "bool";
    public static final String CHAR = "char";

    public static final Set<String> ALL = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(INT, DEC, BOOL, CHAR)));

    private PrimitiveTypes() {}

    public static boolean isPrimitive(String type) {
        return ALL.contains(type);
    }
}
