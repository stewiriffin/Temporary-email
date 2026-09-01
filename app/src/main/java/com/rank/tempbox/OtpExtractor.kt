package com.rank.tempbox

object OtpExtractor {

    private val PATTERNS = listOf(
        Regex("""\b(?i)(?:code|otp|verification|passcode|pin|token is?:?)\s*([A-Z0-9]{4,8})\b"""),
        Regex("""\b\d{4,6}\b""")
    )

    fun extract(text: String): String? {
        for (pattern in PATTERNS) {
            val match = pattern.find(text) ?: continue
            val candidate = match.groupValues.getOrNull(1) ?: match.value
            if (candidate.length in 4..8) return candidate
        }
        return null
    }

    fun extract(html: String?, text: String?): String? {
        text?.let { extract(it) }?.let { return it }
        html?.let { extract(stripHtml(it)) }?.let { return it }
        return null
    }

    private fun stripHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
