package app.kreate.android.themed.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import app.kreate.android.service.player.PreviewPlayer
import app.kreate.android.utils.ConnectivityUtils
import app.kreate.android.utils.PlaylistIds
import app.kreate.android.utils.innertube.CURRENT_LOCALE
import app.kreate.android.utils.innertube.toMediaItem
import app.kreate.util.toDuration
import it.fast4x.innertube.models.bodies.BrowseBody
import it.fast4x.innertube.requests.albumPage
import it.fast4x.innertube.requests.playlistPage
import it.fast4x.rimusic.utils.asMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.knighthat.innertube.model.InnertubeSong
import me.knighthat.utils.Toaster
import it.fast4x.innertube.Innertube as OldInnertube
import me.knighthat.innertube.Innertube as NewInnertube

/** A song to preview and its length in milliseconds (0 if unknown) */
typealias PreviewSong = Pair<MediaItem, Long>

/**
 * Round ▶ at the bottom right of a card's thumbnail (like YouTube Music): a preview of
 * the card's song, or of the song an album, artist or playlist leads with, without
 * touching the queue. Tap again to stop. The rest of the card still opens it.
 *
 * [resolve] finds that song (it may load a page: a spinner shows meanwhile);
 * [key] identifies the card while it loads and plays. [inset] moves the button in,
 * for round thumbnails.
 */
@UnstableApi
@Composable
fun BoxScope.PreviewButton(
    key: String,
    modifier: Modifier = Modifier,
    inset: Dp = 0.dp,
    resolve: suspend () -> PreviewSong?
) {
    val context = LocalContext.current
    val loading = PreviewSongs.loadingKey == key
    val previewing = PreviewPlayer.previewingId == key
    val label = stringResource( if( previewing || loading ) R.string.preview_stop else R.string.preview_play )

    // 48 dp to touch (a near miss would open the card), 32 dp drawn
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.align( Alignment.BottomEnd )
                           .padding( end = inset, bottom = inset )
                           .size( 48.dp )
                           .semantics { contentDescription = label }
                           .clickable( role = Role.Button ) {
                               when {
                                   previewing -> PreviewPlayer.stop()
                                   loading    -> PreviewSongs.cancel()
                                   else       -> PreviewSongs.load( key, resolve ) { song ->
                                       PreviewPlayer.toggle( context, song.first, song.second, false, key )
                                   }
                               }
                           }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size( 32.dp )
                               .clip( CircleShape )
                               .background( Color.Black.copy( alpha = .6f ) )
        ) {
            when {
                loading -> CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size( 20.dp )
                )
                previewing -> {
                    // How far the preview is
                    CircularProgressIndicator(
                        progress = { PreviewPlayer.progress },
                        color = Color.White,
                        trackColor = Color.White.copy( alpha = .25f ),
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size( 26.dp )
                    )
                    Box( Modifier.size( 9.dp ).background( Color.White, RoundedCornerShape( 1.5.dp ) ) )
                }
                else -> Icon(
                    painter = painterResource( R.drawable.play ),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size( 16.dp )
                )
            }
        }
    }
}

/**
 * Which song an album, artist or playlist card previews (its first one, an artist's
 * most popular), loaded on tap. One load at a time: a new tap cancels the pending one,
 * so a slow page can't take over a preview started meanwhile. Found songs are kept,
 * so tapping the same card again starts at once, without loading the page again.
 */
@UnstableApi
object PreviewSongs {

    /** The card whose song is being loaded, for its spinner */
    var loadingKey: String? by mutableStateOf( null )
        private set

    private val scope = CoroutineScope( SupervisorJob() + Dispatchers.Main )
    private var pending: Job? = null
    private val found = object : LinkedHashMap<String, PreviewSong>( 32, .75f, true ) {
        override fun removeEldestEntry( eldest: MutableMap.MutableEntry<String, PreviewSong>? ) = size > 50
    }

    fun load( key: String, resolve: suspend () -> PreviewSong?, play: ( PreviewSong ) -> Unit ) {
        cancel()
        found[key]?.let { play( it ); return }
        if( !ConnectivityUtils.isAvailable.value ) {
            Toaster.noInternet()
            return
        }

        loadingKey = key
        pending = scope.launch {
            // A cancelled load (new tap, card gone) isn't a failure: no message
            val song = try {
                withContext( Dispatchers.IO ) { resolve() }
            } catch( e: kotlinx.coroutines.CancellationException ) {
                throw e
            } catch( e: Exception ) {
                null
            }
            loadingKey = null
            if( song == null )
                Toaster.e( R.string.preview_failed )
            else {
                found[key] = song
                play( song )
            }
        }
    }

    fun cancel() {
        pending?.cancel()
        pending = null
        loadingKey = null
    }

    private fun OldInnertube.SongItem.asPreview(): PreviewSong =
        asMediaItem to durationText.toDuration().inWholeMilliseconds

    /** Only YouTube playlists can be loaded (not charts or podcasts shelves) */
    fun isPlaylist( id: String ): Boolean = PlaylistIds.isBrowsable( id )

    suspend fun album( browseId: String ): PreviewSong? =
        OldInnertube.albumPage( BrowseBody( browseId = browseId ) )
                    ?.getOrNull()?.songsPage?.items?.firstOrNull()?.asPreview()

    suspend fun playlist( id: String ): PreviewSong? =
        OldInnertube.playlistPage( BrowseBody( browseId = PlaylistIds.browseId( id ) ) )
                    ?.getOrNull()?.songsPage?.items?.firstOrNull()?.asPreview()

    /**
     * The first song of the artist's page, the top songs listed by popularity: read the
     * way the artist screen reads it. Its length is often missing: the preview then
     * finds its start point once the song is loaded.
     */
    suspend fun artist( browseId: String ): PreviewSong? =
        NewInnertube.browseArtist( browseId, CURRENT_LOCALE )
                    .getOrNull()
                    ?.sections
                    ?.firstNotNullOfOrNull { section -> section.contents.firstNotNullOfOrNull { it as? InnertubeSong } }
                    ?.let { it.toMediaItem to it.durationText.toDuration().inWholeMilliseconds }
}
