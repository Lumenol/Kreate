package app.kreate.android.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.LocalBottomMenu
import app.kreate.android.constant.MenuPage
import app.kreate.android.service.player.StatefulPlayer
import app.kreate.android.themed.common.component.BottomMenu
import app.kreate.android.themed.rimusic.component.album.AlbumItem
import app.kreate.android.themed.rimusic.component.artist.ArtistItem
import app.kreate.android.themed.rimusic.component.playlist.PlaylistItem
import app.kreate.android.themed.rimusic.component.song.SongItem
import app.kreate.android.utils.innertube.toMediaItem
import it.fast4x.innertube.Innertube
import it.fast4x.rimusic.isVideoEnabled
import it.fast4x.rimusic.thumbnailShape
import it.fast4x.rimusic.ui.styling.LocalAppearance
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.forcePlay
import it.fast4x.rimusic.utils.playVideo
import it.fast4x.rimusic.utils.shimmerEffect
import me.knighthat.innertube.model.InnertubeAlbum
import me.knighthat.innertube.model.InnertubeArtist
import me.knighthat.innertube.model.InnertubeItem
import me.knighthat.innertube.model.InnertubePlaylist
import me.knighthat.innertube.model.InnertubeSong
import org.koin.compose.koinInject
import kotlin.math.max
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.Constraints


object ItemUtils {

    const val COLUMN_SPACING = 10

    @Composable
    fun ThumbnailPlaceholder(
        sizeDp: DpSize,
        modifier: Modifier = Modifier
    ) =
        Box( modifier.size( sizeDp ).clip(thumbnailShape() ).shimmerEffect() )

