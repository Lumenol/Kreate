@file:androidx.media3.common.util.UnstableApi

package app.kreate.di

import androidx.media3.datasource.cache.CacheEvictor
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor

/**
 * Pick the evictor for a media cache.
 *
 * @param maxSizeBytes user-configured ceiling; `0` (disabled) and [Long.MAX_VALUE] (unlimited)
 *                     both mean "no LRU bound"
 * @param allowEviction `false` for the download cache. media3's
 *                      [androidx.media3.exoplayer.offline.DownloadManager] assumes it is the
 *                      only thing removing content from that cache — an evictor there deletes
 *                      finished downloads while their index rows still report
 *                      `STATE_COMPLETED`, so the UI keeps showing a downloaded badge for a
 *                      song that now has to be re-fetched from the network.
 */
fun cacheEvictorFor( maxSizeBytes: Long, allowEviction: Boolean ): CacheEvictor =
    if ( !allowEviction || maxSizeBytes == 0L || maxSizeBytes == Long.MAX_VALUE )
        NoOpCacheEvictor()
    else
        LeastRecentlyUsedCacheEvictor( maxSizeBytes )
