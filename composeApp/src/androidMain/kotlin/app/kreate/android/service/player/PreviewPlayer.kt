package app.kreate.android.service.player

import android.content.Context
import androidx.annotation.MainThread
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import app.kreate.di.CacheType
import org.koin.java.KoinJavaComponent.get

/**
 * Hold a song's thumbnail to hear a few seconds of it without touching the queue.
 *
 * A second, throwaway player: same stream resolution and cache as the main one, but
 * no session (no notification) and no stats listener (nothing goes to history or
 * statistics). The main player is paused while it plays and resumes where it was.
 */
@UnstableApi
object PreviewPlayer {

    /** Id of the song being previewed, to show it on its thumbnail */
    var previewingId: String? by mutableStateOf( null )
        private set

    private var preview: ExoPlayer? = null
    private var resumeMain = false

    @MainThread
    fun start( context: Context, mediaItem: MediaItem ) {
        stop()

        val main: StatefulPlayer = get( StatefulPlayer::class.java )
        // Cut at once (pause() fades out, the two would overlap)
        resumeMain = main.playWhenReady
        main.playWhenReady = false

        val sourceFactory = DefaultMediaSourceFactory(
            get<CacheDataSource.Factory>( CacheDataSource.Factory::class.java, CacheType.CACHE )
                .setUpstreamDataSourceFactory( get( ResolvingDataSource.Factory::class.java ) )
                // Read what's cached, but don't fill the cache with bits of previews
                .setCacheWriteDataSinkFactory( null )
        )
        val player = ExoPlayer.Builder( context )
                              .setMediaSourceFactory( sourceFactory )
                              .setAudioAttributes(
                                  AudioAttributes.Builder()
                                                 .setUsage( C.USAGE_MEDIA )
                                                 .setContentType( C.AUDIO_CONTENT_TYPE_MUSIC )
                                                 .build(),
                                  // The main player was paused by hand: don't take its focus
                                  false
                              )
                              .build()
        preview = player
        previewingId = mediaItem.mediaId

        player.addListener( object : Player.Listener {
            var seeked = false

            override fun onPlaybackStateChanged( playbackState: Int ) {
                // Start around the chorus rather than the intro: about 30 % in
                if( playbackState == Player.STATE_READY && !seeked ) {
                    seeked = true
                    val duration = player.duration
                    if( duration != C.TIME_UNSET && duration > 0 )
                        player.seekTo( duration * 3 / 10 )
                    player.play()
                }
                if( playbackState == Player.STATE_ENDED ) stop()
            }
        } )
        player.setMediaItem(
            mediaItem.buildUpon().setCustomCacheKey( mediaItem.mediaId ).build()
        )
        player.prepare()
    }

    @MainThread
    fun stop() {
        val player = preview ?: return
        preview = null
        previewingId = null
        player.release()

        if( resumeMain ) {
            resumeMain = false
            get<StatefulPlayer>( StatefulPlayer::class.java ).playWhenReady = true
        }
    }
}
