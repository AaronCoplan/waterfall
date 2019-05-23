parser grammar WaterfallParser;

options { tokenVocab = WaterfallLexer; }

program
    : module EOF
    ;

module
    : MODULE name=ID NEWLINE* L_CURLY R_CURLY NEWLINE* // empty module
    | MODULE name=ID NEWLINE* L_CURLY NEWLINE+ topLevelDeclaration* R_CURLY NEWLINE* // module containing code
    ;

topLevelDeclaration
    : typedVariableDeclarationAndAssignment
    | functionImplementation
    ;

functionImplementation
    : FUNCTION name=ID L_PARENS typedArgumentList? R_PARENS (RETURNS returnType=type)? L_CURLY R_CURLY NEWLINE+
    ;

typedArgumentList
    : (typedArgument (COMMA typedArgument)*)
    ;

typedArgument
    : type name=ID
    ;

untypedVariableDeclarationAndAssignment
    : modifier* name=ID COLON_EQUALS INT_LITERAL NEWLINE+
    ;

typedVariableDeclarationAndAssignment
    : modifier* type name=ID EQUALS INT_LITERAL NEWLINE+
    ;

type
    : ID QUESTION_MARK?
    ;

modifier
    : CONST
    ;