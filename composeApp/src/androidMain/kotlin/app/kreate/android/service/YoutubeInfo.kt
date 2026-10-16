package app.kreate.android.service

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.CoroutineExceptionHandler
import android.database.sqlite.SQLiteException
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
import kotlin.time.Duration.Companion.seconds
import me.knighthat.innertube.request.body.Context as InnertubeContext

/**
 * Release date and views of songs, for the library filters: asked to YouTube Music
 * (the same "player" call as playback, without the stream), once a song is played and
 * again a month later, and in the background for the library's songs not known yet.
 */
object YoutubeInfo {

    private val logger = Logger.withTag( "YoutubeInfo" )
    // A database error (a song deleted meanwhile) is logged, never a crash
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, e -> logger.e( e ) { "YouTube info failed" } }
    )

    /** Asked again past this (Settings → Data); `null`: never */
    private val refreshAfter: Long? get() = Preferences.YOUTUBE_INFO_REFRESH.value.asMillis

    // Playback resolves a song several times (chunks): ask once per run
    private val askedOnPlay = ConcurrentHashMap.newKeySet<String>()
    private var filler: Job? = null

    /** The background filling runs: the sheet tells it */
    var isFilling by mutableStateOf( false )
        private set

    private enum class Result { SAVED, NOTHING, FAILED }

    /**
     * Asks YouTube. [SAVED]: date or views saved. [NOTHING]: YouTube answered without
     * either (unavailable video, or a "not a bot" check): nothing is written, a value
     * known before stays. [FAILED]: no answer (network) or the song is gone.
     */
    private suspend fun ask( songId: String ): Result {
        val response = Innertube.player( songId, InnertubeContext.WEB_REMIX_DEFAULT, CURRENT_LOCALE, null, null )
                                .getOrElse {
                                    if( it is CancellationException ) throw it
                                    logger.w( it ) { "no YouTube info for $songId" }
                                    return Result.FAILED
                                }
        val data = response.microformat?.microformatDataRenderer
        val player = response.microformat?.playerMicroformatRenderer
        val date = data?.publishDate ?: player?.publishDate ?: data?.uploadDate
        val publishedAt = date?.take( 10 )?.let {
            runCatching { LocalDate.parse( it ).atStartOfDay().toInstant( ZoneOffset.UTC ).toEpochMilli() }.getOrNull()
        }
        val views = (response.videoDetails?.viewCount ?: data?.viewCount)?.toLongOrNull()
        if( publishedAt == null && views == null ) return Result.NOTHING

        return if( save( SongYoutubeInfo( songId, publishedAt, views, System.currentTimeMillis() ) ) ) Result.SAVED else Result.FAILED
    }

    /** `false` when the song isn't (or no longer) in the database */
    private fun save( info: SongYoutubeInfo ): Boolean = try {
        val table = Database.songYoutubeInfoTable
        // The song row may not be there yet (playback inserts it in the background)
        table.songExists( info.songId ) && run { table.upsert( info ); true }
    } catch( e: SQLiteException ) {
        // Deleted between the check and the write
        logger.w( e ) { "${info.songId} gone, its YouTube info isn't saved" }
        false
    }

    /** A song starts playing: ask if never asked or too old, after its row is in */
    fun refreshOnPlay( songId: String ) {
        if( !askedOnPlay.add( songId ) ) return
        scope.launch {
            delay( 15.seconds )
            val fetchedAt = Database.songYoutubeInfoTable.fetchedAt( songId )
            val after = refreshAfter
            if( fetchedAt == null || (after != null && System.currentTimeMillis() - fetchedAt > after) )
                ask( songId )
        }
    }

    /**
     * Fills the library's songs not known yet, then asks again the ones known for too
     * long (views change; Settings → Data), one every 1.5 s so YouTube doesn't mind.
     * [everything] asks again every song ("refresh now").
     *
     * A song YouTube says nothing of is marked as asked (empty), so it doesn't come back
     * first every time; but 5 of those in a row look like YouTube refusing: the filling
     * stops there and marks none of them. Picks up where it was next time.
     */
    fun fillLibrary( context: Context, everything: Boolean = false ) {
        if( filler?.isActive == true && !everything ) return
        val previous = filler
        filler = scope.launch {
            // The old run must be over before this one says it runs
            previous?.cancelAndJoin()
            isFilling = true
            try {
                val table = Database.songYoutubeInfoTable
                val after = refreshAfter
                val queue = table.missing() + when {
                    everything    -> table.staleBefore( Long.MAX_VALUE )
                    after != null -> table.staleBefore( System.currentTimeMillis() - after )
                    else          -> emptyList()
                }
                val unanswered = mutableListOf<String>()
                for( songId in queue ) {
                    if( !isNetworkAvailable( context ) ) break
                    when( ask( songId ) ) {
                        Result.SAVED -> {
                            // Those just before were isolated cases: mark them as asked
                            unanswered.forEach { id ->
                                if( table.fetchedAt( id ) == null )
                                    save( SongYoutubeInfo( id, null, null, System.currentTimeMillis() ) )
                            }
                            unanswered.clear()
                        }
                        Result.NOTHING, Result.FAILED -> {
                            unanswered += songId
                            if( unanswered.size >= 5 ) {
                                logger.w { "5 songs in a row without an answer, the filling stops here" }
                                break
                            }
                        }
                    }
                    delay( 1500 )
                }
            } finally {
                if( filler === coroutineContext[Job] ) isFilling = false
            }
        }
    }
}
