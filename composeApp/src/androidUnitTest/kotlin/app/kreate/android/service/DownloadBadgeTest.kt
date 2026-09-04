@file:androidx.media3.common.util.UnstableApi

package app.kreate.android.service

import androidx.media3.exoplayer.offline.Download
import it.fast4x.rimusic.enums.DownloadedStateMedia
import kotlin.test.Test
import kotlin.test.assertEquals

class DownloadBadgeTest {

    // --- In-progress states: must win regardless of cache state, including QUEUED (int 0),
    // which a naive truthiness/if-nonzero check on downloadState would miss. ---

    @Test
    fun queuedIsInProgress() {
        DownloadedStateMedia.entries.forEach { cacheState ->
            assertEquals(
                DownloadBadge.IN_PROGRESS,
                DownloadBadge.of( Download.STATE_QUEUED, cacheState ),
                "STATE_QUEUED with cacheState=$cacheState"
            )
        }
    }

    @Test
    fun downloadingIsInProgress() {
        DownloadedStateMedia.entries.forEach { cacheState ->
            assertEquals(
                DownloadBadge.IN_PROGRESS,
                DownloadBadge.of( Download.STATE_DOWNLOADING, cacheState ),
                "STATE_DOWNLOADING with cacheState=$cacheState"
            )
        }
    }

    @Test
    fun restartingIsInProgress() {
        DownloadedStateMedia.entries.forEach { cacheState ->
            assertEquals(
                DownloadBadge.IN_PROGRESS,
                DownloadBadge.of( Download.STATE_RESTARTING, cacheState ),
                "STATE_RESTARTING with cacheState=$cacheState"
            )
        }
    }

    // --- Terminal / inactive states: badge follows cacheState. ---

    @Test
    fun completedFollowsCacheState() {
        assertEquals( DownloadBadge.DOWNLOADED, DownloadBadge.of( Download.STATE_COMPLETED, DownloadedStateMedia.DOWNLOADED ) )
        assertEquals( DownloadBadge.DOWNLOADED, DownloadBadge.of( Download.STATE_COMPLETED, DownloadedStateMedia.CACHED_AND_DOWNLOADED ) )
        assertEquals( DownloadBadge.NOT_DOWNLOADED, DownloadBadge.of( Download.STATE_COMPLETED, DownloadedStateMedia.CACHED ) )
        assertEquals( DownloadBadge.NOT_DOWNLOADED, DownloadBadge.of( Download.STATE_COMPLETED, DownloadedStateMedia.NOT_CACHED_OR_DOWNLOADED ) )
    }

    @Test
    fun failedFollowsCacheState() {
        assertEquals( DownloadBadge.NOT_DOWNLOADED, DownloadBadge.of( Download.STATE_FAILED, DownloadedStateMedia.NOT_CACHED_OR_DOWNLOADED ) )
        assertEquals( DownloadBadge.NOT_DOWNLOADED, DownloadBadge.of( Download.STATE_FAILED, DownloadedStateMedia.CACHED ) )
        assertEquals( DownloadBadge.DOWNLOADED, DownloadBadge.of( Download.STATE_FAILED, DownloadedStateMedia.DOWNLOADED ) )
    }

    @Test
    fun stoppedFollowsCacheState() {
        assertEquals( DownloadBadge.NOT_DOWNLOADED, DownloadBadge.of( Download.STATE_STOPPED, DownloadedStateMedia.NOT_CACHED_OR_DOWNLOADED ) )
        assertEquals( DownloadBadge.DOWNLOADED, DownloadBadge.of( Download.STATE_STOPPED, DownloadedStateMedia.CACHED_AND_DOWNLOADED ) )
    }

    @Test
    fun removingFollowsCacheState() {
        assertEquals( DownloadBadge.NOT_DOWNLOADED, DownloadBadge.of( Download.STATE_REMOVING, DownloadedStateMedia.NOT_CACHED_OR_DOWNLOADED ) )
        assertEquals( DownloadBadge.DOWNLOADED, DownloadBadge.of( Download.STATE_REMOVING, DownloadedStateMedia.DOWNLOADED ) )
    }

    // --- No index row at all: no download in progress, badge follows cacheState only. ---

    @Test
    fun nullDownloadStateFollowsCacheState() {
        assertEquals( DownloadBadge.NOT_DOWNLOADED, DownloadBadge.of( null, DownloadedStateMedia.NOT_CACHED_OR_DOWNLOADED ) )
        assertEquals( DownloadBadge.NOT_DOWNLOADED, DownloadBadge.of( null, DownloadedStateMedia.CACHED ) )
        assertEquals( DownloadBadge.DOWNLOADED, DownloadBadge.of( null, DownloadedStateMedia.DOWNLOADED ) )
        assertEquals( DownloadBadge.DOWNLOADED, DownloadBadge.of( null, DownloadedStateMedia.CACHED_AND_DOWNLOADED ) )
    }
}