    @JvmName("OldInnertubeLazyRowItem")
    @UnstableApi
    @Composable
    fun LazyRowItem(
        navController: NavController,
        innertubeItems: List<Innertube.Item>,
        currentlyPlaying: String?,
        modifier: Modifier = Modifier,
        menu: BottomMenu = LocalBottomMenu.current
    ) {
        val player: StatefulPlayer = koinInject()
        val hapticFeedback = LocalHapticFeedback.current
        val appearance = LocalAppearance.current
        val songItemValues = remember( appearance ) {
            SongItem.Values.from( appearance )
        }
        val albumItemValues = remember( appearance ) {
            AlbumItem.Values.from( appearance )
        }
        val artistItemValues = remember( appearance ) {
            ArtistItem.Values.from( appearance )
        }
        val playlistItemValues = remember( appearance ) {
            PlaylistItem.Values.from( appearance )
        }

        // One height for the whole row, computed from ALL its items (not just the
        // visible ones) so it never changes while scrolling: the tallest cover plus
        // the tallest text block (title up to two lines, then the subtitle lines
        // that item really has), so short rows don't keep empty space either
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val cardHeight = remember( innertubeItems, appearance, density ) {
            val cardWidthPx = with( density ) { AlbumItem.thumbnailSize().width.roundToPx() }
            val subtitleLinePx = textMeasurer.measure( "A", albumItemValues.artistsTextStyle ).size.height

            var coverHeight = 0.dp
            var textPx = 0
            innertubeItems.forEach { item ->
                val subtitleLines = when( item ) {
                    is Innertube.SongItem -> if( item.authors.isNullOrEmpty() ) 0 else 1
                    is Innertube.VideoItem -> if( item.authors.isNullOrEmpty() ) 0 else 1
                    is Innertube.AlbumItem -> ( if( item.authors.isNullOrEmpty() ) 0 else 1 ) + ( if( item.year == null ) 0 else 1 )
                    is Innertube.PlaylistItem -> if( item.channel == null && item.songCount == null ) 0 else 1
                    is Innertube.ArtistItem -> if( item.subscribersCountText.isNullOrBlank() ) 0 else 1
                }
                val cover = when( item ) {
                    is Innertube.PlaylistItem -> PlaylistItem.thumbnailSize().height
                    is Innertube.ArtistItem -> ArtistItem.thumbnailSize().height
                    else -> AlbumItem.thumbnailSize().height
                }
                val titlePx = textMeasurer.measure(
                    text = item.title.orEmpty(),
                    style = albumItemValues.titleTextStyle,
                    maxLines = 2,
                    constraints = Constraints( maxWidth = cardWidthPx )
                ).size.height

                coverHeight = maxOf( coverHeight, cover )
                textPx = maxOf( textPx, titlePx + subtitleLines * subtitleLinePx )
            }
            // Cover's bottom padding + title's vertical padding (AlbumItem.VERTICAL_SPACING
            // each) plus a little room for font metrics: 12 dp clipped the subtitle
            with( density ) { coverHeight + textPx.toDp() } + ( AlbumItem.VERTICAL_SPACING * 3 ).dp + 12.dp
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(COLUMN_SPACING.dp ),
            // Same 16 dp start margin as the section headers
            contentPadding = PaddingValues( horizontal = 16.dp ),
            modifier = modifier.height( cardHeight )
        ) {
            items(
                items = innertubeItems,
                key = Innertube.Item::key
            ) { childItem ->

                when ( childItem ) {
                    // In a row of cards a song is a card too (cover, title, artists),
                    // not a list row squeezed between them
                    is Innertube.SongItem -> AlbumItem.VerticalStructure(
                        widthDp = AlbumItem.thumbnailSize().width,
                        thumbnail = {
                            AlbumItem.Thumbnail( childItem.key, childItem.thumbnail?.url )
                        },
                        firstLine = {
                            AlbumItem.Title(
                                title = childItem.info?.name.orEmpty(),
                                values = albumItemValues,
                                textAlign = TextAlign.Start,
                                maxLines = 2
                            )
                        },
                        secondLine = {
                            AlbumItem.Artists(
                                artistsText = childItem.authors?.joinToString( ", " ) { it.name.orEmpty() }.orEmpty(),
                                values = albumItemValues,
                                textAlign = TextAlign.Start
                            )
                        },
                        onClick = {
                            player.forcePlay( childItem.asMediaItem )
                        },
                        onLongClick = {
                            hapticFeedback.performHapticFeedback( HapticFeedbackType.LongPress )
                            menu.show( MenuPage.Song( childItem.asMediaItem ), true )
                        }
                    )

                    is Innertube.VideoItem -> AlbumItem.VerticalStructure(
                        widthDp = AlbumItem.thumbnailSize().width,
                        thumbnail = {
                            // Videos come as 16:9: crop them square like the other cards
                            AlbumItem.Thumbnail(
                                url = childItem.thumbnail?.url,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.padding( bottom = AlbumItem.VERTICAL_SPACING.dp )
                            )
                        },
                        firstLine = {
                            AlbumItem.Title(
                                title = childItem.info?.name.orEmpty(),
                                values = albumItemValues,
                                textAlign = TextAlign.Start,
                                maxLines = 2
                            )
                        },
                        secondLine = {
                            AlbumItem.Artists(
                                artistsText = childItem.authors?.joinToString( ", " ) { it.name.orEmpty() }.orEmpty(),
                                values = albumItemValues,
                                textAlign = TextAlign.Start
                            )
                        },
                        onClick = {
                            player.stopRadio()
                            if ( isVideoEnabled() )
                                player.playVideo( childItem.asMediaItem )
                            else
                                player.forcePlay( childItem.asMediaItem )
                        },
                        onLongClick = {
                            hapticFeedback.performHapticFeedback( HapticFeedbackType.LongPress )
                            menu.show( MenuPage.Song( childItem.asMediaItem ), true )
                        }
                    )

                    is Innertube.AlbumItem -> AlbumItem.Vertical(
                        innertubeAlbum = childItem,
                        values = albumItemValues,
                        navController = navController
                    )

                    is Innertube.ArtistItem -> ArtistItem.Render(
                        innertubeArtist = childItem,
                        values = artistItemValues,
                        navController = navController
                    )

                    is Innertube.PlaylistItem -> PlaylistItem.Vertical(
                        innertubePlaylist = childItem,
                        values = playlistItemValues,
                        navController = navController
                    )
                }
            }
        }
    }

