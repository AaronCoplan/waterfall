package com.aaroncoplan.waterfall.compiler

/**
 * Thrown by [Main.run] when compilation fails for any reason
 * (bad arguments, missing files, syntax errors, verification errors).
 *
 * The wrapper [Main.main] catches this and exits non-zero so the
 * `./waterfall` script signals failure to its caller. Tests that invoke the
 * compiler in-process call [Main.run] directly so they can observe (or assert
 * against) the failure without ending the JVM.
 */
class CompilerError(message: String) : RuntimeException(message)
