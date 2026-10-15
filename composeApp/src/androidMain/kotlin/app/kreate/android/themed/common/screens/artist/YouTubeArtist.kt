package app.kreate.android.themed.common.screens.artist

import me.knighthat.utils.Toaster
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import app.kreate.android.utils.BlockedArtists
import androidx.compose.runtime.getValue
import app.kreate.android.themed.common.component.DetailMenu
import app.kreate.android.themed.common.component.DetailSkeleton
import app.kreate.android.themed.common.component.shareUrl
import it.fast4x.rimusic.ui.components.ButtonsRow
import it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastMapNotNull
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.LocalBottomMenu
import app.kreate.android.R
import app.kreate.android.coil3.ImageFactory
import app.kreate.android.constant.MenuPage
import app.kreate.android.service.player.StatefulPlayer
import app.kreate.android.themed.common.component.BottomMenu
import app.kreate.android.themed.common.component.tab.DeleteAllDownloadedDialog
import app.kreate.android.themed.common.component.tab.DownloadAllDialog
import app.kreate.android.themed.rimusic.component.album.AlbumItem
import app.kreate.android.themed.rimusic.component.song.SongItem
import app.kreate.android.utils.ItemUtils
import app.kreate.android.utils.innertube.toMediaItem
import app.kreate.android.utils.innertube.toSong
import app.kreate.android.utils.renderDescription
import app.kreate.android.utils.shallowCompare
import app.kreate.android.viewmodel.YoutubeArtistViewModel
import app.kreate.database.models.Song
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.NavRoutes
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.SwipeablePlaylistItem
import it.fast4x.rimusic.ui.components.navigation.header.TabToolBar
import it.fast4x.rimusic.ui.components.themed.Enqueue
import it.fast4x.rimusic.ui.components.themed.PlayNext
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.ui.styling.LocalAppearance
import it.fast4x.rimusic.utils.addNext
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.enqueue
import it.fast4x.rimusic.utils.fadingEdge
import it.fast4x.rimusic.utils.forcePlayAtIndex
import it.fast4x.rimusic.utils.isLandscape
import it.fast4x.rimusic.utils.isNetworkConnected
import it.fast4x.rimusic.utils.medium
import it.fast4x.rimusic.utils.semiBold
import me.knighthat.component.artist.FollowButton
import me.knighthat.component.tab.Radio
import me.knighthat.component.tab.SongShuffler
import me.knighthat.component.ui.screens.DynamicOrientationLayout
import me.knighthat.innertube.Constants
import me.knighthat.innertube.model.InnertubeAlbum
import me.knighthat.innertube.model.InnertubeArtist
import me.knighthat.innertube.model.InnertubeSong
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel


