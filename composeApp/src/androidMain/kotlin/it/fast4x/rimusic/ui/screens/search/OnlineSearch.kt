package it.fast4x.rimusic.ui.screens.search

import androidx.annotation.DrawableRes
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import it.fast4x.rimusic.enums.NavRoutes
import it.fast4x.rimusic.utils.color
import it.fast4x.rimusic.utils.semiBold
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.Preferences
import app.kreate.android.R
import app.kreate.android.service.player.StatefulPlayer
import app.kreate.android.themed.rimusic.component.album.AlbumItem
import app.kreate.android.themed.rimusic.component.song.SongItem
import app.kreate.android.utils.shallowCompare
import app.kreate.database.models.SearchQuery
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.SearchSuggestionsBody
import it.fast4x.innertube.requests.searchSuggestionsWithItems
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.LocalPlayerAwareWindowInsets
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.NavigationBarPosition
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.fast4x.rimusic.ui.components.themed.NonQueuedMediaItemMenu
import it.fast4x.rimusic.ui.components.themed.TitleMiniSection
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.ui.styling.LocalAppearance
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.forcePlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

@UnstableApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalTextApi
@Composable
fun OnlineSearch(
    navController: NavController,
    textFieldValue: TextFieldValue,
    onTextFieldValueChanged: (TextFieldValue) -> Unit,
    onSearch: (String) -> Unit,
    decorationBox: @Composable (@Composable () -> Unit) -> Unit,
) {
    val appearance = LocalAppearance.current
    // Settings
    val isHistoryPaused by Preferences.PAUSE_SEARCH_HISTORY

    var reloadHistory by remember {
        mutableStateOf(false)
    }

    val history by remember( textFieldValue.text, isHistoryPaused, reloadHistory ) {
        if( isHistoryPaused ) return@remember flowOf()

        Database.searchTable
                .findAllContain( textFieldValue.text )
                .distinctUntilChanged()
                .map{ list -> list.reversed() }
    }.collectAsState( emptyList(), Dispatchers.IO )

    //var suggestionsResult by persist<Result<List<String>?>?>("search/online/suggestionsResult")
    var suggestionsResult by remember {
        mutableStateOf<Result<Innertube.SearchSuggestions>?>(null)
    }

    LaunchedEffect(textFieldValue.text) {
        if (textFieldValue.text.isNotEmpty()) {
            delay(200)
            //suggestionsResult =
            //    Innertube.searchSuggestions(SearchSuggestionsBody(input = textFieldValue.text))
            suggestionsResult =
                Innertube.searchSuggestionsWithItems(SearchSuggestionsBody(input = textFieldValue.text))
        }
    }

    /*
    val playlistId = remember(textFieldValue.text) {
        val isPlaylistUrl = listOf(
            "https://www.youtube.com/playlist?",
            "https://youtube.com/playlist?",
            "https://music.youtube.com/playlist?",
            "https://m.youtube.com/playlist?"
        ).any(textFieldValue.text::startsWith)

        if (isPlaylistUrl) textFieldValue.text.toUri().getQueryParameter("list") else null
    }
    */

    val rippleIndication = ripple(bounded = false)
    val timeIconPainter = painterResource(R.drawable.search_circle)
    val closeIconPainter = painterResource(R.drawable.trash)

    val coroutineScope = rememberCoroutineScope()


    var thumbnailRoundness by Preferences.THUMBNAIL_BORDER_RADIUS

    val lazyListState = rememberLazyListState()

    val menuState = LocalMenuState.current
    val hapticFeedback = LocalHapticFeedback.current
    val player: StatefulPlayer = koinInject()
    val (colorPalette, typography) = LocalAppearance.current

    Box(
        modifier = Modifier
            .background(colorPalette().background0)
            //.fillMaxSize()
            .fillMaxHeight()
            .fillMaxWidth(
                if( NavigationBarPosition.Right.isCurrent() )
                    Dimensions.contentWidthRightBar
                else
                    1f
            )
    ) {
        val currentMediaItem by player.currentMediaItemState.collectAsState()
        val songItemValues = remember( colorPalette, typography ) {
            SongItem.Values.from( colorPalette, typography )
        }

        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Bottom + WindowInsetsSides.End).asPaddingValues(),
            modifier = Modifier
                .fillMaxSize()
        ) {
            suggestionsResult?.getOrNull()?.let { suggestions ->
                // Text suggestions first: tapping searches, the arrow only fills the field
                items(items = suggestions.queries) { query ->
                    SearchRow(
                        iconId = R.drawable.search,
                        text = highlightCompletion( query, textFieldValue.text ),
                        onClick = { onSearch(query.replace("/", "", true)) }
                    ) {
                        RowIconButton( R.drawable.up_right_arrow, stringResource( R.string.search_fill ), Modifier.rotate( -90f ) ) {
                            onTextFieldValueChanged(
                                TextFieldValue( text = query, selection = TextRange(query.length) )
                            )
                        }
                    }
                }

                suggestions.recommendedArtist?.let { artist ->
                    item {
                        ArtistRow(
                            name = artist.title.orEmpty(),
                            thumbnailUrl = artist.thumbnail?.url,
                            detail = stringResource( R.string.word_artist )
                        ) {
                            NavRoutes.YT_ARTIST.navigateHere( navController, artist.key )
                        }
                    }
                }
                suggestions.recommendedSong?.let { song ->
                    item {
                        SongItem.Render(
                            innertubeSong = song,
                            hapticFeedback = hapticFeedback,
                            values = songItemValues,
                            isPlaying = song.shallowCompare( currentMediaItem ),
                            onLongClick = {
                                menuState.display {
                                    NonQueuedMediaItemMenu(
                                        navController = navController,
                                        onDismiss = menuState::hide,
                                        mediaItem = song.asMediaItem
                                    )
                                };
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onClick = {
                                player.forcePlay(song.asMediaItem)
                            }
                        )
                    }
                }
                suggestions.recommendedAlbum?.let { album ->
                    item {
                        val albumItemValues = remember( appearance ) {
                            AlbumItem.Values.from( appearance )
                        }
                        AlbumItem.Horizontal( album, albumItemValues, navController )
                    }
                }
            } ?: suggestionsResult?.exceptionOrNull()?.let {
                item {
                    TitleMiniSection(title = stringResource(R.string.searches_no_suggestions),
                        modifier = Modifier.padding(start = 16.dp).padding(vertical = 10.dp)
                    )
                }
            }

            if(history.isNotEmpty())
                item {
                    BasicText(
                        text = stringResource( R.string.search_recent ),
                        style = typography().xs.semiBold.color( colorPalette().textSecondary ),
                        modifier = Modifier.padding( start = 16.dp, top = 16.dp, bottom = 4.dp )
                    )
                }

            // Clock + cross instead of a magnifier, a bin and a pencil on every line
            items(
                items = history,
                key = SearchQuery::id
            ) { searchQuery ->
                SearchRow(
                    iconId = R.drawable.history,
                    text = AnnotatedString( searchQuery.query ),
                    onClick = { onSearch(searchQuery.query.replace("/", "", true)) }
                ) {
                    RowIconButton(
                        iconId = R.drawable.close,
                        description = stringResource( R.string.clear ),
                        onLongClick = {
                            Database.asyncTransaction {
                                history.also( searchTable::delete )
                            }
                            reloadHistory = !reloadHistory
                        }
                    ) {
                        Database.asyncTransaction {
                            searchTable.delete( searchQuery )
                        }
                    }
                }
            }
        }

        FloatingActionsContainerWithScrollToTop(lazyListState = lazyListState)
    }


}

