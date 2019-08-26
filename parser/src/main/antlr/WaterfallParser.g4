parser grammar WaterfallParser;

options { tokenVocab = WaterfallLexer; }

program
    : module EOF
    ;

module
    : MODULE name=ID NEWLINE? L_CURLY NEWLINE* R_CURLY NEWLINE* // empty module
    | MODULE name=ID NEWLINE? L_CURLY NEWLINE+ topLevelDeclaration* R_CURLY NEWLINE* // module containing code
    ;

topLevelDeclaration
    : typedVariableDeclarationAndAssignment
    | functionImplementation
    ;

statement
    : typedVariableDeclarationAndAssignment
    | untypedVariableDeclarationAndAssignment
    | variableAssignment
    | functionCall NEWLINE+     
    ;

variableAssignment
    : name=ID EQUALS INT_LITERAL NEWLINE+
    ;

untypedVariableDeclarationAndAssignment
    : modifier* name=ID COLON_EQUALS expression NEWLINE+
    ;

typedVariableDeclarationAndAssignment
    : modifier* type name=ID EQUALS expression NEWLINE+
    ;

functionImplementation
    : FUNCTION name=ID L_PARENS typedArgumentList? R_PARENS (RETURNS returnType=type)? L_CURLY NEWLINE* R_CURLY NEWLINE+ // empty function
    | FUNCTION name=ID L_PARENS typedArgumentList? R_PARENS (RETURNS returnType=type)? L_CURLY NEWLINE+ statement* R_CURLY NEWLINE+ // function containing code
    ;

typedArgumentList
    : typedArgument (COMMA typedArgument)*
    ;

typedArgument
    : type name=ID
    ;

type
    : QUESTION_MARK? ID
    ;

expression
    : NULL
    | INT_LITERAL
    | DEC_LITERAL
    | STRING_LITERAL
    | lambdaFunction
    | bundleLiteral
    | arrayLiteral
    | functionCall
    | name=ID
    ;

bundleLiteral
    : PIPE expression (COMMA expression)* PIPE
    ;

arrayLiteral
    : L_BRACKET expression (COMMA expression)* R_BRACKET
    ;

lambdaFunction
    : L_PARENS typedArgumentList R_PARENS LAMBDA functionCall
    | L_PARENS typedArgumentList R_PARENS LAMBDA L_CURLY R_CURLY
    ;

functionCall
    : moduleFunctionCall
    | objectFunctionCall
    | localFunctionCall
    ;

localFunctionCall
    : functionName=ID L_PARENS functionCallArguments? R_PARENS
    ;

moduleFunctionCall
    : moduleName=ID DOUBLE_COLON functionName=ID L_PARENS functionCallArguments? R_PARENS
    ;

objectFunctionCall
    : name=ID DOT (name=ID DOT)* functionName=ID L_PARENS functionCallArguments? R_PARENS
    ;

functionCallArguments
    : namedArguments
    | positionalArguments
    ;

positionalArguments
    : value=expression (COMMA value=expression)*
    ;

namedArgument
    : name=ID EQUALS value=expression
    ;

namedArguments
    : namedArgument (COMMA namedArgument)*
    ;

modifier
    : CONST
    | IMM
    ;