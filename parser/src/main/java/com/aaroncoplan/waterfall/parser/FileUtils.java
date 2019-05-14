package com.aaroncoplan.waterfall.parser;

import java.io.File;

public class FileUtils {

    public static boolean isReadableFile(final String filePath) {
        final File file = new File(filePath);
        return file.exists() && file.canRead() && file.isFile();
    }
}
