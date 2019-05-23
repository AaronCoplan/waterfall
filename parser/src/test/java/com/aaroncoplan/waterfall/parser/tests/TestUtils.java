package com.aaroncoplan.waterfall.parser.tests;

import com.aaroncoplan.waterfall.parser.FileParser;
import com.aaroncoplan.waterfall.parser.ParseResult;
import org.junit.Assert;

import java.util.Arrays;

public class TestUtils {

    private static ParseResult execParse(String code) {
        final ParseResult result = FileParser.parseCodeString(null, code);
        if(result.hasErrors()) {
            result.getSyntaxErrors().forEach(System.out::println);
        }
        return result;
    }

    public static void shouldPass(String code) {
        ParseResult result = execParse(code);
        Assert.assertFalse(result.hasErrors());
    }

    public static void shouldPass(String[] code) {
        Arrays.stream(code).forEach(TestUtils::shouldPass);
    }

    public static void shouldFail(String code) {
        ParseResult result = execParse(code);
        Assert.assertTrue(result.hasErrors());
    }

    public static void shouldFail(String[] code) {
        Arrays.stream(code).forEach(TestUtils::shouldFail);
    }
}
