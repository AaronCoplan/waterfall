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
    | ifBlock
    | forBlock
    ;

emptyBlock
    : L_CURLY NEWLINE* R_CURLY
    ;

statementBlock
    : L_CURLY NEWLINE+ statement* R_CURLY
    ;

forBlock
    : forInBlock NEWLINE+
    ;

forInBlock
    : FOR L_PARENS name=ID IN collection=ID R_PARENS emptyBlock
    | FOR L_PARENS name=ID IN collection=ID R_PARENS statementBlock
    ;

ifBlock
    : IF L_PARENS expression R_PARENS emptyBlock elifBlock* elseBlock? NEWLINE+
    | IF L_PARENS expression R_PARENS statementBlock elifBlock* elseBlock? NEWLINE+
    ;

elifBlock
    : ELIF L_PARENS expression R_PARENS emptyBlock
    | ELIF L_PARENS expression R_PARENS statementBlock
    ;

elseBlock
    : ELSE emptyBlock
    | ELSE statementBlock
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
    : FUNCTION name=ID L_PARENS typedArgumentList? R_PARENS (RETURNS returnType=type)? emptyBlock NEWLINE+ // empty function
    | FUNCTION name=ID L_PARENS typedArgumentList? R_PARENS (RETURNS returnType=type)? statementBlock NEWLINE+ // function containing code
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
    : PIPE positionalArgumentList PIPE
    ;

arrayLiteral
    : L_BRACKET positionalArgumentList R_BRACKET
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
    | positionalArguments=positionalArgumentList
    ;

positionalArgumentList
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