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

SEMICOLON
    : ';'
    ;

WHITESPACE
    : [ \t\r\n]+ -> skip
    ;