package app.kreate.android.themed.common.screens.album

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import app.kreate.android.themed.common.component.DetailHeader
import app.kreate.android.themed.common.component.DetailMenu
import app.kreate.android.themed.common.component.DetailSkeleton
import app.kreate.android.themed.common.component.HeaderLink
import app.kreate.android.themed.common.component.LabeledAction
import app.kreate.android.themed.common.component.shareUrl
import it.fast4x.rimusic.utils.forcePlayFromBeginning
import it.fast4x.rimusic.enums.NavRoutes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastJoinToString
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.LocalBottomMenu
import app.kreate.android.R
import app.kreate.android.coil3.ImageFactory
import app.kreate.android.constant.MenuPage
import app.kreate.android.service.player.StatefulPlayer
import app.kreate.android.themed.common.component.tab.DeleteAllDownloadedDialog
import app.kreate.android.themed.common.component.tab.DownloadAllDialog
import app.kreate.android.themed.rimusic.component.ItemSelector
import app.kreate.android.themed.rimusic.component.album.AlbumItem
import app.kreate.android.themed.rimusic.component.album.Bookmark
import app.kreate.android.themed.rimusic.component.song.SongItem
import app.kreate.android.utils.innertube.CURRENT_LOCALE
import app.kreate.android.utils.innertube.toMediaItem
import app.kreate.android.utils.renderDescription
import app.kreate.android.utils.shallowCompare
import app.kreate.database.models.Album
import app.kreate.database.models.Song
import app.kreate.database.models.SongAlbumMap
import app.kreate.util.MODIFIED_PREFIX
import co.touchlab.kermit.Logger
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.components.SwipeablePlaylistItem
import it.fast4x.rimusic.ui.components.themed.Enqueue
import it.fast4x.rimusic.ui.components.themed.PlayNext
import it.fast4x.rimusic.ui.components.themed.PlaylistsMenu
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.ui.styling.LocalAppearance
import it.fast4x.rimusic.utils.addNext
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.asSong
import it.fast4x.rimusic.utils.center
import it.fast4x.rimusic.utils.color
import it.fast4x.rimusic.utils.enqueue
import it.fast4x.rimusic.utils.forcePlayAtIndex
import it.fast4x.rimusic.utils.semiBold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.knighthat.component.album.AlbumModifier
import me.knighthat.component.tab.Locator
import me.knighthat.component.tab.Radio
import me.knighthat.component.tab.SongShuffler
import me.knighthat.component.ui.screens.DynamicOrientationLayout
import me.knighthat.innertube.Constants
import me.knighthat.innertube.Innertube
import me.knighthat.innertube.model.InnertubeAlbum
import me.knighthat.innertube.model.InnertubeSong
import me.knighthat.innertube.model.Section
import me.knighthat.utils.PropUtils
import me.knighthat.utils.Toaster
import org.koin.compose.koinInject

private fun updateAlbumInDatabase(dbAlbum: Album?, innertubeAlbum: InnertubeAlbum ) = Database.asyncTransaction {
    val onlineAlbum = Album(
        id = innertubeAlbum.id,
        title = PropUtils.retainIfModified( dbAlbum?.title, innertubeAlbum.name ),
        thumbnailUrl = PropUtils.retainIfModified(
            dbAlbum?.thumbnailUrl,
            innertubeAlbum.thumbnails.firstOrNull()?.url
        ),
        year = innertubeAlbum.year,
        authorsText = PropUtils.retainIfModified(
            dbAlbum?.authorsText,
            innertubeAlbum.artists.fastJoinToString { it.text }
        ),
        shareUrl = dbAlbum?.shareUrl,
        timestamp = dbAlbum?.timestamp ?: System.currentTimeMillis(),
        bookmarkedAt = dbAlbum?.bookmarkedAt,
        isYoutubeAlbum = true
    )

    // Upsert to override/update default values
    albumTable.upsert( onlineAlbum )

    // Map ignore to make sure only positions
    // are overridden, not the songs themselves
    innertubeAlbum.songs
                 .fastMap( InnertubeSong::toMediaItem )
                 .onEach( ::insertIgnore )
                 .mapIndexed { position, mediaItem ->
                     SongAlbumMap(
                         songId = mediaItem.mediaId,
                         albumId = innertubeAlbum.id,
                         position = position
                     )
                 }
                 .also( songAlbumMapTable::upsert )
}

