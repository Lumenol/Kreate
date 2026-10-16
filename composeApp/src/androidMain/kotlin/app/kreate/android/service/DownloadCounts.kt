package app.kreate.android.service

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import it.fast4x.rimusic.service.MyDownloadHelper

/**
 * What the downloads page holds, counted the same way everywhere (header pill,
 * Library line): a paused download is "paused", not "running".
 */
data class DownloadCounts( val running: Int, val paused: Int, val failed: Int ) {
    val total get() = running + paused + failed
}

@OptIn(UnstableApi::class)
@Composable
fun rememberDownloadCounts(): DownloadCounts {
    val downloads by MyDownloadHelper.instance.downloads.collectAsState()
    // "Pause all" leaves them queued: they count as paused while it lasts
    val allPaused = MyDownloadHelper.instance.downloadManager.downloadsPaused
    var running = 0; var paused = 0; var failed = 0
    downloads.values.forEach {
        when( it.state ) {
            Download.STATE_FAILED -> failed++
            Download.STATE_STOPPED -> paused++
            Download.STATE_QUEUED, Download.STATE_DOWNLOADING, Download.STATE_RESTARTING ->
                if( allPaused ) paused++ else running++
        }
    }
    return DownloadCounts( running, paused, failed )
}
