import kotlin.text.Regex

fun main(args: Array<String>) {
    val pattern = "+917096346*"
    val sb = StringBuilder("^")
    for (char in pattern) {
        when (char) {
            '*'  -> sb.append(".*")
            '?'  -> sb.append(".")
            '.', '+', '^', '$', '{', '}', '[', ']', '(', ')', '|', '\\' ->
                sb.append("\\$char")
            else -> sb.append(char)
        }
    }
    sb.append("$")
    println(sb.toString())
    println(Regex(sb.toString()).matches("+917096346999"))
}
