package it.fast4x.rimusic.ui.screens.history

import it.fast4x.rimusic.typography
import it.fast4x.rimusic.utils.color
import it.fast4x.rimusic.utils.semiBold
import androidx.annotation.OptIn
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastDistinctBy
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.LocalBottomMenu
import app.kreate.android.Preferences
import app.kreate.android.R
import app.kreate.android.constant.MenuPage
import app.kreate.android.service.player.StatefulPlayer
import app.kreate.android.themed.common.component.BottomMenu
import app.kreate.android.themed.rimusic.component.song.SongItem
import app.kreate.android.utils.shallowCompare
import app.kreate.database.models.Event
import it.fast4x.compose.persist.persist
import it.fast4x.innertube.YtMusic
import it.fast4x.innertube.requests.HistoryPage
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.LocalPlayerAwareWindowInsets
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.HistoryType
import it.fast4x.rimusic.enums.NavigationBarPosition
import it.fast4x.rimusic.thumbnailShape
import it.fast4x.rimusic.ui.components.ButtonsRow
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.components.themed.HeaderWithIcon
import it.fast4x.rimusic.ui.components.themed.NonQueuedMediaItemMenuLibrary
import it.fast4x.rimusic.ui.components.themed.Title
import it.fast4x.rimusic.ui.screens.settings.isYouTubeLoggedIn
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.ui.styling.LocalAppearance
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.asSong
import it.fast4x.rimusic.utils.forcePlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

@kotlin.OptIn(ExperimentalTextApi::class)
@OptIn(UnstableApi::class)
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun HistoryList(
    navController: NavController,
    player: StatefulPlayer = koinInject(),
    menu: BottomMenu = LocalBottomMenu.current
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val (colorPalette, typography) = LocalAppearance.current
    val menuState = LocalMenuState.current

    val parentalControlEnabled by Preferences.PARENTAL_CONTROL

    /**
     * Topology:
     *
     * Get all events with its song attached to it as.
     * On each event, calculate days since [Event.timestamp] is assigned.
     *
     * There are 5 categories of this:
     * - "Today",
     * - "Yesterday",
     * - "This week",
     * - "Last week",
     * - The rest are represented in "MMM yyyy" format
     *
     * This function will group each event into their correct category
     * and **remember** it.
     */
    val events by remember {
        Database.eventTable
                .allWithSong()
                .distinctUntilChanged()
                .map { list ->
                    // Computed on each update: a page left open past midnight stays right
                    val today = LocalDate.now()
                    // Calendar weeks: "This week" starts on the locale's first day of the week
                    val weekStart = today.with(
                        java.time.temporal.TemporalAdjusters.previousOrSame(
                            java.time.temporal.WeekFields.of( Locale.getDefault() ).firstDayOfWeek
                        )
                    )
                    val monthFormat = java.time.format.DateTimeFormatter.ofPattern( "LLLL yyyy", Locale.getDefault() )
                    // Parental control hides explicit songs (it used to keep only them)
                    list.filter { !parentalControlEnabled || !it.song.isExplicit }
                        // The query has no order: newest first, explicitly
                        .sortedByDescending { it.event.timestamp }
                        .groupBy {
                            // Calendar days, not periods of 24 hours
                            val day = Instant.ofEpochMilli( it.event.timestamp )
                                             .atZone( ZoneId.systemDefault() )
                                             .toLocalDate()
                            val daysAgo = ChronoUnit.DAYS.between( day, today )

                            when {
                                daysAgo == 0L -> context.getString( R.string.today )
                                daysAgo == 1L -> context.getString( R.string.yesterday )
                                !day.isBefore( weekStart ) -> context.getString( R.string.this_week )
                                !day.isBefore( weekStart.minusWeeks( 1 ) ) -> context.getString( R.string.last_week )
                                // "Septembre 2026", not "sept. 2026"
                                else -> monthFormat.format( day ).replaceFirstChar { c -> c.titlecase( Locale.getDefault() ) }
                            }
                        }
                }
    }.collectAsState( emptyMap(), Dispatchers.IO )

    // Short: where the history comes from
    val buttonsList = mutableListOf(HistoryType.History to stringResource(R.string.history_tab_local))
    buttonsList += HistoryType.YTMHistory to stringResource(R.string.history_tab_ytm)

    var historyType by Preferences.HISTORY_PAGE_TYPE

    var historyPage by persist<Result<HistoryPage>>("home/historyPage")
    LaunchedEffect(Unit, historyType) {
        if (isYouTubeLoggedIn())
            historyPage = YtMusic.getHistory()
    }

    Column (
        modifier = Modifier
            .background(colorPalette().background0)
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
            // No top inset: the page's own title bar is already below the status bar
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Bottom + WindowInsetsSides.End).asPaddingValues(),
            modifier = Modifier
                .background(colorPalette().background0)
                .fillMaxSize()
        ) {

            item(
                key = "tabList", contentType = 0,
            ) {
                ButtonsRow(
                    chips = buttonsList,
                    currentValue = historyType,
                    onValueUpdate = { historyType = it },
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp)
                )
            }

            if( historyType == HistoryType.History )
                events.forEach { (headerStr, details) ->
                    stickyHeader { SectionHeader( headerStr ) }

                    items(
                        items = details.fastDistinctBy { it.song.id },
                        key = { it.event.id }
                    ) { event ->

                        SongItem.Render(
                            song = event.song,
                            hapticFeedback = hapticFeedback,
                            isPlaying = event.song.shallowCompare( currentMediaItem ),
                            values = songItemValues,
                            onClick = {
                                player.forcePlay( event.song.asMediaItem )
                            },
                            onLongClick = {
                                val page = MenuPage.Song(event.song.asMediaItem)
                                menu.show( page, true )
                            }
                        )
                    }
                }

            if ( historyType == HistoryType.YTMHistory )
                historyPage?.getOrNull()?.sections?.forEach { section ->
                    stickyHeader { SectionHeader( section.title ) }

                    items(
                        items = section.songs
                                       .map { it.asMediaItem }
                                       .filter { it.mediaId.isNotEmpty() },
                        key = { it.mediaId }
                    ) { mediaItem ->
                        SongItem.Render(
                            song = mediaItem.asSong,
                            hapticFeedback = hapticFeedback,
                            isPlaying = mediaItem.shallowCompare( currentMediaItem ),
                            values = songItemValues,
                            onClick = {
                                player.forcePlay( mediaItem )
                            },
                            onLongClick = {
                                menuState.display {
                                    NonQueuedMediaItemMenuLibrary(
                                        navController = navController,
                                        mediaItem = mediaItem,
                                        onDismiss = menuState::hide
                                    )
                                }
                            }
                        )
                    }
                }
        }
    }
}

/** Small dated heading ("AUJOURD'HUI"), instead of a big grey band */
@Composable
private fun SectionHeader( text: String ) =
    androidx.compose.foundation.text.BasicText(
        text = text.uppercase(),
        style = typography().xs.semiBold.color( colorPalette().textSecondary )
                            .copy( letterSpacing = androidx.compose.ui.unit.TextUnit( .08f, androidx.compose.ui.unit.TextUnitType.Em ) ),
        modifier = Modifier.fillMaxWidth()
                           .background( colorPalette().background0 )
                           .padding( start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp )
    )
