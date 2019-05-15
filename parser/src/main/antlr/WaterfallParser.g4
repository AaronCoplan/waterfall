parser grammar WaterfallParser;

options { tokenVocab = WaterfallLexer; }

program
    : module EOF
    ;

module
    : MODULE ID L_CURLY R_CURLY
    ;