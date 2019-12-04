parser grammar WaterfallParser;

options { tokenVocab = WaterfallLexer; }

program
    : module EOF
    | emptyModule EOF
    ;

emptyModule
    : MODULE L_CURLY R_CURLY
    ;

module
    : MODULE L_CURLY function R_CURLY
    ;

function
    : FUNCTION functionName=ID L_PARENS type=ID name=ID R_PARENS (RETURNS returnType=ID)? L_CURLY R_CURLY
    ;

functionBody
    : ID C_EQUALS INT_LITERAL SEMICOLON
    ;
