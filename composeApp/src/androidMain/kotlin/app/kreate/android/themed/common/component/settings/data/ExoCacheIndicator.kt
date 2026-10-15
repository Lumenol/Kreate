package app.kreate.android.themed.common.component.settings.data

import androidx.compose.runtime.getValue
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import app.kreate.android.Preferences

@UnstableApi
class ExoCacheIndicator(
    private val preference: Preferences.Long,
    private val cache: Cache,
    /**
     * How to empty [cache]. Defaults to a direct eviction, which is correct for the stream
     * cache. The **download** cache must instead pass `MyDownloadHelper::purgeAllDownloads`,
     * because media3's `DownloadManager` owns it and a direct eviction would leave every
     * index row claiming the song is still downloaded.
     */
    private val clearAll: () -> Unit = { cache.keys.forEach( cache::removeResource ) }
): CacheUsageIndicator() {

    override fun updateProgress() {
        val maxSize by preference
        super.progress = if( maxSize == 0L )
            0f
        else
            cache.cacheSpace.toFloat() / maxSize
    }

    override fun onConfirm() {
        clearAll()
        updateProgress()
        hideDialog()
    }
}
