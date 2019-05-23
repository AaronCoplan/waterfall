parser grammar WaterfallParser;

options { tokenVocab = WaterfallLexer; }

program
    : module EOF
    ;

module
    : MODULE ID NEWLINE* L_CURLY R_CURLY NEWLINE* // empty module
    | MODULE ID NEWLINE* L_CURLY NEWLINE+ codeline* R_CURLY NEWLINE* // module containing code
    ;

codeline
    : modifier* ID COLON_EQUALS INT_LITERAL NEWLINE+
    | modifier* type=ID ID EQUALS INT_LITERAL NEWLINE+
    ;

modifier
    : CONST
    ;