package com.aaroncoplan.waterfall.compiler

import java.lang.RuntimeException

class SymbolTable {

    private enum class EntryType {
        MODULE, FUNCTION
    }
    data class ArgumentDefinition(val name: String, val type: Type)
    private data class Entry(
        val entryType: EntryType,
        val symbolTable: SymbolTable?,
        val type: Type?,
        val argumentList: List<ArgumentDefinition>?
    )

    private val table: MutableMap<String, Entry>
    private val parentSymbolTable: SymbolTable?

    constructor(parentSymbolTable: SymbolTable?) {
        this.parentSymbolTable = parentSymbolTable
        this.table = HashMap()
    }

    private fun recursiveLookup(name: String): Entry? {
        if(table.containsKey(name)) {
            return table[name]
        } else if(parentSymbolTable != null) {
            return parentSymbolTable.recursiveLookup(name)
        } else {
            return null
        }
    }

    fun registerModule(moduleName: String): SymbolTable {
        if (recursiveLookup(moduleName) != null) {
           throw RuntimeException("Duplicate declaration: $moduleName")
        }
        val moduleSymbolTable = SymbolTable(this)
        table[moduleName] = Entry(EntryType.MODULE, moduleSymbolTable, null, null)
        return moduleSymbolTable
    }

    fun registerFunction(functionName: String, returnType: Type?, argumentList: List<ArgumentDefinition>): SymbolTable {
        if (recursiveLookup(functionName) != null) {
            throw RuntimeException("Duplicate declaration: $functionName")
        }
        val functionSymbolTable = SymbolTable(this)
        table[functionName] = Entry(EntryType.FUNCTION, functionSymbolTable, returnType, argumentList)
        return functionSymbolTable
    }
}