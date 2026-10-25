package com.example.lyrics

/**
 * Reads LRC lyrics (synced) or plain text.
 *
 * Supports `[mm:ss.xx]` line times (several per line), `[offset:±ms]`, metadata tags like
 * `[ar:…]`, and "enhanced" word times `<mm:ss.xx>word`.
 */
object LrcParser {
    private val timeTag = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val wordTag = Regex("""<(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?>""")
    private val metaTag = Regex("""^\[([a-zA-Z#]+):(.*)]\s*$""")

    fun parse(raw: String): Lyrics {
        val text = raw.removePrefix("﻿").replace("\r\n", "\n").replace('\r', '\n')
        var offset = 0L
        val timed = ArrayList<LyricLine>()
        var sawTime = false

        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val meta = metaTag.find(line)
            if (meta != null && !timeTag.containsMatchIn(line)) {
                if (meta.groupValues[1].equals("offset", ignoreCase = true)) {
                    offset = meta.groupValues[2].trim().toLongOrNull() ?: 0L
                }
                continue
            }
            val times = ArrayList<Long>()
            var rest = line
            while (true) {
                val m = timeTag.find(rest) ?: break
                if (m.range.first != 0) break
                times += toMs(m)
                rest = rest.substring(m.range.last + 1)
            }
            if (times.isEmpty()) continue
            sawTime = true
            val (lineText, words) = parseWords(rest)
            times.forEach { t -> timed += LyricLine(timeMs = t, text = lineText, words = words) }
        }

        if (sawTime) {
            // LRC offset: positive means lyrics appear sooner.
            val sorted = timed
                .map { it.copy(timeMs = (it.timeMs - offset).coerceAtLeast(0L), words = it.words.shift(-offset)) }
                .sortedBy { it.timeMs }
            return Lyrics(lines = fillWordEnds(sorted), synced = true)
        }
        val plain = text.lines()
            .map { it.trimEnd() }
            .filterNot { metaTag.matches(it.trim()) }
            .dropWhile { it.isBlank() }
            .dropLastWhile { it.isBlank() }
            .map { LyricLine(timeMs = -1L, text = it) }
        return Lyrics(lines = plain, synced = false)
    }

    private fun parseWords(rest: String): Pair<String, List<LyricWord>> {
        if (!wordTag.containsMatchIn(rest)) return rest.trim() to emptyList()
        val words = ArrayList<LyricWord>()
        val matches = wordTag.findAll(rest).toList()
        matches.forEachIndexed { i, m ->
            val start = toMs(m)
            val end = matches.getOrNull(i + 1)?.let { toMs(it) } ?: -1L
            val textEnd = matches.getOrNull(i + 1)?.range?.first ?: rest.length
            val word = rest.substring(m.range.last + 1, textEnd)
            if (word.isNotEmpty()) words += LyricWord(word, start, end)
        }
        val plain = wordTag.replace(rest, "").trim()
        return plain to words
    }

    /** Words without an end time end where the next line starts. */
    private fun fillWordEnds(lines: List<LyricLine>): List<LyricLine> =
        lines.mapIndexed { i, line ->
            if (line.words.isEmpty()) line
            else {
                val nextStart = lines.getOrNull(i + 1)?.timeMs ?: (line.words.last().startMs + 1500L)
                line.copy(words = line.words.map { w -> if (w.endMs < 0) w.copy(endMs = nextStart) else w })
            }
        }

    private fun List<LyricWord>.shift(by: Long) =
        map { it.copy(startMs = (it.startMs + by).coerceAtLeast(0L), endMs = if (it.endMs < 0) it.endMs else (it.endMs + by).coerceAtLeast(0L)) }

    private fun toMs(m: MatchResult): Long {
        val min = m.groupValues[1].toLong()
        val sec = m.groupValues[2].toLong()
        val frac = m.groupValues[3]
        val ms = when (frac.length) {
            0 -> 0L
            1 -> frac.toLong() * 100
            2 -> frac.toLong() * 10
            else -> frac.take(3).toLong()
        }
        return min * 60_000L + sec * 1000L + ms
    }
}
