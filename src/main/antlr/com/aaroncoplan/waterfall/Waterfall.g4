grammar Waterfall;

program
    : (module | type | spec) EOF
    ;

module
    : MODULE ID LEFT_CURLY newline_s (function_declaration | variable_declaration)* RIGHT_CURLY newline_s
    ;

type
    : TYPE ID LEFT_CURLY newline_s RIGHT_CURLY newline_s
    ;

spec
    : SPEC ID LEFT_CURLY newline_s (function_signature)* RIGHT_CURLY newline_s
    ;

// <VARIABLE DECLARATIONS>
variable_declaration
    : typed_variable_declaration_and_assignment
    | inferred_variable_declaration_and_assignment
    ;

typed_variable_declaration_and_assignment
    : modifier? variable_type ID EQUALS assignment_right_hand newline_s
    ;

inferred_variable_declaration_and_assignment
    : modifier? ID COLON_EQUALS assignment_right_hand newline_s
    ;
// </VARIABLE DECLARATIONS>

// <FUNCTION DECLARATIONS>
function_signature
    : FUNC ID LEFT_PARENS (variable_type ID (COMMA variable_type ID)*)? RIGHT_PARENS RETURNS variable_type newline_s
    ;

function_declaration
    : FUNC ID LEFT_PARENS (variable_type ID (COMMA variable_type ID)*)? RIGHT_PARENS RETURNS variable_type LEFT_CURLY newline_s code_block RIGHT_CURLY newline_s
    ;
// </FUNCTION DECLARATIONS>

code_block
    : (block_child)*
    ;

block_child
    : variable_assignment
    | function_call_positional_args
    | function_call_named_args
    | conditional
    | variable_declaration
    | return_statement
    ;

return_statement
    : RETURN assignment_right_hand
    ;

variable_assignment
    : ID EQUALS assignment_right_hand newline_s
    ;

// <CONDITIONALS>
conditional
    : if_statement (elif_statement)* else_statement?
    ;

if_statement
    : IF LEFT_PARENS condition RIGHT_PARENS LEFT_CURLY RIGHT_CURLY newline_s
    ;

elif_statement
    : ELIF LEFT_PARENS condition RIGHT_PARENS LEFT_CURLY RIGHT_CURLY newline_s
    ;

else_statement
    : ELSE LEFT_CURLY RIGHT_CURLY newline_s
    ;
// </CONDITIONALS>

// <FUNCTION CALLS>
function_call_positional_args
    : ID LEFT_PARENS (assignment_right_hand (COMMA assignment_right_hand)*)? RIGHT_PARENS newline_s
    ;

named_arg
    : ID EQUALS assignment_right_hand
    ;

function_call_named_args
    : ID LEFT_PARENS named_arg (COMMA named_arg)* RIGHT_PARENS newline_s
    ;
// </FUNCTION CALLS>

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

comparison
    : assignment_right_hand comparator assignment_right_hand
    ;

condition
    : comparison ((AND | OR) comparison)*
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

ELSE
    : 'else'
    ;

ELIF
    : 'elif'
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

RETURN
    : 'return'
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