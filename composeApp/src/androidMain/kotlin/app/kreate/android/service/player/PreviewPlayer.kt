package app.kreate.android.service.player

import android.content.Context
import androidx.annotation.MainThread
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import app.kreate.android.Preferences
import app.kreate.android.R
import app.kreate.android.service.DownloadCacheState
import app.kreate.android.service.DownloadHelper
import app.kreate.di.CacheType
import app.kreate.di.CompletedDownloadsFirst
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.knighthat.utils.Toaster
import org.koin.java.KoinJavaComponent.get

/**
 * Tap a song's thumbnail to hear a bit of it without touching the queue; tap again
 * (or wait [Preferences.PREVIEW_LENGTH]) to stop.
 *
 * A second player kept aside: same sources as the main one (downloads first, then the
 * cache, then the network), but no session (no notification) and no stats listener
 * (nothing in history or statistics); a song that's only previewed isn't added to the
 * database either. The main player is cut while it plays and resumes, with its usual
 * fade, only if it was playing and the user didn't start it meanwhile.
 */
@UnstableApi
object PreviewPlayer {

    /** What is being previewed (a song's id, or the album, artist or playlist it comes from), to show it on its thumbnail */
    var previewingId: String? by mutableStateOf( null )
        private set
    /** 0 to 1 over the preview's length, for the ring around the thumbnail */
    var progress: Float by mutableFloatStateOf( 0f )
        private set

    /**
     * A song played only as a preview, not by the main player: stream resolution
     * leaves it out of the database. Read from the loading thread.
     */
    @Volatile
    var previewOnlyId: String? = null
        private set

    private val main: StatefulPlayer by lazy { get( StatefulPlayer::class.java ) }
    private var player: ExoPlayer? = null
    private var resumeMain = false
    private var ticker: Job? = null
    private val scope = CoroutineScope( SupervisorJob() + Dispatchers.Main )

    // The user starting the main player during a preview ends the preview (nothing to resume)
    private val mainListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged( playWhenReady: Boolean, reason: Int ) {
            if( playWhenReady && previewingId != null ) {
                resumeMain = false
                stop()
            }
        }
    }

    private fun build( context: Context ): ExoPlayer {
        val streaming = get<CacheDataSource.Factory>( CacheDataSource.Factory::class.java, CacheType.CACHE )
            .setUpstreamDataSourceFactory( get( ResolvingDataSource.Factory::class.java ) )
            // Read what's cached, but don't fill the cache with bits of previews
            .setCacheWriteDataSinkFactory( null )
        val downloaded = get<CacheDataSource.Factory>( CacheDataSource.Factory::class.java, CacheType.DOWNLOAD )
            .setCacheWriteDataSinkFactory( null )
            .setUpstreamDataSourceFactory( streaming )
        val downloadCache: Cache = get( Cache::class.java, CacheType.DOWNLOAD )
        // Downloads first, like the main player: works offline, costs no data
        val sources = DefaultMediaSourceFactory(
            CompletedDownloadsFirst( downloaded, streaming ) { songId ->
                val state = get<DownloadHelper>( DownloadHelper::class.java ).downloads.value[songId]?.state
                DownloadCacheState.isDownloaded( state, downloadCache, songId )
            }
        )

        return ExoPlayer.Builder( context )
                        .setMediaSourceFactory( sources )
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                           .setUsage( C.USAGE_MEDIA )
                                           .setContentType( C.AUDIO_CONTENT_TYPE_MUSIC )
                                           .build(),
                            // The main player is paused by hand: don't take its focus
                            false
                        )
                        .setHandleAudioBecomingNoisy( true )
                        .build()
                        .apply {
                            // Audio only, like the main player
                            trackSelectionParameters = trackSelectionParameters.buildUpon()
                                                                               .setTrackTypeDisabled( C.TRACK_TYPE_VIDEO, true )
                                                                               .build()
                            addListener( object : Player.Listener {
                                override fun onPlaybackStateChanged( playbackState: Int ) {
                                    if( playbackState == Player.STATE_ENDED ) stop()
                                }

                                override fun onPlayerError( error: PlaybackException ) {
                                    Toaster.e( R.string.preview_failed )
                                    stop()
                                }

                                // Unplugging headphones pauses it: end the preview there
                                override fun onPlayWhenReadyChanged( playWhenReady: Boolean, reason: Int ) {
                                    if( !playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY )
                                        stop()
                                }
                            } )
                        }
    }

    /**
     * Starts [mediaItem]'s preview, or stops it if [key] is the one playing. [key] is
     * what the user tapped: the song itself, or the album, artist or playlist it's from.
     */
    @MainThread
    fun toggle(
        context: Context,
        mediaItem: MediaItem,
        durationMs: Long,
        isLocal: Boolean,
        key: String = mediaItem.mediaId
    ) {
        if( previewingId == key ) {
            stop()
            return
        }

        if( previewingId == null ) {
            // Cut at once (pause() fades out, the two would overlap)
            resumeMain = main.isPlaying
            main.playWhenReady = false
            main.addListener( mainListener )
        }

        val player = this.player ?: build( context.applicationContext ).also { this.player = it }
        previewOnlyId = mediaItem.mediaId.takeIf { it != main.currentMediaItem?.mediaId }
        previewingId = key
        progress = 0f

        val startMs = durationMs * Preferences.PREVIEW_START.value.percent / 100
        val item = if( isLocal ) mediaItem else mediaItem.buildUpon().setCustomCacheKey( mediaItem.mediaId ).build()
        player.volume = main.volume
        // Straight to the start point: no loading of the intro first
        player.setMediaItem( item, startMs )
        player.prepare()
        player.play()

        val lengthMs = Preferences.PREVIEW_LENGTH.value.asMillis
        ticker?.cancel()
        ticker = scope.launch {
            while( isActive ) {
                val played = (player.currentPosition - startMs).coerceAtLeast( 0 )
                progress = (played.toFloat() / lengthMs).coerceIn( 0f, 1f )
                if( played >= lengthMs ) {
                    stop()
                    break
                }
                delay( 100 )
            }
        }
    }

    /** Ends the preview, if any, and lets the main player resume */
    @MainThread
    fun stop() {
        if( previewingId == null ) return

        ticker?.cancel()
        ticker = null
        player?.stop()
        player?.clearMediaItems()
        previewingId = null
        previewOnlyId = null
        progress = 0f

        main.removeListener( mainListener )
        if( resumeMain ) {
            resumeMain = false
            // play(): fades in, and prepares the player if it went idle meanwhile
            main.play()
        }
    }
}
