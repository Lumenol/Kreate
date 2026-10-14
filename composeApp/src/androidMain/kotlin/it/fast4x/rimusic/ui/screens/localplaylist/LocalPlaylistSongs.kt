package it.fast4x.rimusic.ui.screens.localplaylist

import app.kreate.android.themed.rimusic.component.playlist.displayName
import app.kreate.android.themed.common.component.DetailHeader
import app.kreate.android.themed.common.component.DetailMenu
import app.kreate.android.themed.common.component.DetailSkeleton
import app.kreate.android.themed.common.component.LabeledAction
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.res.stringResource
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFold
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.navigation.NavController
import app.kreate.android.LocalBottomMenu
import app.kreate.android.Preferences
import app.kreate.android.R
import app.kreate.android.constant.MenuPage
import app.kreate.android.service.player.StatefulPlayer
import app.kreate.android.themed.common.component.BottomMenu
import app.kreate.android.themed.common.component.tab.DeleteAllDownloadedDialog
import app.kreate.android.themed.common.component.tab.DownloadAllDialog
import app.kreate.android.themed.rimusic.component.ItemSelector
import app.kreate.android.themed.rimusic.component.Search
import app.kreate.android.themed.rimusic.component.playlist.PlaylistItem
import app.kreate.android.themed.rimusic.component.playlist.PositionLock
import app.kreate.android.themed.rimusic.component.song.SongItem
import app.kreate.android.utils.shallowCompare
import app.kreate.android.viewmodel.LocalPlaylistViewModel
import app.kreate.constant.PlaylistSongSortBy
import app.kreate.database.models.Song
import app.kreate.database.models.SongPlaylistMap
import app.kreate.di.CacheType
import app.kreate.util.toDuration
import co.touchlab.kermit.Logger
import com.github.doyaaaaaken.kotlincsv.client.KotlinCsvExperimental
import it.fast4x.compose.persist.persistList
import it.fast4x.compose.reordering.draggedItem
import it.fast4x.compose.reordering.rememberReorderingState
import it.fast4x.compose.reordering.reorder
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.NavigationBarPosition
import it.fast4x.rimusic.enums.UiType
import it.fast4x.rimusic.thumbnailShape
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.components.MenuState
import it.fast4x.rimusic.ui.components.SwipeableQueueItem
import it.fast4x.rimusic.ui.components.tab.toolbar.Dialog
import it.fast4x.rimusic.ui.components.themed.Enqueue
import it.fast4x.rimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.fast4x.rimusic.ui.components.themed.IconButton
import it.fast4x.rimusic.ui.components.themed.ListenOnYouTube
import it.fast4x.rimusic.ui.components.themed.PlayNext
import it.fast4x.rimusic.ui.components.themed.PlaylistsMenu
import it.fast4x.rimusic.ui.components.themed.ResetThumbnail
import it.fast4x.rimusic.ui.components.themed.Synchronize
import it.fast4x.rimusic.ui.components.themed.ThumbnailPicker
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.ui.styling.LocalAppearance
import it.fast4x.rimusic.ui.styling.onOverlay
import it.fast4x.rimusic.ui.styling.overlay
import it.fast4x.rimusic.utils.DeletePlaylist
import it.fast4x.rimusic.utils.addNext
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.center
import it.fast4x.rimusic.utils.checkFileExists
import it.fast4x.rimusic.utils.color
import it.fast4x.rimusic.utils.deleteFileIfExists
import it.fast4x.rimusic.utils.enqueue
import it.fast4x.rimusic.utils.forcePlayAtIndex
import it.fast4x.rimusic.utils.forcePlayFromBeginning
import it.fast4x.rimusic.utils.isAtLeastAndroid14
import it.fast4x.rimusic.utils.manageDownload
import it.fast4x.rimusic.utils.saveImageToInternalStorage
import it.fast4x.rimusic.utils.semiBold
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.knighthat.component.ResetCache
import me.knighthat.component.playlist.PinPlaylist
import me.knighthat.component.playlist.RenamePlaylistDialog
import me.knighthat.component.playlist.Reposition
import me.knighthat.component.tab.ExportSongsToCSVDialog
import me.knighthat.component.tab.LikeComponent
import me.knighthat.component.tab.Locator
import me.knighthat.component.tab.SongShuffler
import me.knighthat.utils.Toaster
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.koin.java.KoinJavaComponent.inject
import kotlin.time.Duration


