package org.aaroncoplan.waterfall;

import com.aaroncoplan.waterfall.WaterfallLexer;
import com.aaroncoplan.waterfall.WaterfallParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.antlr.v4.runtime.*;

public class App {

    public String getGreeting() {
        return "Hello world.";
    }

    public static void main(String[] args) {
        // open file and read it in
        final String waterfallCodeFilePath = args[0];
        final StringBuilder code = new StringBuilder();
        try {
            Scanner sc = new Scanner(new File(waterfallCodeFilePath));
            while (sc.hasNextLine()) {
                code.append(sc.nextLine() + "\n");
            }
            sc.close();
        } catch(FileNotFoundException e) {
            System.err.format("File %s could not be found", waterfallCodeFilePath).println();
            System.exit(-1);
        }
        // parse using ANTLR
        final CharStream charStream = CharStreams.fromString(code.toString());
        final WaterfallLexer waterfallLexer = new WaterfallLexer(charStream);
        final CommonTokenStream tokenStream = new CommonTokenStream(waterfallLexer);

        final WaterfallParser waterfallParser = new WaterfallParser(tokenStream);
        waterfallParser.removeErrorListeners();
        SyntaxErrorListener errorListener = new SyntaxErrorListener(waterfallCodeFilePath);
        waterfallParser.addErrorListener(errorListener);

        final WaterfallParser.ProgramContext programAST = waterfallParser.program();
        System.out.println(programAST.toStringTree(waterfallParser));

        final List<String> syntaxErrors = errorListener.getSyntaxErrors();
        if (syntaxErrors.size() > 0) {
            System.out.println("ERROR MESSAGES");
            for (String msg : syntaxErrors) {
                System.out.println(msg);
            }
            System.exit(-1);
        }
        final Scope globalScope = new Scope(null);

        final List<WaterfallParser.CodelineContext> codelineContexts = programAST.code().codeline();
        final List<VariableDeclaration> scopedAST = new ArrayList<VariableDeclaration>();
        // PASS 1
        int statementNumber = 0;
        for (WaterfallParser.CodelineContext codelineContext : codelineContexts) {
            VariableDeclaration variableDeclaration = new VariableDeclaration(
                codelineContext.variableDeclaration(),
                globalScope
            );
            if (globalScope.getReference(variableDeclaration.getVariableName()) != null) {
                System.err.println(
                    "INVALID DECL, var already declared: " + variableDeclaration.getVariableName()
                );
                System.exit(-1);
            }
            globalScope.addReference(variableDeclaration.getVariableName(), statementNumber);

            scopedAST.add(variableDeclaration);

            ++statementNumber;
        }
        globalScope.debugPrint();

        // PASS 2
        statementNumber = 0;
        for (VariableDeclaration variableDeclaration : scopedAST) {
            final Integer refLineNumber = variableDeclaration.getScope().getReference(
                variableDeclaration.getVariableName()
            );
            if (refLineNumber == null || statementNumber < refLineNumber) {
                System.err.println("INVALID STATEMENT");
                System.exit(-1);
            }
            System.out.println("Valid ref: " + variableDeclaration.getVariableName());
            System.out.println(variableDeclaration.emit());
            ++statementNumber;
        }
    }

}

