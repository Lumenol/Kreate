@file:androidx.media3.common.util.UnstableApi

package app.kreate.di

import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import kotlin.test.Test
import kotlin.test.assertTrue

class CacheEvictorsTest {

    /**
     * The download cache is owned by media3's DownloadManager. Any evictor on it would
     * silently delete finished downloads while their index rows still say STATE_COMPLETED.
     */
    @Test
    fun downloadCacheNeverEvicts() {
        assertTrue( cacheEvictorFor( 2_048_000_000L, allowEviction = false ) is NoOpCacheEvictor )
        assertTrue( cacheEvictorFor( 32_000_000L, allowEviction = false ) is NoOpCacheEvictor )
        assertTrue( cacheEvictorFor( Long.MAX_VALUE, allowEviction = false ) is NoOpCacheEvictor )
        assertTrue( cacheEvictorFor( 0L, allowEviction = false ) is NoOpCacheEvictor )
    }

    @Test
    fun streamCacheEvictsWhenBounded() {
        assertTrue( cacheEvictorFor( 2_048_000_000L, allowEviction = true ) is LeastRecentlyUsedCacheEvictor )
    }

    @Test
    fun streamCacheDoesNotEvictWhenUnbounded() {
        assertTrue( cacheEvictorFor( Long.MAX_VALUE, allowEviction = true ) is NoOpCacheEvictor )
        assertTrue( cacheEvictorFor( 0L, allowEviction = true ) is NoOpCacheEvictor )
    }
}
