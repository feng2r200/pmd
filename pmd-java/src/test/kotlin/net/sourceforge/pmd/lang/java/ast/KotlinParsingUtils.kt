package net.sourceforge.pmd.lang.java.ast

import io.kotlintest.Matcher
import io.kotlintest.Result
import io.kotlintest.specs.AbstractFunSpec
import net.sourceforge.pmd.lang.ast.Node
import net.sourceforge.pmd.lang.ast.test.NWrapper
import net.sourceforge.pmd.lang.ast.test.matchNode
import net.sourceforge.pmd.lang.java.ParserTstUtil


/**
 * Represents the different Java language versions.
 */
enum class JavaVersion : Comparable<JavaVersion> {
    J1_3, J1_4, J1_5, J1_6, J1_7, J1_8, J9, J10, J11;

    val pmdName: String = name.removePrefix("J").replace('_', '.')

    /**
     * Overloads the range operator, e.g. (`J9..J11`).
     * If both operands are the same, a singleton is returned.
     */
    operator fun rangeTo(last: JavaVersion): List<JavaVersion> =
            when {
                last == this -> listOf(this)
                last.ordinal > this.ordinal -> values().filter { ver -> ver >= this && ver <= last }
                else -> values().filter { ver -> ver <= this && ver >= last }
            }

    companion object {
        val Latest = values().last()
    }
}


/**
 * Specify several tests at once for different java versions.
 * One test will be generated per version in [javaVersions].
 *
 * @param name Name of the test. Will be postfixed by the specific
 *             java version used to run it
 * @param javaVersions Language versions for which to generate tests
 * @param assertions Assertions and further configuration
 *                   to perform with the parsing context
 */

fun AbstractFunSpec.parserTest(name: String,
                               javaVersions: List<JavaVersion>,
                               assertions: ParsingCtx.() -> Unit) {

    javaVersions.forEach {
        test("$name (Java ${it.pmdName})") {
            ParsingCtx(it).assertions()
        }
    }
}

/**
 * Specify a new test for a single java version.
 *
 * @param name Name of the test. Will be postfixed by the [javaVersion]
 * @param javaVersion Language version to use when parsing
 * @param assertions Assertions and further configuration
 *                   to perform with the parsing context
 */
fun AbstractFunSpec.parserTest(name: String,
                               javaVersion: JavaVersion = JavaVersion.Latest,
                               assertions: ParsingCtx.() -> Unit) {
    parserTest(name, listOf(javaVersion), assertions)
}


