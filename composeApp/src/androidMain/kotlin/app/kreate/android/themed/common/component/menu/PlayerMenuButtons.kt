package app.kreate.android.themed.common.component.menu

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.media3.common.MediaItem
import app.kreate.android.R
import app.kreate.android.constant.MenuPage
import app.kreate.android.service.player.StatefulPlayer
import app.kreate.android.themed.common.component.BottomMenu
import me.knighthat.utils.Toaster
import org.koin.java.KoinJavaComponent.get

/** Android's (or the phone maker's) audio effects panel for the playing session */
class EqualizerButton : MenuButton<MediaItem>() {

    override val iconId: Int = R.drawable.equalizer
    override val tooltipMessageId: Int = R.string.equalizer

    override fun onClick( menu: BottomMenu, item: MediaItem ) {
        menu.hide()
        val context = get<Context>( Context::class.java )
        val player = get<StatefulPlayer>( StatefulPlayer::class.java )
        try {
            context.startActivity(
                Intent( AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL ).apply {
                    putExtra( AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId )
                    putExtra( AudioEffect.EXTRA_PACKAGE_NAME, context.packageName )
                    putExtra( AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC )
                    addFlags( Intent.FLAG_ACTIVITY_NEW_TASK )
                }
            )
        } catch( _: ActivityNotFoundException ) {
            Toaster.e( R.string.info_not_find_application_audio )
        }
    }
}

/** Opens the sleep timer dialog over the menu */
class SleepTimerButton : MenuButton<MediaItem>() {

    override val iconId: Int = R.drawable.sleep
    override val tooltipMessageId: Int = R.string.sleep_timer

    override fun onClick( menu: BottomMenu, item: MediaItem ) {
        menu.isShowingSleepTimer = true
    }
}

/** Rename, change authors or cover: one entry leading to them, they're rarely used */
class EditSongButton : MenuButton<MediaItem>() {

    override val iconId: Int = R.drawable.pencil
    override val tooltipMessageId: Int = R.string.menu_edit_song

    override fun onClick( menu: BottomMenu, item: MediaItem ) = menu.show( MenuPage.EditSong( item ) )
}
