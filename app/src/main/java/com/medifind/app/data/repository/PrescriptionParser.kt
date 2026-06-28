package com.medifind.app.data.repository

object PrescriptionParser {

    fun extractCandidateLines(rawText: String): List<String> {
        return rawText
            .split("\n")
            .map { it.trim() }
            .filter { it.length >= 3 } // filter out noise/very short fragments
            .distinct()
    }
}