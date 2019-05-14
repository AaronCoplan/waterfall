package com.aaroncoplan.waterfall.parser;

import com.aaroncoplan.waterfall.parser.parsing.FileParser;
import com.aaroncoplan.waterfall.parser.parsing.ParseResult;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class ParserTests {

    @Test
    public void testParserResults() {
        final File baseDirectory = new File("");
        Assert.assertTrue(baseDirectory.getAbsolutePath().endsWith("/waterfall"));

        final File shouldPassParsingTestDir = new File(baseDirectory.getAbsolutePath() + "/src/main/resources/test-resources/parsing/should-pass");
        Assert.assertTrue(shouldPassParsingTestDir.exists());
        Assert.assertTrue(shouldPassParsingTestDir.isDirectory());

        final File[] shouldPassFiles = shouldPassParsingTestDir.listFiles((file, name) -> name.endsWith(".wf"));
        Assert.assertTrue(shouldPassFiles.length > 0);

        for(File f : shouldPassFiles) {
            final ParseResult parseResult = FileParser.parseFile(f.getPath());
            if(parseResult.hasErrors()) {
                System.out.println(f.getPath());
                System.out.println(String.join("\n", parseResult.getSyntaxErrors()));
            }
            Assert.assertFalse(parseResult.hasErrors());
            Assert.assertNotNull(parseResult.getFilePath());
            Assert.assertNotNull(parseResult.getProgramAST());
        }

        final File shouldFailParsingTestDir = new File(baseDirectory.getAbsolutePath() + "/src/main/resources/test-resources/parsing/should-fail");
        Assert.assertTrue(shouldFailParsingTestDir.exists());
        Assert.assertTrue(shouldFailParsingTestDir.isDirectory());

        final File[] shouldFailFiles = shouldFailParsingTestDir.listFiles((file, name) -> name.endsWith(".wf"));
        Assert.assertTrue(shouldFailFiles.length > 0);

        for(File f : shouldFailFiles) {
            final ParseResult parseResult = FileParser.parseFile(f.getPath());
            if(!parseResult.hasErrors()) {
                System.out.println(f.getPath());
            }
            Assert.assertTrue(parseResult.hasErrors());
            Assert.assertNotNull(parseResult.getFilePath());
            Assert.assertNotNull(parseResult.getProgramAST());
        }
    }
}
