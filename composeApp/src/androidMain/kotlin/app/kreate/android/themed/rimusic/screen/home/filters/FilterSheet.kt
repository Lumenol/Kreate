package app.kreate.android.themed.rimusic.screen.home.filters

import kotlinx.coroutines.Dispatchers
import it.fast4x.rimusic.Database
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import app.kreate.android.service.YoutubeInfo
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.kreate.android.R
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.themed.InputTextDialog
import it.fast4x.rimusic.utils.color
import it.fast4x.rimusic.utils.semiBold
import kotlin.math.roundToInt

/** Texts of the scales and of the chosen ranges ("5 to 20 plays", "3 to 5 months ago") */
object FilterTexts {

    fun ago( context: Context, index: Int ): String {
        val days = Scales.AGO_DAYS[index]
        return when {
            days == 0   -> context.getString( R.string.library_filters_today )
            days < 30   -> (days / 7).let { if( it == 1 ) context.getString( R.string.library_filters_one_week ) else context.getString( R.string.library_filters_n_weeks, it ) }
            days < 365  -> (days / 30).let { if( it == 1 ) context.getString( R.string.library_filters_one_month ) else context.getString( R.string.library_filters_n_months, it ) }
            else        -> (days / 365).let { if( it == 1 ) context.getString( R.string.library_filters_one_year ) else context.getString( R.string.library_filters_n_years, it ) }
        }
    }

    fun plays( index: Int ): String = Scales.PLAYS[index].toString()

    fun agoRange( context: Context, span: Span ): String {
        val last = Scales.AGO_DAYS.lastIndex
        return when {
            span.from == 0 && span.to == 0 -> context.getString( R.string.library_filters_today )
            span.to == last                -> context.getString( R.string.library_filters_ago_more, ago( context, span.from ) )
            span.from == 0                 -> context.getString( R.string.library_filters_ago_less, ago( context, span.to ) )
            span.from == span.to           -> context.getString( R.string.library_filters_ago_exact, ago( context, span.from ) )
            else -> {
                // "3 months to 5 months" reads "3 to 5 months"
                var low = ago( context, span.from )
                val high = ago( context, span.to )
                if( low.contains( ' ' ) && low.substringAfterLast( ' ' ) == high.substringAfterLast( ' ' ) )
                    low = low.substringBeforeLast( ' ' )
                context.getString( R.string.library_filters_ago_between, low, high )
            }
        }
    }

    fun playsRange( context: Context, span: Span ): String {
        val last = Scales.PLAYS.lastIndex
        return when {
            span.from == 0 && span.to == 0 -> context.getString( R.string.library_filters_plays_never )
            // The last step is open: "100 plays or more", even with both handles on it
            span.to == last                -> context.getString( R.string.library_filters_plays_at_least, plays( span.from ) )
            span.from == span.to           -> if( Scales.PLAYS[span.from] == 1 ) context.getString( R.string.library_filters_plays_one )
                                              else context.getString( R.string.library_filters_plays_exact, plays( span.from ) )
            span.from == 0                 -> context.getString( R.string.library_filters_plays_at_most, plays( span.to ) )
            else                           -> context.getString( R.string.library_filters_plays_between, plays( span.from ), plays( span.to ) )
        }
    }

    fun viewsLabel( context: Context, index: Int ): String = when( val v = Scales.VIEWS[index] ) {
        0L                  -> "0"
        in 1 until 1_000_000            -> "${v / 1_000} k"
        in 1_000_000 until 1_000_000_000 -> "${v / 1_000_000} M"
        else                -> context.getString( R.string.library_filters_views_billion )
    }

    fun releasedRange( context: Context, span: Span ): String {
        val last = Scales.yearsLast
        val to = span.to.coerceAtMost( last )
        return when {
            // The first step is open at the bottom: "1960 and before"
            span.from == 0 && to == 0  -> context.getString( R.string.library_filters_released_first, Scales.year( 0 ).toString() )
            span.from == 0             -> context.getString( R.string.library_filters_released_until, Scales.year( to ).toString() )
            span.from == to            -> context.getString( R.string.library_filters_released_in, Scales.year( to ).toString() )
            to >= last                 -> context.getString( R.string.library_filters_released_since, Scales.year( span.from ).toString() )
            else                       -> context.getString( R.string.library_filters_released_between, Scales.year( span.from ).toString(), Scales.year( to ).toString() )
        }
    }

