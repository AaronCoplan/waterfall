package com.aaroncoplan.waterfall.compiler.symboltables;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    private final SymbolTable parentSymbolTable;
    private final Map<String, Object> nameToInfoMap;

    public SymbolTable(SymbolTable parentSymbolTable) {
        this.parentSymbolTable = parentSymbolTable;
        this.nameToInfoMap = new HashMap<>();
    }

    public void declare(String key, Object info) throws DuplicateDeclarationException {
        // check if it's already been declared in higher scope
        if(parentSymbolTable != null) {
            Object parentInfo = parentSymbolTable.lookup(key);
            if(parentInfo != null) throw new DuplicateDeclarationException();
        }
        // check if it's already been declared in this scope
        Object localInfo = nameToInfoMap.get(key);
        if(localInfo != null) throw new DuplicateDeclarationException();

        // put the key value pair in the map
        nameToInfoMap.put(key, info);
    }

    public Object lookup(String key) {
        Object info = nameToInfoMap.get(key);
        if(info != null) return info;
        if(parentSymbolTable == null) return null;
        return parentSymbolTable.lookup(key);
    }
}
