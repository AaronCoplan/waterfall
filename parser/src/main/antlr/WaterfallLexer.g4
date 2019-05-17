lexer grammar WaterfallLexer;

// built in types
INT: 'int';
DEC: 'dec';
CHAR: 'char';
BOOL: 'bool';

// language keywords
MODULE: 'module';

// modifiers
CONST: 'const';

// literals and identifiers
ID: ('a' .. 'z' | 'A' .. 'Z') ('a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_')*;
INT_LITERAL: ('0' .. '9')+;
DEC_LITERAL: ('0' .. '9')+ DOT ('0' .. '9')+;

// structural
DOT: '.';
EQUALS: '=';
COLON_EQUALS: ':=';
COMMA: ',';
L_PARENS: '(';
R_PARENS: ')';
L_CURLY: '{';
R_CURLY: '}';
L_BRACKET: '[';
R_BRACKET: ']';

// operators
PLUS: '+';
MINUS: '-';
MOD: '%';
POW: '^';
TIMES: '*';
DIVIDE: '/';

// comparators
LESS_THAN: '<';
GREATER_THAN: '>';
LESS_THAN_EQUAL: '<=';
GREATER_THAN_EQUAL: '>=';

// return newlines to parser (is end-statement signal)
NEWLINE: '\r'? '\n';

// ignore whitespace
WS: [ \t]+ -> skip;