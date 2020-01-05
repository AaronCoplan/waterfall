package com.aaroncoplan.waterfall.compiler

import com.aaroncoplan.waterfall.parser.parseCodeString

fun main(args: Array<String>) {
    val code = """
        module Math {
            func addNumbers(int a, int b) returns int {
                return 1;    
            }
            
            func fill(int num, int n) returns int[] {
                return 0;
            }
            
            func doNothing() {}
        }
    """

    val parseResult = parseCodeString("DummyFilepath", code)
    if (parseResult.syntaxErrors.isNotEmpty()) {
        parseResult.syntaxErrors.forEach { println(it) }
        return
    }

    val ast = parseResult.ast

    /* First pass, construct top level symbol tables */

    val globalSymbolTable = SymbolTable(null)

    val module = ast.module()
    val moduleSymbolTable = globalSymbolTable.registerModule(module.name.text)

    module.function().forEach { function ->
        val returnType = if(function.returnType == null) null else if(function.returnType.arrayType() != null) "${function.returnType.arrayType().name.text}[]" else function.returnType.name.text
        val argumentList = if(function.parameterList() == null) emptyList<SymbolTable.ArgumentDefinition>() else function.parameterList().parameter().map { parameter ->
            val argumentType = if(parameter.type().arrayType() != null) "${parameter.type().arrayType().name.text}[]" else parameter.type().name.text
            SymbolTable.ArgumentDefinition(parameter.name.text, Type(argumentType))
        }
        moduleSymbolTable.registerFunction(function.name.text, Type(returnType), argumentList)
    }

    /* Second pass, do actual compilation */
}