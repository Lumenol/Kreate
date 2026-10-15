package app.kreate.android.service

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * media3's DownloadManager owns the download cache. Anything that deletes bytes from it
 * directly leaves the DownloadIndex claiming the song is still downloaded, which is exactly
 * the desync this guard exists to prevent.
 *
 * Only [me.knighthat.impl.DownloadHelperImpl] may touch the download cache directly, and only
 * to clean up bytes with no index row behind them.
 */
class DownloadCacheOwnershipTest {

    private companion object {
        /** Files permitted to combine a DOWNLOAD-cache reference with a direct eviction. */
        val ALLOWED = setOf( "DownloadHelperImpl.kt" )
    }

    private fun androidMainRoot(): File {
        var dir: File? = File( System.getProperty( "user.dir" )!! ).absoluteFile
        while ( dir != null ) {
            File( dir, "src/androidMain/kotlin" ).takeIf( File::isDirectory )?.let { return it }
            File( dir, "composeApp/src/androidMain/kotlin" ).takeIf( File::isDirectory )?.let { return it }
            dir = dir.parentFile
        }
        error( "Could not locate composeApp/src/androidMain/kotlin from ${System.getProperty( "user.dir" )}" )
    }

    @Test
    fun onlyDownloadHelperMayEvictFromTheDownloadCache() {
        val root = androidMainRoot()

        val offenders = root.walkTopDown()
                            .filter { it.isFile && it.extension == "kt" }
                            .filterNot { it.name in ALLOWED }
                            .filter { file ->
                                val text = file.readText()
                                "CacheType.DOWNLOAD" in text && "removeResource" in text
                            }
                            .map { it.relativeTo( root ).path }
                            .toList()

        assertTrue(
            offenders.isEmpty(),
            "These files evict from the download cache directly instead of going through " +
            "DownloadHelper.purgeDownload()/purgeAllDownloads(), which desyncs the " +
            "DownloadIndex from the cached bytes:\n" + offenders.joinToString( "\n" ) { "  - $it" }
        )
    }
}
