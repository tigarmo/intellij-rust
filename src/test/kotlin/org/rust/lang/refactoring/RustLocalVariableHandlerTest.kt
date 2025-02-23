package org.rust.lang.refactoring

import org.intellij.lang.annotations.Language
import org.rust.lang.RustTestCaseBase
import org.rust.lang.core.psi.impl.RustFile

class RustLocalVariableHandlerTest : RustTestCaseBase() {
    override val dataPath = "org/rust/lang/refactoring/fixtures/introduce_variable/"

    fun testExpression() = doTest("""
        fn hello() {
            foo(5 + /*caret*/10);
        }""", """
        fn hello() {
            let i = 10;
            foo(5 + i);
        }""")
    {
        val ref = refactoring()
        val targets = ref.possibleTargets()
        check(targets.size == 3)
        val expr = targets[0]
        ref.replaceElementForAllExpr(expr, listOf(expr))
    }

    fun testExplicitSelectionWorks() {
        myFixture.configureByText("main.rs", """
            fn main() { 1 + <selection>1</selection>;}
        """)
        val targets = refactoring().possibleTargets()
        check(targets.size == 1)
    }

    fun testMultipleOccurrences() = doTest("""
        fn hello() {
            foo(5 + /*caret*/10);
            foo(5 + 10);
        }

        fn foo(x: Int) {

        }""", """
        fn hello() {
            let x = 5 + 10;
            foo(x);
            foo(x);
        }

        fn foo(x: Int) {

        }""")
    {
        val ref = refactoring()
        val targets = ref.possibleTargets()
        check(targets.size == 3)
        val expr = targets[1]
        val occurrences = findOccurrences(expr)
        ref.replaceElementForAllExpr(expr, occurrences)
    }

    fun testCaretAfterElement() = doTest("""
        fn main() {
            1/*caret*/;
        }""", """
        fn main() {
            let i = 1;
        }""")
    {
        val ref = refactoring()
        val targets = ref.possibleTargets()
        check(targets.size == 1)
        ref.replaceElement(targets[0], targets)
    }

    fun testStatement() = doTest("""
        fn hello() {
            foo(5 + /*caret*/10);
        }""", """
        fn hello() {
            let foo = foo(5 + 10);
        }""")
    {
        val ref = refactoring()
        val targets = ref.possibleTargets()
        check(targets.size == 3)
        ref.replaceElement(targets[2], listOf(targets[2]))
    }

    fun testMatch() = doTest("""
        fn bar() {
            ma/*caret*/tch 5 {
                2 => 2,
                _ => 8,
            };
        }""", """
        fn bar() {
            let x = match 5 {
                2 => 2,
                _ => 8,
            };
        }""")
    {
        val ref = refactoring()
        val targets = ref.possibleTargets()
        ref.replaceElement(targets.single(), listOf(targets.single()))
    }

    fun testFile() = doTest("""
        fn read_fle() -> Result<Vec<String, io::Error>> {
            File::op/*caret*/en("res/input.txt")?
        }""", """
        fn read_fle() -> Result<Vec<String, io::Error>> {
            let x = File::open("res/input.txt")?;
        }""")
    {
        val ref = refactoring()
        val targets = ref.possibleTargets()
        check(targets.size == 2)
        ref.replaceElement(targets[1], listOf(targets[1]))
    }

    fun testRefMut() = doTest("""
        fn read_file() -> Result<String, Error> {
            let file = File::open("res/input.txt")?;

            file.read_to_string(&mut String:/*caret*/:new())?;
        }""", """
        fn read_file() -> Result<String, Error> {
            let file = File::open("res/input.txt")?;
            let mut string = String::new();

            file.read_to_string(&mut string)?;
        }""")
    {
        val ref = refactoring()
        val targets = ref.possibleTargets()
        ref.replaceElement(targets[0], listOf(targets[0]))
    }

    private fun doTest(@Language("Rust") before: String, @Language("Rust") after: String, action: () -> Unit) {
        InlineFile(before).withCaret()
        openFileInEditor("main.rs")
        action()
        myFixture.checkResult(after)
    }

    private fun refactoring(): RustIntroduceVariableRefactoring =
        RustIntroduceVariableRefactoring(project, myFixture.editor, myFixture.file as RustFile)
}
