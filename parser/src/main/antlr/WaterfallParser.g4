parser grammar WaterfallParser;

options { tokenVocab = WaterfallLexer; }

program
    : module EOF
    ;

module
    : MODULE name=ID L_CURLY function* R_CURLY
    ;

function
    : FUNCTION functionName=ID L_PARENS parameterList? R_PARENS (RETURNS returnType=type)? L_CURLY R_CURLY
    ;

parameterList
    : parameter (COMMA parameter)*
    ;

parameter
    : type name=ID
    ;

type
    : ID
    | ID L_BRACKET R_BRACKET
    ;