    fun viewsRange( context: Context, span: Span ): String {
        val last = Scales.VIEWS.lastIndex
        return when {
            span.to == last && span.from > 0 -> context.getString( R.string.library_filters_views_more, viewsLabel( context, span.from ) )
            span.from == span.to             -> context.getString( R.string.library_filters_views_between, viewsLabel( context, span.from ), viewsLabel( context, span.from + 1 ) )
            span.from == 0                   -> context.getString( R.string.library_filters_views_less, viewsLabel( context, span.to ) )
            else                             -> context.getString( R.string.library_filters_views_between, viewsLabel( context, span.from ), viewsLabel( context, span.to ) )
        }
    }

    /** One pill per active filter, under the toolbar: its text and how to remove it */
    fun activePills( context: Context, f: SongFilters ): List<Pair<String, SongFilters>> = buildList {
        if( f.downloaded ) add( context.getString( R.string.downloaded ) to f.copy( downloaded = false ) )
        if( f.cached ) add( context.getString( R.string.cached ) to f.copy( cached = false ) )
        if( f.liked ) add( context.getString( R.string.library_filters_liked_only ) to f.copy( liked = false ) )
        if( f.explicit == 1 ) add( context.getString( R.string.library_filters_explicit_hidden ) to f.copy( explicit = 0 ) )
        if( f.explicit == 2 ) add( context.getString( R.string.library_filters_explicit_only_pill ) to f.copy( explicit = 0 ) )
        if( f.playsActive )
            add( playsRange( context, f.plays ) to f.copy( plays = SongFilters().plays ) )
        if( f.lastPlayedActive )
            add( context.getString( R.string.library_filters_pill, context.getString( R.string.library_filters_last_played ), agoRange( context, f.lastPlayed ) )
                         to f.copy( lastPlayed = SongFilters().lastPlayed ) )
        if( f.firstPlayedActive )
            add( context.getString( R.string.library_filters_pill, context.getString( R.string.library_filters_first_played ), agoRange( context, f.firstPlayed ) )
                         to f.copy( firstPlayed = SongFilters().firstPlayed ) )
        if( f.releasedActive )
            add( context.getString( R.string.library_filters_pill, context.getString( R.string.library_filters_released ), releasedRange( context, f.released ) )
                         to f.copy( released = SongFilters().released ) )
        if( f.viewsActive )
            add( viewsRange( context, f.views ) to f.copy( views = SongFilters().views ) )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Chip(
    text: String,
    selected: Boolean,
    enabled: Boolean = true,
    trailingIconId: Int? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape( 50 )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.alpha( if( enabled ) 1f else 0.4f )
                           .clip( shape )
                           .background( if( selected ) colorPalette().accent else colorPalette().background2, shape )
                           .border( 1.dp, if( selected ) colorPalette().accent else colorPalette().background3, shape )
                           .combinedClickable( enabled = enabled, onClick = onClick, onLongClick = onLongClick )
                           .padding( horizontal = 14.dp, vertical = 8.dp )
    ) {
        if( selected ) {
            Icon( painterResource( R.drawable.checkmark ), null, tint = colorPalette().background0, modifier = Modifier.size( 14.dp ) )
            Spacer( Modifier.width( 6.dp ) )
        }
        BasicText( text, style = typography().xs.semiBold.color( if( selected ) colorPalette().background0 else colorPalette().text ) )
        if( trailingIconId != null ) {
            Spacer( Modifier.width( 6.dp ) )
            Icon(
                painterResource( trailingIconId ), null,
                tint = if( selected ) colorPalette().background0 else colorPalette().textSecondary,
                modifier = Modifier.size( 14.dp )
            )
        }
    }
}

@Composable
private fun SectionTitle( title: String, value: String?, active: Boolean, modifier: Modifier = Modifier, expanded: Boolean? = null ) =
    Row( verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth() ) {
        BasicText( title.uppercase(), style = typography().xxs.semiBold.color( colorPalette().textSecondary ), modifier = Modifier.weight( 1f ) )
        if( value != null )
            BasicText( value, style = typography().xs.semiBold.color( if( active ) colorPalette().accent else colorPalette().textSecondary ) )
        if( expanded != null ) {
            Spacer( Modifier.width( 6.dp ) )
            Icon(
                painterResource( R.drawable.chevron_forward ), null,
                tint = colorPalette().textSecondary,
                modifier = Modifier.size( 14.dp ).rotate( if( expanded ) 90f else 0f )
            )
        }
    }

/** A section over a scale: quick values, and a two-handle slider for any range */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RangeSection(
    title: String,
    span: Span,
    last: Int,
    presets: List<Pair<String, Span>>,
    summary: String,
    lowLabel: String,
    highLabel: String,
    enabled: Boolean,
    /** null: always open, without the fold arrow */
    expanded: Boolean?,
    onExpand: () -> Unit,
    onChange: (Span) -> Unit
) {
    val active = enabled && !span.isAll( last )
    Column( verticalArrangement = Arrangement.spacedBy( 6.dp ), modifier = Modifier.alpha( if( enabled ) 1f else 0.4f ) ) {
        SectionTitle(
            title,
            if( !enabled ) stringResource( R.string.library_filters_not_applicable ) else if( active ) summary else stringResource( R.string.library_filters_all ),
            active,
            if( expanded == null ) Modifier.padding( vertical = 6.dp ) else Modifier.clickable( onClick = onExpand ).padding( vertical = 6.dp ),
            expanded = expanded
        )
        if( expanded == false ) return@Column

        FlowRow( horizontalArrangement = Arrangement.spacedBy( 8.dp ), verticalArrangement = Arrangement.spacedBy( 8.dp ) ) {
            presets.forEach { (label, preset) ->
                Chip( label, span == preset, enabled ) {
                    onChange( if( span == preset ) Span( 0, last ) else preset )
                }
            }
        }
        RangeSlider(
            value = span.from.toFloat()..span.to.toFloat(),
            onValueChange = { onChange( Span( it.start.roundToInt(), it.endInclusive.roundToInt() ) ) },
            valueRange = 0f..last.toFloat(),
            steps = (last - 1).coerceAtLeast( 0 ),
            enabled = enabled,
            // Grey while it lets everything through, green once it filters; no dots
            colors = SliderDefaults.colors(
                thumbColor = if( active ) colorPalette().accent else colorPalette().textSecondary,
                activeTrackColor = if( active ) colorPalette().accent else colorPalette().textSecondary.copy( alpha = 0.45f ),
                inactiveTrackColor = colorPalette().background3,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
                disabledThumbColor = colorPalette().textSecondary,
                disabledActiveTrackColor = colorPalette().textSecondary.copy( alpha = 0.45f ),
                disabledInactiveTrackColor = colorPalette().background3,
                disabledActiveTickColor = Color.Transparent,
                disabledInactiveTickColor = Color.Transparent
            )
        )
        Row( Modifier.fillMaxWidth() ) {
            BasicText( lowLabel, style = typography().xxs.color( colorPalette().textSecondary ), modifier = Modifier.weight( 1f ) )
            BasicText( highLabel, style = typography().xxs.color( colorPalette().textSecondary ) )
        }
    }
}

@Composable
private fun Segmented( options: List<String>, selected: Int, onSelect: (Int) -> Unit ) {
    val shape = RoundedCornerShape( 50 )
    Row( Modifier.clip( shape ).background( colorPalette().background2 ).border( 1.dp, colorPalette().background3, shape ).padding( 3.dp ) ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            BasicText(
                label,
                style = typography().xs.semiBold.color( if( on ) colorPalette().background0 else colorPalette().text ),
                modifier = Modifier.clip( shape )
                                   // "All" is the neutral position: grey, not the accent
                                   .background( if( !on ) Color.Transparent else if( i == 0 ) colorPalette().textSecondary else colorPalette().accent )
                                   .clickable { onSelect( i ) }
                                   .padding( horizontal = 14.dp, vertical = 7.dp )
            )
        }
    }
}

