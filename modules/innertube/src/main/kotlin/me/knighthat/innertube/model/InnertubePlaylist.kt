package me.knighthat.innertube.model

import me.knighthat.innertube.response.Runs


interface InnertubePlaylist: InnertubeItem, Descriptive, AccessibleViaUrl, Continued {

    /**
     * Usually contains:
     *
     * - View count
     * - Total number of songs
     * - Total duration
     * - Or all above
     */
    val subtitle: Runs?

    /**
     * Whether the playlist is in the account's library (its bookmark toggle), `null` when
     * the page doesn't offer to save it (the account's own playlists, "Liked music"...)
     */
    val isSavedToLibrary: Boolean? get() = null

    val subtitleText: String?

    val songs: List<InnertubeSong>

    val songContinuation: String?
}