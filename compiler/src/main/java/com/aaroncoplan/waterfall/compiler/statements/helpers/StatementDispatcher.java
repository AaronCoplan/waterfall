package com.aaroncoplan.waterfall.compiler.statements.helpers;

import com.aaroncoplan.waterfall.generated.WaterfallParser;
import com.aaroncoplan.waterfall.compiler.statements.ForBlockData;
import com.aaroncoplan.waterfall.compiler.statements.FunctionCallStatementData;
import com.aaroncoplan.waterfall.compiler.statements.IfBlockData;
import com.aaroncoplan.waterfall.compiler.statements.ReturnStatementData;
import com.aaroncoplan.waterfall.compiler.statements.TypedVariableDeclarationAndAssignmentData;
import com.aaroncoplan.waterfall.compiler.statements.UntypedVariableDeclarationAndAssignmentData;
import com.aaroncoplan.waterfall.compiler.statements.VariableAssignmentData;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class StatementDispatcher {

    private StatementDispatcher() {}

    public static TranslatableStatement fromStatement(String filePath, WaterfallParser.StatementContext stmt) {
        if (stmt.typedVariableDeclarationAndAssignment() != null) {
            return new TypedVariableDeclarationAndAssignmentData(filePath, stmt.typedVariableDeclarationAndAssignment());
        }
        if (stmt.untypedVariableDeclarationAndAssignment() != null) {
            return new UntypedVariableDeclarationAndAssignmentData(filePath, stmt.untypedVariableDeclarationAndAssignment());
        }
        if (stmt.variableAssignment() != null) {
            return new VariableAssignmentData(filePath, stmt.variableAssignment());
        }
        if (stmt.functionCall() != null) {
            return new FunctionCallStatementData(filePath, stmt.functionCall());
        }
        if (stmt.ifBlock() != null) {
            return new IfBlockData(filePath, stmt.ifBlock());
        }
        if (stmt.forBlock() != null) {
            return new ForBlockData(filePath, stmt.forBlock());
        }
        if (stmt.returnStatement() != null) {
            return new ReturnStatementData(filePath, stmt.returnStatement());
        }
        throw new RuntimeException("Unrecognized statement");
    }

    /**
     * Returns the list of inner statements for a function body / block. Accepts either a
     * statementBlock or null (for empty-block functions / blocks).
     */
    public static List<TranslatableStatement> fromStatementBlock(
            String filePath, WaterfallParser.StatementBlockContext block) {
        if (block == null) return Collections.emptyList();
        return block.statement().stream()
                .map(s -> fromStatement(filePath, s))
                .collect(Collectors.toList());
    }
}
