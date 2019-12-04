parser grammar WaterfallParser;

options { tokenVocab = WaterfallLexer; }

program
    : MODULE+ EOF
    ;
