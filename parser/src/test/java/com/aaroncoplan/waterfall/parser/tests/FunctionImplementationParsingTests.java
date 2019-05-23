package com.aaroncoplan.waterfall.parser.tests;

import org.junit.Test;

public class FunctionImplementationParsingTests {

    @Test
    public void noArgs() {
        final String code = "module m {\nfunc myFunc() {}\n}";
        TestUtils.shouldPass(code);
    }

    @Test
    public void singleArg() {
        final String code = "module m {\nfunc myFunc(type1 arg1) {}\n}";
        TestUtils.shouldPass(code);
    }

    @Test
    public void multipleArgs() {
        final String[] code = {
            "module m {\nfunc myFunc(type1 arg1, type2 arg2) {}\n}",
            "module m {\nfunc myFunc(type1 arg1, type2 arg2, type3 arg3) {}\n}"
        };
        TestUtils.shouldPass(code);
    }

    @Test
    public void withReturnType() {
        final String[] code = {
            "module m {\nfunc myFunc(type1 arg1) returns type2 {}\n}",
            "module m {\nfunc myFunc() returns type2 {}\n}",
            "module m {\nfunc myFunc(type1 arg1, type4 arg2) returns type2 {}\n}"
        };
        TestUtils.shouldPass(code);
    }
}
