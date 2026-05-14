package com.aaroncoplan.waterfall.compiler;

/**
 * Thrown by {@link Main#run(String[])} when compilation fails for any reason
 * (bad arguments, missing files, syntax errors, verification errors).
 *
 * The wrapper {@link Main#main(String[])} catches this and exits non-zero so
 * the {@code ./waterfall} script signals failure to its caller. Tests that
 * invoke the compiler in-process call {@code Main.run(...)} directly so they
 * can observe (or assert against) the failure without ending the JVM.
 */
public class CompilerError extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CompilerError(String message) {
        super(message);
    }
}
