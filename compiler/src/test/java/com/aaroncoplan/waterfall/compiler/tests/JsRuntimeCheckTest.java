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
 * For every checked-in js/*.expected file, run {@code node --check} on its contents and
 * assert that node accepts it as syntactically valid JavaScript. If {@code node} is not
 * on PATH the test is assumed-skipped — golden tests still verify the output text.
 */
@RunWith(Parameterized.class)
public class JsRuntimeCheckTest {

    private final String example;
    private final Path goldenPath;

    public JsRuntimeCheckTest(String example, Path goldenPath) {
        this.example = example;
        this.goldenPath = goldenPath;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> cases() throws IOException {
        Path jsGoldens = Paths.get("src/test/resources/golden/js").toAbsolutePath();
        List<Object[]> out = new ArrayList<>();
        if (!Files.isDirectory(jsGoldens)) return out;
        try (Stream<Path> s = Files.list(jsGoldens)) {
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
    public void nodeAcceptsGolden() throws IOException, InterruptedException {
        Assume.assumeTrue("node not on PATH; skipping runtime check", nodeAvailable());
        Path tmp = Files.createTempFile("waterfall-js-", ".js");
        try {
            Files.copy(goldenPath, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            ProcessBuilder pb = new ProcessBuilder("node", "--check", tmp.toString());
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            byte[] output = proc.getInputStream().readAllBytes();
            int exit = proc.waitFor();
            assertEquals("node --check failed for " + example + " with output:\n"
                    + new String(output), 0, exit);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static boolean nodeAvailable() {
        try {
            Process p = new ProcessBuilder("node", "--version").redirectErrorStream(true).start();
            int exit = p.waitFor();
            return exit == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}
