package graphecule.client.builder


private val KEYWORDS = setOf(
    "package",
    "as",
    "typealias",
    "class",
    "this",
    "super",
    "val",
    "var",
    "fun",
    "for",
    "null",
    "true",
    "false",
    "is",
    "in",
    "throw",
    "return",
    "break",
    "continue",
    "object",
    "if",
    "try",
    "else",
    "while",
    "do",
    "when",
    "interface",
    "typeof"
)
internal val String.isKeyword get() = KEYWORDS.contains(this)
internal fun escapeIfKeyword(value: String) = if (value.isKeyword) "`$value`" else value
internal fun String.removeQuotes() = removeSurrounding("\"")