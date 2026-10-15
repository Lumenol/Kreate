package app.kreate.android.service

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Why each failed download failed ("This video is unavailable"...), for the
 * downloads page: the download index only keeps "failed". Kept on disk so
 * the reason is still there after the app restarts.
 */
@UnstableApi
object DownloadFailures : DownloadManager.Listener {

    private val _reasons = MutableStateFlow( emptyMap<String, String>() )
    val reasons = _reasons.asStateFlow()

    private var prefs: SharedPreferences? = null

    /** Loads the saved reasons; safe to call more than once */
    fun init( context: Context ) {
        if( prefs != null ) return
        val p = context.applicationContext.getSharedPreferences( "download_failures", Context.MODE_PRIVATE )
        prefs = p
        _reasons.update { saved -> p.all.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap() + saved }
    }

    private fun set( id: String, reason: String? ) {
        _reasons.update { if( reason == null ) it - id else it + (id to reason) }
        prefs?.edit()?.apply { if( reason == null ) remove( id ) else putString( id, reason ) }?.apply()
    }

    override fun onDownloadChanged( manager: DownloadManager, download: Download, finalException: Exception? ) {
        val id = download.request.id
        if( download.state == Download.STATE_FAILED && finalException != null ) {
            // The deepest message is the one YouTube gave
            val reason = generateSequence<Throwable>( finalException, Throwable::cause )
                .mapNotNull { it.message?.takeIf( String::isNotBlank ) }
                .lastOrNull()
            if( reason != null ) set( id, reason )
        } else if( id in _reasons.value )
            set( id, null )
    }

    override fun onDownloadRemoved( manager: DownloadManager, download: Download ) =
        set( download.request.id, null )
}
