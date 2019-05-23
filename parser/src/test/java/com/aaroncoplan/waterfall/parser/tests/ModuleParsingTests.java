package com.aaroncoplan.waterfall.parser.tests;

import org.junit.Test;

import java.util.Arrays;

public class ModuleParsingTests {

    @Test
    public void noWhiteSpace(){
        final String code = "module a{}";
        TestUtils.shouldPass(code);
    }

    @Test
    public void bracketOnNewline() {
        final String code = "module a\n{}";
        TestUtils.shouldPass(code);
    }

    @Test
    public void testIllegalModuleNames() {
        final String[] code = new String[]{
            "module 9startswithnumber {}",
            "module $dollar$ign {}",
            "module A_B_&illegalcharampersand {}"
        };
        Arrays.stream(code).forEach(TestUtils::shouldFail);
    }
}
