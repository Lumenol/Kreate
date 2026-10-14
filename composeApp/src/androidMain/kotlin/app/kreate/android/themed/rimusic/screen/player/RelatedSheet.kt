package app.kreate.android.themed.rimusic.screen.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import app.kreate.android.R
import app.kreate.android.service.player.StatefulPlayer
import app.kreate.android.themed.rimusic.component.song.SongItem
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.requests.relatedPage
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.themed.Loader
import it.fast4x.rimusic.ui.components.themed.Menu
import it.fast4x.rimusic.utils.asSong
import it.fast4x.rimusic.utils.semiBold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

/**
 * "Related" tab of the player: songs YouTube Music links to [mediaItem].
 * Tapping one plays it and starts a radio from it, like on the home screen.
 */
@Composable
fun RelatedSheet( mediaItem: MediaItem, onDismiss: () -> Unit ) {
    val player: StatefulPlayer = koinInject()
    val hapticFeedback = LocalHapticFeedback.current
    val colorPalette = colorPalette()
    val typography = typography()
    val songItemValues = remember( colorPalette, typography ) {
        SongItem.Values.from( colorPalette, typography )
    }
    val currentMediaItem by player.currentMediaItemState.collectAsState()

    // null = loading
    var songs by remember( mediaItem.mediaId ) { mutableStateOf<List<Innertube.SongItem>?>( null ) }
    LaunchedEffect( mediaItem.mediaId ) {
        songs = withContext( Dispatchers.IO ) {
            Innertube.relatedPage( NextBody(videoId = mediaItem.mediaId) )
                     ?.getOrNull()
                     ?.songs
                     ?.distinctBy( Innertube.SongItem::key )
                     ?.filter { it.key != mediaItem.mediaId }
                     .orEmpty()
        }
    }

    Menu {
        BasicText(
            text = stringResource( R.string.player_tab_related ),
            style = typography.m.semiBold.copy( color = colorPalette.text ),
            modifier = Modifier.padding( horizontal = 16.dp, vertical = 8.dp )
        )

        val list = songs
        when {
            list == null -> Loader()
            list.isEmpty() ->
                Box( Modifier.fillMaxWidth().padding( 24.dp ), contentAlignment = Alignment.Center ) {
                    BasicText(
                        text = stringResource( R.string.player_related_empty ),
                        style = typography.s.copy( color = colorPalette.textSecondary )
                    )
                }
            else -> list.forEach { innertubeSong ->
                val song = innertubeSong.asSong
                SongItem.Render(
                    song = song,
                    hapticFeedback = hapticFeedback,
                    isPlaying = song.id == currentMediaItem?.mediaId,
                    values = songItemValues,
                    onLongClick = {}
                ) {
                    player.startRadio( song, true )
                    onDismiss()
                }
            }
        }
    }
}
