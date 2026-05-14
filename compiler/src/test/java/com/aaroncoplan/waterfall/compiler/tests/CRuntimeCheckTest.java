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
 * For every checked-in c/*.expected file, run `gcc -fsyntax-only` and assert exit 0.
 * Warnings (implicit function declarations, etc.) are suppressed via -Wno-*; only
 * actual syntax errors fail the test. Skipped via Assume if gcc isn't on PATH.
 */
@RunWith(Parameterized.class)
public class CRuntimeCheckTest {

    private final String example;
    private final Path goldenPath;

    public CRuntimeCheckTest(String example, Path goldenPath) {
        this.example = example;
        this.goldenPath = goldenPath;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> cases() throws IOException {
        Path goldens = Paths.get("src/test/resources/golden/c").toAbsolutePath();
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
    public void gccAcceptsGolden() throws IOException, InterruptedException {
        Assume.assumeTrue("gcc not on PATH; skipping runtime check", gccAvailable());
        Path tmp = Files.createTempFile("waterfall-c-", ".c");
        try {
            Files.copy(goldenPath, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            ProcessBuilder pb = new ProcessBuilder(
                    "gcc", "-fsyntax-only",
                    "-Wno-implicit-function-declaration",
                    "-Wno-int-conversion",
                    "-Wno-return-type",
                    tmp.toString());
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            byte[] output = proc.getInputStream().readAllBytes();
            int exit = proc.waitFor();
            assertEquals("gcc -fsyntax-only failed for " + example + ":\n"
                    + new String(output), 0, exit);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static boolean gccAvailable() {
        try {
            Process p = new ProcessBuilder("gcc", "--version").redirectErrorStream(true).start();
            int exit = p.waitFor();
            return exit == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}
