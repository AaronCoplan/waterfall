package com.aaroncoplan.waterfall.compiler.statements.helpers

import com.aaroncoplan.waterfall.generated.WaterfallParser
import com.aaroncoplan.waterfall.compiler.statements.ForBlockData
import com.aaroncoplan.waterfall.compiler.statements.FunctionCallStatementData
import com.aaroncoplan.waterfall.compiler.statements.IfBlockData
import com.aaroncoplan.waterfall.compiler.statements.IncrementStatementData
import com.aaroncoplan.waterfall.compiler.statements.ReturnStatementData
import com.aaroncoplan.waterfall.compiler.statements.TypedVariableDeclarationAndAssignmentData
import com.aaroncoplan.waterfall.compiler.statements.UntypedVariableDeclarationAndAssignmentData
import com.aaroncoplan.waterfall.compiler.statements.VariableAssignmentData
import com.aaroncoplan.waterfall.compiler.statements.WhileBlockData

object StatementDispatcher {

    @JvmStatic
    fun fromStatement(filePath: String, stmt: WaterfallParser.StatementContext): TranslatableStatement {
        stmt.typedVariableDeclarationAndAssignment()?.let { return TypedVariableDeclarationAndAssignmentData(filePath, it) }
        stmt.untypedVariableDeclarationAndAssignment()?.let { return UntypedVariableDeclarationAndAssignmentData(filePath, it) }
        stmt.variableAssignment()?.let { return VariableAssignmentData(filePath, it) }
        stmt.functionCall()?.let { return FunctionCallStatementData(filePath, it) }
        stmt.ifBlock()?.let { return IfBlockData(filePath, it) }
        stmt.forBlock()?.let { return ForBlockData(filePath, it) }
        stmt.whileBlock()?.let { return WhileBlockData(filePath, it) }
        stmt.returnStatement()?.let { return ReturnStatementData(filePath, it) }
        stmt.incrementStatement()?.let { return IncrementStatementData(filePath, it) }
        throw RuntimeException("Unrecognized statement")
    }

    /**
     * Returns the list of inner statements for a function body / block. Accepts either a
     * statementBlock or null (for empty-block functions / blocks).
     */
    @JvmStatic
    fun fromStatementBlock(filePath: String, block: WaterfallParser.StatementBlockContext?): List<TranslatableStatement> {
        if (block == null) return emptyList()
        return block.statement().map { fromStatement(filePath, it) }
    }
}
