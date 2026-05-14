package com.aaroncoplan.waterfall.compiler.tests;

import com.aaroncoplan.waterfall.compiler.Main;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 * Golden / snapshot tests. For every checked-in
 * {@code compiler/src/test/resources/golden/<target>/<example>.expected}
 * file, this test runs the compiler with {@code --target <target>} on the
 * matching {@code examples/<example>.wf} and asserts the captured stdout
 * equals the golden file's contents.
 *
 * Regenerate by running {@code UPDATE_GOLDEN=1 ./gradlew test --tests GoldenTests}.
 */
@RunWith(Parameterized.class)
public class GoldenTests {

    private final String target;
    private final String example;
    private final Path goldenPath;
    private final Path examplePath;

    public GoldenTests(String target, String example, Path goldenPath, Path examplePath) {
        this.target = target;
        this.example = example;
        this.goldenPath = goldenPath;
        this.examplePath = examplePath;
    }

    @Parameterized.Parameters(name = "{0}/{1}")
    public static Collection<Object[]> cases() throws IOException {
        Path goldenRoot = Paths.get("src/test/resources/golden").toAbsolutePath();
        Path examplesRoot = Paths.get("../examples").toAbsolutePath();
        List<Object[]> out = new ArrayList<>();
        if (!Files.isDirectory(goldenRoot)) return out;
        try (Stream<Path> targets = Files.list(goldenRoot)) {
            for (Path targetDir : (Iterable<Path>) targets::iterator) {
                if (!Files.isDirectory(targetDir)) continue;
                String target = targetDir.getFileName().toString();
                try (Stream<Path> goldens = Files.list(targetDir)) {
                    for (Path g : (Iterable<Path>) goldens::iterator) {
                        String fileName = g.getFileName().toString();
                        if (!fileName.endsWith(".expected")) continue;
                        String example = fileName.substring(0, fileName.length() - ".expected".length());
                        Path examplePath = examplesRoot.resolve(example + ".wf");
                        out.add(new Object[]{target, example, g, examplePath});
                    }
                }
            }
        }
        return out;
    }

    @Test
    public void matchesGolden() throws IOException {
        String actual = runCompiler();
        if ("1".equals(System.getenv("UPDATE_GOLDEN"))) {
            Files.createDirectories(goldenPath.getParent());
            Files.write(goldenPath, actual.getBytes());
            return;
        }
        String expected = new String(Files.readAllBytes(goldenPath));
        assertEquals("Golden mismatch for " + target + "/" + example
                + " (set UPDATE_GOLDEN=1 to regenerate)", expected, actual);
    }

    private String runCompiler() {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(new ByteArrayOutputStream()));
        try {
            Main.main(new String[]{"--target", target, examplePath.toString()});
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
        return out.toString();
    }
}
