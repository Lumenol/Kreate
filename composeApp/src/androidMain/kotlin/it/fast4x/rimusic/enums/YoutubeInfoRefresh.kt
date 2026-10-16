package it.fast4x.rimusic.enums

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.kreate.android.R
import me.knighthat.enums.TextView

/** How old a song's YouTube info (release date, views) gets before it's asked again */
enum class YoutubeInfoRefresh( val days: Int? ): TextView {

    Days30( 30 ),

    Days90( 90 ),

    Days180( 180 ),

    Days365( 365 ),

    /** Asked once, never again */
    Never( null );

    val asMillis: Long? = days?.let { it * 86_400_000L }

    override val text: String
        @Composable
        get() = if( days == null ) stringResource( R.string.library_filters_refresh_never )
                else stringResource( R.string.library_filters_refresh_days, days )
}
