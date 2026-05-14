package com.aaroncoplan.waterfall.parser

/**
 * A two-element tuple used throughout the codebase (predates Kotlin's `kotlin.Pair`).
 *
 * Java callers reach the fields directly via `pair.firstVal` / `pair.secondVal` thanks to
 * the `@JvmField` annotations; Kotlin callers see them as ordinary properties.
 */
open class Pair<K, V>(
    @JvmField val firstVal: K,
    @JvmField val secondVal: V
) {
    override fun toString(): String = "($firstVal, $secondVal)"
}
