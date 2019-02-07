package org.aaroncoplan.waterfall;

import com.aaroncoplan.waterfall.WaterfallLexer;
import com.aaroncoplan.waterfall.WaterfallParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.print.DocFlavor;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;

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
      System.err.format(
        "File %s could not be found",
        waterfallCodeFilePath
      ).println();
      System.exit(-1);
    }
    // parse using ANTLR
    final CharStream charStream = CharStreams.fromString(code.toString());
    final WaterfallLexer waterfallLexer = new WaterfallLexer(charStream);
    final CommonTokenStream tokenStream = new CommonTokenStream(waterfallLexer);

    final WaterfallParser waterfallParser = new WaterfallParser(tokenStream);
    waterfallParser.removeErrorListeners();
    SyntaxErrorListener errorListener = new SyntaxErrorListener(
      waterfallCodeFilePath
    );
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
    programAST.code().codeline().forEach(
      clCtx -> {
        WaterfallParser.VariableDeclarationContext variableDeclarationContext = clCtx.variableDeclaration(

        );
        System.out.println(
          variableDeclarationContext.variableType().ID().getText()
        );
        System.out.println(
          variableDeclarationContext.variableName().ID().getText()
        );
        System.out.println(
          variableDeclarationContext.variableValue().INT_LITERAL()
        );
        System.out.println(
          variableDeclarationContext.variableValue().DEC_LITERAL()
        );
        System.out.println();
      }
    );
  }

}

