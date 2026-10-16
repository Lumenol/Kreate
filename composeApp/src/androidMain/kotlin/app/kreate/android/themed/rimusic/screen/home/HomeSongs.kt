package app.kreate.android.themed.rimusic.screen.home

import app.kreate.database.SongPlayStats
import app.kreate.android.themed.rimusic.screen.home.filters.SongFiltersStore
import app.kreate.android.R
import androidx.compose.ui.res.stringResource
import it.fast4x.rimusic.ui.components.tab.toolbar.addAfter
import it.fast4x.rimusic.ui.components.tab.toolbar.MenuHeader
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import app.kreate.android.themed.common.component.initialOf
import app.kreate.android.themed.common.component.FastScroller
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import app.kreate.android.LocalBottomMenu
import app.kreate.android.Preferences
import app.kreate.android.constant.MenuPage
import app.kreate.android.service.player.StatefulPlayer
import app.kreate.android.themed.common.component.BottomMenu
import app.kreate.android.themed.common.component.tab.DeleteAllDownloadedDialog
import app.kreate.android.themed.common.component.tab.DownloadAllDialog
import app.kreate.android.themed.rimusic.component.ItemSelector
import app.kreate.android.themed.rimusic.component.Search
import app.kreate.android.themed.rimusic.component.song.PeriodSelector
import app.kreate.android.themed.rimusic.component.song.SongItem
import app.kreate.android.themed.rimusic.component.tab.Sort
import app.kreate.android.utils.shallowCompare
import app.kreate.constant.SongSortBy
import app.kreate.database.ext.FormatWithSong
import app.kreate.database.models.Song
import app.kreate.di.CacheType
import app.kreate.util.toDuration
import it.fast4x.compose.persist.persistList
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.BuiltInPlaylist
import it.fast4x.rimusic.enums.DurationInMinutes
import it.fast4x.rimusic.service.MyDownloadHelper
import it.fast4x.rimusic.service.modern.isLocal
import it.fast4x.rimusic.thumbnailShape
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.components.SwipeablePlaylistItem
import it.fast4x.rimusic.ui.components.tab.toolbar.Button
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.ui.styling.LocalAppearance
import it.fast4x.rimusic.ui.styling.onOverlay
import it.fast4x.rimusic.ui.styling.overlay
import it.fast4x.rimusic.utils.addNext
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.center
import it.fast4x.rimusic.utils.color
import it.fast4x.rimusic.utils.enqueue
import it.fast4x.rimusic.utils.forcePlayAtIndex
import it.fast4x.rimusic.utils.isDownloadedSong
import it.fast4x.rimusic.utils.manageDownload
import it.fast4x.rimusic.utils.semiBold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import me.knighthat.component.tab.ExportSongsToCSVDialog
import me.knighthat.component.tab.HiddenSongs
import org.koin.compose.koinInject

