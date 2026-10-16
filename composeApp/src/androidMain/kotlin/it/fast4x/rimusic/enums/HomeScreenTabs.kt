package it.fast4x.rimusic.enums

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.kreate.android.R
import it.fast4x.rimusic.ui.screens.settings.isYouTubeLoggedIn
import me.knighthat.enums.TextView

enum class HomeScreenTabs(
    val index: Int,
    @field:StringRes override val androidTextId: Int
): TextView {

    Default( 0, R.string._default ),

    QuickPics( 0, R.string.quick_picks ),

    Songs( 1, R.string.songs ),

    Artists( 2, R.string.artists ),

    Albums( 3, R.string.albums ),

    Playlists( 4, R.string.playlists ),

    Search( 5, android.R.string.search_go );

    // Named like the bottom bar: "Accueil" with a YouTube account, else "Sélection rapide"
    override val text: String
        @Composable
        get() = stringResource(
            if( this == QuickPics && isYouTubeLoggedIn() ) R.string.home else androidTextId
        )
}