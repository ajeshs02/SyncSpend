package com.ajesh.syncspend.csv

import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.domain.model.FlowType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import kotlin.math.abs
import kotlin.math.roundToLong

/** One valid CSV row. [amount] is positive; [categoryName] is never blank (see [CsvImportParser.UNCATEGORIZED]). */
data class ParsedRow(
    val line: Int,
    val date: LocalDate,
    val type: FlowType,
    val categoryName: String,
    val description: String,
    val amount: Double,
)

data class ParseOutcome(
    val rows: List<ParsedRow>,
    /** Rows that couldn't be read (bad date/type/amount, too few columns). */
    val skipped: Int,
    /** First few human-readable reasons, capped so a huge bad file can't flood the dialog. */
    val problems: List<String>,
    /** Non-null when the header is unusable; [rows] is empty then. */
    val headerError: String? = null,
)

/**
 * Pure CSV -> rows step of the import (no Android, no database), so it can be
 * unit-tested. It is deliberately forgiving: columns are found by header name
 * in any order, with a few aliases, so both our own export
 * (`date,type,category,description,amount`) and the previous tracker's
 * (`Date,Type,Category,Amount,Note`) import.
 */
object CsvImportParser {
    const val UNCATEGORIZED = "Uncategorized"
    private const val MAX_PROBLEMS = 5

    private val dateAliases = setOf("date", "day")
    private val typeAliases = setOf("type", "kind", "flow")
    private val categoryAliases = setOf("category", "cat")
    private val amountAliases = setOf("amount", "amt", "value")
    private val descriptionAliases = setOf("description", "note", "notes", "memo", "details", "desc")

    // STRICT so "2026-02-31" is rejected rather than silently clamped to the 28th.
    private val dateFormats = listOf("uuuu-M-d", "d/M/uuuu", "d-M-uuuu").map {
        DateTimeFormatter.ofPattern(it).withResolverStyle(ResolverStyle.STRICT)
    }

    /** UTF-8 (with or without BOM) or UTF-16 with BOM — what spreadsheet apps and other trackers write. */
    fun decode(bytes: ByteArray): String = when {
        bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        else -> String(bytes, Charsets.UTF_8).removePrefix("\uFEFF")
    }

    fun parse(text: String): ParseOutcome {
        val clean = text.removePrefix("\uFEFF")
        val delimiter = sniffDelimiter(clean)
        val table = CsvFormat.parse(clean, delimiter)
        if (table.isEmpty()) return ParseOutcome(emptyList(), 0, emptyList(), headerError = "The file is empty.")

        val header = table.first().map { it.trim().lowercase().removePrefix("\uFEFF") }
        fun col(aliases: Set<String>) = header.indexOfFirst { it in aliases }
        val dateCol = col(dateAliases)
        val typeCol = col(typeAliases)
        val categoryCol = col(categoryAliases)
        val amountCol = col(amountAliases)
        val descriptionCol = col(descriptionAliases)

        val missing = buildList {
            if (dateCol < 0) add("Date")
            if (typeCol < 0) add("Type")
            if (amountCol < 0) add("Amount")
        }
        if (missing.isNotEmpty()) {
            val found = table.first().joinToString(", ") { it.trim() }.ifBlank { "none" }
            return ParseOutcome(
                emptyList(), 0, emptyList(),
                headerError = "This doesn't look like an expense CSV — missing column: ${missing.joinToString(", ")}. Found: $found.",
            )
        }

        val rows = ArrayList<ParsedRow>(table.size)
        var skipped = 0
        val problems = mutableListOf<String>()
        val needed = maxOf(dateCol, typeCol, amountCol)

        table.drop(1).forEachIndexed { index, r ->
            val line = index + 2
            fun skip(reason: String) {
                skipped++
                if (problems.size < MAX_PROBLEMS) problems += "Row $line: $reason"
            }
            if (r.size <= needed) return@forEachIndexed skip("too few columns")
            val date = parseDate(r[dateCol].trim()) ?: return@forEachIndexed skip("bad date \"${r[dateCol].trim()}\"")
            val type = when (r[typeCol].trim().uppercase()) {
                "EXPENSE" -> FlowType.EXPENSE
                "INCOME" -> FlowType.INCOME
                else -> return@forEachIndexed skip("bad type \"${r[typeCol].trim()}\"")
            }
            val amount = parseAmount(r[amountCol]) ?: return@forEachIndexed skip("bad amount \"${r[amountCol].trim()}\"")
            val category = r.getOrNull(categoryCol)?.trim().orEmpty().ifEmpty { UNCATEGORIZED }
            val description = r.getOrNull(descriptionCol)?.trim().orEmpty()
            rows += ParsedRow(line, date, type, category, description, amount)
        }
        return ParseOutcome(rows, skipped, problems)
    }

    /** Picks `,` `;` or tab from the header line, whichever appears most (defaults to comma). */
    private fun sniffDelimiter(text: String): Char {
        val firstLine = text.lineSequence().firstOrNull().orEmpty()
        return listOf(',', ';', '\t').maxByOrNull { d -> firstLine.count { it == d } }
            ?.takeIf { d -> firstLine.contains(d) } ?: ','
    }

    private fun parseDate(raw: String): LocalDate? {
        for (format in dateFormats) {
            try {
                return LocalDate.parse(raw, format)
            } catch (_: DateTimeParseException) {
                // try the next format
            }
        }
        return null
    }

    /** "₹1,250.50" / " 450 " / "-320" -> positive amount; null for blank, zero or unparsable. */
    private fun parseAmount(raw: String): Double? {
        val cleaned = raw.filter { it.isDigit() || it == '.' || it == '-' }
        val value = cleaned.toDoubleOrNull() ?: return null
        val abs = abs((value * 100).roundToLong() / 100.0)
        return abs.takeIf { it > 0 }
    }
}

/**
 * Multiset duplicate detection for re-imports: each existing transaction can
 * "absorb" one identical incoming row, so importing the same file twice adds
 * nothing, while two genuinely identical rows inside one file (two coffees, same
 * day, same price) both come through the first time.
 */
class DuplicateTracker(existing: List<TransactionEntity>) {
    private val counts = HashMap<String, Int>()

    init {
        existing.forEach { counts.merge(key(it.date, it.amount, it.categoryId, it.description), 1, Int::plus) }
    }

    /** True (and consumes one match) when an identical transaction already exists. */
    fun consumeIfDuplicate(date: LocalDate, signedAmount: Double, categoryId: Long, description: String): Boolean {
        val k = key(date, signedAmount, categoryId, description)
        val left = counts[k] ?: return false
        if (left <= 0) return false
        counts[k] = left - 1
        return true
    }

    private fun key(date: LocalDate, signedAmount: Double, categoryId: Long, description: String) =
        "$date|${(signedAmount * 100).roundToLong()}|$categoryId|${description.trim()}"
}
