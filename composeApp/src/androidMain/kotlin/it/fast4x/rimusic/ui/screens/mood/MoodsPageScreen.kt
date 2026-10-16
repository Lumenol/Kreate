package it.fast4x.rimusic.ui.screens.mood

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.R
import it.fast4x.compose.persist.PersistMapCleanup
import app.kreate.android.themed.common.component.DetailSkeleton
import androidx.compose.foundation.text.BasicText
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.utils.color
import it.fast4x.rimusic.utils.semiBold

@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@UnstableApi
@Composable
fun MoodsPageScreen(
    navController: NavController,
    miniPlayer: @Composable () -> Unit = {}
) {
    PersistMapCleanup(tagPrefix = "playlist/$defaultBrowseId")

    // A page of its own like History: title in the bar, mini player and bottom
    // bar stay (it had neither)
    DetailSkeleton(
        navController = navController,
        miniPlayer = miniPlayer,
        title = {
            BasicText(
                text = stringResource( R.string.moods_and_genres ),
                style = typography().l.semiBold.color( colorPalette().text )
            )
        }
    ) {
        MoodsPage( navController = navController )
    }
}
