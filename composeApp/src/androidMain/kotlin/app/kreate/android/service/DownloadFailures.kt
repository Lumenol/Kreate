package app.kreate.android.service

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Why each failed download failed ("This video is unavailable"...), for the
 * downloads page: the download index only keeps "failed".
 */
@UnstableApi
object DownloadFailures : DownloadManager.Listener {

    private val _reasons = MutableStateFlow( emptyMap<String, String>() )
    val reasons = _reasons.asStateFlow()

    override fun onDownloadChanged( manager: DownloadManager, download: Download, finalException: Exception? ) {
        val id = download.request.id
        if( download.state == Download.STATE_FAILED && finalException != null ) {
            // The deepest message is the one YouTube gave
            val reason = generateSequence<Throwable>( finalException, Throwable::cause )
                .mapNotNull { it.message?.takeIf( String::isNotBlank ) }
                .lastOrNull()
            if( reason != null ) _reasons.update { it + (id to reason) }
        } else
            _reasons.update { it - id }
    }

    override fun onDownloadRemoved( manager: DownloadManager, download: Download ) =
        _reasons.update { it - download.request.id }
}
