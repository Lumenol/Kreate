@file:androidx.media3.common.util.UnstableApi

package app.kreate.android.service

import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.ContentMetadata

/**
 * Answers "does this cache actually hold the whole resource?" — the question the
 * download badge must ask, and the one media3's [androidx.media3.exoplayer.offline.DownloadIndex]
 * cannot answer.
 *
 * The index and the cache are independent stores. A row can say
 * [androidx.media3.exoplayer.offline.Download.STATE_COMPLETED] long after the bytes were
 * removed (see `docs/superpowers/plans/2026-09-03-download-state-desync-and-audio-only-playback.md`),
 * which is why the UI must consult both.
 */
object DownloadCacheState {

    /**
     * `true` only when [cache] holds every byte of [key].
     *
     * The content length comes from the cache's own [ContentMetadata], not from the Room
     * `formats` table: `SimpleCache.removeResource` and a full LRU eviction drop the metadata
     * together with the spans, so a wiped resource reports `C.LENGTH_UNSET` (-1) and fails the
     * `> 0` guard. A partially evicted resource keeps its metadata but fails [Cache.isCached].
     *
     * Never throws — a released or uninitialized cache reports `false`, matching how
     * [it.fast4x.rimusic.utils.downloadedStateMedia] already treats an unreachable cache.
     */
    fun isFullyCached( cache: Cache, key: String ): Boolean =
        runCatching {
            val length = ContentMetadata.getContentLength( cache.getContentMetadata( key ) )
            length > 0 && cache.isCached( key, 0, length )
        }.getOrDefault( false )
}
