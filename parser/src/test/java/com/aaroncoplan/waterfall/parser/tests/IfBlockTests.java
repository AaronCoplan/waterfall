package com.aaroncoplan.waterfall.parser.tests;

import org.junit.Test;

public class IfBlockTests {

    private final String template = "module m {\nfunc f() {\n%s\n}\n}";

    @Test
    public void testEmptyIfBlock() {        
        TestUtils.shouldPass(String.format(template, "if(isTrue()) {}"));
    }
    
    @Test
    public void testIfBlockWithContents() {
        TestUtils.shouldPass(String.format(template, "if(isTrue()) {\nx := 4\ny := double(x)\n}"));
    }

    @Test
    public void testNestedIfBlocks() {
        TestUtils.shouldPass(String.format(template, "if(isTrue()){\nif(isFalse()) {}\n}"));        
    }
}