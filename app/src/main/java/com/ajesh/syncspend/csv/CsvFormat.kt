package com.ajesh.syncspend.csv

/**
 * The one CSV schema the app reads and writes:
 * `date,type,category,description,amount` — date yyyy-MM-dd, type
 * EXPENSE|INCOME, amount unsigned with 2 decimals.
 */
object CsvFormat {
    val HEADER = listOf("date", "type", "category", "description", "amount")

    fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else field

    fun row(fields: List<String>): String = fields.joinToString(",") { escape(it) }

    /**
     * Full RFC-4180-style parse (quoted fields, escaped quotes, embedded
     * delimiters/newlines). [delimiter] is `,` for our own files; the importer
     * passes `;` or tab when it sniffs a spreadsheet export in another locale.
     */
    fun parse(text: String, delimiter: Char = ','): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        var fieldStarted = false
        fun endField() { row.add(field.toString()); field.setLength(0); fieldStarted = false }
        fun endRow() {
            endField()
            if (!(row.size == 1 && row[0].isEmpty())) rows.add(row)
            row = mutableListOf()
        }
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes -> when {
                    c == '"' && i + 1 < text.length && text[i + 1] == '"' -> { field.append('"'); i++ }
                    c == '"' -> inQuotes = false
                    else -> field.append(c)
                }
                c == '"' && !fieldStarted -> { inQuotes = true; fieldStarted = true }
                c == delimiter -> endField()
                c == '\r' -> { if (i + 1 < text.length && text[i + 1] == '\n') i++; endRow() }
                c == '\n' -> endRow()
                else -> { field.append(c); fieldStarted = true }
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) endRow()
        return rows
    }
}
