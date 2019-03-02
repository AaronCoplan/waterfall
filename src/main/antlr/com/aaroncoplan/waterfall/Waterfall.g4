grammar Waterfall;

program
    : 'main' LEFT_CURLY newline_s codeBlock RIGHT_CURLY newline_s EOF
    ;

codeBlock
    : (codeline newline_s)*
    ;

codeline
    : typed_variable_declaration_and_assignment
    | inferred_variable_declaration_and_assignment
    | variable_assignment
    | if_statement
    ;

typed_variable_declaration_and_assignment
    : modifier? type ID EQUALS assignment_right_hand
    ;

inferred_variable_declaration_and_assignment
    : modifier? ID COLON_EQUALS assignment_right_hand
    ;

variable_assignment
    : ID EQUALS assignment_right_hand
    ;

if_statement
    : IF LEFT_PARENS conditional RIGHT_PARENS LEFT_CURLY RIGHT_CURLY
    ;

type
    : INT
    | DEC
    | CHAR
    | BOOL
    | ID
    ;

modifier
    : CONST
    | FINAL
    ;

math_operator
    : ADD
    | SUBTRACT
    | MULTIPLY
    | DIVIDE
    | MOD
    | POW
    ;

value
    : INT_LITERAL
    | DEC_LITERAL
    | ID
    ;

assignment_right_hand
    : value (math_operator value)*
    ;

comparator
    : CHECK_EQUAL
    | LESS_THAN
    | GREATER_THAN
    | LESS_THAN_EQUALS
    | GREATER_THAN_EQUALS
    ;

condition
    : assignment_right_hand comparator assignment_right_hand
    ;

conditional
    : condition ((AND | OR) condition)*
    ;

// at least one newline
newline_s
    : NEWLINE NEWLINE*
    ;

// Language Keywords

// Built in types
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

// Modifiers
CONST
    : 'const'
    ;

FINAL
    : 'final'
    ;

AND
    : 'and'
    ;

OR
    : 'or'
    ;

CHECK_EQUAL
    : 'equals'
    ;

IF
    : 'if'
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

DIVIDE
    : '/'
    ;

MULTIPLY
    : '*'
    ;

ADD
    : '+'
    ;

SUBTRACT
    : '-'
    ;

MOD
    : '%'
    ;

POW
    : '^'
    ;

LESS_THAN
    : '<'
    ;

GREATER_THAN
    : '>'
    ;

LESS_THAN_EQUALS
    : '<='
    ;

GREATER_THAN_EQUALS
    : '>='
    ;

LEFT_PARENS
    : '('
    ;

RIGHT_PARENS
    : ')'
    ;

LEFT_CURLY
    : '{'
    ;

RIGHT_CURLY
    : '}'
    ;

// return newlines to parser (is end-statement signal)
NEWLINE
    : '\r'? '\n'
    ;

// ignore whitespace
WS
    : [ \t]+ -> skip
    ;