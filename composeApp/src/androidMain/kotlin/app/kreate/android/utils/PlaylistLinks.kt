package app.kreate.android.utils

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import app.kreate.android.Preferences
import app.kreate.android.utils.innertube.CURRENT_LOCALE
import app.kreate.android.utils.innertube.toMediaItem
import co.touchlab.kermit.Logger
import it.fast4x.rimusic.ui.screens.settings.isYouTubeLoggedIn
import it.fast4x.rimusic.utils.forcePlayAtIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.knighthat.innertube.Innertube

/**
 * Plays the playlist of a `watch?v=…&list=…` link (`shuffle=1` shuffles it),
 * from the linked song when it's in the first page. Radios (`RD…`, except
 * YouTube's own playlists) aren't playlists: they're left to the caller.
 *
 * @return `false` when the link isn't a playlist or it couldn't be loaded
 */
suspend fun playYouTubePlaylistLink(
    player: Player,
    listId: String,
    videoId: String?,
    shuffle: Boolean
): Boolean {
    if( PlaylistIds.isRadio( listId ) ) return false

    val songs = Innertube.browsePlaylist( "VL$listId", CURRENT_LOCALE, isYouTubeLoggedIn() )
                         .onFailure { Logger.e( it ) { "failed to load playlist $listId of a link" } }
                         .getOrNull()
                         ?.songs
                         .orEmpty()
                         // Like a single song link, parental control leaves explicit songs out
                         .filterNot { Preferences.PARENTAL_CONTROL.value && it.isExplicit }
    if( songs.isEmpty() ) return false

    var items: List<MediaItem> = songs.map { it.toMediaItem }
    var index = items.indexOfFirst { it.mediaId == videoId }.coerceAtLeast( 0 )
    if( shuffle ) {
        items = items.shuffled()
        index = 0
    }

    withContext( Dispatchers.Main ) { player.forcePlayAtIndex( items, index ) }
    return true
}
