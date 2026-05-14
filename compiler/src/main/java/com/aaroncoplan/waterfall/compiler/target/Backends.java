package com.aaroncoplan.waterfall.compiler.target;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry of available {@link CodeGenerator}s, indexed by the value passed to
 * {@code --target}. Phase 3+ register additional backends here by adding one
 * line to {@link #REGISTRY}.
 */
public final class Backends {

    public static final String DEFAULT_TARGET = "legacy";

    private static final Map<String, Supplier<CodeGenerator>> REGISTRY = new LinkedHashMap<>();
    static {
        REGISTRY.put("legacy", LegacyTextBackend::new);
    }

    private Backends() {}

    public static CodeGenerator forTarget(String name) {
        Supplier<CodeGenerator> s = REGISTRY.get(name);
        if (s == null) return null;
        return s.get();
    }

    public static String knownTargetsList() {
        return String.join(", ", REGISTRY.keySet());
    }
}
