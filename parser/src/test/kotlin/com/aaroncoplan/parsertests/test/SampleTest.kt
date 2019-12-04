package com.aaroncoplan.parsertests.test

import org.junit.Test

class SampleTest : ParserTest() {
    @Test
    fun myTest() {        
        assertParsePasses(
            /*"""
            module {
                func name(string [] name) {
                    name := 4;
                }
            }
            """*/
            """
            module {}
            """
        )
    }

    @Test
    fun myTest2() {
        assertParsePasses(
            """
            module {
                func a(string a){}
            }
            """
        )
    }

    @Test
    fun myTest3() {
        assertParsePasses(
            """
            module {
                func a(string a) returns string {}
            }
            """
        )
    }
}