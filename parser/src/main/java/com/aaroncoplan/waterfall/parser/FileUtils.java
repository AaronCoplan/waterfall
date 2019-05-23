package com.aaroncoplan.waterfall.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class FileUtils {

    public static Pair<Boolean, String> isReadableFile(final String filePath) {
        final File file = new File(filePath);
        if (!file.exists()) {
            return new Pair<>(false, String.format("File '%s' does not exist", filePath));
        }
        if (!file.canRead()) {
            return new Pair<>(false, String.format("File '%s' cannot be read", filePath));
        }
        if (!file.isFile()) {
            return new Pair<>(false, String.format("File '%s' is not a file", filePath));
        }

        return new Pair<>(true, null);
    }

    static String readFile(final String filePath) {
        final StringBuilder contents = new StringBuilder();
        try {
            Scanner scanner = new Scanner(new File(filePath));
            while (scanner.hasNextLine()) {
                contents.append(scanner.nextLine());
                contents.append('\n');
            }
            scanner.close();
        } catch(FileNotFoundException e) {
            return null;
        }

        return contents.toString();
    }
}
