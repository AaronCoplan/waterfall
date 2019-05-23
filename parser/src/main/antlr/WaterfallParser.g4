parser grammar WaterfallParser;

options { tokenVocab = WaterfallLexer; }

program
    : module EOF
    ;

module
    : MODULE name=ID NEWLINE* L_CURLY R_CURLY NEWLINE* // empty module
    | MODULE name=ID NEWLINE* L_CURLY NEWLINE+ codeline* R_CURLY NEWLINE* // module containing code
    ;

codeline
    : modifier* name=ID COLON_EQUALS INT_LITERAL NEWLINE+
    | modifier* type=TYPE name=ID EQUALS INT_LITERAL NEWLINE+
    ;

modifier
    : CONST
    ;