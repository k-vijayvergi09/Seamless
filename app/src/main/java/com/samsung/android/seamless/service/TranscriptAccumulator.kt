package com.samsung.android.seamless.service

class TranscriptAccumulator {

    private var committed = ""
    private var active = ""

    fun reset(seedTranscript: String = "") {
        committed = normalizeSpaces(seedTranscript)
        active = ""
    }

    fun onData(chunk: String): String {
        active = mergeTranscriptChunks(active, chunk)
        return combined()
    }

    fun onEndSpeech(): String {
        committed = mergeTranscriptChunks(committed, active)
        active = ""
        return committed
    }

    fun combined(): String = mergeTranscriptChunks(committed, active)

    internal fun mergeTranscriptChunks(existing: String, incoming: String): String {
        val base = normalizeSpaces(existing)
        val next = normalizeSpaces(incoming)
        if (base.isBlank()) return next
        if (next.isBlank()) return base
        if (base == next) return base
        if (next.startsWith(base)) return next
        if (base.startsWith(next)) return base

        val overlap = suffixPrefixOverlap(base, next)
        val merged = if (overlap > 0) {
            "$base ${next.drop(overlap)}".trim()
        } else {
            "$base $next"
        }
        return normalizeSpaces(merged)
    }

    private fun normalizeSpaces(input: String): String =
        input.trim().replace(Regex("\\s+"), " ")

    private fun suffixPrefixOverlap(left: String, right: String): Int {
        val max = minOf(left.length, right.length)
        for (len in max downTo 1) {
            if (left.takeLast(len) == right.take(len)) return len
        }
        return 0
    }
}
