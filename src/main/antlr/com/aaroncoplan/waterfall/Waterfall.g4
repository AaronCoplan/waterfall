grammar Waterfall;

program
    : 'main {' newline_s codeBlock '}' newline_s EOF
    ;

codeBlock
    : codeline*
    ;

codeline
    : type ID EQUALS INT_LITERAL newline_s
    | ID COLON_EQUALS INT_LITERAL newline_s
    ;

type
    : 'int'
    | 'dec'
    | 'char'
    | 'bool'
    | ID
    ;

// at least one newline
newline_s
    : NEWLINE NEWLINE*
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

COLON_EQUALS
    : ':='
    ;

EQUALS
    : '='
    ;

NEWLINE:'\r'? '\n' ; // return newlines to parser (is end-statement signal)

// toss out whitespace
WS
    : [ \t]+ -> skip
    ;