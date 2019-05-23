package com.aaroncoplan.waterfall.parser.tests;

import org.junit.Test;

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
    public void newlinesBetweenBrackets() {
        final String[] code = new String[]{
          "module a\n{\n\n}",
          "module b{\n\n\n\n}"
        };
        TestUtils.shouldPass(code);
    }

    @Test
    public void testIllegalModuleNames() {
        final String[] code = new String[]{
          "module 9startswithnumber {}",
          "module $dollar$ign {}",
          "module A_B_&illegalcharampersand {}"
        };
        TestUtils.shouldFail(code);
    }

    @Test
    public void testSingleLineInModule() {
        final String code = "module a\n{\n variable := 4\n }";
        TestUtils.shouldPass(code);
    }
}
