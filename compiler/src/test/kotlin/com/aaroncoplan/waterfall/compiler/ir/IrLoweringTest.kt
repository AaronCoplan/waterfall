package com.aaroncoplan.waterfall.compiler.ir

import com.aaroncoplan.waterfall.compiler.statements.ModuleAst
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.compiler.verifier.Verifier
import com.aaroncoplan.waterfall.parser.FileParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Golden-IR oracle tests for [IrLowering]. For each canonical example, the
 * test compiles the source, verifies it, lowers to IR, and asserts the
 * `IrModule.toString()` output byte-identical matches a committed golden file.
 *
 * **Regenerate goldens:** `UPDATE_GOLDEN_IR=1 ./gradlew :compiler:test --tests IrLoweringTest`
 *
 * **SA-5 note:** Uses Kotlin data class auto-generated `toString`, which is
 * stable within Kotlin 2.0.21 (format: `ClassName(field1=value1, ...)`).
 *
 * **Examples chosen:**
 * - `FibonacciModule.wf` — canonical recursive function; exercises if-block,
 *   while-loop, local vars, compound assignment, increment, IDENTIFIER lookup,
 *   FUNCTION_CALL return types.
 * - `ArrayLiteralsModule.wf` — exercises ARRAY literals and ARRAY_INDEX.
 * - `VariableDeclarationsModule.wf` — minimal case: one top-level typed var.
 */
class IrLoweringTest {

    // Working dir is `compiler/` when Gradle runs tests for :compiler subproject.
    private val examplesRoot = Paths.get("../examples").toAbsolutePath()
    private val goldenIrRoot = Paths.get("src/test/resources/golden-ir").toAbsolutePath()
    // Normalize machine-specific absolute paths to a portable placeholder so
    // golden files are committed portably. The project root is the parent of
    // the `compiler/` working directory.
    private val projectRoot: String = Paths.get("").toAbsolutePath().normalize().parent.toString()

    private fun lowerExample(fileName: String): IrModule {
        val filePath = examplesRoot.resolve(fileName).toString()
        val parseResult = FileParser.parseFile(filePath)
        assertFalse("Expected no syntax errors in $fileName", parseResult.hasErrors())
        val moduleAst = ModuleAst(filePath, parseResult.getProgramAST().module())
        val symbolTable = SymbolTable()
        val verifyResult = Verifier.verifyModule(moduleAst, symbolTable)
        assertTrue(
            "Expected successful verification of $fileName; errors: ${verifyResult.errors}",
            verifyResult.isSuccessful
        )
        return IrLowering.lowerModule(moduleAst, verifyResult.resolvedTypes)
    }

    /** Replace machine-specific absolute paths with a portable placeholder. */
    private fun normalize(s: String): String = s.replace(projectRoot, "PROJECT_ROOT")

    private fun checkOrUpdate(ir: IrModule, goldenName: String) {
        val actual = normalize(ir.toString())
        val goldenPath = goldenIrRoot.resolve("$goldenName.txt")
        if (System.getenv("UPDATE_GOLDEN_IR") == "1") {
            Files.createDirectories(goldenPath.parent)
            Files.write(goldenPath, actual.toByteArray(Charsets.UTF_8))
            return
        }
        val expected = String(Files.readAllBytes(goldenPath), Charsets.UTF_8)
        assertEquals(
            "IR golden mismatch for $goldenName (set UPDATE_GOLDEN_IR=1 to regenerate)",
            expected, actual
        )
    }

    @Test fun fibonacciModuleIr() {
        val ir = lowerExample("FibonacciModule.wf")
        checkOrUpdate(ir, "FibonacciModule")
    }

    @Test fun arrayLiteralsModuleIr() {
        val ir = lowerExample("ArrayLiteralsModule.wf")
        checkOrUpdate(ir, "ArrayLiteralsModule")
    }

    @Test fun variableDeclarationsModuleIr() {
        val ir = lowerExample("VariableDeclarationsModule.wf")
        checkOrUpdate(ir, "VariableDeclarationsModule")
    }
}
