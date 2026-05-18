package com.aaroncoplan.waterfall.compiler.symboltables

import com.aaroncoplan.waterfall.compiler.statements.helpers.SourcePosition
import com.aaroncoplan.waterfall.compiler.typesystem.WaterfallType
import org.junit.Test
import org.junit.Assert.*

class SymbolTableTest {

    private fun pos(line: Int = 1, col: Int = 0) =
        SourcePosition(fileName = "test.wf", line = line, column = col)

    private fun varInfo(type: WaterfallType, readonly: Boolean = false) =
        SymbolInfo(type, readonly, SymbolKind.Variable, pos())

    @Test fun lookupMissingReturnsNull() {
        val st = SymbolTable()
        assertNull(st.lookup("nope"))
    }

    @Test fun declareThenLookup() {
        val st = SymbolTable()
        st.declare("x", varInfo(WaterfallType.IntType))
        assertEquals(WaterfallType.IntType, st.lookup("x")?.type)
        assertFalse(st.lookup("x")!!.isReadonly)
    }

    @Test fun duplicateInSameScopeFails() {
        val st = SymbolTable()
        st.declare("x", varInfo(WaterfallType.IntType))
        val result = st.declare("x", varInfo(WaterfallType.DecType))
        assertTrue(result is DeclareResult.Failure)
    }

    @Test fun shadowingAncestorFails() {
        val parent = SymbolTable()
        parent.declare("x", varInfo(WaterfallType.IntType))
        val child = parent.enterScope()
        val result = child.declare("x", varInfo(WaterfallType.DecType))
        assertTrue(result is DeclareResult.Failure)
    }

    @Test fun childLookupSeesAncestor() {
        val parent = SymbolTable()
        parent.declare("x", varInfo(WaterfallType.IntType))
        val child = parent.enterScope()
        assertEquals(WaterfallType.IntType, child.lookup("x")?.type)
    }

    @Test fun markReadonlyLocalAffectsChild() {
        val parent = SymbolTable()
        parent.declare("x", varInfo(WaterfallType.IntType))
        val child = parent.enterScope()
        child.markReadonlyLocal("x")
        assertTrue(child.lookup("x")!!.isReadonly)
    }

    @Test fun markReadonlyLocalDoesNotAffectParent() {
        val parent = SymbolTable()
        parent.declare("x", varInfo(WaterfallType.IntType))
        val child = parent.enterScope()
        child.markReadonlyLocal("x")
        assertFalse(parent.lookup("x")!!.isReadonly)
    }

    @Test fun markReadonlyLocalDoesNotAffectSibling() {
        val parent = SymbolTable()
        parent.declare("x", varInfo(WaterfallType.IntType))
        val child1 = parent.enterScope()
        child1.markReadonlyLocal("x")
        parent.exitScope(child1)
        val child2 = parent.enterScope()
        assertFalse(child2.lookup("x")!!.isReadonly)
    }

    @Test fun commitReadonlyIsVisibleInInvokingScope() {
        // After commitReadonly on a scope, lookups from THAT scope (and its
        // descendants) return readonly. Round-4 F8 fix: the commit is local
        // to the invoking scope; siblings of the invoking scope are unaffected.
        // See `commitReadonlyDoesNotLeakToSibling` below for the sibling case.
        val parent = SymbolTable()
        parent.declare("x", varInfo(WaterfallType.IntType))
        parent.commitReadonly(setOf("x"))
        assertTrue(parent.lookup("x")!!.isReadonly)
    }

    @Test fun exitScopeReturnsLocalShadow() {
        val parent = SymbolTable()
        parent.declare("x", varInfo(WaterfallType.IntType))
        parent.declare("y", varInfo(WaterfallType.IntType))
        val child = parent.enterScope()
        child.markReadonlyLocal("x")
        val snap = parent.exitScope(child)
        assertEquals(setOf("x"), snap)
    }

    @Test fun lookupReturnsReadonlyWhenAncestorShadowed() {
        val grandparent = SymbolTable()
        grandparent.declare("x", varInfo(WaterfallType.IntType))
        val parent = grandparent.enterScope()
        parent.markReadonlyLocal("x")
        val child = parent.enterScope()
        // The shadow is on parent; child's lookup must see it.
        assertTrue(child.lookup("x")!!.isReadonly)
    }

    @Test fun functionKindIsDistinguishable() {
        val st = SymbolTable()
        val fnInfo = SymbolInfo(
            type = WaterfallType.IntType,
            isReadonly = true,
            kind = SymbolKind.Function(parameters = listOf("a" to WaterfallType.IntType)),
            sourcePosition = pos()
        )
        st.declare("add", fnInfo)
        val looked = st.lookup("add")
        assertTrue(looked!!.kind is SymbolKind.Function)
    }

    /**
     * Walk-depth boundary test (round-4 F8 fix). `commitReadonly` on an
     * intermediate scope must not leak the readonly state to siblings of
     * that scope (which would happen if commitReadonly walked up to the
     * owning scope and mutated `owned` there).
     */
    @Test fun commitReadonlyDoesNotLeakToSibling() {
        val grandparent = SymbolTable()
        grandparent.declare("x", varInfo(WaterfallType.IntType))
        val outerThen = grandparent.enterScope()
        outerThen.commitReadonly(setOf("x"))    // simulates an inner if/else join's commit
        // From outerThen and its descendants, x is readonly.
        assertTrue(outerThen.lookup("x")!!.isReadonly)
        val deeper = outerThen.enterScope()
        assertTrue(deeper.lookup("x")!!.isReadonly)
        // From a sibling of outerThen — what an outer-else branch would see — x is mutable.
        val outerElse = grandparent.enterScope()
        assertFalse(outerElse.lookup("x")!!.isReadonly)
        // From the grandparent itself, x is still mutable.
        assertFalse(grandparent.lookup("x")!!.isReadonly)
    }

    @Test fun exitScopeWithWrongParentFailsLoudly() {
        val a = SymbolTable()
        val b = SymbolTable()
        val childOfA = a.enterScope()
        try {
            b.exitScope(childOfA)
            fail("exitScope should throw IllegalArgumentException when child.parent !== this")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test fun functionParametersPreserveNameTypeOrdering() {
        val st = SymbolTable()
        val fnInfo = SymbolInfo(
            type = WaterfallType.IntType,
            isReadonly = true,
            kind = SymbolKind.Function(parameters = listOf(
                "a" to WaterfallType.IntType,
                "b" to WaterfallType.DecType
            )),
            sourcePosition = pos()
        )
        st.declare("add", fnInfo)
        val looked = st.lookup("add")
        val fnKind = looked!!.kind as SymbolKind.Function
        assertEquals(listOf("a" to WaterfallType.IntType, "b" to WaterfallType.DecType), fnKind.parameters)
    }
}
