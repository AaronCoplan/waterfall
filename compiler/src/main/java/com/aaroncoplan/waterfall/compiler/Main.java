package com.aaroncoplan.waterfall.compiler;

import com.aaroncoplan.waterfall.generated.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.argumentparsing.Arguments;
import com.aaroncoplan.waterfall.compiler.argumentparsing.ArgParser;
import com.aaroncoplan.waterfall.compiler.statements.FunctionImplementationData;
import com.aaroncoplan.waterfall.compiler.statements.ModuleAst;
import com.aaroncoplan.waterfall.compiler.statements.TypedVariableDeclarationAndAssignmentData;
import com.aaroncoplan.waterfall.compiler.statements.helpers.VerificationResult;
import com.aaroncoplan.waterfall.compiler.symboltables.SymbolTable;
import com.aaroncoplan.waterfall.compiler.symboltables.TopLevelSymbolTableGenerator;
import com.aaroncoplan.waterfall.compiler.target.Backends;
import com.aaroncoplan.waterfall.compiler.target.CodeGenerator;
import com.aaroncoplan.waterfall.parser.FileUtils;
import com.aaroncoplan.waterfall.parser.FileParser;
import com.aaroncoplan.waterfall.parser.Pair;
import com.aaroncoplan.waterfall.parser.ParseResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    /**
     * Thin wrapper: catches {@link CompilerError} so the {@code ./waterfall}
     * script exits non-zero on failure. Tests invoke {@link #run(String[])}
     * directly so they can observe the exception.
     */
    public static void main(String[] args) {
        try {
            run(args);
        } catch (CompilerError e) {
            // Diagnostic message was already printed via the run path; surface the
            // top-level summary just in case nothing else made it to stderr.
            if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                System.err.println(e.getMessage());
            }
            System.exit(1);
        }
    }

    /**
     * Compiler entry point. Throws {@link CompilerError} on any failure.
     */
    public static void run(String[] args) {
        logger.info("[START] Argument Parsing");
        final Pair<Arguments, String> argParseResult = ArgParser.parseCommandLineArgs(args);
        final Arguments arguments = argParseResult.firstVal;
        final String errorMsg = argParseResult.secondVal;
        if (arguments == null) {
            System.err.println(errorMsg);
            throw new CompilerError("argument parsing failed");
        }
        logger.info("[END] Argument Parsing");

        final CodeGenerator backend = Backends.forTarget(arguments.getTarget());

        logger.info("[START] Existence Check");
        for (String filePath : arguments.getFiles()) {
            final Pair<Boolean, String> fileCheckResult = FileUtils.isReadableFile(filePath);
            if (!fileCheckResult.firstVal) {
                System.err.println(fileCheckResult.secondVal);
                throw new CompilerError("file existence check failed");
            }
        }
        logger.info("[END] Existence Check");

        logger.info("[START] Parse Files");
        List<ParseResult> parseResultList = arguments.getFiles()
                .stream()
                .map(FileParser::parseFile)
                .collect(Collectors.toList());
        logger.info("[END] Parse Files");

        logger.info("[START] Syntax Errors Check");
        boolean hasErrors = false;
        for (ParseResult parseResult : parseResultList) {
            if (!parseResult.hasErrors()) continue;
            parseResult.getSyntaxErrors().forEach(System.err::println);
            hasErrors = true;
        }
        if (hasErrors) throw new CompilerError("syntax errors");
        logger.info("[END] Syntax Errors Check");

        logger.info("[START] Top Level Symbol Table Creation");
        Map<String, SymbolTable> symbolTableRegistry = new HashMap<>();
        for (ParseResult parseResult : parseResultList) {
            WaterfallParser.ProgramContext ast = parseResult.getProgramAST();
            WaterfallParser.ModuleContext module = ast.module();
            final SymbolTable symbolTable = TopLevelSymbolTableGenerator.generateFromModule(parseResult.getFilePath(), module);
            if (symbolTable == null) throw new CompilerError("top-level symbol table creation failed");
            if (symbolTableRegistry.containsKey(module.name.getText())) {
                System.err.format("Error: the name %s already exists!%n", module.name.getText());
                throw new CompilerError("duplicate module name");
            }
            symbolTableRegistry.put(module.name.getText(), symbolTable);
        }
        logger.info("[END] Top Level Symbol Table Creation");

        logger.info("[START] Inline Verification and Translation");
        for (ParseResult parseResult : parseResultList) {
            WaterfallParser.ProgramContext ast = parseResult.getProgramAST();
            WaterfallParser.ModuleContext module = ast.module();
            final SymbolTable symbolTable = symbolTableRegistry.get(module.name.getText());
            final ModuleAst moduleAst = new ModuleAst(parseResult.getFilePath(), module);

            for (TypedVariableDeclarationAndAssignmentData v : moduleAst.topLevelVariables) {
                VerificationResult r = v.verify(symbolTable);
                if (!r.isSuccessful()) {
                    System.err.format("%s in %s%n", r.getErrorMessage(), v.getSourcePosition().generateMessage());
                    throw new CompilerError("verification failed");
                }
            }
            for (FunctionImplementationData f : moduleAst.functions) {
                VerificationResult r = f.verify(symbolTable);
                if (!r.isSuccessful()) {
                    System.err.format("%s in %s%n", r.getErrorMessage(), f.getSourcePosition().generateMessage());
                    throw new CompilerError("verification failed");
                }
            }

            System.out.println(backend.emitProgram(moduleAst));
        }
        logger.info("[END] Inline Verification and Translation");
    }
}
