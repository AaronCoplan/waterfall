package com.aaroncoplan.waterfall.compiler.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.Test;
import org.junit.Assume;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 * For every checked-in python/*.expected file, run python3's ast.parse on its
 * contents and assert it accepts the output as syntactically valid Python.
 * Skipped via Assume if python3 isn't on PATH.
 */
@RunWith(Parameterized.class)
public class PythonRuntimeCheckTest {

    private final String example;
    private final Path goldenPath;

    public PythonRuntimeCheckTest(String example, Path goldenPath) {
        this.example = example;
        this.goldenPath = goldenPath;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> cases() throws IOException {
        Path goldens = Paths.get("src/test/resources/golden/python").toAbsolutePath();
        List<Object[]> out = new ArrayList<>();
        if (!Files.isDirectory(goldens)) return out;
        try (Stream<Path> s = Files.list(goldens)) {
            for (Path g : (Iterable<Path>) s::iterator) {
                String fileName = g.getFileName().toString();
                if (!fileName.endsWith(".expected")) continue;
                String example = fileName.substring(0, fileName.length() - ".expected".length());
                out.add(new Object[]{example, g});
            }
        }
        return out;
    }

    @Test
    public void python3AcceptsGolden() throws IOException, InterruptedException {
        Assume.assumeTrue("python3 not on PATH; skipping runtime check", pythonAvailable());
        Path tmp = Files.createTempFile("waterfall-py-", ".py");
        try {
            Files.copy(goldenPath, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            ProcessBuilder pb = new ProcessBuilder(
                    "python3", "-c",
                    "import ast,sys; ast.parse(open(sys.argv[1]).read())",
                    tmp.toString());
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            byte[] output = proc.getInputStream().readAllBytes();
            int exit = proc.waitFor();
            assertEquals("python3 ast.parse failed for " + example + " with output:\n"
                    + new String(output), 0, exit);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static boolean pythonAvailable() {
        try {
            Process p = new ProcessBuilder("python3", "--version").redirectErrorStream(true).start();
            int exit = p.waitFor();
            return exit == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}
