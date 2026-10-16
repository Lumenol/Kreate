package app.kreate.android.themed.rimusic.screen.home

import androidx.compose.runtime.LaunchedEffect
import app.kreate.android.themed.rimusic.screen.home.filters.SongFilters
import app.kreate.android.themed.rimusic.screen.home.filters.SongFiltersStore
import app.kreate.android.themed.rimusic.screen.home.filters.FilterSheet
import app.kreate.android.themed.rimusic.screen.home.filters.FilterButton
import app.kreate.android.themed.rimusic.screen.home.filters.ActiveFilters
import it.fast4x.rimusic.ui.components.tab.toolbar.MenuHeader
import it.fast4x.rimusic.utils.semiBold
import it.fast4x.rimusic.utils.color
import it.fast4x.rimusic.typography
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.clickable
import androidx.compose.runtime.collectAsState
import androidx.media3.exoplayer.offline.Download
import it.fast4x.rimusic.service.MyDownloadHelper
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.navigation.NavController
import app.kreate.android.Preferences
import app.kreate.android.R
import app.kreate.android.service.player.StatefulPlayer
import app.kreate.android.themed.common.component.settings.SettingComponents
import app.kreate.android.themed.common.component.settings.data.ExoCacheIndicator
import app.kreate.android.themed.rimusic.component.ItemSelector
import app.kreate.android.themed.rimusic.component.Search
import app.kreate.android.themed.rimusic.screen.home.onDevice.OnDeviceSong
import app.kreate.database.models.Song
import app.kreate.di.CacheType
import co.touchlab.kermit.Logger
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.BuiltInPlaylist
import it.fast4x.rimusic.enums.NavRoutes
import it.fast4x.rimusic.enums.NavigationBarPosition
import it.fast4x.rimusic.enums.UiType
import it.fast4x.rimusic.ui.components.ButtonsRow
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.components.navigation.header.TabToolBar
import it.fast4x.rimusic.ui.components.tab.TabHeader
import it.fast4x.rimusic.ui.components.tab.toolbar.Button
import it.fast4x.rimusic.ui.components.themed.Enqueue
import it.fast4x.rimusic.ui.components.themed.ShareLinks
import app.kreate.android.utils.shareSongLinks
import it.fast4x.rimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.fast4x.rimusic.ui.components.themed.HeaderInfo
import it.fast4x.rimusic.ui.components.themed.MultiFloatingActionsContainer
import it.fast4x.rimusic.ui.components.themed.PlayNext
import it.fast4x.rimusic.ui.components.themed.PlaylistsMenu
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.utils.addNext
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.enqueue
import me.knighthat.component.ResetCache
import me.knighthat.component.tab.ImportSongsFromCSV
import me.knighthat.component.tab.LikeComponent
import me.knighthat.component.tab.Locator
import me.knighthat.component.tab.SongShuffler
import org.koin.compose.koinInject
import androidx.compose.ui.res.stringResource
import it.fast4x.rimusic.utils.forcePlayFromBeginning

@UnstableApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun HomeSongsScreen(
    navController: NavController,
    cache: Cache = koinInject(CacheType.CACHE),
    player: StatefulPlayer = koinInject()
) {
    // Essentials
    val lazyListState = rememberLazyListState()
    val context = LocalContext.current
    val menuState = LocalMenuState.current

    var builtInPlaylist by Preferences.HOME_SONGS_TYPE
    // Favorites, cached and downloaded are filters now: open the same songs, filtered
    LaunchedEffect( Unit ) {
        // The same view as before: these filters alone
        when( builtInPlaylist ) {
            BuiltInPlaylist.Favorites  -> SongFiltersStore.filters = SongFilters( liked = true )
            BuiltInPlaylist.Offline    -> SongFiltersStore.filters = SongFilters( cached = true )
            BuiltInPlaylist.Downloaded -> SongFiltersStore.filters = SongFilters( downloaded = true )
            else                       -> return@LaunchedEffect
        }
        builtInPlaylist = BuiltInPlaylist.All
    }

    val itemsOnDisplayState = remember { mutableStateListOf<Song>() }

    val itemSelector = remember {
        ItemSelector( menuState ) { addAll( itemsOnDisplayState ) }
    }
    fun getSongs() = itemSelector.ifEmpty { itemsOnDisplayState }.toList()
    fun getMediaItems() = getSongs().map( Song::asMediaItem )

    val search = remember { Search(lazyListState) }
    val locator = Locator( lazyListState, ::getSongs )
    val import = ImportSongsFromCSV()
    val shuffle = SongShuffler(::getSongs)
    val playNext = PlayNext {
        player.addNext( getMediaItems(), context )

        // Turn of selector clears the selected list
        itemSelector.isActive = false
    }
    val enqueue = Enqueue {
        player.enqueue( getMediaItems(), context )

        // Turn of selector clears the selected list
        itemSelector.isActive = false
    }
    val shareLinks = ShareLinks {
        if( context.shareSongLinks( getSongs(), itemSelector.isActive ) )
            itemSelector.isActive = false
    }
    val addToFavorite = LikeComponent(::getSongs)
    val addToPlaylist = PlaylistsMenu.init(
        navController = navController,
        mediaItems = { _ -> getMediaItems() },
        onFailure = { throwable, preview ->
            Logger.e { "Failed to add songs to playlist ${preview.playlist.name} on HomeSongsScreen" }
            throwable.printStackTrace()
        },
        finalAction = {
            // Turn of selector clears the selected list
            itemSelector.isActive = false
        }
    )
    val resetCache = ResetCache( ::getSongs )

    val buttons = remember( builtInPlaylist ) {
        // Disable checkboxes when category has changed
        itemSelector.isActive = false

        // Grouped: what to do with the list, then how it's shown, then what erases.
        // Shuffle is already an icon; import is in the Playlists tab, the caches
        // in Settings → Data. Each list screen adds its own after a heading.
        mutableStateListOf<Button>().apply {
            add( MenuHeader( MENU_LIST, R.string.menu_section_list ) )
            add( itemSelector )
            add( shareLinks )
            add( addToPlaylist )
            add( addToFavorite )
            add( playNext )
            add( enqueue )
            add( MenuHeader( MENU_DISPLAY, R.string.menu_section_display ) )
            add( search )
            add( locator )
            add( MenuHeader( MENU_ERASE, null ) )
        }
    }

    Box(
        modifier = Modifier.background( colorPalette().background0 )
            .fillMaxHeight()
            .fillMaxWidth(
                if (NavigationBarPosition.Right.isCurrent())
                    Dimensions.contentWidthRightBar
                else
                    1f
            )
    ) {
        Column( Modifier.fillMaxSize() ) {
            //<editor-fold defaultstate="collapsed" desc="Chips">
            // Favorites, cached and downloaded are in the filters: only the lists of
            // their own kind are left here (the top of a period, the device's files)
            val showMyTopPlaylist by Preferences.HOME_SONGS_SHOW_MOST_PLAYED_CHIP
            val showOnDevice by Preferences.HOME_SONGS_SHOW_ON_DEVICE_CHIP
            val chips = remember( showMyTopPlaylist, showOnDevice ) {
                buildList {
                    add( BuiltInPlaylist.All )
                    if( showMyTopPlaylist )
                        add( BuiltInPlaylist.Top )
                    if( showOnDevice )
                        add( BuiltInPlaylist.OnDevice )
                }
            }
            //</editor-fold>

            // One row: the filter as a single pill ("All ▾"), play all / shuffle as
            // icons, every other action in the "⋮" menu. The chips row is gone.
            TabToolBar.Compact(
                buttons,
                trailing = {
                    if( builtInPlaylist == BuiltInPlaylist.All )
                        FilterButton { menuState.display { FilterSheet( menuState::hide ) } }
                    TabToolBar.IconPill(
                        iconId = R.drawable.play,
                        description = stringResource( R.string.play_all ),
                        filled = true,
                        onClick = { player.forcePlayFromBeginning( getMediaItems() ) }
                    )
                    TabToolBar.IconPill(
                        iconId = R.drawable.shuffle,
                        description = stringResource( R.string.shuffle ),
                        filled = false,
                        onClick = shuffle::onShortClick
                    )
                }
            ) {
                // The count, like on the other Library tabs. Only what's shown: "1234 / 1800"
                // made this part too long, it scrolled sideways
                HeaderInfo( itemsOnDisplayState.size.toString(), R.drawable.musical_notes )
                if( chips.size > 1 )
                TabToolBar.FilterPill(
                    options = chips,
                    current = builtInPlaylist,
                    label = { if( it == BuiltInPlaylist.All ) stringResource( R.string.filter_all ) else it.text },
                    onSelect = { builtInPlaylist = it }
                )
            }

            if( builtInPlaylist == BuiltInPlaylist.All )
                ActiveFilters()

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding( horizontal = 12.dp )
                    .padding( bottom = 8.dp )
                    .fillMaxWidth()
            ) {
                Column {

                    // The downloads line also shows with the "downloaded" filter, which replaced the list
                    when {
                        // No storage bar here: unlabelled, it read as a stray line (it's in Settings → Data)
                        builtInPlaylist == BuiltInPlaylist.Downloaded || builtInPlaylist == BuiltInPlaylist.Offline
                                || (builtInPlaylist == BuiltInPlaylist.All && SongFiltersStore.filters.downloaded) -> {
                            // The finished ones are listed here; one quiet line leads to the
                            // others, only when some are running or failed
                            if( builtInPlaylist === BuiltInPlaylist.Downloaded || SongFiltersStore.filters.downloaded ) {
                                val counts = app.kreate.android.service.rememberDownloadCounts()
                                if( counts.total > 0 )
                                    androidx.compose.foundation.layout.Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                                           .clickable { NavRoutes.DOWNLOADS.navigateHere( navController ) }
                                                           .padding( horizontal = SettingComponents.HORIZONTAL_PADDING.dp, vertical = 8.dp )
                                    ) {
                                        BasicText(
                                            text = listOfNotNull(
                                                stringResource( R.string.downloads_running_count, counts.running ).takeIf { counts.running > 0 },
                                                stringResource( R.string.downloads_paused_count, counts.paused ).takeIf { counts.paused > 0 },
                                                stringResource( R.string.downloads_failed_count, counts.failed ).takeIf { counts.failed > 0 }
                                            ).joinToString( " · " ),
                                            style = typography().xs.semiBold.color( if( counts.failed > 0 ) colorPalette().red else colorPalette().textSecondary ),
                                            modifier = Modifier.weight( 1f )
                                        )
                                        androidx.compose.material3.Icon(
                                            painter = painterResource( R.drawable.chevron_forward ),
                                            contentDescription = stringResource( R.string.downloads_in_progress_title ),
                                            tint = colorPalette().textSecondary,
                                            modifier = Modifier.size( 16.dp )
                                        )
                                    }
                            }
                        }
                        else -> {}
                    }

                }
            }

            // Sticky search bar
            search.SearchBar()

            when( builtInPlaylist ) {
                BuiltInPlaylist.OnDevice -> OnDeviceSong( navController, lazyListState, itemSelector, search, buttons, itemsOnDisplayState, ::getSongs )
                else                     -> HomeSongs( navController, builtInPlaylist, lazyListState, itemSelector, search, buttons, itemsOnDisplayState, ::getSongs )
            }
        }

        FloatingActionsContainerWithScrollToTop(lazyListState = lazyListState)

        val showFloatingIcon by Preferences.SHOW_FLOATING_ICON
        if( UiType.ViMusic.isCurrent() && showFloatingIcon )
            MultiFloatingActionsContainer(
                iconId = R.drawable.search,
                onClick = {
                    NavRoutes.search.navigateHere( navController )
                },
                onClickSettings = {
                    NavRoutes.settings.navigateHere( navController )
                },
                onClickSearch = {
                    NavRoutes.search.navigateHere( navController )
                }
            )
    }
}

/** Keys of the menu headings, for the screens adding their own entries */
const val MENU_LIST = "list"
const val MENU_DISPLAY = "display"
const val MENU_ERASE = "erase"
