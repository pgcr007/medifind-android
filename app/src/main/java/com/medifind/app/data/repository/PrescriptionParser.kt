package com.medifind.app.data.repository

object PrescriptionParser {

    // Column headers and boilerplate that show up on printed pharmacy bills but are
    // never medicine names. Matched case-insensitively against the whole trimmed line.
    private val BLOCKLIST_EXACT = setOf(
        "QTY", "DESCRIPTION", "COM", "BATCH", "EXP", "AMOUNT", "DATE",
        "CASH MEMO", "CASH MEMO NO", "NET AMOUNT", "GROSS", "DISC"
    )

    private val BLOCKLIST_CONTAINS = listOf(
        "please check", "get well soon", "medical stores", "cash memo",
        "net amount", "gross", "disc.", "shop no", "chs.", "tel."
    )

    private val LABEL_LINE_REGEX = Regex("""^(name|doctor)\s*:""", RegexOption.IGNORE_CASE)
    private val PHONE_REGEX = Regex("""\d{10}|MOB[-\s]?\d+""", RegexOption.IGNORE_CASE)
    private val DATE_REGEX = Regex("""\d{1,2}/\d{1,2}/\d{2,4}""")

    // Short pure-alpha tokens like COM, CRN, TFL, GLE, EMI — column codes, not medicines.
    private val SHORT_CODE_REGEX = Regex("""^[A-Za-z]{2,4}$""")

    fun extractCandidateLines(rawText: String): List<String> {
        return rawText
            .split("\n")
            .map { it.trim() }
            .filter { it.length >= 3 }
            .filterNot { LABEL_LINE_REGEX.containsMatchIn(it) }
            .filterNot { PHONE_REGEX.containsMatchIn(it) }
            .filterNot { DATE_REGEX.containsMatchIn(it) }
            .filterNot { it.uppercase() in BLOCKLIST_EXACT }
            .filterNot { line -> BLOCKLIST_CONTAINS.any { kw -> line.lowercase().contains(kw) } }
            .filterNot { SHORT_CODE_REGEX.matches(it) }
            .filterNot { line -> line.count { c -> c.isDigit() } > line.count { c -> c.isLetter() } }
            .distinct()
    }
}