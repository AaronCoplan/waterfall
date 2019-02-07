grammar Waterfall;

program
    : 'main {' code '}' EOF
    ;

code
    : codeline*
    ;

codeline
    : variableDeclaration
    | variableAssignment
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

variableAssignment
    : variableName EQUALS variableValue SEMICOLON
    ;

// Symbols

ID
    : ('a' .. 'z' | 'A' .. 'Z') ('a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_')*
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