@UnstableApi
@ExperimentalFoundationApi
@Composable
fun HomeSongs(
    navController: NavController,
    builtInPlaylist: BuiltInPlaylist,
    lazyListState: LazyListState,
    itemSelector: ItemSelector<Song>,
    search: Search,
    buttons: MutableList<Button>,
    itemsOnDisplay: MutableList<Song>,
    getSongs: () -> List<Song>,
    menu: BottomMenu = LocalBottomMenu.current
) {
    // Essentials
    val player: StatefulPlayer = koinInject()
    val cache: Cache = koinInject(CacheType.CACHE)
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val hapticFeedback = LocalHapticFeedback.current
    val (colorPalette, typography) = LocalAppearance.current

    //<editor-fold defaultstate="collapsed" desc="Settings">
    val parentalControlEnabled by Preferences.PARENTAL_CONTROL
    val maxTopPlaylistItems by Preferences.MAX_NUMBER_OF_TOP_PLAYED
    val includeLocalSongs by Preferences.HOME_SONGS_INCLUDE_ON_DEVICE_IN_ALL
    val excludeSongWithDurationLimit by Preferences.LIMIT_SONGS_WITH_DURATION
    //</editor-fold>

    var items by persistList<Song>( "home/songs" )

    val songSort = remember {
        Sort(menuState, Preferences.HOME_SONGS_SORT_BY, Preferences.HOME_SONGS_SORT_ORDER)
    }
    val topPlaylists = remember { PeriodSelector(menuState) }
    val hiddenSongs = HiddenSongs()
    val exportDialog = ExportSongsToCSVDialog(
        playlistName = builtInPlaylist.text,
        songs = getSongs
    )
    val downloadAllDialog = remember {
        DownloadAllDialog( context, getSongs )
    }
    val deleteDownloadsDialog = remember {
        DeleteAllDownloadedDialog(getSongs)
    }

    /**
     * This variable tells [LazyColumn] to render [SongItemPlaceholder]
     * instead of [SongItem] queried from the database.
     *
     * This indication also tells user that songs are being loaded
     * and not it's definitely not freezing up.
     *
     * > This variable should **_NOT_** be set to `false` while inside **first** phrase,
     * and should **_NOT_** be set to `true` while in **second** phrase.
     */
    // True from the start: the first frame showed the "empty" message over a full library
    var isLoading by rememberSaveable { mutableStateOf(true) }
    var loadedOnce by remember { mutableStateOf( false ) }

    // Downloaded, cached or liked songs are in the library even never played here (the
    // lists they replace showed them): the filters then start from every song
    val everySong = builtInPlaylist == BuiltInPlaylist.All && SongFiltersStore.filters.widensLibrary

    // This phrase loads all songs across types into [items]
    // No filtration applied to this stage, only sort
    LaunchedEffect( builtInPlaylist, topPlaylists.period, songSort.sortBy, songSort.sortOrder, hiddenSongs.isFirstIcon, everySong ) {
        isLoading = true

        val retrievedSongs = when( builtInPlaylist ) {
            BuiltInPlaylist.All -> Database.songTable
                                           .sortAll( songSort.sortBy, songSort.sortOrder, excludeHidden = hiddenSongs.isHiddenExcluded() && !everySong )
                                           .map { list ->
                                               // Include local songs if enabled
                                               list.fastFilter {
                                                   !includeLocalSongs || !it.isLocal
                                               }
                                           }

            BuiltInPlaylist.Downloaded -> {
                // [MyDownloadHelper] provide a list of downloaded songs, which is faster to retrieve
                // than using `Cache.isCached()` call
                val downloaded: List<String> = MyDownloadHelper.instance
                                                               .downloads
                                                               .value
                                                               .values
                                                               .filter { it.state == Download.STATE_COMPLETED }
                                                               .fastMap { it.request.id }
                Database.songTable
                        .sortAll( songSort.sortBy, songSort.sortOrder )
                        .map { list ->
                            list.fastFilter { it.id in downloaded }
                        }
            }

            BuiltInPlaylist.Offline -> Database.formatTable
                                               .sortAllWithSongs( songSort.sortBy, songSort.sortOrder, excludeHidden = hiddenSongs.isHiddenExcluded() )
                                               .map { list ->
                                                   list.fastFilter {
                                                       val contentLength = it.format.contentLength ?: return@fastFilter false
                                                       cache.isCached( it.song.id, 0, contentLength )
                                                   }.map( FormatWithSong::song )
                                               }

            BuiltInPlaylist.Favorites -> Database.songTable.sortFavorites( songSort.sortBy, songSort.sortOrder )

            BuiltInPlaylist.Top -> Database.eventTable
                                           .findSongsMostPlayedBetween(
                                               from = topPlaylists.period.timeStampInMillis(),
                                               limit = maxTopPlaylistItems.toInt()
                                           )
                                           .map { list ->
                                               // Exclude songs with duration higher than what [excludeSongWithDurationLimit] is
                                               list.fastFilter { song ->
                                                   excludeSongWithDurationLimit == DurationInMinutes.Disabled
                                                           || song.durationText.toDuration().inWholeMilliseconds < excludeSongWithDurationLimit.asMillis
                                               }
                                           }

            BuiltInPlaylist.OnDevice -> flowOf( emptyList() )
        }

        retrievedSongs.flowOn( Dispatchers.IO )
                      .distinctUntilChanged()
                      // Scroll list to top to prevent weird artifacts
                      .onEach { lazyListState.scrollToItem( 0, 0 ) }
                      .collect {
                          // Same list as before (two empty filters): the filtering below
                          // won't run again, so loading must end here
                          val unchanged = it == items
                          items = it
                          loadedOnce = true
                          if( unchanged ) isLoading = false
                      }
    }

    //<editor-fold desc="Library filters (the "All" list only)">
    val filters = SongFiltersStore.filters
    val useFilters = builtInPlaylist == BuiltInPlaylist.All && filters.isActive
    // Each source of data is read only while a filter needs it
    val needsStats = useFilters && (filters.playsActive || filters.lastPlayedActive || filters.firstPlayedActive)
    val playStats by remember( needsStats ) {
        if( needsStats ) Database.eventTable.playStats().map { list -> list.associateBy( SongPlayStats::songId ) }
        else flowOf( emptyMap<String, SongPlayStats>() )
    }.collectAsState( emptyMap(), Dispatchers.IO )
    val needsDownloaded = useFilters && filters.downloaded
    val downloadedIds by remember( needsDownloaded ) {
        if( needsDownloaded )
            MyDownloadHelper.instance.downloads.map { all ->
                all.values.filter { it.state == Download.STATE_COMPLETED }.mapTo( HashSet() ) { it.request.id }
            }
        else flowOf( emptySet<String>() )
    }.collectAsState( emptySet(), Dispatchers.IO )
    val needsCached = useFilters && filters.cached
    val cachedIds by remember( needsCached ) {
        if( needsCached )
            Database.formatTable
                    .sortAllWithSongs( songSort.sortBy, songSort.sortOrder )
                    .map { list ->
                        list.filter {
                            val contentLength = it.format.contentLength ?: return@filter false
                            cache.isCached( it.song.id, 0, contentLength )
                        }.mapTo( HashSet() ) { it.song.id }
                    }
        else flowOf( emptySet<String>() )
    }.collectAsState( emptySet(), Dispatchers.IO )
    //</editor-fold>

    LaunchedEffect( items, search.input, useFilters, filters, playStats, downloadedIds, cachedIds ) {
    val now = System.currentTimeMillis()
    val allowed = items.filter { !parentalControlEnabled || !it.isExplicit }
    // "12 / 51" only against the usual list, not against every song known
    SongFiltersStore.totalCount = if( everySong ) -1 else allowed.size
    allowed.filter { !useFilters || filters.matches( it, playStats[it.id], downloadedIds, cachedIds, now ) }
         .also { SongFiltersStore.resultCount = it.size }
         .filter {
             // Without cleaning, user can search explicit songs with "e:"
             // I kinda want this to be a feature, but it seems unnecessary
             val containsTitle = search appearsIn it.cleanTitle()
             val containsArtist = search appearsIn it.cleanArtistsText()

             containsTitle || containsArtist
         }
        .let {
            itemsOnDisplay.clear()
            itemsOnDisplay.addAll( it )

            // Not before the database answered once: the empty list of the first
            // frame would flash the "no songs" message
            if( loadedOnce ) isLoading = false
        }
    }

    LaunchedEffect( builtInPlaylist ) {
        val firstButton = if( builtInPlaylist == BuiltInPlaylist.Top ) topPlaylists else songSort
        buttons.addAfter( MENU_DISPLAY, firstButton )
        buttons.addAfter( MENU_LIST, downloadAllDialog )
        // Export closes the "list" group, just before the "display" heading
        buttons.add( buttons.indexOfFirst { b -> b is MenuHeader && b.key == MENU_DISPLAY }.coerceAtLeast( 0 ), exportDialog )
        buttons.addAfter( MENU_ERASE, deleteDownloadsDialog )
    }

    //<editor-fold defaultstate="collapsed" desc="Dialog Renders">
    exportDialog.Render()
    downloadAllDialog.Render()
    deleteDownloadsDialog.Render()
    //</editor-fold>

    val currentMediaItem by player.currentMediaItemState.collectAsState()
    val songItemValues = remember( colorPalette, typography ) {
        SongItem.Values.from( colorPalette, typography )
    }

    Box( Modifier.fillMaxSize() ) {
        LazyColumn(
            state = lazyListState,
            userScrollEnabled = !isLoading,
            contentPadding = PaddingValues( bottom = Dimensions.bottomSpacer )
        ) {
            if( isLoading )
                items( 20, null ) { SongItem.Placeholder() }

            itemsIndexed(
                items = itemsOnDisplay,
                key = { _, song -> song.id }
            ) { index, song ->
                val mediaItem = song.asMediaItem

                val isLocal by remember { derivedStateOf { mediaItem.isLocal } }
                val isDownloaded = isLocal || isDownloadedSong( mediaItem.mediaId )

                SwipeablePlaylistItem(
                    mediaItem = mediaItem,
                    onPlayNext = { player.addNext( mediaItem ) },
                    onDownload = {
                        if( builtInPlaylist != BuiltInPlaylist.OnDevice ) {
                            cache.removeResource(mediaItem.mediaId)
                            Database.asyncTransaction {
                                formatTable.updateContentLengthOf( mediaItem.mediaId )
                            }
                            if ( !isLocal )
                                manageDownload(
                                    context = context,
                                    mediaItem = mediaItem,
                                    downloadState = isDownloaded
                                )
                        }
                    },
                    onEnqueue = {
                        player.enqueue(mediaItem)
                    }
                ) {
                    SongItem.Render(
                        song = song,
                        hapticFeedback = hapticFeedback,
                        isPlaying = song.shallowCompare( currentMediaItem ),
                        values = songItemValues,
                        itemSelector = itemSelector,
                        modifier = Modifier.animateItem(),
                        thumbnailOverlay = {
                            if ( songSort.sortBy == SongSortBy.TOTAL_PLAY_TIME || builtInPlaylist == BuiltInPlaylist.Top ) {
                                var text = song.formattedTotalPlayTime
                                var typography = typography().xxs
                                var alignment = Alignment.BottomCenter

                                if( builtInPlaylist == BuiltInPlaylist.Top ) {
                                    text = (index + 1).toString()
                                    typography = typography().m
                                    alignment = Alignment.Center
                                }

                                BasicText(
                                    text = text,
                                    style = typography.semiBold.center.color(colorPalette().onOverlay),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .align(alignment)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    colorPalette().overlay
                                                )
                                            ),
                                            shape = thumbnailShape()
                                        )
                                )
                            }
                        },
                        onClick = {
                            search.hideIfEmpty()

                            player.stopRadio()

                            val selectedSongs = getSongs()
                            if( song in selectedSongs )
                                player.forcePlayAtIndex(
                                    selectedSongs.fastMap( Song::asMediaItem ),
                                    selectedSongs.indexOf( song )
                                )
                            else
                                player.forcePlayAtIndex(
                                    itemsOnDisplay.fastMap( Song::asMediaItem ),
                                    index
                                )
                        },
                        onLongClick = {
                            val page = if( song.isLocal ) MenuPage.LocalSong(mediaItem) else MenuPage.Song(mediaItem)
                            menu.show( page, true )
                        }
                    )
                }
            }

            // An empty filter said nothing: tell what would show up here
            if( !isLoading && itemsOnDisplay.isEmpty() )
                item( "empty" ) {
                    androidx.compose.foundation.text.BasicText(
                        text = stringResource(
                            // A search with no result isn't an empty library
                            if( search.input.text.isNotBlank() ) R.string.no_results_found
                            else if( useFilters ) R.string.library_filters_empty
                            else when( builtInPlaylist ) {
                                BuiltInPlaylist.Favorites  -> R.string.songs_empty_favorites
                                BuiltInPlaylist.Offline    -> R.string.songs_empty_cached
                                BuiltInPlaylist.Downloaded -> R.string.songs_empty_downloaded
                                BuiltInPlaylist.Top        -> R.string.songs_empty_top
                                else                       -> R.string.songs_empty_all
                            }
                        ),
                        style = typography().s.color( colorPalette().textSecondary ),
                        modifier = Modifier.padding( horizontal = 24.dp, vertical = 32.dp )
                    )
                }
        }

        // Letter bubble only when sorted by a name
        FastScroller( lazyListState ) { index ->
            val song = itemsOnDisplay.getOrNull( index ) ?: return@FastScroller null
            when( songSort.sortBy ) {
                SongSortBy.TITLE -> initialOf( song.cleanTitle() )
                SongSortBy.ARTIST -> initialOf( song.cleanArtistsText() )
                else -> null
            }
        }
    }
}
