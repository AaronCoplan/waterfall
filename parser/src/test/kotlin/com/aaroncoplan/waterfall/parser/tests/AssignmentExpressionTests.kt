package com.aaroncoplan.waterfall.parser.tests

import org.junit.Test

class AssignmentExpressionTests {

    @Test
    fun assignmentTest() {
        val template = "module a {\nfunc sample() {\n%s\n}\n}"
        val codeLines = arrayOf(
            "str := `super string`",
            "nullVar := NULL",
            "myInt := 45",
            "myDec := 3.45678",
            "max := (int x, int y) ==> {}",
            "min := (int x, int y) ==> Math::min(x, y)",
            "myBundle := |myInt, myDec|",
            "list := [myInt, 46, 47, 48]",
            "functionResult := myObj.mySubObj.mySubSubObj.myFunction(myInt, nullVar)",
            "squareRoot := Math::squareRoot(number = 16, root = 2)"
        )
        TestUtils.shouldPass(codeLines.map { template.format(it) })
    }
}
