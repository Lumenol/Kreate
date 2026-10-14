package it.fast4x.rimusic.ui.screens.localplaylist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.github.doyaaaaaken.kotlincsv.client.KotlinCsvExperimental
import it.fast4x.compose.persist.PersistMapCleanup

@OptIn(KotlinCsvExperimental::class)
@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun LocalPlaylistScreen(
    navController: NavController,
    playlistId: Long,
    modifier: Modifier = Modifier,
    miniPlayer: @Composable () -> Unit = {}
) {
    PersistMapCleanup(tagPrefix = "localPlaylist/$playlistId/")

    // Top bar, main tabs and mini player come from DetailSkeleton, like album pages
    LocalPlaylistSongs(
        navController = navController,
        playlistId = playlistId,
        miniPlayer = miniPlayer
    )
}
