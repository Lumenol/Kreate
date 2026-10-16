package app.kreate.android.themed.rimusic.screen.playlist

import androidx.compose.runtime.collectAsState
import app.kreate.database.models.Playlist
import app.kreate.android.themed.common.component.DetailHeader
import app.kreate.android.themed.common.component.DetailMenu
import app.kreate.android.themed.common.component.DetailSkeleton
import app.kreate.android.themed.common.component.LabeledAction
import app.kreate.android.themed.common.component.shareUrl
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.navigation.NavController
import app.kreate.android.LocalBottomMenu
import app.kreate.android.Preferences
import app.kreate.android.R
import app.kreate.android.coil3.ImageFactory
import app.kreate.android.constant.MenuPage
import app.kreate.android.service.player.StatefulPlayer
import app.kreate.android.themed.common.component.BottomMenu
import app.kreate.android.themed.common.component.LoadMoreContentType
import app.kreate.android.themed.common.component.tab.DeleteAllDownloadedDialog
import app.kreate.android.themed.common.component.tab.DownloadAllDialog
import app.kreate.android.themed.rimusic.component.ItemSelector
import app.kreate.android.themed.rimusic.component.song.SongItem
import app.kreate.android.utils.renderDescription
import app.kreate.android.utils.shallowCompare
import app.kreate.android.viewmodel.YouTubePlaylistViewModel
import app.kreate.database.models.Song
import app.kreate.di.CacheType
import co.touchlab.kermit.Logger
import it.fast4x.innertube.YtMusic
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.UiType
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.components.SwipeablePlaylistItem
import it.fast4x.rimusic.ui.components.tab.toolbar.DualIcon
import it.fast4x.rimusic.ui.components.tab.toolbar.DynamicColor
import it.fast4x.rimusic.ui.components.themed.Enqueue
import it.fast4x.rimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.fast4x.rimusic.ui.components.themed.PlaylistsMenu
import it.fast4x.rimusic.ui.screens.settings.isYouTubeSyncEnabled
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.ui.styling.LocalAppearance
import it.fast4x.rimusic.utils.addNext
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.enqueue
import it.fast4x.rimusic.utils.forcePlayAtIndex
import it.fast4x.rimusic.utils.forcePlayFromBeginning
import it.fast4x.rimusic.utils.isDownloadedSong
import it.fast4x.rimusic.utils.isNetworkAvailable
import it.fast4x.rimusic.utils.manageDownload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.knighthat.component.tab.ExportSongsToCSVDialog
import me.knighthat.component.tab.LikeComponent
import me.knighthat.component.tab.Radio
import me.knighthat.component.tab.SongShuffler
import me.knighthat.component.ui.screens.DynamicOrientationLayout
import me.knighthat.innertube.Constants
import me.knighthat.utils.Toaster
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.java.KoinJavaComponent.inject

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@UnstableApi
@Composable
fun YouTubePlaylist(
    navController: NavController,
    viewModel: YouTubePlaylistViewModel = koinViewModel(),
    menu: BottomMenu = LocalBottomMenu.current,
    miniPlayer: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val player: StatefulPlayer = koinInject()
    val (colorPalette, typography) = LocalAppearance.current
    val hapticFeedback = LocalHapticFeedback.current

    val playlistPage by viewModel.playlistPage.collectAsStateWithLifecycle()
    val continuation by viewModel.continuation.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val currentMediaItem by player.currentMediaItemState.collectAsStateWithLifecycle()

    val itemSelector = remember {
        ItemSelector(menuState) { addAll( songs ) }
    }
    fun getSongs() = itemSelector.ifEmpty { songs }
    fun getMediaItems() = getSongs().map( Song::asMediaItem )

    //<editor-fold desc="Toolbar buttons">
    val shuffle = SongShuffler ( ::getSongs )
    val exportDialog = ExportSongsToCSVDialog(
        playlistBrowseId = playlistPage?.id.orEmpty(),
        playlistName = playlistPage?.name.orEmpty(),
        songs = ::getSongs
    )
    val downloadAllDialog = remember {
        DownloadAllDialog( context, ::getSongs )
    }
    val deleteDownloadsDialog = remember {
        DeleteAllDownloadedDialog(::getSongs)
    }
    val addToPlaylist = PlaylistsMenu.init(
        navController = navController,
        mediaItems = { _ -> getMediaItems() },
        onFailure = { throwable, preview ->
            Logger.e { "Failed to add songs to playlist ${preview.playlist.name} on YouTubePlaylist" }
            throwable.printStackTrace()
        },
        finalAction = {
            // Turn of selector clears the selected list
            itemSelector.isActive = false
        }
    )
    val addToFavorite = LikeComponent( ::getSongs )
    val enqueue = Enqueue {
        player.enqueue( getMediaItems(), context )

        // Turn of selector clears the selected list
        itemSelector.isActive = false
    }
    val radio = Radio( ::getSongs )
    // Saved = in the database with its browse id (that's what removing deletes too)
    val playlistId = remember( viewModel.browseId ) { viewModel.browseId.substringAfter( "VL" ) }
    val savedPlaylist by remember( playlistId ) {
        Database.playlistTable.findByBrowseId( playlistId )
    }.collectAsState( null )
    val saveToYouTubeLibrary = remember {
        object: DualIcon, DynamicColor {

            override val secondIconId: Int = R.drawable.bookmark
            override val iconId: Int = R.drawable.bookmark_outline

            override var isFirstIcon: Boolean by mutableStateOf( true )
            override var isFirstColor: Boolean by mutableStateOf( false )

            // It only ever removed, and showed "saved" whatever the state: tapping a
            // Home mix took it out of the YouTube library and nothing seemed to happen
            override fun onShortClick() {
                if( !isNetworkAvailable( context ) ) {
                    Toaster.noInternet()
                    return
                }

                val saved = !isFirstIcon
                CoroutineScope( Dispatchers.IO ).launch {
                    if( saved )
                        YtMusic.removelikePlaylistOrAlbum( playlistId )
                               .onSuccess {
                                   Database.playlistTable
                                           .findByBrowseId( playlistId )
                                           .first()
                                           ?.let( Database.playlistTable::delete )
                                   Toaster.i( R.string.playlist_removed_from_library )
                               }
                               .onFailure { Toaster.e( R.string.playlist_save_failed ) }
                    else
                        YtMusic.likePlaylistOrAlbum( playlistId )
                               .onSuccess {
                                   if( Database.playlistTable.findByBrowseId( playlistId ).first() == null )
                                       Database.playlistTable.insert(
                                           Playlist(
                                               name = playlistPage?.name ?: playlistId,
                                               browseId = playlistId,
                                               isEditable = false,
                                               isYoutubePlaylist = true
                                           )
                                       )
                                   Toaster.s( R.string.playlist_saved_to_library )
                               }
                               .onFailure { Toaster.e( R.string.playlist_save_failed ) }
                }
            }
        }
    }
    // The icon follows the real state
    LaunchedEffect( savedPlaylist ) { saveToYouTubeLibrary.isFirstIcon = savedPlaylist == null }

    exportDialog.Render()
    downloadAllDialog.Render()
    deleteDownloadsDialog.Render()
    //</editor-fold>
    val songItemValues = remember( colorPalette, typography ) {
        SongItem.Values.from( colorPalette, typography )
    }

    val thumbnailPainter =
        ImageFactory.rememberAsyncImagePainter( playlistPage?.thumbnails?.firstOrNull()?.url )

    DetailSkeleton(
        navController = navController,
        miniPlayer = miniPlayer,
        title = { app.kreate.android.themed.common.component.ScrolledTitle( playlistPage?.name.orEmpty(), viewModel.listState ) },
        actions = {
            DetailMenu(
                listOf( viewModel.search, enqueue, addToPlaylist, addToFavorite, deleteDownloadsDialog )
            )
        }
    ) {
        DynamicOrientationLayout(thumbnailPainter) {
            Box( Modifier.fillMaxSize() ) {
                LazyColumn(
                    state = viewModel.listState,
                    userScrollEnabled = songs.isNotEmpty(),
                    contentPadding = PaddingValues(bottom = Dimensions.bottomSpacer),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    item("header") {
                        DetailHeader(
                            cover = thumbnailPainter,
                            title = playlistPage?.name.orEmpty(),
                            links = emptyList(),
                            details = playlistPage?.subtitleText.orEmpty(),
                            onPlay = {
                                player.stopRadio()
                                player.forcePlayFromBeginning( getMediaItems() )
                            },
                            onShuffle = shuffle::onShortClick
                        ) {
                            if( isYouTubeSyncEnabled() )
                                LabeledAction( saveToYouTubeLibrary, stringResource( R.string.word_save ) )
                            LabeledAction( downloadAllDialog, stringResource( R.string.download ) )
                            LabeledAction( radio, stringResource( R.string.word_radio ) )
                            if( playlistPage?.id?.startsWith( "VL", true ) == true )
                                LabeledAction( painterResource( R.drawable.share_social ), stringResource( R.string.share ) ) {
                                    playlistPage?.shareUrl( Constants.YOUTUBE_MUSIC_URL )?.also( context::shareUrl )
                                }
                        }
                    }

                    item( "search" ) { viewModel.search.SearchBar() }

                    playlistPage?.description?.let {
                        renderDescription( it )
                    }

                    itemsIndexed(
                        items = songs,
                        // Include index to key so when reposition happens, the content
                        // will get updated accordingly
                        key = { i, s -> "${System.identityHashCode(s)} - $i" }
                    ) { index, song ->
                        val isLocal by remember { derivedStateOf { song.isLocal } }
                        val isDownloaded = !isLocal && isDownloadedSong( song.id )
                        val mediaItem = song.asMediaItem

                        SwipeablePlaylistItem(
                            mediaItem = mediaItem,
                            onPlayNext = {
                                player.addNext( mediaItem )
                            },
                            onDownload = {
                                val cache: Cache by inject(Cache::class.java, CacheType.CACHE)
                                cache.removeResource( song.id )
                                Database.asyncTransaction {
                                    formatTable.updateContentLengthOf( song.id )
                                }

                                if (!isLocal)
                                    manageDownload(
                                        context = context,
                                        mediaItem = mediaItem,
                                        downloadState = isDownloaded
                                    )
                            },
                            onEnqueue = {
                                player.enqueue( mediaItem )
                            }
                        ) {
                            SongItem.Render(
                                song = song,
                                hapticFeedback = hapticFeedback,
                                isPlaying = song.shallowCompare( currentMediaItem ),
                                values = songItemValues,
                                itemSelector = itemSelector,
                                modifier = Modifier.animateItem(),
                                onClick = {
                                    player.stopRadio()

                                    val selectedSongs = getSongs()
                                    if( song in selectedSongs )
                                        player.forcePlayAtIndex(
                                            selectedSongs.fastMap( Song::asMediaItem ),
                                            selectedSongs.indexOf( song )
                                        )
                                    else
                                        player.forcePlayAtIndex(
                                            songs.fastMap( Song::asMediaItem ),
                                            index
                                        )
                                },
                                onLongClick = {
                                    val page = MenuPage.Song(mediaItem)
                                    menu.show( page, true )
                                }
                            )
                        }
                    }

                    if ( !continuation.isNullOrEmpty() )
                        item( "loading", LoadMoreContentType ) {
                            repeat( 5 ) { SongItem.Placeholder() }
                        }
                }

                val showFloatingIcon by Preferences.SHOW_FLOATING_ICON
                if( UiType.ViMusic.isCurrent() && showFloatingIcon )
                    FloatingActionsContainerWithScrollToTop(
                        lazyListState = viewModel.listState,
                        iconId = R.drawable.shuffle,
                        onClick = {
                            player.stopRadio()
                            player.forcePlayFromBeginning( getMediaItems() )
                        }
                    )
            }
        }
    }

    // Run once on start
    LaunchedEffect( Unit ) {
        if( playlistPage == null ) viewModel.onFetch()
    }
}