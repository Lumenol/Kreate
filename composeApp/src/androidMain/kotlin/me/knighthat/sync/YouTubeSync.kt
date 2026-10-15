package me.knighthat.sync

import android.content.Context
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import it.fast4x.innertube.YtMusic.dislikeVideoOrSong
import it.fast4x.innertube.YtMusic.likeVideoOrSong
import it.fast4x.innertube.YtMusic.removelikeVideoOrSong
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.service.MyDownloadHelper
import it.fast4x.rimusic.ui.screens.settings.isYouTubeSyncEnabled
import it.fast4x.rimusic.utils.isNetworkConnected
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.knighthat.utils.Toaster

/**
 * Handles YouTube syncing
 */
object YouTubeSync {

    /**
     * Handles toggling like state of a song both locally and remotely.
     *
     * ***Toggle*** only handles 2 out of 3 states of like state:
     * `like` and `neutral`.
     *
     * This function handles these:
     * - Toggle song like state inside database
     * - Download song when liked (if enabled in settings)
     * - Sync like state with YouTube (if applicable)
     *
     * This function must not be called on **main thread**
     */
    @UnstableApi
    suspend fun toggleSongLike( context: Context, mediaItem: MediaItem ) {
        assert( Looper.myLooper() != Looper.getMainLooper() ) {
            "Cannot run YouTubeSync.toggleSongLike on main thread"
        }

        // TODO: Encapsulate this block in a transaction
        // Always ensure song in database before proceed
        Database.insertIgnore( mediaItem )
        Database.songTable.toggleLike( mediaItem.mediaId )

        val likeState = runBlocking {
            Database.songTable.likeState( mediaItem.mediaId ).first()
        }
        MyDownloadHelper.downloadOnLike( mediaItem, likeState )

        syncLikeState( context, mediaItem, likeState )
    }

    /**
     * Dislikes [mediaItem], or brings it back to neutral when it's already
     * disliked (long press on the heart). Disliked songs are left out of radio.
     *
     * This function must not be called on **main thread**
     */
    suspend fun toggleSongDislike( context: Context, mediaItem: MediaItem ) {
        assert( Looper.myLooper() != Looper.getMainLooper() ) {
            "Cannot run YouTubeSync.toggleSongDislike on main thread"
        }

        Database.insertIgnore( mediaItem )
        val wasDisliked = Database.songTable.likeState( mediaItem.mediaId ).first() == false
        val likeState = if( wasDisliked ) null else false
        Database.songTable.likeState( mediaItem.mediaId, likeState )

        syncLikeState( context, mediaItem, likeState )
    }

    /**
     * Cycles the like state of a song through like, dislike and neutral
     * (as the notification and Android Auto like button do), then syncs it with YouTube.
     *
     * This function must not be called on **main thread**
     */
    suspend fun rotateSongLike( context: Context, mediaItem: MediaItem ) {
        assert( Looper.myLooper() != Looper.getMainLooper() ) {
            "Cannot run YouTubeSync.rotateSongLike on main thread"
        }

        Database.insertIgnore( mediaItem )
        Database.songTable.rotateLikeState( mediaItem.mediaId )

        val likeState = Database.songTable.likeState( mediaItem.mediaId ).first()
        syncLikeState( context, mediaItem, likeState )
    }

    /**
     * Sends [likeState] (`true` like, `false` dislike, `null` neutral)
     * of [mediaItem] to YouTube when syncing is enabled.
     */
    private suspend fun syncLikeState( context: Context, mediaItem: MediaItem, likeState: Boolean? ) {
        // Stop here if it's not enabled
        if( !isYouTubeSyncEnabled() ) {
            with( mediaItem.mediaMetadata ) {
                // Skip message if title is not present
                if( title == null ) return@with

                val messageId = when( likeState ) {
                    false -> R.string.added_to_dislikes
                    true -> R.string.added_to_favorites
                    null -> R.string.removed_from_dislikes
                }

                Toaster.s( messageId, "\"$title - $artist\"" )
            }

            return
        }

        if( !isNetworkConnected( context ) ) {
            Toaster.noInternet()
            return
        }

        val response = when( likeState ) {
            true -> likeVideoOrSong( mediaItem.mediaId )
            false -> dislikeVideoOrSong( mediaItem.mediaId )
            null -> removelikeVideoOrSong( mediaItem.mediaId )
        }
        val messageId = when( likeState ) {
            true -> if( response.isSuccess ) R.string.songs_liked_yt else R.string.songs_liked_yt_failed
            false -> if( response.isSuccess ) R.string.songs_disliked_yt else R.string.songs_disliked_yt_failed
            null -> if( response.isSuccess ) R.string.song_unliked_yt else R.string.songs_unliked_yt_failed
        }
        if( response.isSuccess )
            Toaster.s( messageId )
        else
            Toaster.e( messageId )
    }
}