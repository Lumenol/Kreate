package app.kreate.android.themed.common.component.menu

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import app.kreate.android.service.player.StatefulPlayer
import app.kreate.android.themed.common.component.BottomMenu
import it.fast4x.rimusic.utils.forcePlay
import org.koin.java.KoinJavaComponent.get


/** Plays the song now, in place of the queue: for a song opened from a link */
class SongPlayNowButton : MenuButton<MediaItem>() {

    override val iconId: Int = R.drawable.play
    override val tooltipMessageId: Int = R.string.play_now

    @OptIn(UnstableApi::class)
    override fun onClick( menu: BottomMenu, item: MediaItem ) {
        get<StatefulPlayer>(StatefulPlayer::class.java).forcePlay( item )
        menu.hide()
    }
}
