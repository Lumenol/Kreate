package app.kreate.android.service

import app.kreate.android.Preferences
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.kreate.android.utils.innertube.CURRENT_LOCALE
import app.kreate.database.models.SongYoutubeInfo
import co.touchlab.kermit.Logger
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.utils.isNetworkAvailable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.knighthat.innertube.Innertube
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import me.knighthat.innertube.request.body.Context as InnertubeContext

/**
 * Release date and views of songs, for the library filters: asked to YouTube Music
 * (the same "player" call as playback, without the stream), once a song is played and
 * again a month later, and in the background for the library's songs not known yet.
 */
object YoutubeInfo {

    private val logger = Logger.withTag( "YoutubeInfo" )
    private val scope = CoroutineScope( SupervisorJob() + Dispatchers.IO )
    /** Asked again past this (Settings → Data); `null`: never */
    private val refreshAfter: Long? get() = Preferences.YOUTUBE_INFO_REFRESH.value.asMillis

    // Playback resolves a song several times (chunks): ask once per run
    private val askedOnPlay = ConcurrentHashMap.newKeySet<String>()
    private var filler: Job? = null

    /** The background filling runs: the sheet tells it */
    var isFilling by mutableStateOf( false )
        private set

    /** Asks YouTube and saves; `false` when it couldn't (network, unknown song) */
    suspend fun fetch( songId: String ): Boolean {
        val response = Innertube.player( songId, InnertubeContext.WEB_REMIX_DEFAULT, CURRENT_LOCALE, null, null )
                                .onFailure { logger.w( it ) { "no YouTube info for $songId" } }
                                .getOrNull() ?: return false
        val data = response.microformat?.microformatDataRenderer
        val player = response.microformat?.playerMicroformatRenderer
        val date = data?.publishDate ?: player?.publishDate ?: data?.uploadDate
        val publishedAt = date?.take( 10 )?.let {
            runCatching { LocalDate.parse( it ).atStartOfDay().toInstant( ZoneOffset.UTC ).toEpochMilli() }.getOrNull()
        }
        val views = (response.videoDetails?.viewCount ?: data?.viewCount)?.toLongOrNull()

        val table = Database.songYoutubeInfoTable
        // The song row may not be there yet (playback inserts it in the background)
        if( !table.songExists( songId ) ) return false
        table.upsert( SongYoutubeInfo( songId, publishedAt, views, System.currentTimeMillis() ) )
        return true
    }

    /** A song starts playing: ask if never asked or a month old, after its row is in */
    fun refreshOnPlay( songId: String ) {
        if( !askedOnPlay.add( songId ) ) return
        scope.launch {
            delay( 15.seconds )
            val fetchedAt = Database.songYoutubeInfoTable.fetchedAt( songId )
            val after = refreshAfter
            if( fetchedAt == null || (after != null && System.currentTimeMillis() - fetchedAt > after) )
                fetch( songId )
        }
    }

    /**
     * Fills the library's songs not known yet, then asks again the ones known for too
     * long (views change; Settings → Data), one every 1.5 s so YouTube doesn't mind.
     * Stops without network or after a few failures in a row; picks up where it was
     * next time (what's saved stays). [everything] asks again every song ("refresh now").
     */
    fun fillLibrary( context: Context, everything: Boolean = false ) {
        if( filler?.isActive == true ) {
            if( !everything ) return
            filler?.cancel()
        }
        filler = scope.launch {
            isFilling = true
            try {
                var failures = 0
                val table = Database.songYoutubeInfoTable
                val after = refreshAfter
                val queue = table.missing() + when {
                    everything    -> table.staleBefore( Long.MAX_VALUE )
                    after != null -> table.staleBefore( System.currentTimeMillis() - after )
                    else          -> emptyList()
                }
                for( songId in queue ) {
                    if( !isNetworkAvailable( context ) ) break
                    if( fetch( songId ) ) failures = 0
                    else if( ++failures >= 5 ) {
                        logger.w { "too many failures, the filling stops here" }
                        break
                    }
                    delay( 1500 )
                }
            } finally {
                isFilling = false
            }
        }
    }
}
