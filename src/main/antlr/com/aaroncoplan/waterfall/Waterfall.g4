grammar Waterfall;

program
    : 'main {' newline_s codeBlock '}' newline_s EOF
    ;

codeBlock
    : (codeline newline_s)*
    ;

codeline
    : typed_variable_declaration_and_assignment
    | inferred_variable_declaration_and_assignment
    | variable_assignment
    ;

typed_variable_declaration_and_assignment
    : type ID EQUALS INT_LITERAL
    ;

inferred_variable_declaration_and_assignment
    : ID COLON_EQUALS INT_LITERAL
    ;

variable_assignment
    : ID EQUALS INT_LITERAL
    ;

type
    : INT
    | DEC
    | CHAR
    | BOOL
    | ID
    ;

// at least one newline
newline_s
    : NEWLINE NEWLINE*
    ;

// Lexer Symbols

INT
    : 'int'
    ;

DEC
    : 'dec'
    ;

CHAR
    : 'char'
    ;

BOOL
    : 'bool'
    ;

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

// return newlines to parser (is end-statement signal)
NEWLINE
    : '\r'? '\n'
    ;

// ignore whitespace
WS
    : [ \t]+ -> skip
    ;