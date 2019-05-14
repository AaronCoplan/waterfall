// Generated from com/aaroncoplan/waterfall/Waterfall.g4 by ANTLR 4.7.1
package com.aaroncoplan.waterfall;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link WaterfallParser}.
 */
public interface WaterfallListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(WaterfallParser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(WaterfallParser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#module}.
	 * @param ctx the parse tree
	 */
	void enterModule(WaterfallParser.ModuleContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#module}.
	 * @param ctx the parse tree
	 */
	void exitModule(WaterfallParser.ModuleContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(WaterfallParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(WaterfallParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#spec}.
	 * @param ctx the parse tree
	 */
	void enterSpec(WaterfallParser.SpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#spec}.
	 * @param ctx the parse tree
	 */
	void exitSpec(WaterfallParser.SpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#variable_declaration}.
	 * @param ctx the parse tree
	 */
	void enterVariable_declaration(WaterfallParser.Variable_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#variable_declaration}.
	 * @param ctx the parse tree
	 */
	void exitVariable_declaration(WaterfallParser.Variable_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#typed_variable_declaration_and_assignment}.
	 * @param ctx the parse tree
	 */
	void enterTyped_variable_declaration_and_assignment(WaterfallParser.Typed_variable_declaration_and_assignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#typed_variable_declaration_and_assignment}.
	 * @param ctx the parse tree
	 */
	void exitTyped_variable_declaration_and_assignment(WaterfallParser.Typed_variable_declaration_and_assignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#inferred_variable_declaration_and_assignment}.
	 * @param ctx the parse tree
	 */
	void enterInferred_variable_declaration_and_assignment(WaterfallParser.Inferred_variable_declaration_and_assignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#inferred_variable_declaration_and_assignment}.
	 * @param ctx the parse tree
	 */
	void exitInferred_variable_declaration_and_assignment(WaterfallParser.Inferred_variable_declaration_and_assignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#function_signature}.
	 * @param ctx the parse tree
	 */
	void enterFunction_signature(WaterfallParser.Function_signatureContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#function_signature}.
	 * @param ctx the parse tree
	 */
	void exitFunction_signature(WaterfallParser.Function_signatureContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#function_declaration}.
	 * @param ctx the parse tree
	 */
	void enterFunction_declaration(WaterfallParser.Function_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#function_declaration}.
	 * @param ctx the parse tree
	 */
	void exitFunction_declaration(WaterfallParser.Function_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#return_type}.
	 * @param ctx the parse tree
	 */
	void enterReturn_type(WaterfallParser.Return_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#return_type}.
	 * @param ctx the parse tree
	 */
	void exitReturn_type(WaterfallParser.Return_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#code_block}.
	 * @param ctx the parse tree
	 */
	void enterCode_block(WaterfallParser.Code_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#code_block}.
	 * @param ctx the parse tree
	 */
	void exitCode_block(WaterfallParser.Code_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#block_child}.
	 * @param ctx the parse tree
	 */
	void enterBlock_child(WaterfallParser.Block_childContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#block_child}.
	 * @param ctx the parse tree
	 */
	void exitBlock_child(WaterfallParser.Block_childContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#return_statement}.
	 * @param ctx the parse tree
	 */
	void enterReturn_statement(WaterfallParser.Return_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#return_statement}.
	 * @param ctx the parse tree
	 */
	void exitReturn_statement(WaterfallParser.Return_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#variable_assignment}.
	 * @param ctx the parse tree
	 */
	void enterVariable_assignment(WaterfallParser.Variable_assignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#variable_assignment}.
	 * @param ctx the parse tree
	 */
	void exitVariable_assignment(WaterfallParser.Variable_assignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#conditional}.
	 * @param ctx the parse tree
	 */
	void enterConditional(WaterfallParser.ConditionalContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#conditional}.
	 * @param ctx the parse tree
	 */
	void exitConditional(WaterfallParser.ConditionalContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#if_statement}.
	 * @param ctx the parse tree
	 */
	void enterIf_statement(WaterfallParser.If_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#if_statement}.
	 * @param ctx the parse tree
	 */
	void exitIf_statement(WaterfallParser.If_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#elif_statement}.
	 * @param ctx the parse tree
	 */
	void enterElif_statement(WaterfallParser.Elif_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#elif_statement}.
	 * @param ctx the parse tree
	 */
	void exitElif_statement(WaterfallParser.Elif_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#else_statement}.
	 * @param ctx the parse tree
	 */
	void enterElse_statement(WaterfallParser.Else_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#else_statement}.
	 * @param ctx the parse tree
	 */
	void exitElse_statement(WaterfallParser.Else_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#function_call_positional_args}.
	 * @param ctx the parse tree
	 */
	void enterFunction_call_positional_args(WaterfallParser.Function_call_positional_argsContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#function_call_positional_args}.
	 * @param ctx the parse tree
	 */
	void exitFunction_call_positional_args(WaterfallParser.Function_call_positional_argsContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#named_arg}.
	 * @param ctx the parse tree
	 */
	void enterNamed_arg(WaterfallParser.Named_argContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#named_arg}.
	 * @param ctx the parse tree
	 */
	void exitNamed_arg(WaterfallParser.Named_argContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#function_call_named_args}.
	 * @param ctx the parse tree
	 */
	void enterFunction_call_named_args(WaterfallParser.Function_call_named_argsContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#function_call_named_args}.
	 * @param ctx the parse tree
	 */
	void exitFunction_call_named_args(WaterfallParser.Function_call_named_argsContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#variable_type}.
	 * @param ctx the parse tree
	 */
	void enterVariable_type(WaterfallParser.Variable_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#variable_type}.
	 * @param ctx the parse tree
	 */
	void exitVariable_type(WaterfallParser.Variable_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#modifier}.
	 * @param ctx the parse tree
	 */
	void enterModifier(WaterfallParser.ModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#modifier}.
	 * @param ctx the parse tree
	 */
	void exitModifier(WaterfallParser.ModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#math_operator}.
	 * @param ctx the parse tree
	 */
	void enterMath_operator(WaterfallParser.Math_operatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#math_operator}.
	 * @param ctx the parse tree
	 */
	void exitMath_operator(WaterfallParser.Math_operatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#value}.
	 * @param ctx the parse tree
	 */
	void enterValue(WaterfallParser.ValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#value}.
	 * @param ctx the parse tree
	 */
	void exitValue(WaterfallParser.ValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#assignment_right_hand}.
	 * @param ctx the parse tree
	 */
	void enterAssignment_right_hand(WaterfallParser.Assignment_right_handContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#assignment_right_hand}.
	 * @param ctx the parse tree
	 */
	void exitAssignment_right_hand(WaterfallParser.Assignment_right_handContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#comparator}.
	 * @param ctx the parse tree
	 */
	void enterComparator(WaterfallParser.ComparatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#comparator}.
	 * @param ctx the parse tree
	 */
	void exitComparator(WaterfallParser.ComparatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#comparison}.
	 * @param ctx the parse tree
	 */
	void enterComparison(WaterfallParser.ComparisonContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#comparison}.
	 * @param ctx the parse tree
	 */
	void exitComparison(WaterfallParser.ComparisonContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#condition}.
	 * @param ctx the parse tree
	 */
	void enterCondition(WaterfallParser.ConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#condition}.
	 * @param ctx the parse tree
	 */
	void exitCondition(WaterfallParser.ConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link WaterfallParser#newline_s}.
	 * @param ctx the parse tree
	 */
	void enterNewline_s(WaterfallParser.Newline_sContext ctx);
	/**
	 * Exit a parse tree produced by {@link WaterfallParser#newline_s}.
	 * @param ctx the parse tree
	 */
	void exitNewline_s(WaterfallParser.Newline_sContext ctx);
}