/**
 * The filter sheet: shortcuts on one line (the user's own, saved from the current
 * filters), where the song is, liked, explicit, then plays and dates of listening.
 * Everything applies at once; the button tells how many songs are left.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSheet( onDone: () -> Unit ) {
    val context = LocalContext.current
    val f = SongFiltersStore.filters
    fun set( value: SongFilters ) { SongFiltersStore.filters = value }

    val shortcuts = remember { mutableStateListOf<FilterShortcut>().apply { addAll( SongFiltersStore.shortcuts( context ) ) } }
    fun saveShortcuts() = SongFiltersStore.saveShortcuts( shortcuts.toList() )
    var naming by remember { mutableStateOf<Int?>( null ) }        // -1: a new one, else the one renamed
    var editing by remember { mutableStateOf<Int?>( null ) }       // long-pressed shortcut

    // Rarely used sections start folded, unless they filter
    var lastOpen by rememberSaveable { mutableStateOf( true ) }
    var firstOpen by rememberSaveable { mutableStateOf( f.firstPlayedActive ) }
    var releasedOpen by rememberSaveable { mutableStateOf( f.releasedActive ) }
    var viewsOpen by rememberSaveable { mutableStateOf( f.viewsActive ) }
    // What YouTube said of the songs is filled in the background, once these are looked at
    if( releasedOpen || viewsOpen || f.needsYoutubeInfo )
        LaunchedEffect( Unit ) { YoutubeInfo.fillLibrary( context ) }

    Column(
        Modifier.padding( top = 48.dp )
                .fillMaxWidth()
                .background( colorPalette().background1, RoundedCornerShape( topStart = 20.dp, topEnd = 20.dp ) )
                .navigationBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding( start = 20.dp, end = 12.dp, top = 18.dp, bottom = 6.dp )
        ) {
            BasicText( stringResource( R.string.library_filters ), style = typography().l.semiBold.color( colorPalette().text ), modifier = Modifier.weight( 1f ) )
            BasicText(
                stringResource( R.string.library_filters_clear ),
                style = typography().xs.semiBold.color( if( f.isActive ) colorPalette().accent else colorPalette().textSecondary ),
                modifier = Modifier.alpha( if( f.isActive ) 1f else 0.5f )
                                   .clip( RoundedCornerShape( 50 ) )
                                   .clickable( enabled = f.isActive ) { set( SongFilters() ) }
                                   .padding( 8.dp )
            )
        }

        // Shortcuts: lit while the filters are exactly theirs. "Edit" shows a pencil on
        // each, a touch then opens update / rename / delete (a long press does too)
        var editMode by remember { mutableStateOf( false ) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding( start = 20.dp, end = 12.dp, top = 6.dp )
        ) {
            BasicText(
                stringResource( R.string.library_filters_shortcuts ).uppercase(),
                style = typography().xxs.semiBold.color( colorPalette().textSecondary ),
                modifier = Modifier.weight( 1f )
            )
            BasicText(
                stringResource( if( editMode ) R.string.library_filters_shortcuts_done else R.string.library_filters_shortcuts_edit ),
                style = typography().xs.semiBold.color( colorPalette().accent ),
                modifier = Modifier.clip( RoundedCornerShape( 50 ) )
                                   .clickable { editMode = !editMode }
                                   .padding( 8.dp )
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy( 8.dp ),
            modifier = Modifier.horizontalScroll( rememberScrollState() ).padding( horizontal = 20.dp, vertical = 4.dp )
        ) {
            // Always first: the current filters become a shortcut (greyed with none set,
            // or when a shortcut already holds them)
            Chip(
                "+ " + stringResource( R.string.library_filters_save ),
                false,
                enabled = f.isActive && shortcuts.none { it.filters == f }
            ) { naming = -1 }
            shortcuts.forEachIndexed { i, shortcut ->
                Chip(
                    shortcut.name,
                    !editMode && shortcut.filters == f,
                    trailingIconId = if( editMode ) R.drawable.pencil else null,
                    onLongClick = { editing = i }
                ) {
                    if( editMode ) editing = i
                    else set( if( shortcut.filters == f ) SongFilters() else shortcut.filters )
                }
            }
        }

        Box( Modifier.weight( 1f, false ) ) {
            Column(
                verticalArrangement = Arrangement.spacedBy( 18.dp ),
                modifier = Modifier.verticalScroll( rememberScrollState() )
                                   .padding( start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp )
            ) {
                // Where the song is: either ticked one
                Column( verticalArrangement = Arrangement.spacedBy( 8.dp ) ) {
                    val sources = listOfNotNull(
                        stringResource( R.string.downloaded ).takeIf { f.downloaded },
                        stringResource( R.string.cached ).takeIf { f.cached }
                    )
                    SectionTitle(
                        stringResource( R.string.library_filters_source ),
                        if( sources.isEmpty() ) stringResource( R.string.library_filters_all ) else sources.joinToString( " · " ),
                        sources.isNotEmpty()
                    )
                    FlowRow( horizontalArrangement = Arrangement.spacedBy( 8.dp ), verticalArrangement = Arrangement.spacedBy( 8.dp ) ) {
                        Chip( stringResource( R.string.downloaded ), f.downloaded ) { set( f.copy( downloaded = !f.downloaded ) ) }
                        Chip( stringResource( R.string.cached ), f.cached ) { set( f.copy( cached = !f.cached ) ) }
                        Chip( stringResource( R.string.library_filters_liked_only ), f.liked ) { set( f.copy( liked = !f.liked ) ) }
                    }
                }

                Row( verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth() ) {
                    BasicText(
                        stringResource( R.string.library_filters_explicit ).uppercase(),
                        style = typography().xxs.semiBold.color( colorPalette().textSecondary ),
                        modifier = Modifier.weight( 1f )
                    )
                    Segmented(
                        listOf(
                            stringResource( R.string.library_filters_explicit_all ),
                            stringResource( R.string.library_filters_explicit_without ),
                            stringResource( R.string.library_filters_explicit_only )
                        ),
                        f.explicit
                    ) { set( f.copy( explicit = it ) ) }
                }

                val playsLast = Scales.PLAYS.lastIndex
                RangeSection(
                    title = stringResource( R.string.library_filters_plays ),
                    span = f.plays,
                    last = playsLast,
                    presets = listOf(
                        stringResource( R.string.library_filters_preset_never ) to Span( 0, 0 ),
                        stringResource( R.string.library_filters_preset_once ) to Span( 1, 1 ),
                        stringResource( R.string.library_filters_preset_at_least, "5" ) to Span( 4, playsLast ),
                        stringResource( R.string.library_filters_preset_at_least, "20" ) to Span( 6, playsLast )
                    ),
                    summary = FilterTexts.playsRange( context, f.plays ),
                    lowLabel = "0",
                    highLabel = "${Scales.PLAYS.last()} +",
                    enabled = true,
                    expanded = null,
                    onExpand = {},
                    onChange = { set( f.copy( plays = it ) ) }
                )

                val agoLast = Scales.AGO_DAYS.lastIndex
                fun agoPresets( recent: List<Int>, old: List<Int> ) =
                    recent.map { context.getString( R.string.library_filters_preset_less, FilterTexts.ago( context, it ) ) to Span( 0, it ) } +
                    old.map { context.getString( R.string.library_filters_preset_more, FilterTexts.ago( context, it ) ) to Span( it, agoLast ) }
                // Index 1 = 1 week, 3 = 1 month, 8 = 6 months, 10 = 1 year
                RangeSection(
                    title = stringResource( R.string.library_filters_last_played ),
                    span = f.lastPlayed,
                    last = agoLast,
                    presets = agoPresets( listOf( 1, 3 ), listOf( 8, 10 ) ),
                    summary = FilterTexts.agoRange( context, f.lastPlayed ),
                    lowLabel = FilterTexts.ago( context, 0 ),
                    highLabel = FilterTexts.ago( context, agoLast ) + " +",
                    enabled = !f.neverPlayed,
                    expanded = lastOpen,
                    onExpand = { lastOpen = !lastOpen },
                    onChange = { set( f.copy( lastPlayed = it ) ) }
                )
                RangeSection(
                    title = stringResource( R.string.library_filters_first_played ),
                    span = f.firstPlayed,
                    last = agoLast,
                    presets = agoPresets( listOf( 1, 3, 10 ), emptyList() ),
                    summary = FilterTexts.agoRange( context, f.firstPlayed ),
                    lowLabel = FilterTexts.ago( context, 0 ),
                    highLabel = FilterTexts.ago( context, agoLast ) + " +",
                    enabled = !f.neverPlayed,
                    expanded = firstOpen,
                    onExpand = { firstOpen = !firstOpen },
                    onChange = { set( f.copy( firstPlayed = it ) ) }
                )

                val yearsLast = Scales.yearsLast
                val now = Scales.year( yearsLast )
                fun years( from: Int, to: Int ) = Span( from - Scales.FIRST_YEAR, to - Scales.FIRST_YEAR )
                RangeSection(
                    title = stringResource( R.string.library_filters_released ),
                    span = Span( f.released.from, f.released.to.coerceAtMost( yearsLast ) ),
                    last = yearsLast,
                    // This year, the last three decades (from the current one), and before them
                    presets = buildList {
                        val decade = now / 10 * 10
                        add( now.toString() to years( now, now ) )
                        for( d in decade downTo decade - 20 step 10 )
                            add( context.getString( R.string.library_filters_preset_decade, d.toString() ) to years( d, minOf( d + 9, now ) ) )
                        add( context.getString( R.string.library_filters_preset_before, (decade - 20).toString() ) to years( Scales.FIRST_YEAR, decade - 21 ) )
                    },
                    summary = FilterTexts.releasedRange( context, f.released ),
                    lowLabel = stringResource( R.string.library_filters_released_first, Scales.FIRST_YEAR.toString() ),
                    highLabel = now.toString(),
                    enabled = true,
                    expanded = releasedOpen,
                    onExpand = { releasedOpen = !releasedOpen },
                    // Years as picked ("in 2026" stays so next year); only the whole range
                    // goes back to "all", open at the top for the years to come
                    onChange = { set( f.copy( released = if( it == Span( 0, yearsLast ) ) SongFilters().released else it ) ) }
                )
                val viewsLast = Scales.VIEWS.lastIndex
                RangeSection(
                    title = stringResource( R.string.library_filters_views ),
                    span = f.views,
                    last = viewsLast,
                    presets = listOf(
                        context.getString( R.string.library_filters_views_less, FilterTexts.viewsLabel( context, 3 ) ) to Span( 0, 3 ),
                        context.getString( R.string.library_filters_views_more, FilterTexts.viewsLabel( context, 4 ) ) to Span( 4, viewsLast ),
                        context.getString( R.string.library_filters_views_more, FilterTexts.viewsLabel( context, 5 ) ) to Span( 5, viewsLast )
                    ),
                    summary = FilterTexts.viewsRange( context, f.views ),
                    lowLabel = "0",
                    highLabel = FilterTexts.viewsLabel( context, viewsLast ) + " +",
                    enabled = true,
                    expanded = viewsOpen,
                    onExpand = { viewsOpen = !viewsOpen },
                    onChange = { set( f.copy( views = it ) ) }
                )
                if( releasedOpen || viewsOpen || f.needsYoutubeInfo ) {
                    val known by remember { Database.songYoutubeInfoTable.knownCount() }.collectAsState( 0, Dispatchers.IO )
                    val total by remember { Database.songYoutubeInfoTable.libraryCount() }.collectAsState( 0, Dispatchers.IO )
                    BasicText(
                        text = buildString {
                            if( known < total )
                                append( stringResource(
                                    if( YoutubeInfo.isFilling ) R.string.library_filters_youtube_progress else R.string.library_filters_youtube_paused,
                                    known.coerceAtMost( total ), total
                                ) ).append( "\n" )
                            append( stringResource( R.string.library_filters_youtube_note ) )
                        },
                        style = typography().xxs.color( colorPalette().textSecondary )
                    )
                    // Views change: ask again now rather than wait (Settings → Data has the delay)
                    if( !YoutubeInfo.isFilling )
                        BasicText(
                            stringResource( R.string.library_filters_refresh_now ),
                            style = typography().xs.semiBold.color( colorPalette().accent ),
                            modifier = Modifier.clip( RoundedCornerShape( 50 ) )
                                               .clickable { YoutubeInfo.fillLibrary( context, everything = true ) }
                                               .padding( vertical = 6.dp )
                        )
                }
            }
            // The list fades out above the button
            Box(
                Modifier.align( Alignment.BottomCenter ).fillMaxWidth().height( 28.dp )
                        .background( Brush.verticalGradient( listOf( Color.Transparent, colorPalette().background1 ) ) )
            )
        }

        BasicText(
            text = stringResource( R.string.library_filters_show, SongFiltersStore.resultCount ),
            style = typography().s.semiBold.color( colorPalette().background0 ).copy( textAlign = TextAlign.Center ),
            modifier = Modifier.padding( start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp )
                               .fillMaxWidth()
                               .clip( RoundedCornerShape( 50 ) )
                               .background( colorPalette().text )
                               .clickable( onClick = onDone )
                               .padding( vertical = 14.dp )
        )
    }

    // Name a new shortcut, or rename one
    naming?.let { index ->
        InputTextDialog(
            onDismiss = { naming = null },
            title = stringResource( R.string.library_filters_shortcut_name ),
            value = if( index >= 0 ) shortcuts[index].name else "",
            placeholder = stringResource( R.string.library_filters_shortcut_name ),
            setValue = { name ->
                val clean = name.trim()
                if( clean.isNotEmpty() ) {
                    if( index >= 0 ) shortcuts[index] = shortcuts[index].copy( name = clean )
                    // First: it's the one just made, it shows lit where "+ Save" was
                    else shortcuts.add( 0, FilterShortcut( clean, f ) )
                    saveShortcuts()
                }
                naming = null
            }
        )
    }

    // Long press on a shortcut: update it with the current filters, rename or delete it
    editing?.let { index ->
        Dialog( onDismissRequest = { editing = null } ) {
            Column(
                Modifier.clip( RoundedCornerShape( 16.dp ) )
                        .background( colorPalette().background1 )
                        .padding( vertical = 8.dp )
            ) {
                BasicText(
                    shortcuts[index].name,
                    style = typography().s.semiBold.color( colorPalette().text ),
                    modifier = Modifier.padding( horizontal = 20.dp, vertical = 12.dp )
                )
                @Composable
                fun Entry( text: String, enabled: Boolean = true, onClick: () -> Unit ) =
                    BasicText(
                        text,
                        style = typography().xs.semiBold.color( colorPalette().text ),
                        modifier = Modifier.fillMaxWidth()
                                           .alpha( if( enabled ) 1f else 0.4f )
                                           .clickable( enabled = enabled, onClick = onClick )
                                           .padding( horizontal = 20.dp, vertical = 14.dp )
                    )
                Entry( stringResource( R.string.library_filters_shortcut_update ), enabled = f.isActive ) {
                    shortcuts[index] = shortcuts[index].copy( filters = f )
                    saveShortcuts()
                    editing = null
                }
                Entry( stringResource( R.string.rename ) ) {
                    editing = null
                    naming = index
                }
                Entry( stringResource( R.string.delete ) ) {
                    shortcuts.removeAt( index )
                    saveShortcuts()
                    editing = null
                }
            }
        }
    }
}

/** The toolbar's filter button: green with the number of filters on */
@Composable
fun FilterButton( onClick: () -> Unit ) {
    val count = SongFiltersStore.filters.activeCount
    val description = if( count > 0 ) stringResource( R.string.library_filters_on, count ) else stringResource( R.string.library_filters )
    Box {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size( 36.dp )
                               .clip( CircleShape )
                               .background( if( count > 0 ) colorPalette().accent else colorPalette().background2 )
                               .clickable( onClick = onClick )
        ) {
            Icon(
                painterResource( R.drawable.filter ),
                description,
                tint = if( count > 0 ) colorPalette().background0 else colorPalette().text,
                modifier = Modifier.size( 18.dp )
            )
        }
        if( count > 0 )
            BasicText(
                count.toString(),
                style = typography().xxs.semiBold.color( colorPalette().background0 ).copy( textAlign = TextAlign.Center ),
                modifier = Modifier.align( Alignment.TopEnd )
                                   .offset( x = 5.dp, y = (-5).dp )
                                   .size( 18.dp )
                                   .clip( CircleShape )
                                   .background( colorPalette().text )
                                   .padding( top = 1.dp )
            )
    }
}

/** The filters on, under the toolbar: one scrolling line, a touch removes one */
@Composable
fun ActiveFilters( modifier: Modifier = Modifier ) {
    val context = LocalContext.current
    val pills = FilterTexts.activePills( context, SongFiltersStore.filters )
    if( pills.isEmpty() ) return
    Row(
        horizontalArrangement = Arrangement.spacedBy( 8.dp ),
        modifier = modifier.horizontalScroll( rememberScrollState() ).padding( horizontal = 12.dp ).padding( bottom = 8.dp )
    ) {
        pills.forEach { (label, without) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clip( CircleShape )
                                   .background( colorPalette().accent.copy( alpha = 0.18f ) )
                                   .clickable { SongFiltersStore.filters = without }
                                   .padding( start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp )
            ) {
                BasicText( label, style = typography().xs.semiBold.color( colorPalette().accent ) )
                Spacer( Modifier.width( 6.dp ) )
                Icon( painterResource( R.drawable.close ), null, tint = colorPalette().accent, modifier = Modifier.size( 12.dp ) )
            }
        }
    }
}
