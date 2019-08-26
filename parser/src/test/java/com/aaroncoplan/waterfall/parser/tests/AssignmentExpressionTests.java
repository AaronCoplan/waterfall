package com.aaroncoplan.waterfall.parser.tests;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Test;

public class AssignmentExpressionTests {

    @Test
    public void assignmentTest(){
        final String template = "module a {\nfunc sample() {\n%s\n}\n}";
        final String[] codeLines = {                        
            "str := `super string`",
            "nullVar := NULL",
            "myInt := 45",
            "myDec := 3.45678",
            "max := (int x, int y) ==> {}",
            "min := (int x, int y) ==> Math::min(x, y)",
            "myBundle := |myInt, myDec|",
            "list := [myInt, 46, 47, 48]",
            "functionResult := myObj.mySubObj.mySubSubObj.myFunction(myInt, nullVar)",
            "squareRoot := Math::squareRoot(number = 16, root = 2)",            
        };        
        TestUtils.shouldPass(Arrays.stream(codeLines).map(line -> String.format(template, line)).collect(Collectors.toList()));
    }
}
