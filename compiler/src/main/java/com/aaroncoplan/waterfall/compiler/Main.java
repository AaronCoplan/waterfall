package com.aaroncoplan.waterfall.compiler;

import com.aaroncoplan.waterfall.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.argumentparsing.Arguments;
import com.aaroncoplan.waterfall.compiler.argumentparsing.ArgParser;
import com.aaroncoplan.waterfall.parser.FileUtils;
import com.aaroncoplan.waterfall.parser.FileParser;
import com.aaroncoplan.waterfall.parser.Pair;
import com.aaroncoplan.waterfall.parser.ParseResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("[START] Argument Parsing");
        final Pair<Arguments, String> argParseResult = ArgParser.parseCommandLineArgs(args);
        final Arguments arguments = argParseResult.firstVal;
        final String errorMsg = argParseResult.secondVal;
        // if namespace is null, there is an error
        // print out the error message and exit
        if(arguments == null) {
            System.out.println(errorMsg);
            return;
        }
        logger.info("[END] Argument Parsing");

        // firstVal check that each of the files exists
        logger.info("[START] Existence Check");
        for(String filePath : arguments.getFiles()) {
            final Pair<Boolean, String> fileCheckResult = FileUtils.isReadableFile(filePath);
            if(!fileCheckResult.firstVal) {
                System.out.println(fileCheckResult.secondVal);
                return;
            }
        }
        logger.info("[END] Existence Check");

        // all of the files exist, parse them all
        logger.info("[START] Parse Files");
        List<ParseResult> parseResultList = arguments.getFiles()
                .stream()
                .map(FileParser::parseFile)
                .collect(Collectors.toList());
        logger.info("[END] Parse Files");

        // check for syntax error(s)
        // exit if at least 1 file has syntax error(s)
        logger.info("[START] Syntax Errors Check");
        boolean hasErrors = false;
        for(ParseResult parseResult : parseResultList) {
            if(!parseResult.hasErrors()) continue;
            parseResult.getSyntaxErrors().forEach(System.out::println);
            hasErrors = true;
        }
        if(hasErrors) return;
        logger.info("[END] Syntax Errors Check");

        logger.info("[START] Top Level Symbol Table Creation");
        for(ParseResult parseResult : parseResultList) {
            WaterfallParser.ProgramContext ast = parseResult.getProgramAST();
            WaterfallParser.ModuleContext module = ast.module();

            final String moduleName = module.name.getText();

            final SymbolTable symbolTable = new SymbolTable(null);

            for(WaterfallParser.TopLevelDeclarationContext topLevelDeclaration : module.topLevelDeclaration()) {
                if(topLevelDeclaration.typedVariableDeclarationAndAssignment() != null) {
                    WaterfallParser.TypedVariableDeclarationAndAssignmentContext typedVariableDeclarationAndAssignment = topLevelDeclaration.typedVariableDeclarationAndAssignment();
                    String variableType = typedVariableDeclarationAndAssignment.type().getText();
                    String variableName = typedVariableDeclarationAndAssignment.name.getText();
                    try{
                        symbolTable.declare(variableName, variableType);
                    } catch (DuplicateDeclarationException e) {
                        int line = typedVariableDeclarationAndAssignment.start.getLine();
                        int charPosition = typedVariableDeclarationAndAssignment.start.getCharPositionInLine();
                        System.out.format("Duplicate declaration when declaring %s in %s at %d:%d", variableName, moduleName, line, charPosition).println();
                        return;
                    }

                } else if(topLevelDeclaration.functionImplementation() != null) {
                    WaterfallParser.FunctionImplementationContext functionImplementation = topLevelDeclaration.functionImplementation();
                    String functionName = functionImplementation.name.getText();
                    String returnType = functionImplementation.returnType == null ? null : functionImplementation.returnType.getText();
                    List<WaterfallParser.TypedArgumentContext> typedArgumentsContext = functionImplementation.typedArgumentList() == null ? Collections.emptyList() : functionImplementation.typedArgumentList().typedArgument();
                    List<Pair<String, String>> typedArguments = typedArgumentsContext.stream().map(arg -> new Pair<>(arg.type().getText(), arg.name.getText())).collect(Collectors.toList());
                    try {
                        symbolTable.declare(functionName, returnType);
                    } catch (DuplicateDeclarationException e) {
                        int line = functionImplementation.start.getLine();
                        int charPosition = functionImplementation.start.getCharPositionInLine();
                        System.out.format("Duplicate declaration when declaring %s in %s at %d:%d", functionName, moduleName, line, charPosition).println();
                        return;
                    }
                }
            }
        }
        logger.info("[END] Top Level Symbol Table Creation");
    }

}