@OptIn(UnstableApi::class)
private fun LazyListScope.renderSections(
    navController: NavController,
    hapticFeedback: HapticFeedback,
    currentMedia: MediaItem?,
    songItemValues: SongItem.Values,
    artistPage: InnertubeArtist,
    sectionTextModifier: Modifier,
    player: StatefulPlayer,
    menu: BottomMenu
) = artistPage.sections.forEach { section ->
    // Don't show section if the title or contents is null or blank
    if( section.title.isNullOrBlank() || section.contents.isEmpty() ) return@forEach

    item( "${section.title}Header" ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = sectionTextModifier.fillMaxWidth()
        ) {
            Text(
                text = section.title!!,
                style = typography().m.semiBold,
                modifier = Modifier.weight( 1f )
            )

            // TODO: Add support for playlists
            if( section.browseId != null && section.contents.fastAll { it is InnertubeSong || it is InnertubeAlbum} )
                BasicText(
                    text = stringResource( R.string.word_see_all ),
                    style = typography().xs.semiBold.copy( colorPalette().textSecondary ),
                    modifier = Modifier.clip( CircleShape )
                                       .border( 1.dp, colorPalette().textDisabled, CircleShape )
                                       .clickable {
                        val path = "${section.browseId}?params=${section.params}"

                        val route: NavRoutes = if( section.contents.fastAll { it is InnertubeSong } )
                            NavRoutes.YT_PLAYLIST
                        else if( section.contents.fastAll { it is InnertubeAlbum } )
                            NavRoutes.artistAlbums
                        else
                            return@clickable

                        route.navigateHere( navController, path )
                    }
                                       .padding( horizontal = 10.dp, vertical = 6.dp )
                )
        }
    }

     /*
         Use mapNotNull with `as?` to avoid unnecessary checking (double traversal)
         Double traversal example:

         if( items.fastAll { it is InnertubeSong } )
            items.fastMap { it as InnertubeSong }
      */
    section.contents
           .fastMapNotNull { it as? InnertubeSong }
           // YouTube lists the same song from its single, album and compilations
           .distinctBy { it.name.trim().lowercase() }
           .also { songs ->
               itemsIndexed(
                   items = songs,
                   key = { i, s -> "${System.identityHashCode( s )} - $i" }
               ) { index, song ->
                   val mediaItem = song.toMediaItem

                   SwipeablePlaylistItem(
                       mediaItem = mediaItem,
                       onPlayNext = {
                           player.addNext( mediaItem )
                       }
                   ) {
                       SongItem.Render(
                           song = song.toSong,
                           hapticFeedback = hapticFeedback,
                           isPlaying = song.shallowCompare( currentMedia ),
                           values = songItemValues,
                           showThumbnail = true,
                           onClick = {
                               player.stopRadio()
                               player.forcePlayAtIndex(
                                   songs.map( InnertubeSong::toMediaItem ),
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
           }

    section.contents
           .filterNot { it is InnertubeSong }
           .also { items ->
               item {
                   ItemUtils.LazyRowItem(
                       navController = navController,
                       innertubeItems = items,
                       currentlyPlaying = null
                   )
               }
           }
}

@OptIn(UnstableApi::class)
private fun LazyListScope.renderLibrarySongs(
    navController: NavController,
    hapticFeedback: HapticFeedback,
    currentMedia: MediaItem?,
    songItemValues: SongItem.Values,
    sectionTextModifier: Modifier,
    songs: List<Song>,
    player: StatefulPlayer,
    menu: BottomMenu
) {
    item( "songs" ) {
        Text(
            text = stringResource( R.string.songs ),
            style = typography().m.semiBold,
            modifier = sectionTextModifier.fillMaxWidth()
        )
    }

    itemsIndexed(
        items = songs,
        key = { i, s -> "${System.identityHashCode( s )} - $i" }
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
                isPlaying = song.shallowCompare( currentMedia ),
                values = songItemValues,
                showThumbnail = true,
                onClick = {
                    player.stopRadio()
                    player.forcePlayAtIndex(
                        songs.map( Song::asMediaItem ),
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
}

@OptIn(UnstableApi::class)
@Composable
fun YouTubeArtist(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: YoutubeArtistViewModel = koinViewModel(),
    menu: BottomMenu = LocalBottomMenu.current,
    miniPlayer: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val player: StatefulPlayer = koinInject()
    val (colorPalette, typography) = LocalAppearance.current
    val hapticFeedback = LocalHapticFeedback.current
    val saveableStateHolder = rememberSaveableStateHolder()
    val dbArtist by viewModel.dbArtist.collectAsStateWithLifecycle()
    val artistPage by viewModel.artistPage.collectAsStateWithLifecycle()
    val (tabIndex, onTabChanged) = remember { mutableIntStateOf(
            if( isNetworkConnected( context ) ) 0 else 1
    ) }
    val currentMedia by player.currentMediaItemState.collectAsStateWithLifecycle()
    val artistLibrarySongs by viewModel.artistLibrarySongs.collectAsStateWithLifecycle()
    val songItemValues = remember( colorPalette, typography ) {
        SongItem.Values.from( colorPalette, typography )
    }
    //<editor-fold desc="Thumbnail URL">
    val thumbnailUrl = remember( dbArtist, artistPage ) {
        if( dbArtist == null )
            artistPage?.thumbnails?.firstOrNull()?.url
        else
            dbArtist?.thumbnailUrl
    }
    val thumbnailPainter = ImageFactory.rememberAsyncImagePainter( thumbnailUrl )
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Buttons">
    val followButton = FollowButton { dbArtist }
    val shuffler = SongShuffler( viewModel::getSongs )
    val downloadAllDialog = remember {
        DownloadAllDialog( context, viewModel::getSongs )
    }
    val deleteAllDownloadsDialog = remember {
        DeleteAllDownloadedDialog(viewModel::getSongs)
    }
    val radio = Radio(viewModel::getSongs)
    val playNext = PlayNext {
        viewModel.getMediaItems().let {
            player.addNext( it, context )
        }
    }
    val enqueue = Enqueue {
        viewModel.getMediaItems().let {
            player.enqueue( it, context )
        }
    }

    val share = remember {
        object: MenuIcon {
            override val iconId: Int = R.drawable.share_social
            override val menuIconTitle: String
                @Composable
                get() = stringResource( R.string.share )

            override fun onShortClick() {
                artistPage?.shareUrl( Constants.YOUTUBE_MUSIC_URL )?.also( context::shareUrl )
            }
        }
    }
    // Radio and the infinite queue leave the artist out
    var isBlocked by remember { mutableStateOf( BlockedArtists.isBlocked( viewModel.browseId ) ) }
    val block = object: MenuIcon {
        override val iconId: Int = R.drawable.close
        override val menuIconTitle: String
            @Composable
            get() = stringResource( if( isBlocked ) R.string.unblock_artist else R.string.block_artist )

        override fun onShortClick() {
            isBlocked = BlockedArtists.toggle( viewModel.browseId )
            val name = artistPage?.name ?: dbArtist?.cleanName().orEmpty()
            Toaster.s( if( isBlocked ) R.string.artist_blocked else R.string.artist_unblocked, name )
        }
    }
    downloadAllDialog.Render()
    deleteAllDownloadsDialog.Render()
    //</editor-fold>

    DynamicOrientationLayout( thumbnailPainter ) {
        PullToRefreshBox(
            isRefreshing = viewModel.isRefreshing,
            onRefresh = {
                viewModel.isRefreshing = true
                viewModel.onRefresh()
            },
            modifier = modifier.fillMaxSize()
        ) {
            DetailSkeleton(
                navController = navController,
                miniPlayer = miniPlayer,
                actions = {
                    DetailMenu( listOf(share, playNext, enqueue, downloadAllDialog, deleteAllDownloadsDialog, block) )
                }
            ) {
                val currentTabIndex = tabIndex
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    LazyColumn(
                        state = viewModel.listState,
                        contentPadding = PaddingValues(bottom = Dimensions.bottomSpacer),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item( "header" ) {
                            Box( Modifier.fillMaxWidth() ) {
                                if ( !isLandscape )
                                    Image(
                                        painter = thumbnailPainter,
                                        contentDescription = null,
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier
                                            .aspectRatio(4f / 3)      // Limit height
                                            .fillMaxWidth()
                                            .align(Alignment.Center)
                                            .fadingEdge(
                                                top = WindowInsets.systemBars
                                                    .asPaddingValues()
                                                    .calculateTopPadding() + Dimensions.fadeSpacingTop,
                                                bottom = Dimensions.fadeSpacingBottom
                                            )
                                    )

                                // Full name on up to 2 lines, no scrolling text
                                Column(
                                    modifier = Modifier.align( Alignment.BottomStart )
                                                       .padding( horizontal = 20.dp )
                                ) {
                                    BasicText(
                                        // Use local artist name (custom name), otherwise, use online name
                                        text = (dbArtist?.cleanName() ?: artistPage?.name).orEmpty(),
                                        style = typography.l.semiBold.copy(
                                            color = colorPalette.text,
                                            fontSize = 34.sp,
                                            lineHeight = 38.sp
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    artistPage?.shortNumMonthlyAudience?.also { monthlyAudience ->
                                        BasicText(
                                            text = monthlyAudience,
                                            style = typography.xs.medium.copy( colorPalette.textSecondary ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        item( "action_buttons") {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy( 10.dp ),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                                                   .padding( horizontal = 20.dp )
                                                   .padding( top = 12.dp )
                            ) {
                                followButton.ToolBarButton()
                                TabToolBar.PillButton(
                                    text = stringResource( R.string.word_radio ),
                                    iconId = radio.iconId,
                                    filled = false,
                                    onClick = radio::onShortClick
                                )
                                Spacer( Modifier.weight( 1f ) )
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size( 52.dp )
                                                       .clip( CircleShape )
                                                       .background( colorPalette.text )
                                                       .clickable( onClick = shuffler::onShortClick )
                                ) {
                                    Icon(
                                        painter = painterResource( R.drawable.play ),
                                        contentDescription = stringResource( R.string.shuffle ),
                                        tint = colorPalette.background0,
                                        modifier = Modifier.size( 24.dp )
                                    )
                                }
                            }
                        }

                        // Used to be a bottom bar of its own ("Songs" / "Library")
                        item( "tabs" ) {
                            ButtonsRow(
                                chips = listOf(
                                    0 to stringResource( R.string.artist_tab_popular ),
                                    1 to stringResource( R.string.artist_tab_in_library )
                                ),
                                currentValue = tabIndex,
                                onValueUpdate = onTabChanged,
                                modifier = Modifier.fillMaxWidth().padding( top = 12.dp )
                            )
                        }

                        if( artistPage == null && viewModel.isRefreshing ) {
                            items( 5 ) { SongItem.Placeholder() }

                            items( 2 ) {
                                ItemUtils.PlaceholderRowItem {
                                    AlbumItem.VerticalPlaceholder()
                                }
                            }
                        } else if( artistPage != null && currentTabIndex == 0 )
                            renderSections(
                                navController = navController,
                                hapticFeedback = hapticFeedback,
                                currentMedia = currentMedia,
                                songItemValues = songItemValues,
                                artistPage = artistPage!!,
                                sectionTextModifier = viewModel.sectionTextModifier,
                                player = player,
                                menu = menu
                            )
                        else if( currentTabIndex == 1 )
                            renderLibrarySongs(
                                navController = navController,
                                hapticFeedback = hapticFeedback,
                                currentMedia = currentMedia,
                                songItemValues = songItemValues,
                                sectionTextModifier = viewModel.sectionTextModifier,
                                songs = artistLibrarySongs,
                                player = player,
                                menu = menu
                            )

                        artistPage?.description?.also( this::renderDescription )
                    }
                }
            }
        }
    }

    // Run once on start
    LaunchedEffect( Unit ) {
        if( artistPage == null ) viewModel.onRefresh()
    }
}