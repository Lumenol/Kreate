package it.fast4x.rimusic.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.kreate.android.R
import me.knighthat.enums.TextView

/** Where a song's preview (tap on its thumbnail) starts, as a share of its length */
enum class PreviewStart( val percent: Int ): TextView {

    Beginning( 0 ),

    Percent15( 15 ),

    Percent30( 30 ),

    Middle( 50 );

    override val text: String
        @Composable
        get() = when( this ) {
            Beginning -> stringResource( R.string.preview_start_beginning )
            Middle    -> stringResource( R.string.preview_start_middle )
            else      -> "$percent %"
        }
}
