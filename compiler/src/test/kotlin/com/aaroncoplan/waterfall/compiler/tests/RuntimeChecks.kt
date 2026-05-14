package com.aaroncoplan.waterfall.compiler.tests

import org.junit.Assert.assertEquals
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * Shared scaffolding for the per-target "the language's own toolchain accepts
 * this output" tests. Each subclass plugs in its golden subdirectory, the
 * temp-file suffix, the command line to run, and a probe to check whether the
 * tool is on PATH.
 */
abstract class RuntimeCheckBase(
    protected val example: String,
    protected val goldenPath: Path,
) {

    protected abstract val toolName: String
    protected abstract val tempSuffix: String
    protected abstract fun commandFor(tmpPath: Path): List<String>

    private fun toolAvailable(): Boolean = try {
        val p = ProcessBuilder(toolName, "--version").redirectErrorStream(true).start()
        p.waitFor() == 0
    } catch (e: Exception) {
        false
    }

    protected fun checkWithTool(): Pair<Int, String> {
        val tmp = Files.createTempFile("waterfall-", tempSuffix)
        return try {
            Files.copy(goldenPath, tmp, StandardCopyOption.REPLACE_EXISTING)
            val pb = ProcessBuilder(commandFor(tmp)).redirectErrorStream(true)
            val proc = pb.start()
            val output = String(proc.inputStream.readAllBytes())
            val exit = proc.waitFor()
            exit to output
        } finally {
            Files.deleteIfExists(tmp)
        }
    }

    protected fun assumeAvailable() {
        Assume.assumeTrue("$toolName not on PATH; skipping runtime check", toolAvailable())
    }

    companion object {
        @JvmStatic
        fun goldenCases(targetSubdir: String): Collection<Array<Any>> {
            val goldens = Paths.get("src/test/resources/golden/$targetSubdir").toAbsolutePath()
            val out = mutableListOf<Array<Any>>()
            if (!Files.isDirectory(goldens)) return out
            Files.list(goldens).use { s ->
                for (g in s) {
                    val name = g.fileName.toString()
                    if (!name.endsWith(".expected")) continue
                    val example = name.removeSuffix(".expected")
                    out.add(arrayOf(example, g))
                }
            }
            return out
        }
    }
}

/**
 * For every checked-in `js/<example>.expected` file, run `node --check` on its
 * contents and assert that node accepts it as syntactically valid JavaScript.
 * If `node` isn't on PATH the test is assumed-skipped.
 */
@RunWith(Parameterized::class)
class JsRuntimeCheckTest(example: String, goldenPath: Path) : RuntimeCheckBase(example, goldenPath) {

    override val toolName = "node"
    override val tempSuffix = ".js"
    override fun commandFor(tmpPath: Path) = listOf("node", "--check", tmpPath.toString())

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): Collection<Array<Any>> = goldenCases("js")
    }

    @Test
    fun nodeAcceptsGolden() {
        assumeAvailable()
        val (exit, output) = checkWithTool()
        assertEquals("node --check failed for $example with output:\n$output", 0, exit)
    }
}

/**
 * For every checked-in `python/<example>.expected` file, run python3's
 * `ast.parse` on its contents and assert it accepts the output as syntactically
 * valid Python. Skipped if `python3` isn't on PATH.
 */
@RunWith(Parameterized::class)
class PythonRuntimeCheckTest(example: String, goldenPath: Path) : RuntimeCheckBase(example, goldenPath) {

    override val toolName = "python3"
    override val tempSuffix = ".py"
    override fun commandFor(tmpPath: Path) = listOf(
        "python3", "-c",
        "import ast,sys; ast.parse(open(sys.argv[1]).read())",
        tmpPath.toString()
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): Collection<Array<Any>> = goldenCases("python")
    }

    @Test
    fun python3AcceptsGolden() {
        assumeAvailable()
        val (exit, output) = checkWithTool()
        assertEquals("python3 ast.parse failed for $example with output:\n$output", 0, exit)
    }
}

/**
 * For every checked-in `c/<example>.expected` file, run `gcc -fsyntax-only` and
 * assert exit 0. Warnings (implicit function declarations, etc.) are suppressed
 * via `-Wno-*`; only actual syntax errors fail the test. Skipped if `gcc` isn't
 * on PATH.
 */
@RunWith(Parameterized::class)
class CRuntimeCheckTest(example: String, goldenPath: Path) : RuntimeCheckBase(example, goldenPath) {

    override val toolName = "gcc"
    override val tempSuffix = ".c"
    override fun commandFor(tmpPath: Path) = listOf(
        "gcc", "-fsyntax-only",
        "-Wno-implicit-function-declaration",
        "-Wno-int-conversion",
        "-Wno-return-type",
        tmpPath.toString()
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): Collection<Array<Any>> = goldenCases("c")
    }

    @Test
    fun gccAcceptsGolden() {
        assumeAvailable()
        val (exit, output) = checkWithTool()
        assertEquals("gcc -fsyntax-only failed for $example:\n$output", 0, exit)
    }
}
