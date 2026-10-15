@file:androidx.media3.common.util.UnstableApi

package app.kreate.android.service

import androidx.media3.exoplayer.offline.Download
import it.fast4x.rimusic.enums.DownloadedStateMedia

/**
 * The single "what should the download badge look like" answer, shared by every place that
 * renders one: `app.kreate.android.themed.rimusic.component.song.SongItem`, the player's
 * `ActionBar`, and the queue/playlist swipe actions.
 *
 * `maxParallelDownloads = 3` in `DownloadHelperImpl` means a 4th+ tap on download sits in
 * [Download.STATE_QUEUED] (int `0`) — often for a long while — before it ever reaches
 * [Download.STATE_DOWNLOADING]. A badge that only special-cases `STATE_DOWNLOADING` renders the
 * plain "not downloaded" icon for a queued song, which is indistinguishable from a tap that did
 * nothing at all. [Download.STATE_RESTARTING] is the same situation after a transient failure
 * retry. All three states are therefore folded into one [IN_PROGRESS] badge here, so every call
 * site gets the fix by construction instead of re-deriving the state list (and re-missing a
 * state) on its own.
 *
 * This returns an enum, not a drawable resource id, on purpose: `SongItem` resolves icons
 * through Android's `R.drawable.*`, while the Compose-Multiplatform `ActionBar` resolves them
 * through `Res.drawable.*` — two different resource systems with no shared id space. Each call
 * site maps [DownloadBadge] to whichever one it needs; this function stays free of both (and of
 * Compose entirely) so it is a plain, trivially unit-testable function of two enums.
 */
enum class DownloadBadge {
    NOT_DOWNLOADED,
    IN_PROGRESS,
    DOWNLOADED;

    companion object {

        /**
         * @param downloadState media3's `Download.state` for this song, or `null` when the
         *                      download index has no row for it (never downloaded, or removed)
         * @param cacheState    the result of [it.fast4x.rimusic.utils.downloadedStateMedia] —
         *                      already reconciles the download index against the actual cache
         *                      bytes, so [DOWNLOADED] here means truly downloaded, not just a
         *                      possibly-stale index row
         */
        fun of( downloadState: Int?, cacheState: DownloadedStateMedia ): DownloadBadge =
            when( downloadState ) {
                Download.STATE_QUEUED,
                Download.STATE_DOWNLOADING,
                Download.STATE_RESTARTING -> IN_PROGRESS

                // A removal in flight is not "downloaded" — show it as not-downloaded
                // regardless of cacheState, same as every other call site now agrees on.
                Download.STATE_REMOVING   -> NOT_DOWNLOADED

                else                      -> when( cacheState ) {
                    DownloadedStateMedia.DOWNLOADED,
                    DownloadedStateMedia.CACHED_AND_DOWNLOADED -> DOWNLOADED

                    else                                       -> NOT_DOWNLOADED
                }
            }
    }
}
