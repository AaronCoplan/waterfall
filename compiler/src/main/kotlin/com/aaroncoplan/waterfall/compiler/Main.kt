package com.aaroncoplan.waterfall.compiler

import com.aaroncoplan.waterfall.compiler.argumentparsing.ArgParser
import com.aaroncoplan.waterfall.compiler.statements.ModuleAst
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable
import com.aaroncoplan.waterfall.compiler.target.Backends
import com.aaroncoplan.waterfall.parser.FileParser
import com.aaroncoplan.waterfall.parser.FileUtils
import org.apache.logging.log4j.LogManager
import kotlin.system.exitProcess

object Main {

    private val logger = LogManager.getLogger(Main::class.java)

    /**
     * Thin wrapper: catches [CompilerError] so the `./waterfall` script exits
     * non-zero on failure. Tests invoke [run] directly so they can observe the
     * exception.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            run(args)
        } catch (e: CompilerError) {
            // Diagnostic message was already printed via the run path; surface the
            // top-level summary just in case nothing else made it to stderr.
            val msg = e.message
            if (!msg.isNullOrEmpty()) System.err.println(msg)
            exitProcess(1)
        }
    }

    /** Compiler entry point. Throws [CompilerError] on any failure. */
    @JvmStatic
    fun run(args: Array<String>) {
        logger.info("[START] Argument Parsing")
        val argParseResult = ArgParser.parseCommandLineArgs(args)
        val arguments = argParseResult.firstVal
        val errorMsg = argParseResult.secondVal
        if (arguments == null) {
            System.err.println(errorMsg)
            throw CompilerError("argument parsing failed")
        }
        logger.info("[END] Argument Parsing")

        val backend = Backends.forTarget(arguments.getTarget())
            ?: throw CompilerError("unknown target: ${arguments.getTarget()}")

        logger.info("[START] Existence Check")
        for (filePath in arguments.getFiles()) {
            val fileCheckResult = FileUtils.isReadableFile(filePath)
            if (!fileCheckResult.firstVal) {
                System.err.println(fileCheckResult.secondVal)
                throw CompilerError("file existence check failed")
            }
        }
        logger.info("[END] Existence Check")

        logger.info("[START] Parse Files")
        val parseResultList = arguments.getFiles().map { FileParser.parseFile(it) }
        logger.info("[END] Parse Files")

        logger.info("[START] Syntax Errors Check")
        var hasErrors = false
        for (parseResult in parseResultList) {
            if (!parseResult.hasErrors()) continue
            parseResult.getSyntaxErrors().forEach { System.err.println(it) }
            hasErrors = true
        }
        if (hasErrors) throw CompilerError("syntax errors")
        logger.info("[END] Syntax Errors Check")

        logger.info("[START] Verification and Translation")
        val seenModuleNames = HashSet<String>()
        for (parseResult in parseResultList) {
            val ast = parseResult.getProgramAST()
            val module = ast.module()
            val moduleName = module.name.text
            if (!seenModuleNames.add(moduleName)) {
                System.err.println("Error: the name $moduleName already exists!")
                throw CompilerError("duplicate module name")
            }

            // Fresh top-level scope per module. Each top-level decl's verify() declares
            // itself into this scope, surfacing duplicate-top-level errors. Function
            // bodies create their own child scope and recurse — inner var-decls now
            // declare too, so a duplicate `int x = 1` inside a function body fails.
            val symbolTable = SymbolTable(null)
            val moduleAst = ModuleAst(parseResult.getFilePath(), module)

            for (v in moduleAst.topLevelVariables) {
                val r = v.verify(symbolTable)
                if (!r.isSuccessful()) {
                    System.err.println("${r.getErrorMessage()} in ${v.getSourcePosition().generateMessage()}")
                    throw CompilerError("verification failed")
                }
            }
            for (f in moduleAst.functions) {
                val r = f.verify(symbolTable)
                if (!r.isSuccessful()) {
                    System.err.println("${r.getErrorMessage()} in ${f.getSourcePosition().generateMessage()}")
                    throw CompilerError("verification failed")
                }
            }

            println(backend.emitProgram(moduleAst))
        }
        logger.info("[END] Verification and Translation")
    }
}
