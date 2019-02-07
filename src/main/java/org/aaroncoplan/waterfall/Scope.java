package org.aaroncoplan.waterfall;

import java.util.HashMap;
import java.util.Map;

public class Scope {
    private final Scope parentScope;
    private final Map<String, Integer> referenceMap;

    public Scope(Scope parentScope) {
        this.parentScope = parentScope;
        this.referenceMap = new HashMap<String, Integer>();
    }

    public void addReference(String refName, int statementNumber) {
        referenceMap.put(refName, statementNumber);
    }

    public Integer getReference(String refName) {
        return referenceMap.get(refName);
    }

    public void debugPrint() {
        this.referenceMap.entrySet().forEach(
            entry -> {
                System.out.println(entry.getKey() + ", " + entry.getValue());
            }
        );
    }

}

