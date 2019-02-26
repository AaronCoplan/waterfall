grammar Waterfall;

program
    : 'main {' codeBlock '}' NEWLINE? EOF
    ;

codeBlock
    : codeline*
    ;

codeline
    : ID EQUALS INT_LITERAL NEWLINE
    ;


// Lexer Symbols

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

NEWLINE:'\r'? '\n' ; // return newlines to parser (is end-statement signal)

WS : [ \t]+ -> skip ; // toss out whitespace