@OptIn(ExperimentalCoroutinesApi::class)
@KotlinCsvExperimental
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation", "UnrememberedMutableState")
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun LocalPlaylistSongs(
    navController: NavController,
    playlistId: Long,
    miniPlayer: @Composable () -> Unit = {},
    menu: BottomMenu = LocalBottomMenu.current,
    menuState: MenuState = LocalMenuState.current,
    viewModel: LocalPlaylistViewModel = koinViewModel { parametersOf(menuState) }
) {
    // Essentials
    val context = LocalContext.current
    val player: StatefulPlayer = koinInject()
    val hapticFeedback = LocalHapticFeedback.current
    val (colorPalette, typography) = LocalAppearance.current
    val lazyListState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current

    // Settings
    val parentalControlEnabled by Preferences.PARENTAL_CONTROL
    var isRecommendationEnabled by Preferences.LOCAL_PLAYLIST_SMART_RECOMMENDATION

    // Non-vital
    val thumbnailUrl = remember { mutableStateOf("") }

    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    var itemsOnDisplay by persistList<Song>("localPlaylist/$playlistId/songs/on_display")

    val itemSelector = remember {
        ItemSelector( menuState ) { addAll( itemsOnDisplay ) }
    }

    fun getSongs() = itemSelector.ifEmpty { itemsOnDisplay }
    fun getMediaItems() = getSongs().map( Song::asMediaItem )

    val search = remember { Search(lazyListState) }
    val shuffle = SongShuffler ( ::getSongs )
    val renameDialog = RenamePlaylistDialog { playlist }
    val exportDialog = ExportSongsToCSVDialog(
        playlistBrowseId = playlist?.browseId.orEmpty(),
        playlistName = playlist?.name ?: "",
        songs = ::getSongs
    )
    val deleteDialog = DeletePlaylist {
        Database.asyncTransaction {
            playlist?.let( playlistTable::delete )
        }

        onDismiss()

        if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED)
            navController.popBackStack()
    }
    val renumberDialog = Reposition(playlistId)
    val downloadAllDialog = remember {
        DownloadAllDialog( context, ::getSongs )
    }
    val deleteDownloadsDialog = remember {
        DeleteAllDownloadedDialog(::getSongs)
    }
    val editThumbnailLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uri != null) {
            val thumbnailName = "playlist_${playlist?.id}"
            val permaUri = saveImageToInternalStorage(context, uri, "thumbnail", thumbnailName)
            thumbnailUrl.value = permaUri.toString()
        } else {
            Toaster.w( R.string.thumbnail_not_selected )
        }
    }
    val pin = PinPlaylist( playlist )
    val positionLock = remember( viewModel.sort.sortOrder ) { PositionLock(viewModel.sort.sortOrder) }
    LaunchedEffect( itemSelector.isActive ) {
        // Setting this field to true means disable it
        if( itemSelector.isActive )
            positionLock.isFirstIcon = true
    }
    // Either position lock or item selector can be turned on at a time
    LaunchedEffect( positionLock.isFirstIcon ) {
        if( !positionLock.isFirstIcon ) {
            // Open to move position
            itemSelector.isActive = false
            // Disable smart recommendation, it breaks the index
            isRecommendationEnabled = false
        }
    }

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
    val addToFavorite = LikeComponent( ::getSongs )

    val addToPlaylist = PlaylistsMenu.init(
        navController,
        { getMediaItems() },
        { throwable, preview ->
            Logger.e( tag = "LocalPlaylistSongs" ) { "Failed to add songs to playlist ${preview.playlist.name}" }
            throwable.printStackTrace()
        },
        {
            // Turn of selector clears the selected list
            itemSelector.isActive = false
        }
    )

    // TODO: Rework
    fun sync() {
    }
    val syncComponent = Synchronize { sync() }
    val listenOnYT = ListenOnYouTube {
        val browseId = playlist?.browseId?.removePrefix( "VL" )

        player.pause()
        uriHandler.openUri( "https://youtube.com/playlist?list=$browseId" )
    }
    val resetCache = ResetCache( ::getSongs )

    fun openEditThumbnailPicker() {
        editThumbnailLauncher.launch("image/*")
    }
    val thumbnailPicker = ThumbnailPicker { openEditThumbnailPicker() }

    fun resetThumbnail() {
        if(thumbnailUrl.value == "") {
            Toaster.w( R.string.no_thumbnail_present )
            return
        }
        val thumbnailName = "thumbnail/playlist_${playlist?.id}"
        val retVal = deleteFileIfExists(context, thumbnailName)
        if(retVal == true){
            Toaster.s( R.string.removed_thumbnail )
            thumbnailUrl.value = ""
        } else
            Toaster.e( R.string.failed_to_remove_thumbnail )
    }
    val resetThumbnail = ResetThumbnail { resetThumbnail() }

    val locator = Locator( lazyListState, ::getSongs, 3 )

    //<editor-fold defaultstate="collapsed" desc="Smart recommendation">
    val relatedSongs by viewModel.relatedSongs.collectAsStateWithLifecycle()

    LaunchedEffect( isRecommendationEnabled ) {
        if( isRecommendationEnabled )
            // Lock position if this feature is enabled
            positionLock.isFirstIcon =  isRecommendationEnabled
    }
    //</editor-fold>
    LaunchedEffect( items, relatedSongs, search.input, parentalControlEnabled ) {
        items.toMutableList()
             .apply {
                 relatedSongs.forEach { (song, index) ->
                     // Make sure position can't go outside permissible range
                     // causing [IndexOutOfBoundException]
                     val position = index.coerceIn(0, size)
                     add( position, song )
                 }
             }
             .distinctBy( Song::id )
             .filter { !parentalControlEnabled || !it.isExplicit }
             .filter { song ->
                 // Without cleaning, user can search explicit songs with "e:"
                 // I kinda want this to be a feature, but it seems unnecessary
                 val containsName = search appearsIn song.cleanTitle()
                 val containsArtist = search appearsIn song.cleanArtistsText()

                 containsName || containsArtist
             }
            .let { itemsOnDisplay = it }
    }
    LaunchedEffect( playlist?.name ) {
//        renameDialog.playlistName = playlistPreview?.playlist?.name?.let { name ->
//            if( name.startsWith( MONTHLY_PREFIX, true ) )
//                getTitleMonthlyPlaylist(context, name.substringAfter(MONTHLY_PREFIX))
//            else
//                name.substringAfter( PINNED_PREFIX )
//                    .substringAfter( PIPED_PREFIX )
//        } ?: "Unknown"

        val thumbnailName = "thumbnail/playlist_${playlistId}"
        val presentThumbnailUrl: String? = checkFileExists(context, thumbnailName)
        if (presentThumbnailUrl != null) {
            thumbnailUrl.value = presentThumbnailUrl
        }
    }

    var autosync by Preferences.AUTO_SYNC

    val thumbnailRoundness by Preferences.THUMBNAIL_BORDER_RADIUS

    val reorderingState = rememberReorderingState(
        lazyListState = lazyListState,
        key = items,
        onDragEnd = { fromIndex, toIndex ->
            Database.asyncTransaction {
                if( !isAtLeastAndroid14 ) {
                    // This block should function exactly to the SQL statement
                    // Except it's slower
                    val mutableItems = items.toMutableList()
                    val movedSong = mutableItems.removeAt( fromIndex )
                    mutableItems.add( toIndex, movedSong )

                    mutableItems.mapIndexed { index, song ->
                                    SongPlaylistMap( song.id, playlistId, index )
                                }
                                .also( songPlaylistMapTable::updateReplace )
                } else
                    // SQL statement makes faster adjustment with better optimization
                    // Unfortunately, it requires Android 31+ to work.
                    songPlaylistMapTable.move( playlistId, fromIndex, toIndex )
            }
        },
        extraItemCount = 1
    )

    renameDialog.Render()
    exportDialog.Render()
    deleteDialog.Render()
    (renumberDialog as Dialog).Render()
    downloadAllDialog.Render()
    deleteDownloadsDialog.Render()

    val rippleIndication = ripple(bounded = false)

    val playlistNotMonthlyType = playlist?.isMonthly == false

    val currentMediaItem by player.currentMediaItemState.collectAsState()
    val songItemValues = remember( colorPalette, typography ) {
        SongItem.Values.from( colorPalette, typography )
    }

    DetailSkeleton(
        navController = navController,
        miniPlayer = miniPlayer,
        actions = {
            DetailMenu(
                buildList {
                    if ( viewModel.sort.sortBy == PlaylistSongSortBy.POSITION )
                        add( positionLock )
                    add( playNext )
                    add( enqueue )
                    add( addToPlaylist )
                    add( addToFavorite )
                    add( itemSelector )
                    add( deleteDownloadsDialog )
                    if( !playlist?.browseId.isNullOrBlank() ) {
                        add( syncComponent )
                        add( listenOnYT )
                    }
                    add( renumberDialog )
                    add( exportDialog )
                    add( thumbnailPicker )
                    add( resetThumbnail )
                    add( resetCache )
                    add( deleteDialog )
                }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .background(colorPalette().background0)
                //.fillMaxSize()
                .fillMaxHeight()
                .fillMaxWidth(
                    if (NavigationBarPosition.Right.isCurrent())
                        Dimensions.contentWidthRightBar
                    else
                        1f
                )
        ) {
            //LookaheadScope {
            LazyColumn(
                state = reorderingState.lazyListState,
                contentPadding = PaddingValues(bottom = Dimensions.bottomSpacer),
                modifier = Modifier
                    .background(colorPalette().background0)
                    .fillMaxSize()
            ) {
                item(
                    key = "header",
                    contentType = 0
                ) {
                    val details = remember( items ) {
                        val minutes = items.fastFold( Duration.ZERO ) { acc, song ->
                            acc + song.durationText.toDuration()
                        }.inWholeMinutes
                        val duration = if( minutes >= 60 ) "%d h %02d".format( minutes / 60, minutes % 60 ) else "$minutes min"
                        "${context.resources.getQuantityString( R.plurals.song, items.size, items.size )} · $duration"
                    }

                    DetailHeader(
                        cover = null,
                        coverContent = { coverModifier ->
                            playlist?.let {
                                PlaylistItem.Thumbnail(
                                    playlist = it,
                                    modifier = coverModifier,
                                    sizeDp = DpSize( 208.dp, 208.dp ),
                                    showPlatformIcon = false
                                )
                            }
                        },
                        title = playlist?.displayName().orEmpty(),
                        links = emptyList(),
                        details = details,
                        onPlay = {
                            player.stopRadio()
                            player.forcePlayFromBeginning( getMediaItems() )
                        },
                        onShuffle = shuffle::onShortClick
                    ) {
                        if( playlistNotMonthlyType )
                            LabeledAction( pin, stringResource( R.string.word_pin ) )
                        LabeledAction( downloadAllDialog, stringResource( R.string.download ) )
                        // Smart recommendation: related songs mixed into the playlist
                        LabeledAction(
                            icon = painterResource( R.drawable.smart_shuffle ),
                            label = stringResource( R.string.word_suggestions ),
                            selected = isRecommendationEnabled,
                            onClick = { isRecommendationEnabled = !isRecommendationEnabled }
                        )
                        LabeledAction( renameDialog, stringResource( R.string.rename ) )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding( horizontal = 10.dp ).fillMaxWidth()
                    ) {
                        viewModel.sort.ToolBarButton()
                        Spacer( Modifier.weight( 1f ) )
                        search.ToolBarButton()
                        locator.ToolBarButton()
                    }

                    search.SearchBar()
                }

                itemsIndexed(
                    items = itemsOnDisplay,
                    key = { _, song -> song.id },
                    contentType = { _, song -> song },
                ) { index, song ->
                    val mediaItem = song.asMediaItem

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .draggedItem(
                                reorderingState = reorderingState,
                                index = index
                            )
                            .zIndex(2f)
                    ) {
                        val isLocal by remember { derivedStateOf { song.isLocal } }

                        // Drag anchor
                        if ( !positionLock.isLocked() ) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 16.dp) // Accommodate horizontal padding of SongItem
                                                   .size(24.dp)
                                    .zIndex(2f)
                                    .align(Alignment.CenterEnd),
                                contentAlignment = Alignment.Center
                            ) {

                                IconButton(
                                    icon = R.drawable.reorder,
                                    color = colorPalette().textDisabled,
                                    indication = rippleIndication,
                                    onClick = {},
                                    modifier = Modifier
                                        .reorder(
                                            reorderingState = reorderingState,
                                            index = index
                                        )
                                )
                            }
                        }

                        SwipeableQueueItem(
                            mediaItem = mediaItem,
                            onPlayNext = {
                                player.addNext(mediaItem)
                            },
                            onRemoveFromQueue = {
                                Database.asyncTransaction {
                                    songPlaylistMapTable.deleteBySongId( song.id, playlistId )
                                }

                                Toaster.s(
                                    context.getString(
                                        R.string.success_removed_song_from_playlist,
                                        "${song.cleanArtistsText()} - ${song.cleanTitle()}",
                                        playlist!!.name
                                    )
                                )
                            },
                            onDownload = {
                                val cache: Cache by inject(Cache::class.java, CacheType.CACHE)
                                cache.removeResource(song.id)
                                Database.asyncTransaction {
                                    formatTable.updateContentLengthOf( song.id )
                                }

                                if (!isLocal) {
                                    manageDownload(
                                        context = context,
                                        mediaItem = mediaItem,
                                        downloadState = song.isLocal
                                    )
                                }
                            },
                            onEnqueue = {
                                player.enqueue(mediaItem)
                            },
                        ) {
                            SongItem.Render(
                                song = song,
                                hapticFeedback = hapticFeedback,
                                isPlaying = song.shallowCompare( currentMediaItem ),
                                values = songItemValues,
                                isInPlaylistScreen = true,
                                itemSelector = itemSelector,
                                isRecommended = song in relatedSongs,
                                trailingContent = {
                                    if( !positionLock.isLocked() )
                                        // Create a fake box to store drag anchor and checkbox
                                        Box( Modifier.width( 24.dp ) )
                                },
                                thumbnailOverlay = {
                                    if ( viewModel.sort.sortBy == PlaylistSongSortBy.TOTAL_PLAY_TIME ) {
                                        BasicText(
                                            text = song.formattedTotalPlayTime,
                                            style = typography().xxs.semiBold.center.color(
                                                colorPalette().onOverlay
                                            ),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    brush = Brush.verticalGradient(
                                                        colors = listOf(
                                                            Color.Transparent,
                                                            colorPalette().overlay
                                                        )
                                                    ),
                                                    shape = thumbnailShape()
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                .align(Alignment.BottomCenter)
                                        )
                                    }
                                },
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
                                            itemsOnDisplay.fastMap( Song::asMediaItem ),
                                            index
                                        )

                                    /*
                                        Due to the small size of checkboxes,
                                        we shouldn't disable [itemSelector]
                                     */

                                    search.hideIfEmpty()
                                },
                                onLongClick = {
                                    val page =
                                        if( isLocal )
                                            MenuPage.LocalPlaylistLocalSong(playlist!!, mediaItem)
                                        else
                                            MenuPage.LocalPlaylistSong(playlist!!, mediaItem)
                                    menu.show( page, true )
                                }
                            )
                        }
                    }

                }
            }

            FloatingActionsContainerWithScrollToTop(lazyListState = lazyListState)

            val showFloatingIcon by Preferences.SHOW_FLOATING_ICON
            if ( UiType.ViMusic.isCurrent() && showFloatingIcon )
                FloatingActionsContainerWithScrollToTop(
                    lazyListState = lazyListState,
                    iconId = R.drawable.shuffle,
                    visible = !reorderingState.isDragging,
                    onClick = {
                        getMediaItems().let { songs ->
                            if (songs.isNotEmpty()) {
                                player.stopRadio()
                                player
                                      ?.forcePlayFromBeginning( songs.shuffled() )
                            }
                        }
                    }
                )
        }
    }
}
