package pt.vcc.vccmusic.data

private val parentheticalText = Regex("\\s*\\([^)]*\\)")

/** Remove notas entre parênteses dos textos apresentados ao utilizador. */
fun String.withoutParentheticalText(): String =
    replace(parentheticalText, " ").replace(Regex("\\s+"), " ").trim()
