package com.aaroncoplan.waterfall.compiler.tests

import com.aaroncoplan.waterfall.compiler.CompilerError
import com.aaroncoplan.waterfall.compiler.Main
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Golden / snapshot tests. For every checked-in
 * `compiler/src/test/resources/golden/<target>/<example>.expected` file, this
 * test runs the compiler with `--target <target>` on the matching
 * `examples/<example>.wf` and asserts the captured stdout equals the golden
 * file's contents.
 *
 * Regenerate by running `UPDATE_GOLDEN=1 ./gradlew test --tests GoldenTests`.
 */
@RunWith(Parameterized::class)
class GoldenTests(
    private val target: String,
    private val example: String,
    private val goldenPath: Path,
    private val examplePath: Path,
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}/{1}")
        fun cases(): Collection<Array<Any>> {
            val goldenRoot = Paths.get("src/test/resources/golden").toAbsolutePath()
            val examplesRoot = Paths.get("../examples").toAbsolutePath()
            val out = mutableListOf<Array<Any>>()
            if (!Files.isDirectory(goldenRoot)) return out
            Files.list(goldenRoot).use { targets ->
                for (targetDir in targets) {
                    if (!Files.isDirectory(targetDir)) continue
                    val target = targetDir.fileName.toString()
                    Files.list(targetDir).use { goldens ->
                        for (g in goldens) {
                            val name = g.fileName.toString()
                            if (!name.endsWith(".expected")) continue
                            val example = name.removeSuffix(".expected")
                            val examplePath = examplesRoot.resolve("$example.wf")
                            out.add(arrayOf(target, example, g, examplePath))
                        }
                    }
                }
            }
            return out
        }
    }

    @Test
    fun matchesGolden() {
        val actual = runCompiler()
        if (System.getenv("UPDATE_GOLDEN") == "1") {
            Files.createDirectories(goldenPath.parent)
            Files.write(goldenPath, actual.toByteArray())
            return
        }
        val expected = String(Files.readAllBytes(goldenPath))
        assertEquals(
            "Golden mismatch for $target/$example (set UPDATE_GOLDEN=1 to regenerate)",
            expected, actual
        )
    }

    private fun runCompiler(): String {
        val originalOut = System.out
        val originalErr = System.err
        val out = ByteArrayOutputStream()
        System.setOut(PrintStream(out))
        System.setErr(PrintStream(ByteArrayOutputStream()))
        try {
            // Some example files exercise error paths (duplicate decls) which now throw
            // CompilerError. Swallow it here — what the golden cares about is the stdout
            // captured before the throw.
            try {
                Main.run(arrayOf("--target", target, examplePath.toString()))
            } catch (ignored: CompilerError) {
                // expected for Duplicate*Module examples
            }
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
        }
        return out.toString()
    }
}
