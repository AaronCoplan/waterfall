package com.aaroncoplan.waterfall.compiler.argumentparsing

import com.aaroncoplan.waterfall.compiler.target.Backends
import com.aaroncoplan.waterfall.parser.Pair
import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.helper.HelpScreenException
import net.sourceforge.argparse4j.inf.ArgumentParserException

object ArgParser {

    @JvmStatic
    fun parseCommandLineArgs(args: Array<String>): Pair<Arguments?, String?> {
        val argumentParser = ArgumentParsers.newFor("waterfall")
            .build()
            .defaultHelp(true)
            .description("Waterfall programming language")

        argumentParser.addArgument("files")
            .nargs("+")
            .help("List of files to compile")

        argumentParser.addArgument("--target")
            .setDefault(Backends.DEFAULT_TARGET)
            .help("Target language to emit. Known: ${Backends.knownTargetsList()}")

        return try {
            val namespace = argumentParser.parseArgs(args)
            val target = namespace.getString("target")
            if (Backends.forTarget(target) == null) {
                Pair(null, "Unknown target '$target'. Known targets: ${Backends.knownTargetsList()}")
            } else {
                Pair(Arguments(namespace.getList("files"), target), null)
            }
        } catch (e: ArgumentParserException) {
            val errorMessage = if (e is HelpScreenException) "" else e.message
            Pair(null, errorMessage)
        }
    }
}
