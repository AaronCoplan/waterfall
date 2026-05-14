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
import com.aaroncoplan.waterfall.compiler.statements.helpers.TranslatableStatement;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The original C-like emitter. Output is byte-identical to what the compiler
 * produced before the backend abstraction landed — see
 * {@code notes/phase-1-outputs.txt} for the captured baseline.
 */
public class LegacyTextBackend implements CodeGenerator {

    @Override
    public String name() {
        return "legacy";
    }

    @Override
    public String emitProgram(ModuleAst module) {
        String declarations = module.topLevelVariables.stream()
                .map(v -> v.translate(this))
                .collect(Collectors.joining("\n"));
        String functions = module.functions.stream()
                .map(f -> f.translate(this))
                .collect(Collectors.joining("\n"));
        // Original Container.generate(): headers + "\n\n" + declarations + "\n\n" + functions,
        // with "\n\n" only when the preceding block is non-empty.
        String declarationSpacing = module.topLevelVariables.isEmpty() ? "" : "\n\n";
        return declarations + declarationSpacing + functions;
    }

    @Override
    public String emitTypedVarDecl(TypedVariableDeclarationAndAssignmentData s) {
        return String.format("%s %s = %s;", s.type, s.name, emitExpression(s.value));
    }

    @Override
    public String emitUntypedVarDecl(UntypedVariableDeclarationAndAssignmentData s) {
        return String.format("%s %s = %s;", s.inferredType, s.name, emitExpression(s.value));
    }

    @Override
    public String emitVarAssignment(VariableAssignmentData s) {
        return String.format("%s = %d;", s.name, s.value);
    }

    @Override
    public String emitFunctionImpl(FunctionImplementationData s) {
        final String returnType = s.returnType == null ? "void" : s.returnType;
        final String args = s.typedArguments.stream()
                .map(arg -> arg.firstVal + " " + arg.secondVal)
                .collect(Collectors.joining(", "));
        final String body = s.statements.stream()
                .map(stmt -> stmt.translate(this))
                .collect(Collectors.joining("\n"));
        return String.format("%s %s(%s) {%s}", returnType, s.name, args, body);
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
    public String emitForBlock(ForBlockData s) {
        return String.format("for (auto %s : %s) {%s}", s.iteratorName, s.collectionName, joinBody(s.body));
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
            case NULL_LITERAL: return "NULL";
            case INT_LITERAL:
            case DEC_LITERAL:
            case IDENTIFIER:
            case STRING_LITERAL:
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
        String args = c.namedArguments.isEmpty()
                ? c.positionalArguments.stream().map(this::emitExpression).collect(Collectors.joining(", "))
                : c.namedArguments.stream()
                    .map(p -> p.firstVal + "=" + emitExpression(p.secondVal))
                    .collect(Collectors.joining(", "));
        switch (c.kind) {
            case LOCAL:
                return String.format("%s(%s)", c.functionName, args);
            case MODULE:
                return String.format("%s_%s(%s)", c.moduleName, c.functionName, args);
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
                .map(p -> p.firstVal + " " + p.secondVal)
                .collect(Collectors.joining(", "));
        String bodyText = l.body == null ? "{}" : emitFunctionCall(l.body);
        return String.format("(%s) ==> %s", argList, bodyText);
    }

    @Override
    public String emitArrayLiteral(ArrayLiteralData a) {
        return "[" + a.elements.stream().map(this::emitExpression).collect(Collectors.joining(", ")) + "]";
    }

    @Override
    public String emitBundleLiteral(BundleLiteralData b) {
        return "|" + b.elements.stream().map(this::emitExpression).collect(Collectors.joining(", ")) + "|";
    }

    private String joinBody(List<TranslatableStatement> body) {
        return body.stream().map(s -> s.translate(this)).collect(Collectors.joining("\n"));
    }
}
