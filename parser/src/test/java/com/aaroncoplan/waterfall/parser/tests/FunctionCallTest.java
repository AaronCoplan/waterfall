package com.aaroncoplan.waterfall.parser.tests;

import org.junit.Test;

public class FunctionCallTest {

    @Test
    public void testModuleFunctionCallPositionalArgs() {
        final String code = "module m {\nfunc f() {\nMyModule::myFunc(a, b, c, 877)\n}\n}";        
        TestUtils.shouldPass(code);
    }

    @Test
    public void testModuleFunctionCallNamedArgs() {
        final String code = "module m {\nfunc f() {\nMyModule::myFunc(x = 1, b = z)\n}\n}";
        TestUtils.shouldPass(code);
    }

    @Test
    public void testModuleFunctionCallMixedArgs() {
        final String code = "module m {\nfunc f() {\nMyModule::myFunc(1, x=2)\n}\n}";
        TestUtils.shouldFail(code);
    }

    @Test
    public void testObjectFunctionCallPositionalArgs() {
        final String code = "module m {\nfunc f() {\nmyObject.myFunction(1,2,x, anotherFunc())\n}\n}";
        TestUtils.shouldPass(code);
    }

    @Test
    public void testObjectFunctionCallNamedArgs() {
        final String code = "module m {\nfunc f() {\nmyObject.myFunction(x = 1, b = 2, c = [1,2,3])\n}\n}";
        TestUtils.shouldPass(code);
    }

    @Test
    public void testObjectFunctionCallMixedArgs() {
        final String code = "module m {\nfunc f() {\nmyObject.myFunction(x = 1, 2, 3)\n}\n}";
        TestUtils.shouldFail(code);
    }
}