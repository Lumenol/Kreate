package app.kreate.android.themed.common.component.menu

import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import app.kreate.android.themed.common.component.BottomMenu
import it.fast4x.rimusic.service.MyDownloadHelper
import it.fast4x.rimusic.utils.asSong

/**
 * "Download" in the player's ⋮ (the playing song had no way to be downloaded from
 * the player), or "Remove the download" when it already is: [downloaded] is read when
 * the menu opens, the way the download icon of song rows reads it.
 */
@UnstableApi
class SongDownloadButton( private val downloaded: Boolean ) : MenuButton<MediaItem>() {

    override val iconId: Int = if( downloaded ) R.drawable.downloaded else R.drawable.download
    override val tooltipMessageId: Int = if( downloaded ) R.string.menu_remove_download else R.string.download

    override fun onClick( menu: BottomMenu, item: MediaItem ) =
        MyDownloadHelper.handleDownload( item.asSong, removeIfDownloaded = true )
}
