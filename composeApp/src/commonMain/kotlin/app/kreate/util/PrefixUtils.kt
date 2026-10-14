package app.kreate.util

const val MODIFIED_PREFIX = "modified:"
/**
 * Legacy marker of explicit songs, now carried by `Song.isExplicit`. Songs saved
 * before still have it in their title, so it's cleaned out for display.
 */
private const val LEGACY_EXPLICIT_PREFIX = "e:"


/**
 * Assumption: all prefixes end with ":" and have at least 1 (other) character.
 * Removes a "prefix of prefixes" including multiple times the same prefix (at different locations).
 *
 * Additionally, drop `isYoutubePlaylist` column because it can be determined by `browseId`
 * and doesn't require a hard-coded value
 */
fun cleanPrefix(text: String): String {
    val splitText = text.split(":")
    var i = 0
    while (i < splitText.size-1) {
        if ("${splitText[i]}:" !in listOf(MODIFIED_PREFIX, LEGACY_EXPLICIT_PREFIX)) {
            break
        }
        i++
    }
    if(i >= splitText.size) return ""
    return splitText.subList(i, splitText.size).joinToString(":")
}
