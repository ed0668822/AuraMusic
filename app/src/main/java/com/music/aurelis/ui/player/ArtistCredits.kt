package com.music.aurelis.ui.player

/**
 * One credit in an artist line.
 */
data class ArtistCredit(
    val name: String,
    /** Null for local music: no page to navigate to. */
    val browseId: String? = null,
)

/**
 * The credits to draw for a track.
 *
 * For local music the artist field is a plain string. Split on common
 * separators so each name can be displayed individually in the marquee.
 */
fun artistCredits(artist: String): List<ArtistCredit> {
    val text = artist.trim()
    if (text.isEmpty()) return emptyList()

    val cuts = CREDIT_SEPARATOR.findAll(text).toList()
    if (cuts.isEmpty()) {
        return listOf(ArtistCredit(name = text))
    }

    val out = mutableListOf<ArtistCredit>()
    var start = 0
    for (cut in cuts) {
        val name = text.substring(start, cut.range.first).trim()
        if (name.isNotEmpty()) {
            out += ArtistCredit(name = name)
        }
        start = cut.range.last + 1
    }
    val tail = text.substring(start).trim()
    if (tail.isNotEmpty()) out += ArtistCredit(name = tail)
    if (out.isEmpty()) out += ArtistCredit(name = text)
    return out
}

/** Common separators in artist bylines: comma, ampersand, "feat.", "ft.", "with", " x ". */
private val CREDIT_SEPARATOR = Regex(
    """\s*(?:,|&|\bfeat\.?\b|\bft\.?\b|\bwith\b|\s+x\s+)\s*""",
    RegexOption.IGNORE_CASE,
)
