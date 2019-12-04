package com.aaroncoplan.parsertests.test

import org.junit.Test

class ModuleDeclarationTest : ParserTest() {
    @Test
    fun testEmptyModulePasses() {        
        assertParsePasses(            
            """
            module name {}
            """
        )
    }

    @Test
    fun testModuleWithIllegalNameFails() {
        assertParseFails(
            """
            module 123name {}
            """
        )
    }

    @Test
    fun testModuleWithNewlinesPasses() {
        assertParsePasses(
            """
            module name {
                


            }
            """
        )
    }

    @Test
    fun testModuleWithSpacesPasses() {
        assertParsePasses(
            """
            module name {      }
            """
        )
    }

    @Test
    fun testModuleWithNewlinesAndSpacesPasses() {
        assertParsePasses(
            """
            module name {




                    }
            """            
        )
    }
}