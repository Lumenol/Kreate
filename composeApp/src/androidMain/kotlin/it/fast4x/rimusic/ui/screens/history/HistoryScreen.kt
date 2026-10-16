package it.fast4x.rimusic.ui.screens.history

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.R
import it.fast4x.compose.persist.PersistMapCleanup
import it.fast4x.rimusic.utils.color
import it.fast4x.rimusic.utils.semiBold

@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@UnstableApi
@Composable
fun HistoryScreen(
    navController: NavController,
    miniPlayer: @Composable () -> Unit = {},
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    PersistMapCleanup(tagPrefix = "history")

    // A page of its own like Downloads: back arrow and title, the bottom bar
    // stays. It used the app header + a one-tab bar, a third of the screen
    app.kreate.android.themed.common.component.DetailSkeleton(
        navController = navController,
        miniPlayer = miniPlayer,
        title = {
            androidx.compose.foundation.text.BasicText(
                text = stringResource( R.string.history ),
                style = it.fast4x.rimusic.typography().l.semiBold.color( it.fast4x.rimusic.colorPalette().text )
            )
        }
    ) {
        HistoryList( navController = navController )
    }
}
