package com.aaroncoplan.waterfall.compiler.symboltables

class SymbolTable(private val parentSymbolTable: SymbolTable?) {

    private val nameToInfoMap: MutableMap<String, Any?> = HashMap()

    @Throws(DuplicateDeclarationException::class)
    fun declare(key: String, info: Any?) {
        // check if it's already been declared in higher scope
        if (parentSymbolTable != null) {
            val parentInfo = parentSymbolTable.lookup(key)
            if (parentInfo != null) throw DuplicateDeclarationException()
        }
        // check if it's already been declared in this scope
        val localInfo = nameToInfoMap[key]
        if (localInfo != null) throw DuplicateDeclarationException()

        nameToInfoMap[key] = info
    }

    private fun lookup(key: String): Any? {
        val info = nameToInfoMap[key]
        if (info != null) return info
        return parentSymbolTable?.lookup(key)
    }
}
