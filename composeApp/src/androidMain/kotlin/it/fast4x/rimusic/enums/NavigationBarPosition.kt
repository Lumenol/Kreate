package it.fast4x.rimusic.enums

import androidx.annotation.StringRes
import app.kreate.android.Preferences
import app.kreate.android.R
import me.knighthat.enums.TextView
import androidx.compose.runtime.mutableStateOf

enum class NavigationBarPosition(
    @field:StringRes override val androidTextId: Int
): TextView {

    Left( R.string.direction_left ),
    Right( R.string.direction_right ),
    Top( R.string.direction_top ),
    Bottom( R.string.direction_bottom );

    companion object {

        /**
         * Kept up to date by [it.fast4x.rimusic.MainActivity] from its configuration.
         * A state, so screens reading [current] recompose when the phone rotates.
         */
        val isLandscape = mutableStateOf( false )

        /**
         * The user's setting, except in landscape where a top or bottom bar becomes
         * a rail on the left: horizontal bars ate most of the height there.
         */
        fun current(): NavigationBarPosition {
            val position = Preferences.NAVIGATION_BAR_POSITION.value
            return if( isLandscape.value && ( position == Top || position == Bottom ) ) Left else position
        }
    }

    fun isCurrent(): Boolean = current() == this
}