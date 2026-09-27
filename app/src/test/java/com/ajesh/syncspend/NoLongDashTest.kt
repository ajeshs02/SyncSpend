package com.ajesh.syncspend

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The app never shows the long dashes (em and en dash) in its text: sentences are punctuated normally
 * and ranges use a plain hyphen. This scans the shipped sources (code and resources, not comments) so one
 * cannot creep back in.
 */
class NoLongDashTest {
    private val longDashes = charArrayOf('—', '–')

    /** Source with block comments and end-of-line comments removed (a `//` inside a string literal is kept). */
    private fun code(text: String): String {
        val noBlocks = text.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)) { m -> "\n".repeat(m.value.count { it == '\n' }) }
        return noBlocks.lines().joinToString("\n") { line ->
            var inString = false
            var i = 0
            var cut = line.length
            while (i < line.length) {
                val c = line[i]
                if (c == '\\' && inString) i++
                else if (c == '"') inString = !inString
                else if (!inString && c == '/' && line.getOrNull(i + 1) == '/') { cut = i; break }
                i++
            }
            line.substring(0, cut)
        }
    }

    @Test fun noEmOrEnDashInCodeStrings() {
        val offenders = File("src/main/java").walkTopDown().filter { it.extension == "kt" }.flatMap { file ->
            code(file.readText()).lines().withIndex().filter { (_, line) -> line.any { it in longDashes } }
                .map { (n, line) -> "${file.path}:${n + 1}: ${line.trim()}" }
        }.toList()
        assertTrue("Long dashes in code:\n" + offenders.joinToString("\n"), offenders.isEmpty())
    }

    @Test fun noEmOrEnDashInResources() {
        val offenders = File("src/main/res").walkTopDown().filter { it.isFile && it.extension == "xml" }.flatMap { file ->
            file.readText().replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)) { m -> "\n".repeat(m.value.count { it == '\n' }) }
                .lines().withIndex().filter { (_, line) -> line.any { it in longDashes } }
                .map { (n, line) -> "${file.path}:${n + 1}: ${line.trim()}" }
        }.toList()
        assertTrue("Long dashes in resources:\n" + offenders.joinToString("\n"), offenders.isEmpty())
    }
}
