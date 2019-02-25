package org.aaroncoplan.waterfall;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class ParserTests {

    @Test
    public void listFilesInResources() {
        final File baseDirectory = new File("");
        Assert.assertTrue(baseDirectory.getAbsolutePath().endsWith("/waterfall"));

        final File parsingTestFilesDirectory = new File(baseDirectory.getAbsolutePath() + "/src/main/resources/test-resources/parsing");
        Assert.assertTrue(parsingTestFilesDirectory.exists());
        Assert.assertTrue(parsingTestFilesDirectory.isDirectory());

        
    }
}