/** "man" typed, "maneskin" suggested: the part YouTube adds is bold */
private fun highlightCompletion( suggestion: String, typed: String ): AnnotatedString =
    buildAnnotatedString {
        if( typed.isNotBlank() && suggestion.startsWith( typed, true ) ) {
            append( suggestion.take( typed.length ) )
            withStyle( SpanStyle(fontWeight = FontWeight.Bold) ) { append( suggestion.drop( typed.length ) ) }
        } else
            append( suggestion )
    }

@Composable
private fun SearchRow(
    @DrawableRes iconId: Int,
    text: AnnotatedString,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit
) =
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
                           .clickable( onClick = onClick )
                           .padding( start = 16.dp, end = 4.dp, top = 2.dp, bottom = 2.dp )
    ) {
        Icon(
            painter = painterResource( iconId ),
            contentDescription = null,
            tint = colorPalette().textSecondary,
            modifier = Modifier.size( 20.dp )
        )
        BasicText(
            text = text,
            style = typography().s.color( colorPalette().text ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight( 1f ).padding( horizontal = 16.dp )
        )
        trailing()
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowIconButton(
    @DrawableRes iconId: Int,
    description: String,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit
) =
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size( 44.dp )
                           .clip( CircleShape )
                           .combinedClickable( onClick = onClick, onLongClick = onLongClick )
    ) {
        Icon(
            painter = painterResource( iconId ),
            contentDescription = description,
            tint = colorPalette().textSecondary,
            modifier = modifier.size( 18.dp )
        )
    }

