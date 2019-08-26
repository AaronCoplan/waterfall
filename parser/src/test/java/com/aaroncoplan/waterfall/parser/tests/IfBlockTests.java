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

    @Test
    public void testIfWithElif() {
        TestUtils.shouldPass(String.format(template, "if(isTrue()){} elif(isFalse()){}"));
    }

    @Test
    public void testIfWithMultipleElifs() {
        TestUtils.shouldPass(String.format(template, "if(isTrue()){} elif(isFalse()) {} elif(anotherCondition){}"));
    }

    @Test
    public void testElifAlone() {
        TestUtils.shouldFail(String.format(template, "elif(isFalse()){}"));
    }

    @Test
    public void testElseAlone() {
        TestUtils.shouldFail(String.format(template, "else(isFalse()){}"));
    }

    @Test
    public void testIfElse() {
        TestUtils.shouldPass(String.format(template, "if(condition){} else{}"));
    }

    @Test
    public void testIfElifElse() {
        TestUtils.shouldPass(String.format(template, "if(condition){} elif(condition){} else{}"));
    }

    @Test
    public void testIfMultipleElifElse() {
        TestUtils.shouldPass(String.format(template, "if(condition) {} elif(anotherCondition) {} elif(anotherAnotherCondition){} else {}"));
    }
}