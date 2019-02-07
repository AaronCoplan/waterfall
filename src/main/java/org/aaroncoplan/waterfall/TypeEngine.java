package org.aaroncoplan.waterfall;

public class TypeEngine {

    public boolean isValidType(String typeName) {
        return "int".equals(typeName) || "dec".equals(typeName);
    }

}