    /**
     * Lays items out as a wrapping grid (multiple lines) instead of one horizontal
     * scrolling row. Used for pages that are a single big grid — e.g. "Mixed for
     * you" — where a lone horizontal line reads poorly.
     */
    @OptIn(ExperimentalLayoutApi::class)
    @UnstableApi
    @Composable
    fun GridItems(
        navController: NavController,
        innertubeItems: List<Innertube.Item>,
        currentlyPlaying: String?,
        modifier: Modifier = Modifier,
        menu: BottomMenu = LocalBottomMenu.current
    ) {
        val player: StatefulPlayer = koinInject()
        val hapticFeedback = LocalHapticFeedback.current
        val appearance = LocalAppearance.current
        val songItemValues = remember( appearance ) { SongItem.Values.from( appearance ) }
        val albumItemValues = remember( appearance ) { AlbumItem.Values.from( appearance ) }
        val artistItemValues = remember( appearance ) { ArtistItem.Values.from( appearance ) }
        val playlistItemValues = remember( appearance ) { PlaylistItem.Values.from( appearance ) }

        val spacing = COLUMN_SPACING.dp
        val cellWidth = PlaylistItem.thumbnailSize().width
        BoxWithConstraints(
            modifier = modifier.fillMaxWidth(),
            // Center the whole grid as a block; items stay left-aligned inside it.
            contentAlignment = Alignment.TopCenter
        ) {
            val columns = max( 1, ( (maxWidth + spacing) / (cellWidth + spacing) ).toInt() )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy( spacing ),
                maxItemsInEachRow = columns,
                modifier = Modifier.width( cellWidth * columns + spacing * (columns - 1) )
            ) {
            innertubeItems.forEach { childItem ->
                when ( childItem ) {
                    is Innertube.SongItem -> SongItem.Render(
                        innertubeSong = childItem,
                        hapticFeedback = hapticFeedback,
                        values = songItemValues,
                        isPlaying = childItem.key == currentlyPlaying,
                        onClick = { player.forcePlay( childItem.asMediaItem ) },
                        onLongClick = { menu.show( MenuPage.Song(childItem.asMediaItem), true ) }
                    )

                    is Innertube.VideoItem -> SongItem.Render(
                        innertubeVideo = childItem,
                        hapticFeedback = hapticFeedback,
                        isPlaying = currentlyPlaying == childItem.key,
                        values = songItemValues,
                        thumbnailSizeDp = SongItem.thumbnailSize(),
                        onClick = {
                            player.stopRadio()
                            if ( isVideoEnabled() ) player.playVideo( childItem.asMediaItem )
                            else player.forcePlay( childItem.asMediaItem )
                        },
                        onLongClick = { menu.show( MenuPage.Song(childItem.asMediaItem), true ) }
                    )

                    is Innertube.AlbumItem -> AlbumItem.Vertical(
                        innertubeAlbum = childItem,
                        values = albumItemValues,
                        navController = navController
                    )

                    is Innertube.ArtistItem -> ArtistItem.Render(
                        innertubeArtist = childItem,
                        values = artistItemValues,
                        navController = navController
                    )

                    is Innertube.PlaylistItem -> PlaylistItem.Vertical(
                        innertubePlaylist = childItem,
                        values = playlistItemValues,
                        navController = navController
                    )
                }
            }
            }
        }
    }

    @UnstableApi
    @Composable
    fun LazyRowItem(
        navController: NavController,
        innertubeItems: List<InnertubeItem>,
        currentlyPlaying: String?,
        modifier: Modifier = Modifier,
        player: StatefulPlayer = koinInject(),
        menu: BottomMenu = LocalBottomMenu.current
    ) {
        val hapticFeedback = LocalHapticFeedback.current
        val appearance = LocalAppearance.current
        val (songIV, albumIV, artistIV, playlistIV) = remember( appearance ) {
            Quadruple(
                SongItem.Values.from( appearance ),
                AlbumItem.Values.from( appearance ),
                ArtistItem.Values.from( appearance ),
                PlaylistItem.Values.from( appearance )
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(COLUMN_SPACING.dp ),
            // Same 16 dp start margin as the section headers
            contentPadding = PaddingValues( horizontal = 16.dp ),
            modifier = modifier
        ) {
            items(
                items = innertubeItems,
                key = System::identityHashCode
            ) { item ->
                when( item ) {
                    is InnertubeSong -> SongItem.Render(
                        innertubeSong = item,
                        hapticFeedback = hapticFeedback,
                        values = songIV,
                        isPlaying = item.id == currentlyPlaying,
                        onClick = {
                            player.forcePlay( item.toMediaItem )
                        },
                        onLongClick = {
                            val page = MenuPage.Song(item.toMediaItem)
                            menu.show( page, true )
                        }
                    )

                    is InnertubeAlbum -> AlbumItem.Vertical(
                        innertubeAlbum = item,
                        values = albumIV,
                        navController = navController
                    )

                    is InnertubeArtist -> ArtistItem.Render(
                        innertubeArtist = item,
                        values = artistIV,
                        navController = navController
                    )

                    is InnertubePlaylist -> PlaylistItem.Vertical(
                        innertubePlaylist = item,
                        values = playlistIV,
                        navController = navController
                    )
                }
            }
        }
    }

    @Composable
    fun PlaceholderRowItem(
        modifier: Modifier = Modifier,
        itemContent: @Composable LazyItemScope.(index: Int) -> Unit
    ) =
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(COLUMN_SPACING.dp ),
            userScrollEnabled = false,
            modifier = modifier
        ) {
            items( 10, itemContent =  itemContent )
        }

    private data class Quadruple<T1, T2, T3, T4>(val t1: T1, val t2: T2, val t3: T3, val t4: T4)
}