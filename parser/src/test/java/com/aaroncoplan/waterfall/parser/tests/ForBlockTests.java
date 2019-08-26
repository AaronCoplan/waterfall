package com.aaroncoplan.waterfall.parser.tests;

import org.junit.Test;

public class ForBlockTests {

    private final String template = "module m {\nfunc f() {\nmyList := [1,2,3]\n%s\n}\n}";

    @Test
    public void testForInEmptyBlock() {
        TestUtils.shouldPass(String.format(template, "for (x in myList) {}"));
    }

    @Test
    public void testForInWithStatements() {
        TestUtils.shouldPass(String.format(template, "for (x in myList){\nprint(x)\n}"));        
    }
}