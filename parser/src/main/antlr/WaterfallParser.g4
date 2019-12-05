parser grammar WaterfallParser;

options { tokenVocab = WaterfallLexer; }

program
    : module EOF
    ;

module
    : MODULE name=ID L_CURLY function* R_CURLY
    ;

function
    : FUNCTION functionName=ID L_PARENS parameterList? R_PARENS (RETURNS returnType=type)? L_CURLY statementBlock? R_CURLY
    ;

statementBlock
    : statement+
    ;

statement
    : variableAssignment
    | typedVariableAssignment
    | variableReassignment    
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
    : ID
    | ID L_BRACKET R_BRACKET
    ;