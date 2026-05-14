package com.aaroncoplan.waterfall.compiler.target;

import com.aaroncoplan.waterfall.compiler.statements.ArrayLiteralData;
import com.aaroncoplan.waterfall.compiler.statements.BundleLiteralData;
import com.aaroncoplan.waterfall.compiler.statements.ExpressionData;
import com.aaroncoplan.waterfall.compiler.statements.ForBlockData;
import com.aaroncoplan.waterfall.compiler.statements.FunctionCallData;
import com.aaroncoplan.waterfall.compiler.statements.FunctionCallStatementData;
import com.aaroncoplan.waterfall.compiler.statements.FunctionImplementationData;
import com.aaroncoplan.waterfall.compiler.statements.IfBlockData;
import com.aaroncoplan.waterfall.compiler.statements.IncrementStatementData;
import com.aaroncoplan.waterfall.compiler.statements.LambdaFunctionData;
import com.aaroncoplan.waterfall.compiler.statements.ModuleAst;
import com.aaroncoplan.waterfall.compiler.statements.ReturnStatementData;
import com.aaroncoplan.waterfall.compiler.statements.TypedVariableDeclarationAndAssignmentData;
import com.aaroncoplan.waterfall.compiler.statements.UntypedVariableDeclarationAndAssignmentData;
import com.aaroncoplan.waterfall.compiler.statements.VariableAssignmentData;
import com.aaroncoplan.waterfall.compiler.statements.WhileBlockData;
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement;
import com.aaroncoplan.waterfall.compiler.typesystem.PrimitiveTypes;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Emits C99. Forces the type system to be real: each Waterfall primitive maps to a C
 * type ({@code int}→{@code int}, {@code dec}→{@code double}, {@code bool}→{@code bool},
 * {@code char}→{@code char}). Many constructs (lambdas, bundles, for-in over an
 * unknown collection, object method calls) don't have a direct C equivalent and emit
 * a best-guess placeholder with a {@code /* TODO(audit) ... *&#47;} comment — these
 * are the spots that surface real follow-up work.
 */
public class CBackend implements CodeGenerator {

    @Override
    public String name() {
        return "c";
    }

    @Override
    public String emitProgram(ModuleAst module) {
        StringBuilder sb = new StringBuilder();
        sb.append("// module ").append(module.name).append("\n");
        sb.append("#include <stdio.h>\n");
        sb.append("#include <stdbool.h>\n");
        sb.append("#include <string.h>\n\n");
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

    /**
     * Map a Waterfall type name to its C equivalent. Unknown types pass through;
     * gcc -fsyntax-only will reject them.
     */
    private static String cType(String wfType) {
        if (wfType == null) return "void";
        switch (wfType) {
            case PrimitiveTypes.INT:  return "int";
            case PrimitiveTypes.DEC:  return "double";
            case PrimitiveTypes.BOOL: return "bool";
            case PrimitiveTypes.CHAR: return "char";
            default: return wfType;
        }
    }

    @Override
    public String emitTypedVarDecl(TypedVariableDeclarationAndAssignmentData s) {
        String prefix = s.isImmutable() ? "const " : "";
        return String.format("%s%s %s = %s;", prefix, cType(s.type), s.name, emitExpression(s.value));
    }

    @Override
    public String emitUntypedVarDecl(UntypedVariableDeclarationAndAssignmentData s) {
        String prefix = s.isImmutable() ? "const " : "";
        return String.format("%s%s %s = %s;", prefix, cType(s.inferredType), s.name, emitExpression(s.value));
    }

    @Override
    public String emitVarAssignment(VariableAssignmentData s) {
        return String.format("%s %s %s;", s.name, s.op, emitExpression(s.value));
    }

    @Override
    public String emitFunctionImpl(FunctionImplementationData s) {
        final String returnType = cType(s.returnType);
        final String args = s.typedArguments.stream()
                .map(arg -> cType(arg.firstVal) + " " + arg.secondVal)
                .collect(Collectors.joining(", "));
        final String argsOrVoid = args.isEmpty() ? "void" : args;
        if (s.statements.isEmpty()) {
            return String.format("%s %s(%s) {}", returnType, s.name, argsOrVoid);
        }
        String body = s.statements.stream()
                .map(stmt -> "    " + stmt.translate(this))
                .collect(Collectors.joining("\n"));
        return String.format("%s %s(%s) {\n%s\n}", returnType, s.name, argsOrVoid, body);
    }

    @Override
    public String emitIfBlock(IfBlockData s) {
        StringBuilder sb = new StringBuilder();
        sb.append("if (").append(emitExpression(s.ifBranch.condition)).append(") {");
        sb.append(joinBody(s.ifBranch.body));
        sb.append("}");
        for (IfBlockData.Branch elif : s.elifBranches) {
            sb.append(" else if (").append(emitExpression(elif.condition)).append(") {");
            sb.append(joinBody(elif.body));
            sb.append("}");
        }
        if (s.elseBody != null) {
            sb.append(" else {");
            sb.append(joinBody(s.elseBody));
            sb.append("}");
        }
        return sb.toString();
    }

    @Override
    public String emitWhileBlock(WhileBlockData s) {
        return String.format("while (%s) {%s}", emitExpression(s.condition), joinBody(s.body));
    }

    @Override
    public String emitForBlock(ForBlockData s) {
        // TODO(audit): C has no for-in. Emitting a zero-iteration loop that still declares
        // the iterator so the body's reference type-checks. Real lowering needs collection
        // semantics (pointer + length, iterator protocol, etc.).
        return String.format(
                "for (int %s = 0; %s < 0; %s++) /* TODO(audit): for-in over %s */ {%s}",
                s.iteratorName, s.iteratorName, s.iteratorName, s.collectionName, joinBody(s.body));
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
    public String emitIncrementStatement(IncrementStatementData s) {
        return s.name + s.op + ";";
    }

    @Override
    public String emitExpression(ExpressionData e) {
        switch (e.kind) {
            case NULL_LITERAL: return "NULL";
            case BOOL_LITERAL:
                // <stdbool.h> is already in the always-included set today; phase 8c will
                // switch this to a demand-driven add.
                return e.literalText;
            case INT_LITERAL:
            case DEC_LITERAL:
            case IDENTIFIER:
                return e.literalText;
            case STRING_LITERAL:
                // Source: `text` (backticks). C wants double quotes. No escape processing.
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
            case ARRAY_INDEX:
                return e.arrayIndex.target + "[" + emitExpression(e.arrayIndex.index) + "]";
            case CAST:
                return "((" + cType(e.castTargetType) + ")(" + emitExpression(e.castOperand) + "))";
            case BINARY_OP: {
                if ("^".equals(e.op)) {
                    // C: ^ is XOR, not power. README defines ^ as power, so lower to pow().
                    // TODO(audit): also needs `#include <math.h>` and `-lm` at link time.
                    return "pow(" + emitExpression(e.left) + ", " + emitExpression(e.right) + ")";
                }
                String cOp;
                switch (e.op) {
                    case "and": cOp = "&&"; break;
                    case "or":  cOp = "||"; break;
                    case "equals": cOp = "=="; break;
                    default: cOp = e.op; break;  // +, -, *, /, %, <, >, <=, >=
                }
                return "(" + emitExpression(e.left) + " " + cOp + " " + emitExpression(e.right) + ")";
            }
            default: throw new RuntimeException("Unrecognized expression kind " + e.kind);
        }
    }

    @Override
    public String emitFunctionCall(FunctionCallData c) {
        String args = c.namedArguments.isEmpty()
                ? c.positionalArguments.stream().map(this::emitExpression).collect(Collectors.joining(", "))
                : c.namedArguments.stream()
                    // TODO(audit): named args dropped; C has no native named-arg support.
                    .map(p -> emitExpression(p.secondVal))
                    .collect(Collectors.joining(", "));
        switch (c.kind) {
            case LOCAL:
                return String.format("%s(%s)", c.functionName, args);
            case MODULE:
                // TODO(audit): Mod::fn -> Mod_fn mangle. Real solution: header-per-module.
                return String.format("%s_%s(%s)", c.moduleName, c.functionName, args);
            case OBJECT:
                // TODO(audit): C has no method dispatch. Best-guess: receiver as first arg.
                String receiver = String.join(".", c.receiverPath);
                String all = args.isEmpty() ? "&" + receiver : "&" + receiver + ", " + args;
                return String.format("%s_%s(%s)", receiver, c.functionName, all);
            default:
                throw new RuntimeException("Unrecognized call kind " + c.kind);
        }
    }

    @Override
    public String emitLambda(LambdaFunctionData l) {
        // TODO(audit): C has no anonymous functions. Emitting a placeholder so the surrounding
        // assignment still type-checks against a generic function-pointer-ish identifier.
        return "/* TODO(audit): lambda */ NULL";
    }

    @Override
    public String emitArrayLiteral(ArrayLiteralData a) {
        // C99 compound literal: emits as `(int[]){a, b, c}` for INT-only arrays.
        // TODO(audit): hardcoded to int element type; real solution needs element-type inference.
        return "(int[]){" + a.elements.stream().map(this::emitExpression).collect(Collectors.joining(", ")) + "}";
    }

    @Override
    public String emitBundleLiteral(BundleLiteralData b) {
        // TODO(audit): bundle semantics undefined; emit a placeholder struct literal.
        return "/* TODO(audit): bundle */ {0}";
    }

    private String joinBody(List<TranslatableStatement> body) {
        return body.stream().map(s -> s.translate(this)).collect(Collectors.joining("\n"));
    }
}
