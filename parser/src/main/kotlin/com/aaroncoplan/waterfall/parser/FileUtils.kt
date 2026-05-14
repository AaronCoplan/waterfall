package com.aaroncoplan.waterfall.parser

import java.io.File
import java.io.FileNotFoundException
import java.util.Scanner

object FileUtils {

    @JvmStatic
    fun isReadableFile(filePath: String): Pair<Boolean, String?> {
        val file = File(filePath)
        if (!file.exists()) {
            return Pair(false, "File '$filePath' does not exist")
        }
        if (!file.canRead()) {
            return Pair(false, "File '$filePath' cannot be read")
        }
        if (!file.isFile) {
            return Pair(false, "File '$filePath' is not a file")
        }
        return Pair(true, null)
    }

    /** Package-private in the Java version; kept internal here for FileParser only. */
    @JvmStatic
    internal fun readFile(filePath: String): String? {
        val contents = StringBuilder()
        return try {
            Scanner(File(filePath)).use { scanner ->
                while (scanner.hasNextLine()) {
                    contents.append(scanner.nextLine())
                    contents.append('\n')
                }
            }
            contents.toString()
        } catch (e: FileNotFoundException) {
            null
        }
    }
}
