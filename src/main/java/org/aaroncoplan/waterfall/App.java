package org.aaroncoplan.waterfall;

import com.aaroncoplan.waterfall.WaterfallLexer;
import com.aaroncoplan.waterfall.WaterfallParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import net.sourceforge.argparse4j.inf.Namespace;

import org.antlr.v4.runtime.*;

public class App {

    public String getGreeting() {
        return "Hello world.";
    }

    public static void main(
        String[] args
    ) /*
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
        final List<Object> scopedAST = new ArrayList<Object>();
        // PASS 1
        int statementNumber = 0;
        for (WaterfallParser.CodelineContext codelineContext : codelineContexts) {
            if (codelineContext.variableDeclaration() != null) {
                VariableDeclaration variableDeclaration = new VariableDeclaration(
                    codelineContext.variableDeclaration(),
                    globalScope
                );
                if (globalScope.getReference(variableDeclaration.getVariableName()) != null) {
                    System.err.println(
                        "INVALID DECL, var already declared: " + variableDeclaration.getVariableName(

                        )
                    );
                    System.exit(-1);
                }
                globalScope.addReference(variableDeclaration.getVariableName(), statementNumber);

                scopedAST.add(variableDeclaration);
            } else if (codelineContext.variableAssignment() != null) {
                VariableAssignment variableAssignment = new VariableAssignment(
                    codelineContext.variableAssignment(),
                    globalScope
                );
                scopedAST.add(variableAssignment);
            } else {
                throw new RuntimeException("Unknown statement type");
            }
            ++statementNumber;
        }
        globalScope.debugPrint();

        // PASS 2
        statementNumber = 0;
        for (Object obj : scopedAST) {
            if (obj instanceof VariableDeclaration) {
                final VariableDeclaration variableDeclaration = (VariableDeclaration) obj;
                final Integer refLineNumber = variableDeclaration.getScope().getReference(
                    variableDeclaration.getVariableName()
                );
                if (refLineNumber == null || statementNumber < refLineNumber) {
                    System.err.println("INVALID STATEMENT");
                    System.exit(-1);
                }
                System.out.println("Valid ref: " + variableDeclaration.getVariableName());
                System.out.println("[EMIT] " + variableDeclaration.emit());
            } else if (obj instanceof VariableAssignment) {
                final VariableAssignment variableAssignment = (VariableAssignment) obj;
                final Integer refLineNumber = variableAssignment.getScope().getReference(
                    variableAssignment.getVariableName()
                );
                if (refLineNumber == null || statementNumber < refLineNumber) {
                    System.err.println("Using variable w/o or before declaration");
                    System.exit(-1);
                }
                System.out.println("Valid assignment: " + variableAssignment.getVariableName());
                //System.out.println(variableAssignment.emit());
            } else {
                throw new RuntimeException("Unknown obj type in Scoped AST");
            }
            ++statementNumber;
        }
        */
    {
        final Namespace namespace = CommandLineArgParser.parse(args);

        final Object files = namespace.get("files");
        if (files == null || !(files instanceof ArrayList)) {
            System.out.println("[ERROR] Files listed to compile are not a list of strings.");
            System.exit(-1);
        }
        @SuppressWarnings("unchecked")
        ArrayList<String> fileList = (ArrayList<String>) files;
        for (String filePath : fileList) {
            final ParseResult parseResult = FileParser.parseFile(filePath);
            System.out.println(filePath);
        }
    }

}

