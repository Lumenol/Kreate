@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package app.kreate.android.themed.common.screens.downloads

import android.content.Context
import app.kreate.android.service.player.StatefulPlayer
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.forcePlay
import org.koin.compose.koinInject
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import app.kreate.android.R
import app.kreate.android.coil3.ImageFactory
import app.kreate.android.service.DownloadFailures
import app.kreate.android.themed.common.component.DetailSkeleton
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.service.MyDownloadHelper
import it.fast4x.rimusic.service.MyDownloadService
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.utils.color
import it.fast4x.rimusic.utils.medium
import it.fast4x.rimusic.utils.semiBold
import kotlinx.coroutines.delay

/** Downloads not finished yet, newest states every second (the index only reports state changes) */
@Composable
private fun rememberPendingDownloads(): List<Download> {
    val manager = MyDownloadHelper.instance.downloadManager
    val known by MyDownloadHelper.instance.downloads.collectAsState()
    var current by remember { mutableStateOf( manager.currentDownloads.toList() ) }
    LaunchedEffect( Unit ) {
        while( true ) {
            current = manager.currentDownloads.toList()
            delay( 1_000 )
        }
    }
    // Running ones with their progress, then the failed ones the manager no longer tracks
    val running = current.filter { it.state != Download.STATE_REMOVING }
    val failed = known.values.filter { it.state == Download.STATE_FAILED && running.none { r -> r.request.id == it.request.id } }
    // Those downloading first, then waiting, then failed
    return running.sortedBy { if( it.state == Download.STATE_DOWNLOADING ) 0 else 1 } + failed
}

/** Downloads in progress, waiting, paused or failed: the finished ones are in Library → Downloaded */
@Composable
fun DownloadsScreen( navController: NavController, miniPlayer: @Composable () -> Unit = {} ) {
    val context = LocalContext.current
    val downloads = rememberPendingDownloads()
    remember { DownloadFailures.init( context ) }
    val manager = MyDownloadHelper.instance.downloadManager
    var paused by remember { mutableStateOf( manager.downloadsPaused ) }
    DisposableEffect( manager ) {
        val listener = object : DownloadManager.Listener {
            override fun onDownloadsPausedChanged( downloadManager: DownloadManager, downloadsPaused: Boolean ) {
                paused = downloadsPaused
            }
        }
        manager.addListener( listener )
        // A new DownloadManager starts paused and only the service un-pauses it:
        // without this, after a restart the page showed everything "Paused"
        DownloadService.start( context, MyDownloadService::class.java )
        onDispose { manager.removeListener( listener ) }
    }

    DetailSkeleton(
        navController = navController,
        miniPlayer = miniPlayer,
        // Up there, apart from the row: it empties the whole list
        actions = {
            if( downloads.isNotEmpty() )
                BasicText(
                    text = stringResource( R.string.downloads_cancel_all ),
                    style = typography().xs.semiBold.color( colorPalette().textSecondary ),
                    modifier = Modifier.clip( CircleShape )
                                       .clickable { downloads.forEach { cancel( context, it ) } }
                                       .padding( horizontal = 12.dp, vertical = 8.dp )
                )
        },
        title = {
            BasicText(
                text = stringResource( R.string.downloads_page_title ),
                style = typography().l.semiBold.color( colorPalette().text )
            )
        }
    ) {
        LazyColumn(
            contentPadding = PaddingValues( bottom = 120.dp ),
            modifier = Modifier.fillMaxSize().background( colorPalette().background0 )
        ) {
            item( "actions" ) {
                // Short labels on one line; "Retry" only when something failed
                val failed = downloads.filter { it.state == Download.STATE_FAILED }
                Row(
                    horizontalArrangement = Arrangement.spacedBy( 8.dp ),
                    modifier = Modifier.fillMaxWidth()
                                       .horizontalScroll( rememberScrollState() )      // Never on 2 lines
                                       .padding( horizontal = 16.dp, vertical = 8.dp )
                ) {
                    Pill(
                        text = stringResource( if( paused ) R.string.downloads_resume else R.string.downloads_pause ),
                        iconId = if( paused ) R.drawable.play else R.drawable.pause,
                        enabled = downloads.isNotEmpty()
                    ) {
                        if( paused ) {
                            DownloadService.sendResumeDownloads( context, MyDownloadService::class.java, false )
                            // Also lifts the pauses set on single rows
                            DownloadService.sendSetStopReason( context, MyDownloadService::class.java, null, Download.STOP_REASON_NONE, false )
                        }
                        else DownloadService.sendPauseDownloads( context, MyDownloadService::class.java, false )
                        paused = !paused
                    }
                    if( failed.isNotEmpty() )
                        Pill(
                            text = stringResource( R.string.downloads_retry_failed, failed.size ),
                            iconId = R.drawable.refresh_circle,
                            enabled = true
                        ) {
                            failed.forEach {
                                DownloadService.sendAddDownload( context, MyDownloadService::class.java, it.request, false )
                            }
                        }
                }
            }

            if( downloads.isEmpty() )
                item( "empty" ) {
                    BasicText(
                        text = stringResource( R.string.downloads_none_in_progress ),
                        style = typography().s.medium.color( colorPalette().textSecondary ),
                        modifier = Modifier.padding( 24.dp )
                    )
                }

            items( downloads, key = { it.request.id } ) { download ->
                DownloadRow( download, paused, context ) {
                    // Everything paused: run this one only, keep the others stopped
                    downloads.filter { it !== download && it.state != Download.STATE_FAILED }
                             .forEach { setPaused( context, it, true ) }
                    setPaused( context, download, false )
                    DownloadService.sendResumeDownloads( context, MyDownloadService::class.java, false )
                    paused = false
                }
            }
        }
    }
}

