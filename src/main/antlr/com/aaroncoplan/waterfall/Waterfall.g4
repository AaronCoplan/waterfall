grammar Waterfall;

program
    : 'main {' code '}' EOF
    ;

code
    : codeline*
    ;

codeline
    : 'hello' SEMICOLON
    | 'goodbye' SEMICOLON
    ;

DECIMAL_LITERAL
    : ('0' .. '9')+
    | ('0' .. '9')+ DOT ('0' .. '9')+
    ;

INTEGER_LITERAL
    : ('0' .. '9')+
    ;

DOT
    : '.'
    ;

SEMICOLON
    : ';'
    ;

WHITESPACE
    : [ \t\r\n]+ -> skip
    ;