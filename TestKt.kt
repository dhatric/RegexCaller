import kotlin.text.Regex

fun main() {
    val char = '+'
    val s = "\\$char"
    println("length: " + s.length)
    println("chars: " + s.toCharArray().toList())
}
