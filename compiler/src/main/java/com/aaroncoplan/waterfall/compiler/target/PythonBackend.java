package com.aaroncoplan.waterfall.compiler.target;

import com.aaroncoplan.waterfall.compiler.statements.ArrayLiteralData;
import com.aaroncoplan.waterfall.compiler.statements.BundleLiteralData;
import com.aaroncoplan.waterfall.compiler.statements.ExpressionData;
import com.aaroncoplan.waterfall.compiler.statements.ForBlockData;
import com.aaroncoplan.waterfall.compiler.statements.FunctionCallData;
import com.aaroncoplan.waterfall.compiler.statements.FunctionCallStatementData;
import com.aaroncoplan.waterfall.compiler.statements.FunctionImplementationData;
import com.aaroncoplan.waterfall.compiler.statements.IfBlockData;
import com.aaroncoplan.waterfall.compiler.statements.LambdaFunctionData;
import com.aaroncoplan.waterfall.compiler.statements.ModuleAst;
import com.aaroncoplan.waterfall.compiler.statements.TypedVariableDeclarationAndAssignmentData;
import com.aaroncoplan.waterfall.compiler.statements.UntypedVariableDeclarationAndAssignmentData;
import com.aaroncoplan.waterfall.compiler.statements.VariableAssignmentData;
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Emits Python 3. Types are dropped (Python is dynamic). Compounds use
 * indentation rather than braces, so each emit* method returns its node at
 * "indent level 0" and the {@link #indent(String, int)} helper adds leading
 * whitespace when placing it inside a parent block.
 */
public class PythonBackend implements CodeGenerator {

    private static final String INDENT_UNIT = "    ";

    @Override
    public String name() {
        return "python";
    }

    @Override
    public String emitProgram(ModuleAst module) {
        StringBuilder sb = new StringBuilder();
        sb.append("# module ").append(module.name).append("\n");
        for (TypedVariableDeclarationAndAssignmentData v : module.topLevelVariables) {
            sb.append(emitTypedVarDecl(v)).append("\n");
        }
        if (!module.topLevelVariables.isEmpty() && !module.functions.isEmpty()) {
            sb.append("\n");
        }
        for (int i = 0; i < module.functions.size(); i++) {
            sb.append(emitFunctionImpl(module.functions.get(i)));
            if (i < module.functions.size() - 1) sb.append("\n\n\n");
            else sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public String emitTypedVarDecl(TypedVariableDeclarationAndAssignmentData s) {
        // TODO(audit): const/imm not enforced at runtime by Python; emitted as a plain assignment.
        return String.format("%s = %s", s.name, emitExpression(s.value));
    }

    @Override
    public String emitUntypedVarDecl(UntypedVariableDeclarationAndAssignmentData s) {
        return String.format("%s = %s", s.name, emitExpression(s.value));
    }

    @Override
    public String emitVarAssignment(VariableAssignmentData s) {
        return String.format("%s = %d", s.name, s.value);
    }

    @Override
    public String emitFunctionImpl(FunctionImplementationData s) {
        String args = s.typedArguments.stream()
                .map(p -> p.secondVal)
                .collect(Collectors.joining(", "));
        String header = String.format("def %s(%s):", s.name, args);
        if (s.statements.isEmpty()) {
            return header + "\n" + INDENT_UNIT + "pass";
        }
        return header + "\n" + indent(joinStatements(s.statements), 1);
    }

    @Override
    public String emitIfBlock(IfBlockData s) {
        StringBuilder sb = new StringBuilder();
        sb.append("if ").append(emitExpression(s.ifBranch.condition)).append(":\n");
        sb.append(indent(bodyOrPass(s.ifBranch.body), 1));
        for (IfBlockData.Branch elif : s.elifBranches) {
            sb.append("\n").append("elif ").append(emitExpression(elif.condition)).append(":\n");
            sb.append(indent(bodyOrPass(elif.body), 1));
        }
        if (s.elseBody != null) {
            sb.append("\n").append("else:\n");
            sb.append(indent(bodyOrPass(s.elseBody), 1));
        }
        return sb.toString();
    }

    @Override
    public String emitForBlock(ForBlockData s) {
        return String.format("for %s in %s:\n%s",
                s.iteratorName, s.collectionName,
                indent(bodyOrPass(s.body), 1));
    }

    @Override
    public String emitFunctionCallStatement(FunctionCallStatementData s) {
        return emitFunctionCall(s.call);
    }

    @Override
    public String emitExpression(ExpressionData e) {
        switch (e.kind) {
            case NULL_LITERAL: return "None";
            case INT_LITERAL:
            case DEC_LITERAL:
            case IDENTIFIER:
                return e.literalText;
            case STRING_LITERAL:
                // Source: `...` backtick-delimited. Python uses single/double quotes.
                // TODO(audit): only strips the backticks; doesn't escape embedded " or
                // resolve \\ escapes. None of the current examples exercise strings.
                if (e.literalText.length() >= 2
                        && e.literalText.charAt(0) == '`'
                        && e.literalText.charAt(e.literalText.length() - 1) == '`') {
                    return "\"" + e.literalText.substring(1, e.literalText.length() - 1) + "\"";
                }
                return e.literalText;
            case LAMBDA: return emitLambda(e.lambda);
            case BUNDLE: return emitBundleLiteral(e.bundle);
            case ARRAY: return emitArrayLiteral(e.array);
            case FUNCTION_CALL: return emitFunctionCall(e.functionCall);
            default: throw new RuntimeException("Unrecognized expression kind " + e.kind);
        }
    }

    @Override
    public String emitFunctionCall(FunctionCallData c) {
        String args;
        if (!c.namedArguments.isEmpty()) {
            args = c.namedArguments.stream()
                    .map(p -> p.firstVal + "=" + emitExpression(p.secondVal))
                    .collect(Collectors.joining(", "));
        } else {
            args = c.positionalArguments.stream()
                    .map(this::emitExpression)
                    .collect(Collectors.joining(", "));
        }
        switch (c.kind) {
            case LOCAL:
                return String.format("%s(%s)", c.functionName, args);
            case MODULE:
                return String.format("%s.%s(%s)", c.moduleName, c.functionName, args);
            case OBJECT:
                String receiver = String.join(".", c.receiverPath);
                return String.format("%s.%s(%s)", receiver, c.functionName, args);
            default:
                throw new RuntimeException("Unrecognized call kind " + c.kind);
        }
    }

    @Override
    public String emitLambda(LambdaFunctionData l) {
        String argList = l.typedArguments.stream()
                .map(p -> p.secondVal)
                .collect(Collectors.joining(", "));
        if (l.body == null) {
            // Empty lambda body. Python can't have a statement-less lambda; use None.
            return String.format("(lambda %s: None)", argList);
        }
        return String.format("(lambda %s: %s)", argList, emitFunctionCall(l.body));
    }

    @Override
    public String emitArrayLiteral(ArrayLiteralData a) {
        return "[" + a.elements.stream().map(this::emitExpression).collect(Collectors.joining(", ")) + "]";
    }

    @Override
    public String emitBundleLiteral(BundleLiteralData b) {
        // TODO(audit): bundle semantics aren't defined yet; emitting as a Python list.
        return "[" + b.elements.stream().map(this::emitExpression).collect(Collectors.joining(", ")) + "]";
    }

    private String joinStatements(List<TranslatableStatement> body) {
        return body.stream().map(s -> s.translate(this)).collect(Collectors.joining("\n"));
    }

    private String bodyOrPass(List<TranslatableStatement> body) {
        return body.isEmpty() ? "pass" : joinStatements(body);
    }

    private static String indent(String text, int level) {
        if (level <= 0) return text;
        StringBuilder pad = new StringBuilder();
        for (int i = 0; i < level; i++) pad.append(INDENT_UNIT);
        String prefix = pad.toString();
        return Arrays.stream(text.split("\n", -1))
                .map(line -> line.isEmpty() ? line : prefix + line)
                .collect(Collectors.joining("\n"));
    }
}
