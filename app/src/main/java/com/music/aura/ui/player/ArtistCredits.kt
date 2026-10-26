package com.music.aura.ui.player

data class ArtistCredit(
    val name: String,
    val browseId: String? = null,
)

private val CREDIT_SEPARATOR = Regex("\\s*(?:,|&| and | ft\\.? | feat\\.? | x )\\s*", RegexOption.IGNORE_CASE)

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