private fun cancel( context: Context, download: Download ) =
    DownloadService.sendRemoveDownload( context, MyDownloadService::class.java, download.request.id, false )

/** Any non-zero reason keeps a single download stopped until it's lifted */
private const val STOP_REASON_USER = 1

private fun setPaused( context: Context, download: Download, pause: Boolean ) =
    DownloadService.sendSetStopReason(
        context, MyDownloadService::class.java, download.request.id,
        if( pause ) STOP_REASON_USER else Download.STOP_REASON_NONE, false
    )

@Composable
private fun DownloadRow( download: Download, paused: Boolean, context: Context, resumeOnly: () -> Unit ) {
    val songId = download.request.id
    val song by remember( songId ) { Database.songTable.findById( songId ) }.collectAsState( null )
    val title = song?.cleanTitle() ?: download.request.data.decodeToString()
    val failed = download.state == Download.STATE_FAILED
    val percent = download.percentDownloaded.takeIf { it >= 0f }
    val player: StatefulPlayer = koinInject()
    val stopped = paused || download.state == Download.STATE_STOPPED
    // A failed row unfolds on long press so the whole reason can be read
    var expanded by remember( songId ) { mutableStateOf( false ) }
    val unfolded = failed && expanded

    val reasons by DownloadFailures.reasons.collectAsState()
    val status = when {
        // YouTube's own reason when the download gave one ("Video unavailable"...)
        failed -> reasons[songId]?.let { stringResource( R.string.downloads_failed_because, it ) }
                  ?: stringResource( R.string.downloads_failed )
        paused || download.state == Download.STATE_STOPPED -> stringResource( R.string.downloads_paused )
        download.state == Download.STATE_QUEUED || download.state == Download.STATE_RESTARTING ->
            stringResource( R.string.downloads_waiting )
        percent != null -> "${percent.toInt()} %"
        // Size not known yet: started, but no percentage to show
        else -> stringResource( R.string.downloads_downloading )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy( 14.dp ),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
                           .combinedClickable(
                               // Plays the song, to know what it is
                               // Already the current song: don't restart it from 0
                               onClick = {
                                   if( player.currentMediaItem?.mediaId != songId )
                                       song?.let { player.forcePlay( it.asMediaItem ) }
                               },
                               onLongClick = if( failed ) { { expanded = !expanded } } else null
                           )
                           .padding( horizontal = 16.dp, vertical = 8.dp )
    ) {
        Image(
            painter = ImageFactory.rememberAsyncImagePainter( song?.thumbnailUrl ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size( 48.dp ).clip( RoundedCornerShape( 6.dp ) ).background( colorPalette().background2 )
        )
        Column( Modifier.weight( 1f ) ) {
            BasicText(
                text = title,
                style = typography().s.semiBold.color( colorPalette().text ),
                maxLines = if( unfolded ) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis
            )
            val artists = song?.cleanArtistsText()?.takeIf( String::isNotBlank )
            if( unfolded ) {
                artists?.let {
                    BasicText( it, style = typography().xs.color( colorPalette().textSecondary ) )
                }
                BasicText(
                    text = status,
                    style = typography().xs.color( colorPalette().red ),
                    modifier = Modifier.padding( top = 2.dp )
                )
            } else {
                // Artists and state on their own lines: together, the state ("62 %",
                // "En pause") was the part cut off
                artists?.let {
                    BasicText( it, style = typography().xs.color( colorPalette().textSecondary ), maxLines = 1, overflow = TextOverflow.Ellipsis )
                }
                BasicText(
                    text = status,
                    style = typography().xs.semiBold.color( if( failed ) colorPalette().red else colorPalette().textSecondary ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if( !failed )
                LinearProgressIndicator(
                    progress = { (percent ?: 0f) / 100f },
                    color = colorPalette().text,
                    trackColor = colorPalette().background2,
                    modifier = Modifier.fillMaxWidth().padding( top = 6.dp ).height( 3.dp )
                )
        }
        if( !failed )
            IconButton( onClick = { if( paused ) resumeOnly() else setPaused( context, download, !stopped ) } ) {
                Icon(
                    painterResource( if( stopped ) R.drawable.play else R.drawable.pause ),
                    stringResource( if( stopped ) R.string.downloads_resume else R.string.downloads_pause ),
                    tint = colorPalette().text,
                    modifier = Modifier.size( 20.dp )
                )
            }
        else
            IconButton( onClick = {
                DownloadService.sendAddDownload( context, MyDownloadService::class.java, download.request, false )
            } ) {
                Icon( painterResource( R.drawable.refresh_circle ), stringResource( R.string.downloads_retry ), tint = colorPalette().text, modifier = Modifier.size( 20.dp ) )
            }
        IconButton( onClick = { cancel( context, download ) } ) {
            Icon( painterResource( R.drawable.close ), stringResource( R.string.downloads_cancel ), tint = colorPalette().textSecondary, modifier = Modifier.size( 20.dp ) )
        }
    }
}

@Composable
private fun Pill( text: String, iconId: Int, enabled: Boolean, onClick: () -> Unit ) =
    Row(
        horizontalArrangement = Arrangement.spacedBy( 6.dp ),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip( CircleShape )
                           .background( colorPalette().background2 )
                           .clickable( enabled = enabled, onClick = onClick )
                           .padding( horizontal = 12.dp, vertical = 8.dp )
    ) {
        val color = if( enabled ) colorPalette().text else colorPalette().textDisabled
        Icon( painterResource( iconId ), null, tint = color, modifier = Modifier.size( 14.dp ) )
        BasicText( text, style = typography().xs.semiBold.color( color ), maxLines = 1, softWrap = false )
    }