data class ParsingCtx(val javaVersion: JavaVersion = JavaVersion.Latest,
                      val importedTypes: MutableList<Class<*>> = mutableListOf(),
                      val otherImports: MutableList<String> = mutableListOf()) {

    private val imports: List<String>
        get() {
            val types = importedTypes.mapNotNull { it.canonicalName }.map { "import $it;" }
            return types + otherImports.map { "import $it;" }
        }

    inline fun <reified N : Node> matchExpr(ignoreChildren: Boolean = false,
                                            noinline nodeSpec: NWrapper<N>.() -> Unit): Matcher<String> =
            object : Matcher<String> {
                override fun test(value: String): Result =
                        matchNode(ignoreChildren, nodeSpec).test(parseExpression<N>(value))

            }

    inline fun <reified N : Node> matchStmt(ignoreChildren: Boolean = false,
                                            noinline nodeSpec: NWrapper<N>.() -> Unit) =
            object : Matcher<String> {

                override fun test(value: String): Result =
                        matchNode(ignoreChildren, nodeSpec).test(parseStatement<N>(value))

            }


    /**
     * Parse the string in an expression context. The parsed expression is the child of the
     * returned [ASTExpression]. Sometimes there may be many layers between the returned node
     * and the node of interest, e.g. when the expression is contained in a PrimaryExpression/PrimaryPrefix
     * construct, in which case [parseExpression] saves some keystrokes.
     *
     * @param expr The expression to parse
     *
     * @return An [ASTExpression] whose child is the given expression
     *
     * @throws ParseException If the argument is no valid expression for the given language version
     */
    fun parseAstExpression(expr: String): ASTExpression {

        val source = """
        ${imports.joinToString(separator = "\n")}
        class Foo {
            {
              Object o = $expr;
            }
        }
    """.trimIndent()

        val acu = ParserTstUtil.parseAndTypeResolveJava(javaVersion.pmdName, source)

        return acu.getFirstDescendantOfType(ASTVariableInitializer::class.java).jjtGetChild(0) as ASTExpression
    }


    /**
     * Parse the string in a statement context. The parsed statement is the child of the
     * returned [ASTBlockStatement]. Note that [parseStatement] can save you some keystrokes
     * because it finds a descendant of the wanted type.
     *
     * @param statement The statement to parse
     *
     * @return An [ASTBlockStatement] whose child is the given statement
     *
     * @throws ParseException If the argument is no valid statement for the given language version.
     *                        Don't forget the semicolon!
     */
    fun parseAstStatement(statement: String): ASTBlockStatement {

        // place the param in a statement parsing context
        val source = """
            ${imports.joinToString(separator = "\n")}
            class Foo {
               {
                 $statement
               }
            }
        """.trimIndent()

        val root = ParserTstUtil.parseAndTypeResolveJava(javaVersion.pmdName, source)

        return root.getFirstDescendantOfType(ASTBlockStatement::class.java)
    }


    /**
     * Parse the string in an expression context, and finds the first descendant of type [N].
     * The descendant is searched for by [findFirstNodeOnStraightLine], to prevent accidental
     * mis-selection of a node. In such a case, a [NoSuchElementException] is thrown, and you
     * should fix your test case.
     *
     * @param expr The expression to parse
     * @param N type of node to find
     *
     * @return The first descendant of type [N] found in the parsed expression
     *
     * @throws NoSuchElementException If no node of type [N] is found by [findFirstNodeOnStraightLine]
     * @throws ParseException If the expression is no valid expression for the given language version
     */
    inline fun <reified N : Node> parseExpression(expr: String): N =
            parseAstExpression(expr).findFirstNodeOnStraightLine(N::class.java)
            ?: throw NoSuchElementException("No node of type ${N::class.java.simpleName} in the given expression:\n\t$expr")


    /**
     * Parse the string in a statement context, and finds the first descendant of type [N].
     * The descendant is searched for by [findFirstNodeOnStraightLine], to prevent accidental
     * mis-selection of a node. In such a case, a [NoSuchElementException] is thrown, and you
     * should fix your test case.
     *
     * @param stmt The statement to parse
     * @param N The type of node to find
     *
     * @return The first descendant of type [N] found in the parsed expression
     *
     * @throws NoSuchElementException If no node of type [N] is found by [findFirstNodeOnStraightLine]
     * @throws ParseException If the argument is no valid statement for the given language version.
     *                        Don't forget the semicolon!
     */
    inline fun <reified N : Node> parseStatement(stmt: String): N =
            parseAstStatement(stmt).findFirstNodeOnStraightLine(N::class.java)
            ?: throw NoSuchElementException("No node of type ${N::class.java.simpleName} in the given statement:\n\t$stmt")


    /**
     * Finds the first descendant of type [N] of [this] node which is
     * accessible in a straight line. The descendant must be accessible
     * from the [this] on a path where each node has a single child.
     *
     * If one node has another child, the search is aborted and the method
     * returns null.
     */
    fun <N : Node> Node.findFirstNodeOnStraightLine(klass: Class<N>): N? {
        return when {
            klass.isInstance(this) -> {
                @SuppressWarnings("UNCHECKED_CAST")
                val n = this as N
                n
            }
            else -> if (this.jjtGetNumChildren() == 1) jjtGetChild(0).findFirstNodeOnStraightLine(klass) else null
        }
    }
}

