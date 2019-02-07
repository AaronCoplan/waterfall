grammar Waterfall;

program
    : 'main {' code '}' EOF
    ;

code
    : codeline*
    ;

codeline
    : variableDeclaration
    ;

variableType
    : ID
    ;

variableName
    : ID
    ;

variableValue
    : INT_LITERAL
    | DEC_LITERAL
    ;

variableDeclaration
    : variableType variableName EQUALS variableValue SEMICOLON
    ;

// Symbols

ID
    : ('a' .. 'z' | 'A' .. 'Z' | '_')+
    ;

INT_LITERAL
    : ('0' .. '9')+
    ;

DEC_LITERAL
    : ('0' .. '9')+ DOT ('0' .. '9')+
    ;

DOT
    : '.'
    ;

EQUALS
    : '='
    ;

SEMICOLON
    : ';'
    ;

WHITESPACE
    : [ \t\r\n]+ -> skip
    ;