package com.aaroncoplan.waterfall.compiler.target

/**
 * Registry of available [CodeGenerator]s, indexed by the value passed to
 * `--target`. New backends register by adding one line to [REGISTRY].
 */
object Backends {

    const val DEFAULT_TARGET: String = "legacy"

    private val REGISTRY: LinkedHashMap<String, () -> CodeGenerator> = linkedMapOf(
        "legacy" to { LegacyTextBackend() },
        "js"     to { JavaScriptBackend() },
        "python" to { PythonBackend() },
        "c"      to { CBackend() }
    )

    @JvmStatic
    fun forTarget(name: String?): CodeGenerator? = REGISTRY[name]?.invoke()

    @JvmStatic
    fun knownTargetsList(): String = REGISTRY.keys.joinToString(", ")
}
