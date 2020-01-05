parser grammar WaterfallParser;

options { tokenVocab = WaterfallLexer; }

program
    : module EOF
    ;

module
    : MODULE name=ID L_CURLY function* R_CURLY
    ;

function
    : FUNCTION name=ID L_PARENS parameterList? R_PARENS (RETURNS returnType=type)? L_CURLY statementBlock? R_CURLY
    ;

statementBlock
    : statement+
    ;

statement
    : variableAssignment
    | typedVariableAssignment
    | variableReassignment
    | functionCall
    | returnStatement
    ;

returnStatement
    : RETURN value? SEMICOLON
    ;

variableAssignment
    : name=ID C_EQUALS value SEMICOLON
    ;

typedVariableAssignment
    : type name=ID EQUALS value SEMICOLON
    ;

variableReassignment
    : name=ID EQUALS value SEMICOLON
    ;

functionCall
    : name=ID L_PARENS (argumentList | namedArgumentList)? R_PARENS SEMICOLON
    ;

argumentList
    : argument (COMMA argument)*
    ;

namedArgumentList
    : namedArgument (COMMA namedArgument)*
    ;

namedArgument
    : name=ID EQUALS argument
    ;

argument
    : name=ID
    | value
    ;

parameterList
    : parameter (COMMA parameter)*
    ;

parameter
    : type name=ID
    ;

value
    : INT_LITERAL
    | DEC_LITERAL
    | STRING_LITERAL
    ;

type
    : name=ID
    | arrayType
    ;

arrayType
    : name=ID L_BRACKET R_BRACKET
    ;