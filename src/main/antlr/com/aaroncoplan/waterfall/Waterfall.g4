grammar Waterfall;

program
    : container newline_s EOF
    ;

container
    : (MODULE | TYPE | SPEC) ID LEFT_CURLY newline_s (variable_declaration | function_declaration)* RIGHT_CURLY
    | type
    | spec
    ;

module
    : MODULE ID LEFT_CURLY newline_s (function_declaration newline_s | variable_declaration newline_s)* RIGHT_CURLY
    ;

type
    : TYPE ID LEFT_CURLY newline_s RIGHT_CURLY
    ;

spec
    : SPEC ID LEFT_CURLY newline_s (function_signature newline_s)+ RIGHT_CURLY
    ;

codeBlock
    : (codeline newline_s)*
    ;

variable_declaration
    : typed_variable_declaration_and_assignment
    | inferred_variable_declaration_and_assignment
    ;

function_signature
    : FUNC ID LEFT_PARENS (variable_type ID (COMMA variable_type ID)*)? RIGHT_PARENS RETURNS variable_type
    ;

function_declaration
    : function_signature LEFT_CURLY newline_s RIGHT_CURLY
    ;

codeline
    : variable_assignment
    | function_call_positional_args
    | function_call_named_args
    ;

scoped_statement
    : if_statement
    ;

typed_variable_declaration_and_assignment
    : modifier? variable_type ID EQUALS assignment_right_hand
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

function_call_positional_args
    : ID LEFT_PARENS (assignment_right_hand (COMMA assignment_right_hand)*)? RIGHT_PARENS
    ;

named_arg
    : ID EQUALS assignment_right_hand
    ;

function_call_named_args
    : ID LEFT_PARENS named_arg (COMMA named_arg)* RIGHT_PARENS
    ;

variable_type
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

MODULE
    : 'module'
    ;

TYPE
    : 'type'
    ;

SPEC
    : 'spec'
    ;

FUNC
    : 'func'
    ;

RETURNS
    : 'returns'
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

COMMA
    : ','
    ;

// return newlines to parser (is end-statement signal)
NEWLINE
    : '\r'? '\n'
    ;

// ignore whitespace
WS
    : [ \t]+ -> skip
    ;