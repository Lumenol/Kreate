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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import app.kreate.android.service.player.PreviewPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.knighthat.utils.Toaster
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.BrowseBody
import it.fast4x.innertube.requests.albumPage
import it.fast4x.innertube.requests.artistPage
import it.fast4x.innertube.requests.playlistPage
import it.fast4x.rimusic.utils.asMediaItem
import app.kreate.util.toDuration

/** A song to preview and its length in milliseconds (0 if unknown) */
typealias PreviewSong = Pair<MediaItem, Long>

/**
 * Round ▶ at the bottom right of a card's thumbnail (like YouTube Music): a preview of
 * the card's song, or of the song an album, artist or playlist leads with, without
 * touching the queue. Tap again to stop. The rest of the card still opens it.
 *
 * [resolve] finds that song (it may load a page: a spinner shows meanwhile);
 * [key] identifies the card while it plays.
 */
@UnstableApi
@Composable
fun BoxScope.PreviewButton(
    key: String,
    modifier: Modifier = Modifier,
    resolve: suspend () -> PreviewSong?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember( key ) { mutableStateOf( false ) }
    val previewing = PreviewPlayer.previewingId == key

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.align( Alignment.BottomEnd )
                           .padding( 6.dp )
                           .size( 32.dp )
                           .clip( CircleShape )
                           .background( Color.Black.copy( alpha = .6f ) )
                           .clickable(
                               role = Role.Button,
                               onClickLabel = stringResource( if( previewing ) R.string.preview_stop else R.string.preview_play )
                           ) {
                               if( previewing ) {
                                   PreviewPlayer.stop()
                                   return@clickable
                               }
                               if( loading ) return@clickable
                               scope.launch {
                                   loading = true
                                   val song = runCatching { withContext( Dispatchers.IO ) { resolve() } }.getOrNull()
                                   loading = false
                                   if( song == null )
                                       Toaster.e( R.string.preview_failed )
                                   else
                                       PreviewPlayer.toggle( context, song.first, song.second, false, key )
                               }
                           }
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

/** Which song an album, artist or playlist card previews: its first one (an artist's most popular) */
@UnstableApi
object PreviewSongs {

    private fun Innertube.SongItem.asPreview(): PreviewSong =
        asMediaItem to durationText.toDuration().inWholeMilliseconds

    suspend fun album( browseId: String ): PreviewSong? =
        Innertube.albumPage( BrowseBody( browseId = browseId ) )
                 ?.getOrNull()?.songsPage?.items?.firstOrNull()?.asPreview()

    suspend fun playlist( browseId: String ): PreviewSong? =
        Innertube.playlistPage( BrowseBody( browseId = browseId ) )
                 ?.getOrNull()?.songsPage?.items?.firstOrNull()?.asPreview()

    /** The first of the artist's top songs, which YouTube Music lists by popularity */
    suspend fun artist( browseId: String ): PreviewSong? =
        Innertube.artistPage( BrowseBody( browseId = browseId ) )
                 ?.getOrNull()?.songs?.firstOrNull()?.asPreview()
}
