package com.aaroncoplan.waterfall.compiler

class Type {

    val name: String

    constructor(name: String?) {
        this.name = name ?: "void"
    }
}