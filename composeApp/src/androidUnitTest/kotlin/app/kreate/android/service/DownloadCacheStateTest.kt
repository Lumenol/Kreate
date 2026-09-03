@file:androidx.media3.common.util.UnstableApi

package app.kreate.android.service

import androidx.media3.common.C
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheSpan
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.ContentMetadataMutations
import java.io.File
import java.util.NavigableSet
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Minimal [Cache] stand-in. Only the three members [DownloadCacheState.isFullyCached]
 * touches are implemented; everything else throws so an accidental new dependency
 * shows up as a loud failure rather than a silent default.
 */
private class FakeCache(
    private val contentLength: Long,
    private val cachedBytes: Long,
    private val throwOnAccess: Boolean = false
) : Cache {

    private inner class FakeMetadata : ContentMetadata {
        override fun get( key: String, defaultValue: ByteArray? ): ByteArray? = defaultValue
        override fun get( key: String, defaultValue: String? ): String? = defaultValue
        override fun get( key: String, defaultValue: Long ): Long =
            if ( key == ContentMetadata.KEY_CONTENT_LENGTH ) contentLength else defaultValue
        override fun contains( key: String ): Boolean = key == ContentMetadata.KEY_CONTENT_LENGTH
    }

    override fun getContentMetadata( key: String ): ContentMetadata {
        if ( throwOnAccess ) throw IllegalStateException( "cache released" )
        return FakeMetadata()
    }

    override fun isCached( key: String, position: Long, length: Long ): Boolean {
        if ( throwOnAccess ) throw IllegalStateException( "cache released" )
        return cachedBytes >= position + length
    }

    private fun nope(): Nothing = throw UnsupportedOperationException( "Not needed for this test" )
    override fun getUid(): Long = nope()
    override fun release() = nope()
    override fun addListener( key: String, listener: Cache.Listener ): NavigableSet<CacheSpan> = nope()
    override fun removeListener( key: String, listener: Cache.Listener ) = nope()
    override fun getCachedSpans( key: String ): NavigableSet<CacheSpan> = nope()
    override fun getKeys(): MutableSet<String> = nope()
    override fun getCacheSpace(): Long = nope()
    override fun startReadWrite( key: String, position: Long, length: Long ): CacheSpan = nope()
    override fun startReadWriteNonBlocking( key: String, position: Long, length: Long ): CacheSpan = nope()
    override fun startFile( key: String, position: Long, length: Long ): File = nope()
    override fun commitFile( file: File, length: Long ) = nope()
    override fun releaseHoleSpan( holeSpan: CacheSpan ) = nope()
    override fun removeResource( key: String ) = nope()
    override fun removeSpan( span: CacheSpan ) = nope()
    override fun getCachedLength( key: String, position: Long, length: Long ): Long = nope()
    override fun getCachedBytes( key: String, position: Long, length: Long ): Long = nope()
    override fun applyContentMetadataMutations( key: String, mutations: ContentMetadataMutations ) = nope()
}

class DownloadCacheStateTest {

    @Test
    fun fullyCachedResourceIsReported() {
        val cache = FakeCache( contentLength = 4_000_000L, cachedBytes = 4_000_000L )
        assertTrue( DownloadCacheState.isFullyCached( cache, "abc" ) )
    }

    /** Bytes wiped by removeResource(): the metadata goes with them, so length is UNSET. */
    @Test
    fun wipedResourceIsNotReported() {
        val cache = FakeCache( contentLength = C.LENGTH_UNSET.toLong(), cachedBytes = 0L )
        assertFalse( DownloadCacheState.isFullyCached( cache, "abc" ) )
    }

    /** Partially evicted by an LRU evictor: metadata survives, spans do not cover it. */
    @Test
    fun partiallyEvictedResourceIsNotReported() {
        val cache = FakeCache( contentLength = 4_000_000L, cachedBytes = 512_000L )
        assertFalse( DownloadCacheState.isFullyCached( cache, "abc" ) )
    }

    /** A zero-length resource is never "downloaded". */
    @Test
    fun zeroLengthResourceIsNotReported() {
        val cache = FakeCache( contentLength = 0L, cachedBytes = 0L )
        assertFalse( DownloadCacheState.isFullyCached( cache, "abc" ) )
    }

    /** A released cache must degrade to false, never propagate the exception into composition. */
    @Test
    fun releasedCacheIsNotReported() {
        val cache = FakeCache( contentLength = 4_000_000L, cachedBytes = 4_000_000L, throwOnAccess = true )
        assertFalse( DownloadCacheState.isFullyCached( cache, "abc" ) )
    }
}
