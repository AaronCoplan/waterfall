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
import com.aaroncoplan.waterfall.compiler.statements.ReturnStatementData;
import com.aaroncoplan.waterfall.compiler.statements.TypedVariableDeclarationAndAssignmentData;
import com.aaroncoplan.waterfall.compiler.statements.UntypedVariableDeclarationAndAssignmentData;
import com.aaroncoplan.waterfall.compiler.statements.VariableAssignmentData;
import com.aaroncoplan.waterfall.compiler.statements.WhileBlockData;
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Emits JavaScript. Types are dropped; const/imm modifiers map to `const`,
 * everything else to `let`. for-in -> for...of, lambdas -> arrow functions.
 * Bundle literals are best-guessed to arrays (TODO(audit) flagged) and
 * Module::fn(x) -> Module.fn(x).
 */
public class JavaScriptBackend implements CodeGenerator {

    private final String indentUnit = "    ";

    @Override
    public String name() {
        return "js";
    }

    @Override
    public String emitProgram(ModuleAst module) {
        StringBuilder sb = new StringBuilder();
        sb.append("// module ").append(module.name).append("\n");
        for (TypedVariableDeclarationAndAssignmentData v : module.topLevelVariables) {
            sb.append(emitTypedVarDecl(v)).append("\n");
        }
        if (!module.topLevelVariables.isEmpty() && !module.functions.isEmpty()) {
            sb.append("\n");
        }
        for (int i = 0; i < module.functions.size(); i++) {
            sb.append(emitFunctionImpl(module.functions.get(i)));
            if (i < module.functions.size() - 1) sb.append("\n\n");
            else sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public String emitTypedVarDecl(TypedVariableDeclarationAndAssignmentData s) {
        String keyword = s.isImmutable() ? "const" : "let";
        return String.format("%s %s = %s;", keyword, s.name, emitExpression(s.value));
    }

    @Override
    public String emitUntypedVarDecl(UntypedVariableDeclarationAndAssignmentData s) {
        String keyword = s.isImmutable() ? "const" : "let";
        return String.format("%s %s = %s;", keyword, s.name, emitExpression(s.value));
    }

    @Override
    public String emitVarAssignment(VariableAssignmentData s) {
        return String.format("%s = %d;", s.name, s.value);
    }

    @Override
    public String emitFunctionImpl(FunctionImplementationData s) {
        final String args = s.typedArguments.stream()
                .map(arg -> arg.secondVal)
                .collect(Collectors.joining(", "));
        if (s.statements.isEmpty()) {
            return String.format("function %s(%s) {}", s.name, args);
        }
        String body = indentBlock(s.statements);
        return String.format("function %s(%s) {\n%s\n}", s.name, args, body);
    }

    @Override
    public String emitIfBlock(IfBlockData s) {
        StringBuilder sb = new StringBuilder();
        sb.append("if (").append(emitExpression(s.ifBranch.condition)).append(") {");
        sb.append(blockBody(s.ifBranch.body));
        sb.append("}");
        for (IfBlockData.Branch elif : s.elifBranches) {
            sb.append(" else if (").append(emitExpression(elif.condition)).append(") {");
            sb.append(blockBody(elif.body));
            sb.append("}");
        }
        if (s.elseBody != null) {
            sb.append(" else {");
            sb.append(blockBody(s.elseBody));
            sb.append("}");
        }
        return sb.toString();
    }

    @Override
    public String emitForBlock(ForBlockData s) {
        return String.format("for (const %s of %s) {%s}",
                s.iteratorName, s.collectionName, blockBody(s.body));
    }

    @Override
    public String emitWhileBlock(WhileBlockData s) {
        return String.format("while (%s) {%s}", emitExpression(s.condition), blockBody(s.body));
    }

    @Override
    public String emitFunctionCallStatement(FunctionCallStatementData s) {
        return emitFunctionCall(s.call) + ";";
    }

    @Override
    public String emitReturnStatement(ReturnStatementData s) {
        return s.value == null ? "return;" : "return " + emitExpression(s.value) + ";";
    }

    @Override
    public String emitExpression(ExpressionData e) {
        switch (e.kind) {
            case NULL_LITERAL: return "null";
            case INT_LITERAL:
            case DEC_LITERAL:
            case IDENTIFIER:
                return e.literalText;
            case STRING_LITERAL:
                // Source string literals are backtick-delimited; JS template literals also
                // use backticks. Source text already includes them. TODO(audit): if a
                // literal contains ${ it needs escaping — none of the current examples do.
                return e.literalText;
            case LAMBDA: return emitLambda(e.lambda);
            case BUNDLE: return emitBundleLiteral(e.bundle);
            case ARRAY: return emitArrayLiteral(e.array);
            case FUNCTION_CALL: return emitFunctionCall(e.functionCall);
            case BINARY_OP: {
                String jsOp;
                switch (e.op) {
                    case "and": jsOp = "&&"; break;
                    case "or":  jsOp = "||"; break;
                    case "equals": jsOp = "==="; break;
                    default: throw new RuntimeException("Unrecognized binary op " + e.op);
                }
                return "(" + emitExpression(e.left) + " " + jsOp + " " + emitExpression(e.right) + ")";
            }
            default: throw new RuntimeException("Unrecognized expression kind " + e.kind);
        }
    }

    @Override
    public String emitFunctionCall(FunctionCallData c) {
        String args;
        if (!c.namedArguments.isEmpty()) {
            // TODO(audit): named args -> single object literal. The callee must be written
            // to destructure { name: value, ... } as its first parameter.
            args = "{" + c.namedArguments.stream()
                    .map(p -> p.firstVal + ": " + emitExpression(p.secondVal))
                    .collect(Collectors.joining(", ")) + "}";
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
        String bodyText = l.body == null ? "{}" : emitFunctionCall(l.body);
        return String.format("(%s) => %s", argList, bodyText);
    }

    @Override
    public String emitArrayLiteral(ArrayLiteralData a) {
        return "[" + a.elements.stream().map(this::emitExpression).collect(Collectors.joining(", ")) + "]";
    }

    @Override
    public String emitBundleLiteral(BundleLiteralData b) {
        // TODO(audit): bundle semantics aren't defined yet; emitting as a JS array.
        return "[" + b.elements.stream().map(this::emitExpression).collect(Collectors.joining(", ")) + "]";
    }

    private String blockBody(List<TranslatableStatement> body) {
        return body.stream().map(s -> s.translate(this)).collect(Collectors.joining("\n"));
    }

    private String indentBlock(List<TranslatableStatement> body) {
        return body.stream()
                .map(s -> s.translate(this))
                .map(s -> indentUnit + s)
                .collect(Collectors.joining("\n"));
    }
}
