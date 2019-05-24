parser grammar WaterfallParser;

options { tokenVocab = WaterfallLexer; }

program
    : module EOF
    ;

module
    : MODULE name=ID NEWLINE* L_CURLY NEWLINE* R_CURLY NEWLINE* // empty module
    | MODULE name=ID NEWLINE* L_CURLY NEWLINE+ topLevelDeclaration* R_CURLY NEWLINE* // module containing code
    ;

topLevelDeclaration
    : typedVariableDeclarationAndAssignment
    | functionImplementation
    ;

statement
    : typedVariableDeclarationAndAssignment
    | untypedVariableDeclarationAndAssignment
    | variableAssignment
    ;

variableAssignment
    : name=ID EQUALS INT_LITERAL NEWLINE+
    ;

untypedVariableDeclarationAndAssignment
    : modifier* name=ID COLON_EQUALS INT_LITERAL NEWLINE+
    ;

typedVariableDeclarationAndAssignment
    : modifier* type name=ID EQUALS INT_LITERAL NEWLINE+
    ;

functionImplementation
    : FUNCTION name=ID L_PARENS typedArgumentList? R_PARENS (RETURNS returnType=type)? L_CURLY NEWLINE* R_CURLY NEWLINE+
    | FUNCTION name=ID L_PARENS typedArgumentList? R_PARENS (RETURNS returnType=type)? L_CURLY NEWLINE+ statement* R_CURLY NEWLINE+
    ;

typedArgumentList
    : (typedArgument (COMMA typedArgument)*)
    ;

typedArgument
    : type name=ID
    ;

type
    : ID QUESTION_MARK?
    ;

modifier
    : CONST
    ;