// Generated from com/aaroncoplan/waterfall/Waterfall.g4 by ANTLR 4.7.1
package com.aaroncoplan.waterfall;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class WaterfallParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.7.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		INT=1, DEC=2, CHAR=3, BOOL=4, CONST=5, FINAL=6, AND=7, OR=8, CHECK_EQUAL=9, 
		IF=10, ELSE=11, ELIF=12, MODULE=13, TYPE=14, SPEC=15, FUNC=16, RETURN=17, 
		RETURNS=18, ID=19, INT_LITERAL=20, DEC_LITERAL=21, DOT=22, COLON_EQUALS=23, 
		EQUALS=24, DIVIDE=25, MULTIPLY=26, ADD=27, SUBTRACT=28, MOD=29, POW=30, 
		LESS_THAN=31, GREATER_THAN=32, LESS_THAN_EQUALS=33, GREATER_THAN_EQUALS=34, 
		LEFT_PARENS=35, RIGHT_PARENS=36, LEFT_CURLY=37, RIGHT_CURLY=38, COMMA=39, 
		NEWLINE=40, WS=41;
	public static final int
		RULE_program = 0, RULE_module = 1, RULE_type = 2, RULE_spec = 3, RULE_variable_declaration = 4, 
		RULE_typed_variable_declaration_and_assignment = 5, RULE_inferred_variable_declaration_and_assignment = 6, 
		RULE_function_signature = 7, RULE_function_declaration = 8, RULE_return_type = 9, 
		RULE_code_block = 10, RULE_block_child = 11, RULE_return_statement = 12, 
		RULE_variable_assignment = 13, RULE_conditional = 14, RULE_if_statement = 15, 
		RULE_elif_statement = 16, RULE_else_statement = 17, RULE_function_call_positional_args = 18, 
		RULE_named_arg = 19, RULE_function_call_named_args = 20, RULE_variable_type = 21, 
		RULE_modifier = 22, RULE_math_operator = 23, RULE_value = 24, RULE_assignment_right_hand = 25, 
		RULE_comparator = 26, RULE_comparison = 27, RULE_condition = 28, RULE_newline_s = 29;
	public static final String[] ruleNames = {
		"program", "module", "type", "spec", "variable_declaration", "typed_variable_declaration_and_assignment", 
		"inferred_variable_declaration_and_assignment", "function_signature", 
		"function_declaration", "return_type", "code_block", "block_child", "return_statement", 
		"variable_assignment", "conditional", "if_statement", "elif_statement", 
		"else_statement", "function_call_positional_args", "named_arg", "function_call_named_args", 
		"variable_type", "modifier", "math_operator", "value", "assignment_right_hand", 
		"comparator", "comparison", "condition", "newline_s"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'int'", "'dec'", "'char'", "'bool'", "'const'", "'final'", "'and'", 
		"'or'", "'equals'", "'if'", "'else'", "'elif'", "'module'", "'type'", 
		"'spec'", "'func'", "'return'", "'returns'", null, null, null, "'.'", 
		"':='", "'='", "'/'", "'*'", "'+'", "'-'", "'%'", "'^'", "'<'", "'>'", 
		"'<='", "'>='", "'('", "')'", "'{'", "'}'", "','"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "INT", "DEC", "CHAR", "BOOL", "CONST", "FINAL", "AND", "OR", "CHECK_EQUAL", 
		"IF", "ELSE", "ELIF", "MODULE", "TYPE", "SPEC", "FUNC", "RETURN", "RETURNS", 
		"ID", "INT_LITERAL", "DEC_LITERAL", "DOT", "COLON_EQUALS", "EQUALS", "DIVIDE", 
		"MULTIPLY", "ADD", "SUBTRACT", "MOD", "POW", "LESS_THAN", "GREATER_THAN", 
		"LESS_THAN_EQUALS", "GREATER_THAN_EQUALS", "LEFT_PARENS", "RIGHT_PARENS", 
		"LEFT_CURLY", "RIGHT_CURLY", "COMMA", "NEWLINE", "WS"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "Waterfall.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public WaterfallParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class ProgramContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(WaterfallParser.EOF, 0); }
		public ModuleContext module() {
			return getRuleContext(ModuleContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public SpecContext spec() {
			return getRuleContext(SpecContext.class,0);
		}
		public ProgramContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_program; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterProgram(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitProgram(this);
		}
	}

	public final ProgramContext program() throws RecognitionException {
		ProgramContext _localctx = new ProgramContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_program);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(63);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case MODULE:
				{
				setState(60);
				module();
				}
				break;
			case TYPE:
				{
				setState(61);
				type();
				}
				break;
			case SPEC:
				{
				setState(62);
				spec();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(65);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ModuleContext extends ParserRuleContext {
		public TerminalNode MODULE() { return getToken(WaterfallParser.MODULE, 0); }
		public TerminalNode ID() { return getToken(WaterfallParser.ID, 0); }
		public TerminalNode LEFT_CURLY() { return getToken(WaterfallParser.LEFT_CURLY, 0); }
		public List<Newline_sContext> newline_s() {
			return getRuleContexts(Newline_sContext.class);
		}
		public Newline_sContext newline_s(int i) {
			return getRuleContext(Newline_sContext.class,i);
		}
		public TerminalNode RIGHT_CURLY() { return getToken(WaterfallParser.RIGHT_CURLY, 0); }
		public List<Variable_declarationContext> variable_declaration() {
			return getRuleContexts(Variable_declarationContext.class);
		}
		public Variable_declarationContext variable_declaration(int i) {
			return getRuleContext(Variable_declarationContext.class,i);
		}
		public List<Function_declarationContext> function_declaration() {
			return getRuleContexts(Function_declarationContext.class);
		}
		public Function_declarationContext function_declaration(int i) {
			return getRuleContext(Function_declarationContext.class,i);
		}
		public ModuleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_module; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterModule(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitModule(this);
		}
	}

	public final ModuleContext module() throws RecognitionException {
		ModuleContext _localctx = new ModuleContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_module);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(67);
			match(MODULE);
			setState(68);
			match(ID);
			setState(69);
			match(LEFT_CURLY);
			setState(70);
			newline_s();
			setState(74);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << INT) | (1L << DEC) | (1L << CHAR) | (1L << BOOL) | (1L << CONST) | (1L << FINAL) | (1L << ID))) != 0)) {
				{
				{
				setState(71);
				variable_declaration();
				}
				}
				setState(76);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(80);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FUNC) {
				{
				{
				setState(77);
				function_declaration();
				}
				}
				setState(82);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(83);
			match(RIGHT_CURLY);
			setState(84);
			newline_s();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeContext extends ParserRuleContext {
		public TerminalNode TYPE() { return getToken(WaterfallParser.TYPE, 0); }
		public TerminalNode ID() { return getToken(WaterfallParser.ID, 0); }
		public TerminalNode LEFT_CURLY() { return getToken(WaterfallParser.LEFT_CURLY, 0); }
		public List<Newline_sContext> newline_s() {
			return getRuleContexts(Newline_sContext.class);
		}
		public Newline_sContext newline_s(int i) {
			return getRuleContext(Newline_sContext.class,i);
		}
		public TerminalNode RIGHT_CURLY() { return getToken(WaterfallParser.RIGHT_CURLY, 0); }
		public TypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitType(this);
		}
	}

	public final TypeContext type() throws RecognitionException {
		TypeContext _localctx = new TypeContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_type);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(86);
			match(TYPE);
			setState(87);
			match(ID);
			setState(88);
			match(LEFT_CURLY);
			setState(89);
			newline_s();
			setState(90);
			match(RIGHT_CURLY);
			setState(91);
			newline_s();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SpecContext extends ParserRuleContext {
		public TerminalNode SPEC() { return getToken(WaterfallParser.SPEC, 0); }
		public TerminalNode ID() { return getToken(WaterfallParser.ID, 0); }
		public TerminalNode LEFT_CURLY() { return getToken(WaterfallParser.LEFT_CURLY, 0); }
		public List<Newline_sContext> newline_s() {
			return getRuleContexts(Newline_sContext.class);
		}
		public Newline_sContext newline_s(int i) {
			return getRuleContext(Newline_sContext.class,i);
		}
		public TerminalNode RIGHT_CURLY() { return getToken(WaterfallParser.RIGHT_CURLY, 0); }
		public List<Function_signatureContext> function_signature() {
			return getRuleContexts(Function_signatureContext.class);
		}
		public Function_signatureContext function_signature(int i) {
			return getRuleContext(Function_signatureContext.class,i);
		}
		public SpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_spec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterSpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitSpec(this);
		}
	}

	public final SpecContext spec() throws RecognitionException {
		SpecContext _localctx = new SpecContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_spec);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(93);
			match(SPEC);
			setState(94);
			match(ID);
			setState(95);
			match(LEFT_CURLY);
			setState(96);
			newline_s();
			setState(100);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FUNC) {
				{
				{
				setState(97);
				function_signature();
				}
				}
				setState(102);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(103);
			match(RIGHT_CURLY);
			setState(104);
			newline_s();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Variable_declarationContext extends ParserRuleContext {
		public Typed_variable_declaration_and_assignmentContext typed_variable_declaration_and_assignment() {
			return getRuleContext(Typed_variable_declaration_and_assignmentContext.class,0);
		}
		public Inferred_variable_declaration_and_assignmentContext inferred_variable_declaration_and_assignment() {
			return getRuleContext(Inferred_variable_declaration_and_assignmentContext.class,0);
		}
		public Variable_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variable_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterVariable_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitVariable_declaration(this);
		}
	}

	public final Variable_declarationContext variable_declaration() throws RecognitionException {
		Variable_declarationContext _localctx = new Variable_declarationContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_variable_declaration);
		try {
			setState(108);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,4,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(106);
				typed_variable_declaration_and_assignment();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(107);
				inferred_variable_declaration_and_assignment();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Typed_variable_declaration_and_assignmentContext extends ParserRuleContext {
		public Variable_typeContext variable_type() {
			return getRuleContext(Variable_typeContext.class,0);
		}
		public TerminalNode ID() { return getToken(WaterfallParser.ID, 0); }
		public TerminalNode EQUALS() { return getToken(WaterfallParser.EQUALS, 0); }
		public Assignment_right_handContext assignment_right_hand() {
			return getRuleContext(Assignment_right_handContext.class,0);
		}
		public Newline_sContext newline_s() {
			return getRuleContext(Newline_sContext.class,0);
		}
		public ModifierContext modifier() {
			return getRuleContext(ModifierContext.class,0);
		}
		public Typed_variable_declaration_and_assignmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typed_variable_declaration_and_assignment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterTyped_variable_declaration_and_assignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitTyped_variable_declaration_and_assignment(this);
		}
	}

	public final Typed_variable_declaration_and_assignmentContext typed_variable_declaration_and_assignment() throws RecognitionException {
		Typed_variable_declaration_and_assignmentContext _localctx = new Typed_variable_declaration_and_assignmentContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_typed_variable_declaration_and_assignment);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(111);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==CONST || _la==FINAL) {
				{
				setState(110);
				modifier();
				}
			}

			setState(113);
			variable_type();
			setState(114);
			match(ID);
			setState(115);
			match(EQUALS);
			setState(116);
			assignment_right_hand();
			setState(117);
			newline_s();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Inferred_variable_declaration_and_assignmentContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(WaterfallParser.ID, 0); }
		public TerminalNode COLON_EQUALS() { return getToken(WaterfallParser.COLON_EQUALS, 0); }
		public Assignment_right_handContext assignment_right_hand() {
			return getRuleContext(Assignment_right_handContext.class,0);
		}
		public Newline_sContext newline_s() {
			return getRuleContext(Newline_sContext.class,0);
		}
		public ModifierContext modifier() {
			return getRuleContext(ModifierContext.class,0);
		}
		public Inferred_variable_declaration_and_assignmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inferred_variable_declaration_and_assignment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterInferred_variable_declaration_and_assignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitInferred_variable_declaration_and_assignment(this);
		}
	}

	public final Inferred_variable_declaration_and_assignmentContext inferred_variable_declaration_and_assignment() throws RecognitionException {
		Inferred_variable_declaration_and_assignmentContext _localctx = new Inferred_variable_declaration_and_assignmentContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_inferred_variable_declaration_and_assignment);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(120);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==CONST || _la==FINAL) {
				{
				setState(119);
				modifier();
				}
			}

			setState(122);
			match(ID);
			setState(123);
			match(COLON_EQUALS);
			setState(124);
			assignment_right_hand();
			setState(125);
			newline_s();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Function_signatureContext extends ParserRuleContext {
		public TerminalNode FUNC() { return getToken(WaterfallParser.FUNC, 0); }
		public List<TerminalNode> ID() { return getTokens(WaterfallParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(WaterfallParser.ID, i);
		}
		public TerminalNode LEFT_PARENS() { return getToken(WaterfallParser.LEFT_PARENS, 0); }
		public TerminalNode RIGHT_PARENS() { return getToken(WaterfallParser.RIGHT_PARENS, 0); }
		public Newline_sContext newline_s() {
			return getRuleContext(Newline_sContext.class,0);
		}
		public List<Variable_typeContext> variable_type() {
			return getRuleContexts(Variable_typeContext.class);
		}
		public Variable_typeContext variable_type(int i) {
			return getRuleContext(Variable_typeContext.class,i);
		}
		public Return_typeContext return_type() {
			return getRuleContext(Return_typeContext.class,0);
		}
		public List<TerminalNode> COMMA() { return getTokens(WaterfallParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(WaterfallParser.COMMA, i);
		}
		public Function_signatureContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_function_signature; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterFunction_signature(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitFunction_signature(this);
		}
	}

	public final Function_signatureContext function_signature() throws RecognitionException {
		Function_signatureContext _localctx = new Function_signatureContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_function_signature);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(127);
			match(FUNC);
			setState(128);
			match(ID);
			setState(129);
			match(LEFT_PARENS);
			setState(141);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << INT) | (1L << DEC) | (1L << CHAR) | (1L << BOOL) | (1L << ID))) != 0)) {
				{
				setState(130);
				variable_type();
				setState(131);
				match(ID);
				setState(138);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(132);
					match(COMMA);
					setState(133);
					variable_type();
					setState(134);
					match(ID);
					}
					}
					setState(140);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(143);
			match(RIGHT_PARENS);
			setState(145);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==RETURNS) {
				{
				setState(144);
				return_type();
				}
			}

			setState(147);
			newline_s();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Function_declarationContext extends ParserRuleContext {
		public TerminalNode FUNC() { return getToken(WaterfallParser.FUNC, 0); }
		public List<TerminalNode> ID() { return getTokens(WaterfallParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(WaterfallParser.ID, i);
		}
		public TerminalNode LEFT_PARENS() { return getToken(WaterfallParser.LEFT_PARENS, 0); }
		public TerminalNode RIGHT_PARENS() { return getToken(WaterfallParser.RIGHT_PARENS, 0); }
		public TerminalNode LEFT_CURLY() { return getToken(WaterfallParser.LEFT_CURLY, 0); }
		public List<Newline_sContext> newline_s() {
			return getRuleContexts(Newline_sContext.class);
		}
		public Newline_sContext newline_s(int i) {
			return getRuleContext(Newline_sContext.class,i);
		}
		public Code_blockContext code_block() {
			return getRuleContext(Code_blockContext.class,0);
		}
		public TerminalNode RIGHT_CURLY() { return getToken(WaterfallParser.RIGHT_CURLY, 0); }
		public List<Variable_typeContext> variable_type() {
			return getRuleContexts(Variable_typeContext.class);
		}
		public Variable_typeContext variable_type(int i) {
			return getRuleContext(Variable_typeContext.class,i);
		}
		public Return_typeContext return_type() {
			return getRuleContext(Return_typeContext.class,0);
		}
		public List<TerminalNode> COMMA() { return getTokens(WaterfallParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(WaterfallParser.COMMA, i);
		}
		public Function_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_function_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterFunction_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitFunction_declaration(this);
		}
	}

	public final Function_declarationContext function_declaration() throws RecognitionException {
		Function_declarationContext _localctx = new Function_declarationContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_function_declaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(149);
			match(FUNC);
			setState(150);
			match(ID);
			setState(151);
			match(LEFT_PARENS);
			setState(163);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << INT) | (1L << DEC) | (1L << CHAR) | (1L << BOOL) | (1L << ID))) != 0)) {
				{
				setState(152);
				variable_type();
				setState(153);
				match(ID);
				setState(160);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(154);
					match(COMMA);
					setState(155);
					variable_type();
					setState(156);
					match(ID);
					}
					}
					setState(162);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(165);
			match(RIGHT_PARENS);
			setState(167);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==RETURNS) {
				{
				setState(166);
				return_type();
				}
			}

			setState(169);
			match(LEFT_CURLY);
			setState(170);
			newline_s();
			setState(171);
			code_block();
			setState(172);
			match(RIGHT_CURLY);
			setState(173);
			newline_s();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Return_typeContext extends ParserRuleContext {
		public TerminalNode RETURNS() { return getToken(WaterfallParser.RETURNS, 0); }
		public Variable_typeContext variable_type() {
			return getRuleContext(Variable_typeContext.class,0);
		}
		public Return_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_return_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterReturn_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitReturn_type(this);
		}
	}

	public final Return_typeContext return_type() throws RecognitionException {
		Return_typeContext _localctx = new Return_typeContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_return_type);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(175);
			match(RETURNS);
			setState(176);
			variable_type();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Code_blockContext extends ParserRuleContext {
		public List<Block_childContext> block_child() {
			return getRuleContexts(Block_childContext.class);
		}
		public Block_childContext block_child(int i) {
			return getRuleContext(Block_childContext.class,i);
		}
		public Code_blockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_code_block; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterCode_block(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitCode_block(this);
		}
	}

	public final Code_blockContext code_block() throws RecognitionException {
		Code_blockContext _localctx = new Code_blockContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_code_block);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(181);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << INT) | (1L << DEC) | (1L << CHAR) | (1L << BOOL) | (1L << CONST) | (1L << FINAL) | (1L << IF) | (1L << RETURN) | (1L << ID))) != 0)) {
				{
				{
				setState(178);
				block_child();
				}
				}
				setState(183);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Block_childContext extends ParserRuleContext {
		public Variable_assignmentContext variable_assignment() {
			return getRuleContext(Variable_assignmentContext.class,0);
		}
		public Function_call_positional_argsContext function_call_positional_args() {
			return getRuleContext(Function_call_positional_argsContext.class,0);
		}
		public Function_call_named_argsContext function_call_named_args() {
			return getRuleContext(Function_call_named_argsContext.class,0);
		}
		public ConditionalContext conditional() {
			return getRuleContext(ConditionalContext.class,0);
		}
		public Variable_declarationContext variable_declaration() {
			return getRuleContext(Variable_declarationContext.class,0);
		}
		public Return_statementContext return_statement() {
			return getRuleContext(Return_statementContext.class,0);
		}
		public Block_childContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_block_child; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterBlock_child(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitBlock_child(this);
		}
	}

	public final Block_childContext block_child() throws RecognitionException {
		Block_childContext _localctx = new Block_childContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_block_child);
		try {
			setState(190);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(184);
				variable_assignment();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(185);
				function_call_positional_args();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(186);
				function_call_named_args();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(187);
				conditional();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(188);
				variable_declaration();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(189);
				return_statement();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Return_statementContext extends ParserRuleContext {
		public TerminalNode RETURN() { return getToken(WaterfallParser.RETURN, 0); }
		public Assignment_right_handContext assignment_right_hand() {
			return getRuleContext(Assignment_right_handContext.class,0);
		}
		public Return_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_return_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterReturn_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitReturn_statement(this);
		}
	}

	public final Return_statementContext return_statement() throws RecognitionException {
		Return_statementContext _localctx = new Return_statementContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_return_statement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(192);
			match(RETURN);
			setState(193);
			assignment_right_hand();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Variable_assignmentContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(WaterfallParser.ID, 0); }
		public TerminalNode EQUALS() { return getToken(WaterfallParser.EQUALS, 0); }
		public Assignment_right_handContext assignment_right_hand() {
			return getRuleContext(Assignment_right_handContext.class,0);
		}
		public Newline_sContext newline_s() {
			return getRuleContext(Newline_sContext.class,0);
		}
		public Variable_assignmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variable_assignment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterVariable_assignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitVariable_assignment(this);
		}
	}

	public final Variable_assignmentContext variable_assignment() throws RecognitionException {
		Variable_assignmentContext _localctx = new Variable_assignmentContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_variable_assignment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(195);
			match(ID);
			setState(196);
			match(EQUALS);
			setState(197);
			assignment_right_hand();
			setState(198);
			newline_s();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ConditionalContext extends ParserRuleContext {
		public If_statementContext if_statement() {
			return getRuleContext(If_statementContext.class,0);
		}
		public List<Elif_statementContext> elif_statement() {
			return getRuleContexts(Elif_statementContext.class);
		}
		public Elif_statementContext elif_statement(int i) {
			return getRuleContext(Elif_statementContext.class,i);
		}
		public Else_statementContext else_statement() {
			return getRuleContext(Else_statementContext.class,0);
		}
		public ConditionalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_conditional; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterConditional(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitConditional(this);
		}
	}

	public final ConditionalContext conditional() throws RecognitionException {
		ConditionalContext _localctx = new ConditionalContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_conditional);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(200);
			if_statement();
			setState(204);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ELIF) {
				{
				{
				setState(201);
				elif_statement();
				}
				}
				setState(206);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(208);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ELSE) {
				{
				setState(207);
				else_statement();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class If_statementContext extends ParserRuleContext {
		public TerminalNode IF() { return getToken(WaterfallParser.IF, 0); }
		public TerminalNode LEFT_PARENS() { return getToken(WaterfallParser.LEFT_PARENS, 0); }
		public ConditionContext condition() {
			return getRuleContext(ConditionContext.class,0);
		}
		public TerminalNode RIGHT_PARENS() { return getToken(WaterfallParser.RIGHT_PARENS, 0); }
		public TerminalNode LEFT_CURLY() { return getToken(WaterfallParser.LEFT_CURLY, 0); }
		public List<Newline_sContext> newline_s() {
			return getRuleContexts(Newline_sContext.class);
		}
		public Newline_sContext newline_s(int i) {
			return getRuleContext(Newline_sContext.class,i);
		}
		public Code_blockContext code_block() {
			return getRuleContext(Code_blockContext.class,0);
		}
		public TerminalNode RIGHT_CURLY() { return getToken(WaterfallParser.RIGHT_CURLY, 0); }
		public If_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_if_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterIf_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitIf_statement(this);
		}
	}

	public final If_statementContext if_statement() throws RecognitionException {
		If_statementContext _localctx = new If_statementContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_if_statement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(210);
			match(IF);
			setState(211);
			match(LEFT_PARENS);
			setState(212);
			condition();
			setState(213);
			match(RIGHT_PARENS);
			setState(214);
			match(LEFT_CURLY);
			setState(215);
			newline_s();
			setState(216);
			code_block();
			setState(217);
			match(RIGHT_CURLY);
			setState(218);
			newline_s();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Elif_statementContext extends ParserRuleContext {
		public TerminalNode ELIF() { return getToken(WaterfallParser.ELIF, 0); }
		public TerminalNode LEFT_PARENS() { return getToken(WaterfallParser.LEFT_PARENS, 0); }
		public ConditionContext condition() {
			return getRuleContext(ConditionContext.class,0);
		}
		public TerminalNode RIGHT_PARENS() { return getToken(WaterfallParser.RIGHT_PARENS, 0); }
		public TerminalNode LEFT_CURLY() { return getToken(WaterfallParser.LEFT_CURLY, 0); }
		public TerminalNode RIGHT_CURLY() { return getToken(WaterfallParser.RIGHT_CURLY, 0); }
		public Newline_sContext newline_s() {
			return getRuleContext(Newline_sContext.class,0);
		}
		public Elif_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_elif_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterElif_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitElif_statement(this);
		}
	}

	public final Elif_statementContext elif_statement() throws RecognitionException {
		Elif_statementContext _localctx = new Elif_statementContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_elif_statement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(220);
			match(ELIF);
			setState(221);
			match(LEFT_PARENS);
			setState(222);
			condition();
			setState(223);
			match(RIGHT_PARENS);
			setState(224);
			match(LEFT_CURLY);
			setState(225);
			match(RIGHT_CURLY);
			setState(226);
			newline_s();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Else_statementContext extends ParserRuleContext {
		public TerminalNode ELSE() { return getToken(WaterfallParser.ELSE, 0); }
		public TerminalNode LEFT_CURLY() { return getToken(WaterfallParser.LEFT_CURLY, 0); }
		public TerminalNode RIGHT_CURLY() { return getToken(WaterfallParser.RIGHT_CURLY, 0); }
		public Newline_sContext newline_s() {
			return getRuleContext(Newline_sContext.class,0);
		}
		public Else_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_else_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterElse_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitElse_statement(this);
		}
	}

	public final Else_statementContext else_statement() throws RecognitionException {
		Else_statementContext _localctx = new Else_statementContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_else_statement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(228);
			match(ELSE);
			setState(229);
			match(LEFT_CURLY);
			setState(230);
			match(RIGHT_CURLY);
			setState(231);
			newline_s();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Function_call_positional_argsContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(WaterfallParser.ID, 0); }
		public TerminalNode LEFT_PARENS() { return getToken(WaterfallParser.LEFT_PARENS, 0); }
		public TerminalNode RIGHT_PARENS() { return getToken(WaterfallParser.RIGHT_PARENS, 0); }
		public Newline_sContext newline_s() {
			return getRuleContext(Newline_sContext.class,0);
		}
		public List<Assignment_right_handContext> assignment_right_hand() {
			return getRuleContexts(Assignment_right_handContext.class);
		}
		public Assignment_right_handContext assignment_right_hand(int i) {
			return getRuleContext(Assignment_right_handContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(WaterfallParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(WaterfallParser.COMMA, i);
		}
		public Function_call_positional_argsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_function_call_positional_args; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterFunction_call_positional_args(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitFunction_call_positional_args(this);
		}
	}

	public final Function_call_positional_argsContext function_call_positional_args() throws RecognitionException {
		Function_call_positional_argsContext _localctx = new Function_call_positional_argsContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_function_call_positional_args);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(233);
			match(ID);
			setState(234);
			match(LEFT_PARENS);
			setState(243);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ID) | (1L << INT_LITERAL) | (1L << DEC_LITERAL))) != 0)) {
				{
				setState(235);
				assignment_right_hand();
				setState(240);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(236);
					match(COMMA);
					setState(237);
					assignment_right_hand();
					}
					}
					setState(242);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(245);
			match(RIGHT_PARENS);
			setState(246);
			newline_s();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Named_argContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(WaterfallParser.ID, 0); }
		public TerminalNode EQUALS() { return getToken(WaterfallParser.EQUALS, 0); }
		public Assignment_right_handContext assignment_right_hand() {
			return getRuleContext(Assignment_right_handContext.class,0);
		}
		public Named_argContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_named_arg; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterNamed_arg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitNamed_arg(this);
		}
	}

	public final Named_argContext named_arg() throws RecognitionException {
		Named_argContext _localctx = new Named_argContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_named_arg);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(248);
			match(ID);
			setState(249);
			match(EQUALS);
			setState(250);
			assignment_right_hand();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Function_call_named_argsContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(WaterfallParser.ID, 0); }
		public TerminalNode LEFT_PARENS() { return getToken(WaterfallParser.LEFT_PARENS, 0); }
		public List<Named_argContext> named_arg() {
			return getRuleContexts(Named_argContext.class);
		}
		public Named_argContext named_arg(int i) {
			return getRuleContext(Named_argContext.class,i);
		}
		public TerminalNode RIGHT_PARENS() { return getToken(WaterfallParser.RIGHT_PARENS, 0); }
		public Newline_sContext newline_s() {
			return getRuleContext(Newline_sContext.class,0);
		}
		public List<TerminalNode> COMMA() { return getTokens(WaterfallParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(WaterfallParser.COMMA, i);
		}
		public Function_call_named_argsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_function_call_named_args; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterFunction_call_named_args(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitFunction_call_named_args(this);
		}
	}

	public final Function_call_named_argsContext function_call_named_args() throws RecognitionException {
		Function_call_named_argsContext _localctx = new Function_call_named_argsContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_function_call_named_args);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(252);
			match(ID);
			setState(253);
			match(LEFT_PARENS);
			setState(254);
			named_arg();
			setState(259);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(255);
				match(COMMA);
				setState(256);
				named_arg();
				}
				}
				setState(261);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(262);
			match(RIGHT_PARENS);
			setState(263);
			newline_s();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Variable_typeContext extends ParserRuleContext {
		public TerminalNode INT() { return getToken(WaterfallParser.INT, 0); }
		public TerminalNode DEC() { return getToken(WaterfallParser.DEC, 0); }
		public TerminalNode CHAR() { return getToken(WaterfallParser.CHAR, 0); }
		public TerminalNode BOOL() { return getToken(WaterfallParser.BOOL, 0); }
		public TerminalNode ID() { return getToken(WaterfallParser.ID, 0); }
		public Variable_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variable_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterVariable_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitVariable_type(this);
		}
	}

	public final Variable_typeContext variable_type() throws RecognitionException {
		Variable_typeContext _localctx = new Variable_typeContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_variable_type);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(265);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << INT) | (1L << DEC) | (1L << CHAR) | (1L << BOOL) | (1L << ID))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ModifierContext extends ParserRuleContext {
		public TerminalNode CONST() { return getToken(WaterfallParser.CONST, 0); }
		public TerminalNode FINAL() { return getToken(WaterfallParser.FINAL, 0); }
		public ModifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_modifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterModifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitModifier(this);
		}
	}

	public final ModifierContext modifier() throws RecognitionException {
		ModifierContext _localctx = new ModifierContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_modifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(267);
			_la = _input.LA(1);
			if ( !(_la==CONST || _la==FINAL) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Math_operatorContext extends ParserRuleContext {
		public TerminalNode ADD() { return getToken(WaterfallParser.ADD, 0); }
		public TerminalNode SUBTRACT() { return getToken(WaterfallParser.SUBTRACT, 0); }
		public TerminalNode MULTIPLY() { return getToken(WaterfallParser.MULTIPLY, 0); }
		public TerminalNode DIVIDE() { return getToken(WaterfallParser.DIVIDE, 0); }
		public TerminalNode MOD() { return getToken(WaterfallParser.MOD, 0); }
		public TerminalNode POW() { return getToken(WaterfallParser.POW, 0); }
		public Math_operatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_math_operator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterMath_operator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitMath_operator(this);
		}
	}

	public final Math_operatorContext math_operator() throws RecognitionException {
		Math_operatorContext _localctx = new Math_operatorContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_math_operator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(269);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << DIVIDE) | (1L << MULTIPLY) | (1L << ADD) | (1L << SUBTRACT) | (1L << MOD) | (1L << POW))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ValueContext extends ParserRuleContext {
		public TerminalNode INT_LITERAL() { return getToken(WaterfallParser.INT_LITERAL, 0); }
		public TerminalNode DEC_LITERAL() { return getToken(WaterfallParser.DEC_LITERAL, 0); }
		public TerminalNode ID() { return getToken(WaterfallParser.ID, 0); }
		public ValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_value; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitValue(this);
		}
	}

	public final ValueContext value() throws RecognitionException {
		ValueContext _localctx = new ValueContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_value);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(271);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ID) | (1L << INT_LITERAL) | (1L << DEC_LITERAL))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Assignment_right_handContext extends ParserRuleContext {
		public List<ValueContext> value() {
			return getRuleContexts(ValueContext.class);
		}
		public ValueContext value(int i) {
			return getRuleContext(ValueContext.class,i);
		}
		public List<Math_operatorContext> math_operator() {
			return getRuleContexts(Math_operatorContext.class);
		}
		public Math_operatorContext math_operator(int i) {
			return getRuleContext(Math_operatorContext.class,i);
		}
		public Assignment_right_handContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignment_right_hand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterAssignment_right_hand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitAssignment_right_hand(this);
		}
	}

	public final Assignment_right_handContext assignment_right_hand() throws RecognitionException {
		Assignment_right_handContext _localctx = new Assignment_right_handContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_assignment_right_hand);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(273);
			value();
			setState(279);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << DIVIDE) | (1L << MULTIPLY) | (1L << ADD) | (1L << SUBTRACT) | (1L << MOD) | (1L << POW))) != 0)) {
				{
				{
				setState(274);
				math_operator();
				setState(275);
				value();
				}
				}
				setState(281);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ComparatorContext extends ParserRuleContext {
		public TerminalNode CHECK_EQUAL() { return getToken(WaterfallParser.CHECK_EQUAL, 0); }
		public TerminalNode LESS_THAN() { return getToken(WaterfallParser.LESS_THAN, 0); }
		public TerminalNode GREATER_THAN() { return getToken(WaterfallParser.GREATER_THAN, 0); }
		public TerminalNode LESS_THAN_EQUALS() { return getToken(WaterfallParser.LESS_THAN_EQUALS, 0); }
		public TerminalNode GREATER_THAN_EQUALS() { return getToken(WaterfallParser.GREATER_THAN_EQUALS, 0); }
		public ComparatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comparator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterComparator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitComparator(this);
		}
	}

	public final ComparatorContext comparator() throws RecognitionException {
		ComparatorContext _localctx = new ComparatorContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_comparator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(282);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << CHECK_EQUAL) | (1L << LESS_THAN) | (1L << GREATER_THAN) | (1L << LESS_THAN_EQUALS) | (1L << GREATER_THAN_EQUALS))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ComparisonContext extends ParserRuleContext {
		public List<Assignment_right_handContext> assignment_right_hand() {
			return getRuleContexts(Assignment_right_handContext.class);
		}
		public Assignment_right_handContext assignment_right_hand(int i) {
			return getRuleContext(Assignment_right_handContext.class,i);
		}
		public ComparatorContext comparator() {
			return getRuleContext(ComparatorContext.class,0);
		}
		public ComparisonContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comparison; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterComparison(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitComparison(this);
		}
	}

	public final ComparisonContext comparison() throws RecognitionException {
		ComparisonContext _localctx = new ComparisonContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_comparison);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(284);
			assignment_right_hand();
			setState(285);
			comparator();
			setState(286);
			assignment_right_hand();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ConditionContext extends ParserRuleContext {
		public List<ComparisonContext> comparison() {
			return getRuleContexts(ComparisonContext.class);
		}
		public ComparisonContext comparison(int i) {
			return getRuleContext(ComparisonContext.class,i);
		}
		public List<TerminalNode> AND() { return getTokens(WaterfallParser.AND); }
		public TerminalNode AND(int i) {
			return getToken(WaterfallParser.AND, i);
		}
		public List<TerminalNode> OR() { return getTokens(WaterfallParser.OR); }
		public TerminalNode OR(int i) {
			return getToken(WaterfallParser.OR, i);
		}
		public ConditionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_condition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterCondition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitCondition(this);
		}
	}

	public final ConditionContext condition() throws RecognitionException {
		ConditionContext _localctx = new ConditionContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_condition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(288);
			comparison();
			setState(293);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AND || _la==OR) {
				{
				{
				setState(289);
				_la = _input.LA(1);
				if ( !(_la==AND || _la==OR) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(290);
				comparison();
				}
				}
				setState(295);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Newline_sContext extends ParserRuleContext {
		public List<TerminalNode> NEWLINE() { return getTokens(WaterfallParser.NEWLINE); }
		public TerminalNode NEWLINE(int i) {
			return getToken(WaterfallParser.NEWLINE, i);
		}
		public Newline_sContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_newline_s; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).enterNewline_s(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WaterfallListener ) ((WaterfallListener)listener).exitNewline_s(this);
		}
	}

	public final Newline_sContext newline_s() throws RecognitionException {
		Newline_sContext _localctx = new Newline_sContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_newline_s);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(296);
			match(NEWLINE);
			setState(300);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NEWLINE) {
				{
				{
				setState(297);
				match(NEWLINE);
				}
				}
				setState(302);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3+\u0132\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\3\2\3\2\3"+
		"\2\5\2B\n\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\7\3K\n\3\f\3\16\3N\13\3\3\3\7"+
		"\3Q\n\3\f\3\16\3T\13\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\5\3\5"+
		"\3\5\3\5\3\5\7\5e\n\5\f\5\16\5h\13\5\3\5\3\5\3\5\3\6\3\6\5\6o\n\6\3\7"+
		"\5\7r\n\7\3\7\3\7\3\7\3\7\3\7\3\7\3\b\5\b{\n\b\3\b\3\b\3\b\3\b\3\b\3\t"+
		"\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\7\t\u008b\n\t\f\t\16\t\u008e\13\t\5\t"+
		"\u0090\n\t\3\t\3\t\5\t\u0094\n\t\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3"+
		"\n\3\n\7\n\u00a1\n\n\f\n\16\n\u00a4\13\n\5\n\u00a6\n\n\3\n\3\n\5\n\u00aa"+
		"\n\n\3\n\3\n\3\n\3\n\3\n\3\n\3\13\3\13\3\13\3\f\7\f\u00b6\n\f\f\f\16\f"+
		"\u00b9\13\f\3\r\3\r\3\r\3\r\3\r\3\r\5\r\u00c1\n\r\3\16\3\16\3\16\3\17"+
		"\3\17\3\17\3\17\3\17\3\20\3\20\7\20\u00cd\n\20\f\20\16\20\u00d0\13\20"+
		"\3\20\5\20\u00d3\n\20\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21"+
		"\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\23\3\23\3\23\3\23\3\23\3\24"+
		"\3\24\3\24\3\24\3\24\7\24\u00f1\n\24\f\24\16\24\u00f4\13\24\5\24\u00f6"+
		"\n\24\3\24\3\24\3\24\3\25\3\25\3\25\3\25\3\26\3\26\3\26\3\26\3\26\7\26"+
		"\u0104\n\26\f\26\16\26\u0107\13\26\3\26\3\26\3\26\3\27\3\27\3\30\3\30"+
		"\3\31\3\31\3\32\3\32\3\33\3\33\3\33\3\33\7\33\u0118\n\33\f\33\16\33\u011b"+
		"\13\33\3\34\3\34\3\35\3\35\3\35\3\35\3\36\3\36\3\36\7\36\u0126\n\36\f"+
		"\36\16\36\u0129\13\36\3\37\3\37\7\37\u012d\n\37\f\37\16\37\u0130\13\37"+
		"\3\37\2\2 \2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(*,.\60\62\64\66"+
		"8:<\2\b\4\2\3\6\25\25\3\2\7\b\3\2\33 \3\2\25\27\4\2\13\13!$\3\2\t\n\2"+
		"\u012f\2A\3\2\2\2\4E\3\2\2\2\6X\3\2\2\2\b_\3\2\2\2\nn\3\2\2\2\fq\3\2\2"+
		"\2\16z\3\2\2\2\20\u0081\3\2\2\2\22\u0097\3\2\2\2\24\u00b1\3\2\2\2\26\u00b7"+
		"\3\2\2\2\30\u00c0\3\2\2\2\32\u00c2\3\2\2\2\34\u00c5\3\2\2\2\36\u00ca\3"+
		"\2\2\2 \u00d4\3\2\2\2\"\u00de\3\2\2\2$\u00e6\3\2\2\2&\u00eb\3\2\2\2(\u00fa"+
		"\3\2\2\2*\u00fe\3\2\2\2,\u010b\3\2\2\2.\u010d\3\2\2\2\60\u010f\3\2\2\2"+
		"\62\u0111\3\2\2\2\64\u0113\3\2\2\2\66\u011c\3\2\2\28\u011e\3\2\2\2:\u0122"+
		"\3\2\2\2<\u012a\3\2\2\2>B\5\4\3\2?B\5\6\4\2@B\5\b\5\2A>\3\2\2\2A?\3\2"+
		"\2\2A@\3\2\2\2BC\3\2\2\2CD\7\2\2\3D\3\3\2\2\2EF\7\17\2\2FG\7\25\2\2GH"+
		"\7\'\2\2HL\5<\37\2IK\5\n\6\2JI\3\2\2\2KN\3\2\2\2LJ\3\2\2\2LM\3\2\2\2M"+
		"R\3\2\2\2NL\3\2\2\2OQ\5\22\n\2PO\3\2\2\2QT\3\2\2\2RP\3\2\2\2RS\3\2\2\2"+
		"SU\3\2\2\2TR\3\2\2\2UV\7(\2\2VW\5<\37\2W\5\3\2\2\2XY\7\20\2\2YZ\7\25\2"+
		"\2Z[\7\'\2\2[\\\5<\37\2\\]\7(\2\2]^\5<\37\2^\7\3\2\2\2_`\7\21\2\2`a\7"+
		"\25\2\2ab\7\'\2\2bf\5<\37\2ce\5\20\t\2dc\3\2\2\2eh\3\2\2\2fd\3\2\2\2f"+
		"g\3\2\2\2gi\3\2\2\2hf\3\2\2\2ij\7(\2\2jk\5<\37\2k\t\3\2\2\2lo\5\f\7\2"+
		"mo\5\16\b\2nl\3\2\2\2nm\3\2\2\2o\13\3\2\2\2pr\5.\30\2qp\3\2\2\2qr\3\2"+
		"\2\2rs\3\2\2\2st\5,\27\2tu\7\25\2\2uv\7\32\2\2vw\5\64\33\2wx\5<\37\2x"+
		"\r\3\2\2\2y{\5.\30\2zy\3\2\2\2z{\3\2\2\2{|\3\2\2\2|}\7\25\2\2}~\7\31\2"+
		"\2~\177\5\64\33\2\177\u0080\5<\37\2\u0080\17\3\2\2\2\u0081\u0082\7\22"+
		"\2\2\u0082\u0083\7\25\2\2\u0083\u008f\7%\2\2\u0084\u0085\5,\27\2\u0085"+
		"\u008c\7\25\2\2\u0086\u0087\7)\2\2\u0087\u0088\5,\27\2\u0088\u0089\7\25"+
		"\2\2\u0089\u008b\3\2\2\2\u008a\u0086\3\2\2\2\u008b\u008e\3\2\2\2\u008c"+
		"\u008a\3\2\2\2\u008c\u008d\3\2\2\2\u008d\u0090\3\2\2\2\u008e\u008c\3\2"+
		"\2\2\u008f\u0084\3\2\2\2\u008f\u0090\3\2\2\2\u0090\u0091\3\2\2\2\u0091"+
		"\u0093\7&\2\2\u0092\u0094\5\24\13\2\u0093\u0092\3\2\2\2\u0093\u0094\3"+
		"\2\2\2\u0094\u0095\3\2\2\2\u0095\u0096\5<\37\2\u0096\21\3\2\2\2\u0097"+
		"\u0098\7\22\2\2\u0098\u0099\7\25\2\2\u0099\u00a5\7%\2\2\u009a\u009b\5"+
		",\27\2\u009b\u00a2\7\25\2\2\u009c\u009d\7)\2\2\u009d\u009e\5,\27\2\u009e"+
		"\u009f\7\25\2\2\u009f\u00a1\3\2\2\2\u00a0\u009c\3\2\2\2\u00a1\u00a4\3"+
		"\2\2\2\u00a2\u00a0\3\2\2\2\u00a2\u00a3\3\2\2\2\u00a3\u00a6\3\2\2\2\u00a4"+
		"\u00a2\3\2\2\2\u00a5\u009a\3\2\2\2\u00a5\u00a6\3\2\2\2\u00a6\u00a7\3\2"+
		"\2\2\u00a7\u00a9\7&\2\2\u00a8\u00aa\5\24\13\2\u00a9\u00a8\3\2\2\2\u00a9"+
		"\u00aa\3\2\2\2\u00aa\u00ab\3\2\2\2\u00ab\u00ac\7\'\2\2\u00ac\u00ad\5<"+
		"\37\2\u00ad\u00ae\5\26\f\2\u00ae\u00af\7(\2\2\u00af\u00b0\5<\37\2\u00b0"+
		"\23\3\2\2\2\u00b1\u00b2\7\24\2\2\u00b2\u00b3\5,\27\2\u00b3\25\3\2\2\2"+
		"\u00b4\u00b6\5\30\r\2\u00b5\u00b4\3\2\2\2\u00b6\u00b9\3\2\2\2\u00b7\u00b5"+
		"\3\2\2\2\u00b7\u00b8\3\2\2\2\u00b8\27\3\2\2\2\u00b9\u00b7\3\2\2\2\u00ba"+
		"\u00c1\5\34\17\2\u00bb\u00c1\5&\24\2\u00bc\u00c1\5*\26\2\u00bd\u00c1\5"+
		"\36\20\2\u00be\u00c1\5\n\6\2\u00bf\u00c1\5\32\16\2\u00c0\u00ba\3\2\2\2"+
		"\u00c0\u00bb\3\2\2\2\u00c0\u00bc\3\2\2\2\u00c0\u00bd\3\2\2\2\u00c0\u00be"+
		"\3\2\2\2\u00c0\u00bf\3\2\2\2\u00c1\31\3\2\2\2\u00c2\u00c3\7\23\2\2\u00c3"+
		"\u00c4\5\64\33\2\u00c4\33\3\2\2\2\u00c5\u00c6\7\25\2\2\u00c6\u00c7\7\32"+
		"\2\2\u00c7\u00c8\5\64\33\2\u00c8\u00c9\5<\37\2\u00c9\35\3\2\2\2\u00ca"+
		"\u00ce\5 \21\2\u00cb\u00cd\5\"\22\2\u00cc\u00cb\3\2\2\2\u00cd\u00d0\3"+
		"\2\2\2\u00ce\u00cc\3\2\2\2\u00ce\u00cf\3\2\2\2\u00cf\u00d2\3\2\2\2\u00d0"+
		"\u00ce\3\2\2\2\u00d1\u00d3\5$\23\2\u00d2\u00d1\3\2\2\2\u00d2\u00d3\3\2"+
		"\2\2\u00d3\37\3\2\2\2\u00d4\u00d5\7\f\2\2\u00d5\u00d6\7%\2\2\u00d6\u00d7"+
		"\5:\36\2\u00d7\u00d8\7&\2\2\u00d8\u00d9\7\'\2\2\u00d9\u00da\5<\37\2\u00da"+
		"\u00db\5\26\f\2\u00db\u00dc\7(\2\2\u00dc\u00dd\5<\37\2\u00dd!\3\2\2\2"+
		"\u00de\u00df\7\16\2\2\u00df\u00e0\7%\2\2\u00e0\u00e1\5:\36\2\u00e1\u00e2"+
		"\7&\2\2\u00e2\u00e3\7\'\2\2\u00e3\u00e4\7(\2\2\u00e4\u00e5\5<\37\2\u00e5"+
		"#\3\2\2\2\u00e6\u00e7\7\r\2\2\u00e7\u00e8\7\'\2\2\u00e8\u00e9\7(\2\2\u00e9"+
		"\u00ea\5<\37\2\u00ea%\3\2\2\2\u00eb\u00ec\7\25\2\2\u00ec\u00f5\7%\2\2"+
		"\u00ed\u00f2\5\64\33\2\u00ee\u00ef\7)\2\2\u00ef\u00f1\5\64\33\2\u00f0"+
		"\u00ee\3\2\2\2\u00f1\u00f4\3\2\2\2\u00f2\u00f0\3\2\2\2\u00f2\u00f3\3\2"+
		"\2\2\u00f3\u00f6\3\2\2\2\u00f4\u00f2\3\2\2\2\u00f5\u00ed\3\2\2\2\u00f5"+
		"\u00f6\3\2\2\2\u00f6\u00f7\3\2\2\2\u00f7\u00f8\7&\2\2\u00f8\u00f9\5<\37"+
		"\2\u00f9\'\3\2\2\2\u00fa\u00fb\7\25\2\2\u00fb\u00fc\7\32\2\2\u00fc\u00fd"+
		"\5\64\33\2\u00fd)\3\2\2\2\u00fe\u00ff\7\25\2\2\u00ff\u0100\7%\2\2\u0100"+
		"\u0105\5(\25\2\u0101\u0102\7)\2\2\u0102\u0104\5(\25\2\u0103\u0101\3\2"+
		"\2\2\u0104\u0107\3\2\2\2\u0105\u0103\3\2\2\2\u0105\u0106\3\2\2\2\u0106"+
		"\u0108\3\2\2\2\u0107\u0105\3\2\2\2\u0108\u0109\7&\2\2\u0109\u010a\5<\37"+
		"\2\u010a+\3\2\2\2\u010b\u010c\t\2\2\2\u010c-\3\2\2\2\u010d\u010e\t\3\2"+
		"\2\u010e/\3\2\2\2\u010f\u0110\t\4\2\2\u0110\61\3\2\2\2\u0111\u0112\t\5"+
		"\2\2\u0112\63\3\2\2\2\u0113\u0119\5\62\32\2\u0114\u0115\5\60\31\2\u0115"+
		"\u0116\5\62\32\2\u0116\u0118\3\2\2\2\u0117\u0114\3\2\2\2\u0118\u011b\3"+
		"\2\2\2\u0119\u0117\3\2\2\2\u0119\u011a\3\2\2\2\u011a\65\3\2\2\2\u011b"+
		"\u0119\3\2\2\2\u011c\u011d\t\6\2\2\u011d\67\3\2\2\2\u011e\u011f\5\64\33"+
		"\2\u011f\u0120\5\66\34\2\u0120\u0121\5\64\33\2\u01219\3\2\2\2\u0122\u0127"+
		"\58\35\2\u0123\u0124\t\7\2\2\u0124\u0126\58\35\2\u0125\u0123\3\2\2\2\u0126"+
		"\u0129\3\2\2\2\u0127\u0125\3\2\2\2\u0127\u0128\3\2\2\2\u0128;\3\2\2\2"+
		"\u0129\u0127\3\2\2\2\u012a\u012e\7*\2\2\u012b\u012d\7*\2\2\u012c\u012b"+
		"\3\2\2\2\u012d\u0130\3\2\2\2\u012e\u012c\3\2\2\2\u012e\u012f\3\2\2\2\u012f"+
		"=\3\2\2\2\u0130\u012e\3\2\2\2\31ALRfnqz\u008c\u008f\u0093\u00a2\u00a5"+
		"\u00a9\u00b7\u00c0\u00ce\u00d2\u00f2\u00f5\u0105\u0119\u0127\u012e";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}