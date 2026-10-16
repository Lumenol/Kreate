package app.kreate.android.themed.common.component.menu

import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import app.kreate.android.themed.common.component.BottomMenu
import it.fast4x.rimusic.service.MyDownloadHelper
import it.fast4x.rimusic.utils.asSong
import me.knighthat.utils.Toaster

/**
 * "Download" in the player's ⋮ (the playing song had no way to be downloaded from the
 * player), "Downloading…" while it is, "Remove the download" once it is. [state] is read
 * when the menu opens, like the download icon of song rows; the menu closes on tap, so
 * a stale label can't be tapped twice.
 */
@UnstableApi
class SongDownloadButton( private val state: State ) : MenuButton<MediaItem>() {

    enum class State { NotDownloaded, InProgress, Downloaded }

    override val iconId: Int = when( state ) {
        State.Downloaded -> R.drawable.downloaded
        State.InProgress -> R.drawable.download_progress
        State.NotDownloaded -> R.drawable.download
    }
    override val tooltipMessageId: Int = when( state ) {
        State.Downloaded -> R.string.menu_remove_download
        State.InProgress -> R.string.menu_downloading
        State.NotDownloaded -> R.string.download
    }

    override fun onClick( menu: BottomMenu, item: MediaItem ) {
        when( state ) {
            // Only removes when it really is downloaded (re-checked there)
            State.Downloaded -> MyDownloadHelper.handleDownload( item.asSong, removeIfDownloaded = true )
            // Already on its way: nothing to start again (see the downloads page)
            State.InProgress -> Toaster.i( R.string.menu_downloading )
            // Also resumes a paused one and retries a failed one
            State.NotDownloaded -> MyDownloadHelper.addDownload( item )
        }
        menu.hide()
    }
}
