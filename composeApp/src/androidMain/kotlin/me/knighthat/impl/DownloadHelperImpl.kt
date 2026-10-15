package me.knighthat.impl

import app.kreate.di.DOWNLOAD_QUALITY_PARAM
import android.content.Context
import android.database.sqlite.SQLiteDatabaseLockedException
import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
import app.kreate.android.Preferences
import app.kreate.android.coil3.ImageFactory
import app.kreate.android.service.DownloadCacheState
import app.kreate.android.service.DownloadHelper
import app.kreate.database.models.Song
import app.kreate.di.CacheType
import co.touchlab.kermit.Logger
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.service.MyDownloadService
import it.fast4x.rimusic.service.modern.isLocal
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.asSong
import it.fast4x.rimusic.utils.download
import it.fast4x.rimusic.utils.downloadSyncedLyrics
import it.fast4x.rimusic.utils.isNetworkConnected
import it.fast4x.rimusic.utils.removeDownload
import it.fast4x.rimusic.utils.thumbnail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.knighthat.utils.Toaster
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import java.util.concurrent.Executors


@OptIn(UnstableApi::class)
class DownloadHelperImpl(
    private val context: Context,
): DownloadHelper, KoinComponent {

    companion object {

        private const val NUM_PARALLEL_DOWNLOADS = 3
        private const val NUM_RETRIES = 2
        private const val EXECUTOR_NAME = "DownloadHelper-Executor-Scope"
        private const val TAG = "DownloadHelperImpl"
    }

    private val executor = Executors.newCachedThreadPool()
    private val coroutineScope = CoroutineScope(
        executor.asCoroutineDispatcher() +
                SupervisorJob() +
                CoroutineName(EXECUTOR_NAME)
    )
    private val downloadCache: Cache by inject( CacheType.DOWNLOAD )

    private fun stateName( state: Int ): String = when( state ) {
        Download.STATE_QUEUED       -> "QUEUED"
        Download.STATE_STOPPED      -> "STOPPED"
        Download.STATE_DOWNLOADING  -> "DOWNLOADING"
        Download.STATE_COMPLETED    -> "COMPLETED"
        Download.STATE_FAILED       -> "FAILED"
        Download.STATE_REMOVING     -> "REMOVING"
        Download.STATE_RESTARTING   -> "RESTARTING"
        else                        -> "UNKNOWN($state)"
    }

    override val downloads: MutableStateFlow<Map<String, Download>>
    override val downloadManager by lazy {
        val listener = object: DownloadManager.Listener {
            override fun onDownloadChanged(
                downloadManager: DownloadManager,
                download: Download,
                finalException: Exception?
            ) = syncDownloads(download)

            override fun onDownloadRemoved(
                downloadManager: DownloadManager,
                download: Download
            ) = syncDownloads(download)
        }

        val manager = DownloadManager(
            context,
            StandaloneDatabaseProvider(context),
            get(CacheType.DOWNLOAD),
            get<ResolvingDataSource.Factory>(),
            executor
        )

        manager.maxParallelDownloads = NUM_PARALLEL_DOWNLOADS
        manager.minRetryCount = NUM_RETRIES
        manager.requirements = Requirements(Requirements.NETWORK)
        manager.addListener( listener )

        manager
    }

    private lateinit var downloadNotificationHelper: DownloadNotificationHelper

    init {
        val results = mutableMapOf<String, Download>()
        // At startup the database can still be locked by another component:
        // retry a few times rather than crash, and start empty as a last resort
        for( attempt in 1..3 )
            try {
                downloadManager.downloadIndex.getDownloads().use { cursor ->
                    while ( cursor.moveToNext() ) {
                        results[cursor.download.request.id] = cursor.download
                    }
                }
                break
            } catch( err: SQLiteDatabaseLockedException ) {
                Logger.w( err, "DownloadHelperImpl" ) { "Download index locked (attempt $attempt)" }
                results.clear()
                Thread.sleep( 200L * attempt )
            }
        downloads = MutableStateFlow(results)

        // The index and the download cache are independent stores. Log how far apart they start
        // out: *every* COMPLETED row reporting no bytes means the cache's own index failed to
        // load, while a handful means real byte loss (an old LRU eviction, a manual wipe). Info
        // level, because this is the discriminator a "download button does nothing" report needs
        // and release builds drop anything below it.
        runCatching {
            val completed = results.values.count { it.state == Download.STATE_COMPLETED }
            val stale = results.values.count {
                it.state == Download.STATE_COMPLETED
                        && !DownloadCacheState.isFullyCached( downloadCache, it.request.id )
            }
            Logger.i( TAG ) {
                "download index: ${results.size} rows, $completed completed, $stale of those" +
                " hold no bytes in the download cache" +
                " (cacheSpace=${downloadCache.cacheSpace}B, keys=${downloadCache.keys.size})"
            }
        }.onFailure {
            Logger.w( TAG ) { "Could not reconcile download index against cache: ${it.message}" }
        }
    }

    @Synchronized
    private fun syncDownloads( download: Download ) =
        downloads.update { map ->
            map.toMutableMap().apply {
                set(download.request.id, download)
            }
        }

    override fun getDownload(songId: String): Flow<Download?> = downloads.map { it[songId] }

    override fun getDownloadNotificationHelper(): DownloadNotificationHelper {
        if (!::downloadNotificationHelper.isInitialized) {
            downloadNotificationHelper =
                DownloadNotificationHelper(context, DownloadHelper.DOWNLOAD_NOTIFICATION_CHANNEL_ID)
        }
        return downloadNotificationHelper
    }

    override fun addDownload( mediaItem: MediaItem ) {
        if (mediaItem.isLocal) return

        if( !isNetworkConnected( context ) ) {
            Toaster.noInternet()
            return
        }

        val downloadRequest = downloadRequestOf(
            mediaItem.mediaId,
            "${mediaItem.mediaMetadata.artist.toString()} - ${mediaItem.mediaMetadata.title.toString()}".encodeToByteArray() // Title in notification
        )

        Database.asyncTransaction {
            insertIgnore( mediaItem )
        }

        val imageUrl = mediaItem.mediaMetadata.artworkUri.thumbnail(1200)

//            sendAddDownload(
//                context,MyDownloadService::class.java,downloadRequest,false
//            )

        coroutineScope.launch {
            context.download<MyDownloadService>(downloadRequest).exceptionOrNull()?.let {
                if (it is CancellationException) throw it

                Logger.e( it, "DownloadHelperImpl" ) { "addDownload failed!"}
            }
            downloadSyncedLyrics( mediaItem.asSong )

            ImageFactory.requestBuilder( imageUrl.toString() ) {
                bitmapConfig( Bitmap.Config.ARGB_8888 )
                allowHardware( false )
            }
        }
    }

    override fun removeDownload( mediaItem: MediaItem ) {
        if (mediaItem.isLocal) return

        //sendRemoveDownload(context,MyDownloadService::class.java,mediaItem.mediaId,false)
        coroutineScope.launch {
            context.removeDownload<MyDownloadService>(mediaItem.mediaId).exceptionOrNull()?.let {
                if (it is CancellationException) throw it

                Logger.e( it, "DownloadHelperImpl" ) { "removeDownload failed!"}
            }
        }
    }

    override fun purgeDownload( songId: String ) {
        coroutineScope.launch {
            if ( songId in downloads.value ) {
                // DownloadManager owns the bytes for anything it has an index row for;
                // it removes the row and the cached resource together.
                context.removeDownload<MyDownloadService>( songId ).exceptionOrNull()?.let {
                    if (it is CancellationException) throw it

                    Logger.e( it, "DownloadHelperImpl" ) { "purgeDownload failed for $songId!" }
                }
            } else {
                // No index row, so DownloadManager.removeDownload() would be a no-op and the
                // bytes would linger forever. Free them directly — nothing else can.
                runCatching { downloadCache.removeResource( songId ) }
                    .onFailure {
                        Logger.w( "DownloadHelperImpl" ) { "Could not evict orphaned bytes for $songId: ${it.message}" }
                    }
            }
        }
    }

    /** The uri carries the download quality when it isn't the playback one, see [DOWNLOAD_QUALITY_PARAM] */
    private fun downloadRequestOf( songId: String, notificationTitle: ByteArray ): DownloadRequest {
        val quality = Preferences.DOWNLOAD_QUALITY.value.format
        val uri = if( quality == null ) songId else "$songId?$DOWNLOAD_QUALITY_PARAM=$quality"
        return DownloadRequest.Builder( songId, uri.toUri() )
                              .setCustomCacheKey( songId )
                              .setData( notificationTitle )
                              .build()
    }

    override fun redownloadAll(): Int {
        val completed = downloads.value.values.filter { it.state == Download.STATE_COMPLETED }
        if( completed.isEmpty() ) return 0
        if( !isNetworkConnected( context ) ) {
            Toaster.noInternet()
            return 0
        }

        coroutineScope.launch {
            completed.forEach { download ->
                val songId = download.request.id
                // Removing first: adding a completed download again downloads nothing,
                // its bytes are already there. The manager runs both in order.
                context.removeDownload<MyDownloadService>( songId )
                context.download<MyDownloadService>( downloadRequestOf( songId, download.request.data ) )
            }
        }
        return completed.size
    }

    override fun purgeAllDownloads() {
        coroutineScope.launch {
            runCatching {
                DownloadService.sendRemoveAllDownloads(
                    /* context    = */ context,
                    /* clazz      = */ MyDownloadService::class.java,
                    /* foreground = */ false
                )
            }.onFailure {
                if (it is CancellationException) throw it

                Logger.e( it, "DownloadHelperImpl" ) { "purgeAllDownloads failed!" }
            }
        }
    }

    override fun autoDownload( mediaItem: MediaItem ) {
        if ( Preferences.AUTO_DOWNLOAD.value ) {
            // Same both-stores predicate as the badge: a COMPLETED row whose bytes are gone
            // must download again, not be skipped as already present.
            val indexState = downloads.value[mediaItem.mediaId]?.state
            if( !DownloadCacheState.isDownloaded( indexState, downloadCache, mediaItem.mediaId ) )
                addDownload( mediaItem )
        }
    }

    override fun autoDownloadWhenLiked( mediaItem: MediaItem ) {
        if ( Preferences.AUTO_DOWNLOAD_ON_LIKE.value ) {
            Database.asyncQuery {
                runBlocking {
                    if( songTable.isLiked( mediaItem.mediaId ).first() )
                        autoDownload(mediaItem)
                    else
                        removeDownload(mediaItem)
                }
            }
        }
    }

    override fun downloadOnLike( mediaItem: MediaItem, likeState: Boolean? ) {
        // Only continues when this setting is enabled
        val isSettingEnabled by Preferences.AUTO_DOWNLOAD_ON_LIKE
        if( !isSettingEnabled || !isNetworkConnected( context ) )
            return

        // [likeState] is a tri-state value,
        // only `true` represents like, so
        // `true` must be value set to download
        if( likeState == true )
            autoDownload( mediaItem)
        else
            removeDownload( mediaItem)
    }

    override fun handleDownload( song: Song, removeIfDownloaded: Boolean ) {
        if( song.isLocal ) {
            Logger.i( TAG ) { "handleDownload ${song.id} ignored: local file" }
            return
        }

        val indexState = downloads.value[song.id]?.state
        // Must be the predicate the badge renders. Deciding from the index alone makes a tap on
        // a badge that reads "not downloaded" remove a stale COMPLETED row instead of starting
        // the download — the badge never changes and the button appears dead. media3 re-queues
        // an existing COMPLETED row on addDownload, so the stale case needs no removal first.
        val isDownloaded = DownloadCacheState.isDownloaded( indexState, downloadCache, song.id )

        Logger.i( TAG ) {
            "handleDownload ${song.id}: index=${indexState?.let( ::stateName ) ?: "none"}" +
            " downloaded=$isDownloaded removeIfDownloaded=$removeIfDownloaded"
        }

        if( isDownloaded && removeIfDownloaded )
            removeDownload( song.asMediaItem )
        else if( !isDownloaded )
            addDownload( song.asMediaItem )
    }
}