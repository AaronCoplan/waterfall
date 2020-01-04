lexer grammar WaterfallLexer;

MODULE: 'module';
FUNCTION: 'func';
RETURNS: 'returns';
RETURN: 'return';

ID: ('a' .. 'z' | 'A' .. 'Z') (('a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_')+ ('a' .. 'z' | 'A' .. 'Z' | '0' .. '9'))?;
INT_LITERAL: ('0' .. '9')+;
DEC_LITERAL: ('0' .. '9')+ DOT ('0' .. '9')+;
STRING_LITERAL: '`' ( '\\`' | ~('\n'|'\r') )*? '`';

DOT: '.';
COMMA: ',';
SEMICOLON: ';';
C_EQUALS: ':=';
EQUALS: '=';
L_PARENS: '(';
R_PARENS: ')';
L_BRACKET: '[';
R_BRACKET: ']';
L_CURLY: '{';
R_CURLY: '}';

WS: [ \t\r\n]+ -> skip;