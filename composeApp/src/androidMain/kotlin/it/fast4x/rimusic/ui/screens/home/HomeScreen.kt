package it.fast4x.rimusic.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.Preferences
import app.kreate.android.R
import app.kreate.android.themed.rimusic.screen.home.HomeSongsScreen
import it.fast4x.compose.persist.PersistMapCleanup
import it.fast4x.rimusic.enums.NavRoutes
import it.fast4x.rimusic.models.toUiMood
import it.fast4x.rimusic.ui.components.Skeleton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.fast4x.rimusic.ui.components.ButtonsRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.utils.color
import it.fast4x.rimusic.utils.semiBold
import it.fast4x.rimusic.enums.BuiltInPlaylist


@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun HomeScreen(
    navController: NavController,
    onPlaylistUrl: (String) -> Unit,
    miniPlayer: @Composable () -> Unit = {}
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    PersistMapCleanup("home/")

    val (tabIndex, onTabChanged) = Preferences.HOME_TAB_INDEX

    // Three tabs like most music apps: Home (the account's content), Discover
    // (new releases, moods, charts) and Library, where songs, artists, albums
    // and playlists (tabs 1 to 4) become chips. Library reopens the last one used.
    var lastLibraryTab by rememberSaveable { mutableIntStateOf( LIBRARY_TABS.first ) }
    if( tabIndex in LIBRARY_TABS ) lastLibraryTab = tabIndex
    val libraryTab = if( tabIndex in LIBRARY_TABS ) tabIndex else lastLibraryTab

    Skeleton(
        navController,
        tabIndex,
        onTabChanged,
        miniPlayer,
        navBarContent = { Item ->
            if ( Preferences.QUICK_PICKS_PAGE.value ) {
                Item(0, stringResource(R.string.home), R.drawable.home)
                Item(DISCOVER_TAB, stringResource(R.string.discover), R.drawable.globe)
            }
            Item(libraryTab, stringResource(R.string.library), R.drawable.library)
        }
    ) { currentTabIndex ->
        saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
            if( currentTabIndex in LIBRARY_TABS )
                LibraryTabs(
                    currentTab = currentTabIndex,
                    onTabChanged = onTabChanged,
                    onHistory = { NavRoutes.history.navigateHere( navController ) },
                    onDownloads = {
                        Preferences.HOME_SONGS_TYPE.value = BuiltInPlaylist.Downloaded
                        onTabChanged( 1 )
                    }
                ) {
                    LibraryTab( currentTabIndex, navController )
                }
            else when (currentTabIndex) {
                DISCOVER_TAB -> HomeQuickPicks(
                    onSearchClick = {
                        NavRoutes.search.navigateHere( navController )
                    },
                    onMoodClick = { mood ->
                        navController.currentBackStackEntry?.savedStateHandle?.set("mood", mood.toUiMood())
                        NavRoutes.mood.navigateHere( navController )
                    },
                    onSettingsClick = {
                        NavRoutes.settings.navigateHere( navController )
                    },
                    navController = navController,
                    content = HomeContent.Discover
                )

                else -> HomeQuickPicks(
                    onSearchClick = {
                        NavRoutes.search.navigateHere( navController )
                    },
                    onMoodClick = { mood ->
                        navController.currentBackStackEntry?.savedStateHandle?.set("mood", mood.toUiMood())
                        NavRoutes.mood.navigateHere( navController )
                    },
                    onSettingsClick = {
                        NavRoutes.settings.navigateHere( navController )
                    },
                    navController = navController,
                    content = HomeContent.Home
                )
            }
        }
    }
}

private const val DISCOVER_TAB = 6
private val LIBRARY_TABS = 1..4

/** Songs, artists, albums and playlists as chips on top of the Library tab */
@Composable
private fun LibraryTabs(
    currentTab: Int,
    onTabChanged: (Int) -> Unit,
    onHistory: () -> Unit,
    onDownloads: () -> Unit,
    content: @Composable () -> Unit
) {
    Column( Modifier.fillMaxSize() ) {
        ButtonsRow(
            chips = listOf(
                1 to stringResource( R.string.songs ),
                2 to stringResource( R.string.artists ),
                3 to stringResource( R.string.albums ),
                4 to stringResource( R.string.playlists )
            ),
            currentValue = currentTab,
            onValueUpdate = onTabChanged,
            modifier = Modifier.padding( vertical = 8.dp )
        )

        // History and downloads used to hide in the avatar menu and a song filter
        Row(
            horizontalArrangement = Arrangement.spacedBy( 8.dp ),
            modifier = Modifier.padding( horizontal = 16.dp ).padding( bottom = 4.dp )
        ) {
            LibraryShortcut( stringResource( R.string.history ), R.drawable.history, onHistory, Modifier.weight( 1f ) )
            LibraryShortcut( stringResource( R.string.downloaded ), R.drawable.download, onDownloads, Modifier.weight( 1f ) )
        }

        Box( Modifier.weight( 1f ) ) { content() }
    }
}

@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
private fun LibraryTab( tab: Int, navController: NavController ) {
            when (tab) {
                1 -> HomeSongsScreen( navController )

                2 -> HomeArtists(
                    navController = navController,
                    onSearchClick = {
                        NavRoutes.search.navigateHere( navController )
                    },
                    onSettingsClick = {
                        NavRoutes.settings.navigateHere( navController )
                    }
                )

                3 -> HomeAlbums(
                    navController = navController,
                    onSearchClick = {
                        NavRoutes.search.navigateHere( navController )
                    },
                    onSettingsClick = {
                        NavRoutes.settings.navigateHere( navController )
                    }
                )

                4 -> HomeLibrary(
                    navController,
                    onSearchClick = {
                        NavRoutes.search.navigateHere( navController )
                    },
                    onSettingsClick = {
                        NavRoutes.settings.navigateHere( navController )
                    }

                )
            }
}

@Composable
private fun LibraryShortcut(
    text: String,
    iconId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) =
    Row(
        horizontalArrangement = Arrangement.spacedBy( 10.dp ),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clip( RoundedCornerShape( 10.dp ) )
                           .background( colorPalette().background1 )
                           .clickable( onClick = onClick )
                           .padding( horizontal = 12.dp, vertical = 10.dp )
    ) {
        Icon(
            painter = painterResource( iconId ),
            contentDescription = null,
            tint = colorPalette().text,
            modifier = Modifier.size( 20.dp )
        )
        BasicText(
            text = text,
            style = typography().xs.semiBold.color( colorPalette().text ),
            maxLines = 1
        )
    }