@ExperimentalFoundationApi
@UnstableApi
private fun LazyListScope.renderSection(
    navController: NavController,
    section: Section,
    sectionTextModifier: Modifier
) {
    stickyHeader( System.identityHashCode( section ) ) {
        Text(
            text = if( !section.title.isNullOrBlank() )
                section.title!!
            else if( section.contents.fastAll { it is InnertubeSong } )
                stringResource( R.string.songs )
            else
                "",
            style = typography().m.semiBold,
            modifier = sectionTextModifier.fillMaxWidth()
        )
    }

    section.contents.fastMapNotNull { it as? InnertubeAlbum }.also {
        item( section.title ) {
            val appearance = LocalAppearance.current
            val albumItemValues = remember( appearance ) {
                AlbumItem.Values.from( appearance )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(AlbumItem.COLUMN_SPACING.dp )
            ) {
                this@LazyRow.items(
                    items = it,
                    key = InnertubeAlbum::id
                ) { item ->
                    AlbumItem.Vertical( item, albumItemValues, navController )
                }
            }
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@UnstableApi
@Composable
fun YouTubeAlbum(
    navController: NavController,
    browseId: String,
    params: String?,
    miniPlayer: @Composable () -> Unit = {}
) {
    //<editor-fold desc="Essentials">
    val player: StatefulPlayer = koinInject()
    val hapticFeedback = LocalHapticFeedback.current
    val (colorPalette, typography) = LocalAppearance.current
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val lazyListState = rememberLazyListState()
    val menu = LocalBottomMenu.current
    //</editor-fold>

    var albumPage: InnertubeAlbum? by remember { mutableStateOf( null ) }
    val dbAlbum: Album? by remember {
        Database.albumTable
                .findById( browseId )
    }.collectAsState( null, Dispatchers.IO )
    val items by remember {
        Database.songAlbumMapTable
                .allSongsOf( browseId )
    }.collectAsState( emptyList(), Dispatchers.IO )
    var isRefreshing by remember { mutableStateOf( false ) }
    val sectionTextModifier = remember {
        Modifier.padding( 16.dp, 24.dp, 16.dp, 8.dp )
    }

    //<editor-fold desc="Buttons">
    val itemSelector = remember {
        ItemSelector( menuState ) { addAll( items ) }
    }

    fun getSongs() = itemSelector.ifEmpty { items }
    fun getMediaItems() = getSongs().map( Song::asMediaItem )

    val bookmark = remember { Bookmark(browseId) }
    val deleteAllDownloadsDialog = remember {
        DeleteAllDownloadedDialog(::getSongs)
    }
    val downloadALlDialog = remember {
        DownloadAllDialog( context, ::getSongs )
    }
    val shuffle = SongShuffler {
        getMediaItems().map( MediaItem::asSong )
    }
    val radio = Radio( ::getSongs )
    val locator = Locator( lazyListState, ::getSongs )
    val playNext = PlayNext {
        getMediaItems().let {
            player.addNext( it, context )

            // Turn of selector clears the selected list
            itemSelector.isActive = false
        }
    }
    val enqueue = Enqueue {
        getMediaItems().let {
            player.enqueue( it, context )

            // Turn of selector clears the selected list
            itemSelector.isActive = false
        }
    }
    val addToPlaylist = PlaylistsMenu.init(
        navController,
        { getMediaItems() },
        { throwable, preview ->
            Logger.e( throwable, "YouTubeAlbum" ) {
                "Failed to add songs to playlist ${preview.playlist.name} on HomeSongs"
            }
            throwable.printStackTrace()
        },
        {
            // Turn of selector clears the selected list
            itemSelector.isActive = false
        }
    )
    //<editor-fold defaultstate="collapsed" desc="Album modifiers">
    val changeTitle = AlbumModifier(
        iconId = R.drawable.title_edit,
        messageId = R.string.update_title,
        getDefaultValue = { dbAlbum?.cleanTitle() ?: "" },
    ) {
        updateTitle( browseId, "$MODIFIED_PREFIX$it" )
    }
    val changeAuthors = AlbumModifier(
        iconId = R.drawable.artists_edit,
        messageId = R.string.update_authors,
        getDefaultValue = { dbAlbum?.cleanAuthorsText() ?: "" },
    ) {
        updateAuthors( browseId, "$MODIFIED_PREFIX$it" )
    }
    val changeCover = AlbumModifier(
        iconId = R.drawable.cover_edit,
        messageId = R.string.update_cover,
        getDefaultValue = { dbAlbum?.cleanThumbnailUrl() ?: "" },
    ) {
        updateCover( browseId, "$MODIFIED_PREFIX$it" )
    }
    //</editor-fold>

    downloadALlDialog.Render()
    deleteAllDownloadsDialog.Render()
    changeTitle.Render()
    changeAuthors.Render()
    changeCover.Render()
    //</editor-fold>

    fun onRefresh() = CoroutineScope( Dispatchers.IO ).launch {
        Innertube.browseAlbum( browseId, CURRENT_LOCALE, params )
                 .onSuccess {
                     albumPage = it
                     updateAlbumInDatabase( dbAlbum, it )
                 }
                 .onFailure { err ->
                     Logger.e( "", err, "YouTubeAlbum" )
                     Toaster.e( R.string.error_failed_to_load_album )
                 }

        isRefreshing = false
    }
    LaunchedEffect( Unit ) { onRefresh() }

    val currentMediaItem by player.currentMediaItemState.collectAsState()
    val songItemValues = remember( colorPalette, typography ) {
        SongItem.Values.from( colorPalette, typography )
    }

    val thumbnailPainter = ImageFactory.rememberAsyncImagePainter( dbAlbum?.cleanThumbnailUrl() )
    DetailSkeleton(
        navController = navController,
        miniPlayer = miniPlayer,
        actions = {
            DetailMenu(
                listOf(
                    playNext, enqueue, addToPlaylist, deleteAllDownloadsDialog, locator,
                    itemSelector, changeTitle, changeAuthors, changeCover
                )
            )
        }
    ) {
        DynamicOrientationLayout( thumbnailPainter ) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    onRefresh()
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = lazyListState,
                    userScrollEnabled = albumPage != null || dbAlbum != null,
                    contentPadding = PaddingValues(bottom = Dimensions.bottomSpacer),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    item( "header" ) {
                        val links = remember( albumPage, dbAlbum ) {
                            albumPage?.artists
                                     ?.filter { it.text.isNotBlank() }
                                     ?.map { artist ->
                                         HeaderLink(
                                             text = artist.text,
                                             onClick = artist.navigationEndpoint?.browseEndpoint?.let { endpoint ->
                                                 {
                                                     NavRoutes.YT_ARTIST.navigateHere(
                                                         navController,
                                                         "${endpoint.browseId}?params=${endpoint.params}"
                                                     )
                                                 }
                                             }
                                         )
                                     }
                                ?: dbAlbum?.cleanAuthorsText()?.let { listOf( HeaderLink(it, null) ) }
                                ?: emptyList()
                        }
                        val details = remember( albumPage ) {
                            listOfNotNull(
                                albumPage?.year,
                                albumPage?.subtitle?.runs?.fastJoinToString( "" ) { it.text }
                            ).filter( String::isNotBlank ).joinToString( " · " )
                        }

                        DetailHeader(
                            cover = thumbnailPainter,
                            title = dbAlbum?.cleanTitle().orEmpty(),
                            links = links,
                            details = details,
                            onPlay = {
                                player.stopRadio()
                                player.forcePlayFromBeginning( getMediaItems() )
                            },
                            onShuffle = shuffle::onShortClick
                        ) {
                            bookmark.ObserveState()
                            LabeledAction( bookmark, stringResource( R.string.word_save ) )
                            LabeledAction( downloadALlDialog, stringResource( R.string.download ) )
                            LabeledAction( radio, stringResource( R.string.word_radio ) )
                            LabeledAction( painterResource( R.drawable.share_social ), stringResource( R.string.share ) ) {
                                albumPage?.shareUrl( Constants.YOUTUBE_MUSIC_URL )?.also( context::shareUrl )
                            }
                        }
                    }

                    if( items.isEmpty() )
                        items( 10 ) { SongItem.Placeholder() }
                    else
                        itemsIndexed(
                            items = items,
                            key = { i, s -> "${System.identityHashCode( s )} - $i"}
                        ) { index, song ->
                            val mediaItem = song.asMediaItem

                            SwipeablePlaylistItem(
                                mediaItem = mediaItem,
                                onPlayNext = {
                                    player.addNext( mediaItem )
                                }
                            ) {
                                SongItem.Render(
                                    song = song,
                                    hapticFeedback = hapticFeedback,
                                    isPlaying = song.shallowCompare( currentMediaItem ),
                                    values = songItemValues,
                                    itemSelector = itemSelector,
                                    showThumbnail = false,
                                    thumbnailOverlay = {
                                        BasicText(
                                            text = "${index + 1}",
                                            style = typography().s
                                                                .semiBold
                                                                .center
                                                                .color(
                                                                    colorPalette().textDisabled
                                                                ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.width( SongItem.thumbnailSize().width )
                                                               .align( Alignment.Center )
                                        )
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
                                                items.fastMap( Song::asMediaItem ),
                                                index
                                            )

                                        /*
                                            Due to the small size of checkboxes,
                                            we shouldn't disable [itemSelector]
                                         */
                                    },
                                    onLongClick = {
                                        val page = MenuPage.Song(mediaItem)
                                        menu.show( page, true )
                                    }
                                )
                            }
                        }

                    albumPage?.sections?.fastForEach {
                        renderSection( navController, it, sectionTextModifier )
                    }

                    albumPage?.description?.also( this::renderDescription )
                }
            }
        }
    }
}