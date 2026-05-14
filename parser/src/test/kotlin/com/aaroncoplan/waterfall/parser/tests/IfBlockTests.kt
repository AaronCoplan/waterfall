package com.aaroncoplan.waterfall.parser.tests

import org.junit.Test

class IfBlockTests {

    private val template = "module m {\nfunc f() {\n%s\n}\n}"

    @Test
    fun testEmptyIfBlock() {
        TestUtils.shouldPass(template.format("if(isTrue()) {}"))
    }

    @Test
    fun testIfBlockWithContents() {
        TestUtils.shouldPass(template.format("if(isTrue()) {\nx := 4\ny := double(x)\n}"))
    }

    @Test
    fun testNestedIfBlocks() {
        TestUtils.shouldPass(template.format("if(isTrue()){\nif(isFalse()) {}\n}"))
    }

    @Test
    fun testIfWithElif() {
        TestUtils.shouldPass(template.format("if(isTrue()){} elif(isFalse()){}"))
    }

    @Test
    fun testIfWithMultipleElifs() {
        TestUtils.shouldPass(template.format("if(isTrue()){} elif(isFalse()) {} elif(anotherCondition){}"))
    }

    @Test
    fun testElifAlone() {
        TestUtils.shouldFail(template.format("elif(isFalse()){}"))
    }

    @Test
    fun testElseAlone() {
        TestUtils.shouldFail(template.format("else(isFalse()){}"))
    }

    @Test
    fun testIfElse() {
        TestUtils.shouldPass(template.format("if(condition){} else{}"))
    }

    @Test
    fun testIfElifElse() {
        TestUtils.shouldPass(template.format("if(condition){} elif(condition){} else{}"))
    }

    @Test
    fun testIfMultipleElifElse() {
        TestUtils.shouldPass(template.format("if(condition) {} elif(anotherCondition) {} elif(anotherAnotherCondition){} else {}"))
    }
}
