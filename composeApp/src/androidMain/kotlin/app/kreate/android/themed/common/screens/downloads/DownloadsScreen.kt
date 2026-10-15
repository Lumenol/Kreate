@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package app.kreate.android.themed.common.screens.downloads

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import app.kreate.android.R
import app.kreate.android.coil3.ImageFactory
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
    var paused by remember { mutableStateOf( MyDownloadHelper.instance.downloadManager.downloadsPaused ) }

    DetailSkeleton(
        navController = navController,
        miniPlayer = miniPlayer,
        title = {
            BasicText(
                text = stringResource( R.string.downloads_in_progress_title ),
                style = typography().l.semiBold.color( colorPalette().text )
            )
        }
    ) {
        LazyColumn(
            contentPadding = PaddingValues( bottom = 120.dp ),
            modifier = Modifier.fillMaxSize().background( colorPalette().background0 )
        ) {
            item( "actions" ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy( 10.dp ),
                    modifier = Modifier.fillMaxWidth().padding( horizontal = 16.dp, vertical = 8.dp )
                ) {
                    Pill(
                        text = stringResource( if( paused ) R.string.downloads_resume else R.string.downloads_pause_all ),
                        iconId = if( paused ) R.drawable.play else R.drawable.pause,
                        enabled = downloads.isNotEmpty()
                    ) {
                        if( paused ) DownloadService.sendResumeDownloads( context, MyDownloadService::class.java, false )
                        else DownloadService.sendPauseDownloads( context, MyDownloadService::class.java, false )
                        paused = !paused
                    }
                    Pill(
                        text = stringResource( R.string.downloads_cancel_all ),
                        iconId = R.drawable.close,
                        enabled = downloads.isNotEmpty()
                    ) {
                        downloads.forEach { cancel( context, it ) }
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
                DownloadRow( download, paused, context )
            }
        }
    }
}

private fun cancel( context: Context, download: Download ) =
    DownloadService.sendRemoveDownload( context, MyDownloadService::class.java, download.request.id, false )

@Composable
private fun DownloadRow( download: Download, paused: Boolean, context: Context ) {
    val songId = download.request.id
    val song by remember( songId ) { Database.songTable.findById( songId ) }.collectAsState( null )
    val title = song?.cleanTitle() ?: download.request.data.decodeToString()
    val failed = download.state == Download.STATE_FAILED
    val percent = download.percentDownloaded.takeIf { it >= 0f }

    val status = when {
        failed -> stringResource( R.string.downloads_failed )
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
        modifier = Modifier.fillMaxWidth().padding( horizontal = 16.dp, vertical = 8.dp )
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            BasicText(
                text = listOfNotNull( song?.cleanArtistsText()?.takeIf( String::isNotBlank ), status ).joinToString( " · " ),
                style = typography().xs.color( if( failed ) colorPalette().red else colorPalette().textSecondary ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if( !failed )
                LinearProgressIndicator(
                    progress = { (percent ?: 0f) / 100f },
                    color = colorPalette().text,
                    trackColor = colorPalette().background2,
                    modifier = Modifier.fillMaxWidth().padding( top = 6.dp ).height( 3.dp )
                )
        }
        if( failed )
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
                           .padding( horizontal = 14.dp, vertical = 9.dp )
    ) {
        val color = if( enabled ) colorPalette().text else colorPalette().textDisabled
        Icon( painterResource( iconId ), null, tint = color, modifier = Modifier.size( 16.dp ) )
        BasicText( text, style = typography().xs.semiBold.color( color ) )
    }
