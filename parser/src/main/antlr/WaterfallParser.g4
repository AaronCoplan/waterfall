parser grammar WaterfallParser;

options { tokenVocab = WaterfallLexer; }

program
    : module EOF
    ;

module
    : MODULE ID L_CURLY codeline* R_CURLY
    ;

codeline
    : modifier* ID COLON_EQUALS INT_LITERAL NEWLINE+
    | modifier* type=ID ID EQUALS INT_LITERAL NEWLINE+
    ;

modifier
    : CONST
    ;