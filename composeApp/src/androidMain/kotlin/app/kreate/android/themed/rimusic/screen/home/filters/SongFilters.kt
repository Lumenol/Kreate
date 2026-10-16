package app.kreate.android.themed.rimusic.screen.home.filters

import app.kreate.database.models.SongYoutubeInfo
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.kreate.android.Preferences
import app.kreate.android.R
import app.kreate.database.SongPlayStats
import app.kreate.database.models.Song
import co.touchlab.kermit.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray

/** A range over one of the [Scales]: indexes, both ends included */
@Serializable
data class Span( val from: Int, val to: Int ) {
    fun isAll( last: Int ) = from == 0 && to == last
}

/**
 * The library's song filters. Kept in the preferences: they stay on when the user
 * leaves the library, the green filter button tells they're on.
 */
@Serializable
data class SongFilters(
    val downloaded: Boolean = false,
    val cached: Boolean = false,
    val liked: Boolean = false,
    /** 0 = all, 1 = without explicit songs, 2 = only them */
    val explicit: Int = 0,
    val plays: Span = Span( 0, Scales.PLAYS.lastIndex ),
    val lastPlayed: Span = Span( 0, Scales.AGO_DAYS.lastIndex ),
    val firstPlayed: Span = Span( 0, Scales.AGO_DAYS.lastIndex ),
    /** Year put online on YouTube; the default spans every year, whatever the current one */
    val released: Span = Span( 0, Int.MAX_VALUE ),
    val views: Span = Span( 0, Scales.VIEWS.lastIndex )
) {
    val playsActive get() = !plays.isAll( Scales.PLAYS.lastIndex )
    /** "Never listened": the dates of listening mean nothing then */
    val neverPlayed get() = plays == Span( 0, 0 )
    val lastPlayedActive get() = !neverPlayed && !lastPlayed.isAll( Scales.AGO_DAYS.lastIndex )
    val firstPlayedActive get() = !neverPlayed && !firstPlayed.isAll( Scales.AGO_DAYS.lastIndex )
    val releasedActive get() = released.from > 0 || released.to < Scales.yearsLast
    val viewsActive get() = !views.isAll( Scales.VIEWS.lastIndex )
    /** These read what YouTube said of the songs */
    val needsYoutubeInfo get() = releasedActive || viewsActive

    val activeCount: Int get() =
        listOf( downloaded, cached, liked, explicit != 0, playsActive, lastPlayedActive, firstPlayedActive, releasedActive, viewsActive ).count { it }
    val isActive get() = activeCount > 0
    /** Downloaded or cached ones are looked up only when asked for */
    val needsSources get() = downloaded || cached
    /**
     * These pick songs never played in Kreate too: downloaded and liked ones (the lists
     * they replace showed them all) and "never played". Cached keeps the usual list and
     * its hidden songs switch, like the cached list did.
     */
    val widensLibrary get() = downloaded || liked || (playsActive && plays.from == 0)

    /** Indexes back inside the scales: a preference from another version can't crash the list */
    fun clamped(): SongFilters {
        fun Span.fit( last: Int ): Span {
            val a = from.coerceIn( 0, last )
            return Span( a, to.coerceIn( a, last ) )
        }
        return copy(
            explicit = explicit.coerceIn( 0, 2 ),
            plays = plays.fit( Scales.PLAYS.lastIndex ),
            lastPlayed = lastPlayed.fit( Scales.AGO_DAYS.lastIndex ),
            firstPlayed = firstPlayed.fit( Scales.AGO_DAYS.lastIndex ),
            // "All" (open at the top) stays all; years picked stay as they were
            released = if( released == SongFilters().released ) released else released.fit( Scales.yearsLast ),
            views = views.fit( Scales.VIEWS.lastIndex )
        )
    }

    fun matches(
        song: Song,
        stats: SongPlayStats?,
        info: SongYoutubeInfo?,
        downloadedIds: Set<String>,
        cachedIds: Set<String>,
        now: Long
    ): Boolean {
        if( needsSources && !( (downloaded && song.id in downloadedIds) || (cached && song.id in cachedIds) ) )
            return false
        // A negative date is a dislike
        if( liked && (song.likedAt ?: 0L) <= 0L ) return false
        if( explicit == 1 && song.isExplicit ) return false
        if( explicit == 2 && !song.isExplicit ) return false

        if( playsActive && !Scales.inPlays( plays, stats?.plays ?: 0 ) ) return false
        if( lastPlayedActive ) {
            val at = stats?.lastPlayed ?: return false
            if( !Scales.inAgo( lastPlayed, Scales.daysAgo( at, now ) ) ) return false
        }
        if( firstPlayedActive ) {
            val at = stats?.firstPlayed ?: return false
            if( !Scales.inAgo( firstPlayed, Scales.daysAgo( at, now ) ) ) return false
        }
        // Songs YouTube said nothing of yet are left out of these two
        if( releasedActive ) {
            val at = info?.publishedAt ?: return false
            if( !Scales.inYears( released, Scales.yearOf( at ) ) ) return false
        }
        if( viewsActive ) {
            val count = info?.viewCount ?: return false
            if( !Scales.inViews( views, count ) ) return false
        }
        return true
    }
}

/** The steps of the sliders: what the user picks are indexes in these */
object Scales {
    val PLAYS = intArrayOf( 0, 1, 2, 3, 5, 10, 20, 50, 100 )
    /** "How long ago", in days: today, 1 and 2 weeks, 1 to 6 months, 9 months, 1, 2 and 3 years */
    val AGO_DAYS = intArrayOf( 0, 7, 14, 30, 60, 90, 120, 150, 180, 270, 365, 730, 1095 )

    fun daysAgo( at: Long, now: Long ): Int = ((now - at) / 86_400_000L).toInt().coerceAtLeast( 0 )

    /** Years from 1960 ("1960 and before") to this one */
    const val FIRST_YEAR = 1960
    val yearsLast: Int get() = java.time.Year.now().value - FIRST_YEAR
    fun year( index: Int ): Int = FIRST_YEAR + index.coerceAtMost( yearsLast )
    fun yearOf( epochMillis: Long ): Int =
        java.time.Instant.ofEpochMilli( epochMillis ).atZone( java.time.ZoneOffset.UTC ).year

    /** Both ends included; the first step takes everything before it */
    fun inYears( span: Span, year: Int ): Boolean =
        (span.from == 0 || year >= year( span.from )) && (span.to >= yearsLast || year <= year( span.to ))

    val VIEWS = longArrayOf( 0, 10_000, 100_000, 1_000_000, 10_000_000, 100_000_000, 1_000_000_000 )

    /** Like durations: end excluded, one step = that whole step, the last one open */
    fun inViews( span: Span, count: Long ): Boolean {
        if( count < VIEWS[span.from] ) return false
        if( span.to == VIEWS.lastIndex ) return true
        val end = if( span.from == span.to ) span.to + 1 else span.to
        return count < VIEWS[end]
    }

    /** Counts, both ends included ("5 to 20 plays"); the last step is open ("100 and more") */
    fun inPlays( span: Span, plays: Int ): Boolean =
        plays >= PLAYS[span.from] && (span.to == PLAYS.lastIndex || plays <= PLAYS[span.to])

    /**
     * Durations, end excluded so that ranges side by side don't overlap ("less than 1
     * week" / "more than 1 week"). Both handles on one step take that whole step
     * ("3 months ago" = from 3 to 4 months); the last step is open ("3 years and more").
     */
    fun inAgo( span: Span, days: Int ): Boolean {
        if( days < AGO_DAYS[span.from] ) return false
        if( span.to == AGO_DAYS.lastIndex ) return true
        val end = if( span.from == span.to ) span.to + 1 else span.to
        return days < AGO_DAYS[end]
    }
}

/** A named set of filters, one touch away */
@Serializable
data class FilterShortcut( val name: String, val filters: SongFilters )

object SongFiltersStore {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    /** How many songs the current filters let through, for the sheet's button */
    var resultCount by mutableIntStateOf( 0 )
    /** How many there are without the filters, for the "42 / 51" count; -1: not shown */
    var totalCount by mutableIntStateOf( 0 )

    // Read once, then kept: the sheet and the list read it on every frame
    private var current by mutableStateOf<SongFilters?>( null )

    var filters: SongFilters
        get() = current ?: (decode( Preferences.LIBRARY_SONG_FILTERS.value ) ?: SongFilters()).also { current = it }
        set( value ) {
            current = value
            Preferences.LIBRARY_SONG_FILTERS.value = json.encodeToString( value )
        }

    private fun decode( text: String ): SongFilters? =
        if( text.isBlank() ) null
        else runCatching { json.decodeFromString<SongFilters>( text ).clamped() }
                .onFailure { Logger.w( it ) { "unreadable library filters, reset" } }
                .getOrNull()

    /** The user's shortcuts; the first time, a few useful ones */
    fun shortcuts( context: Context ): List<FilterShortcut> {
        val text = Preferences.LIBRARY_FILTER_SHORTCUTS.value
        if( text.isBlank() ) return defaults( context ).also( ::saveShortcuts )

        // One by one: an unreadable shortcut is left out, never the others. Nothing is
        // written back until the user changes the list
        val entries = runCatching { json.parseToJsonElement( text ).jsonArray }
            .onFailure { Logger.w( it ) { "unreadable filter shortcuts" } }
            .getOrNull()
            ?: return emptyList()
        return entries.mapNotNull { entry ->
            runCatching { json.decodeFromJsonElement<FilterShortcut>( entry ) }
                .onFailure { Logger.w( it ) { "unreadable filter shortcut left out" } }
                .getOrNull()
                ?.let { it.copy( filters = it.filters.clamped() ) }
        }
    }

    fun saveShortcuts( list: List<FilterShortcut> ) {
        Preferences.LIBRARY_FILTER_SHORTCUTS.value = json.encodeToString( list )
    }

    private fun defaults( context: Context ) = listOf(
        FilterShortcut(
            context.getString( R.string.library_filters_shortcut_discoveries ),
            SongFilters( firstPlayed = Span( 0, 3 ) )                       // first heard less than a month ago
        ),
        FilterShortcut(
            context.getString( R.string.library_filters_shortcut_forgotten_likes ),
            SongFilters( liked = true, lastPlayed = Span( 8, Scales.AGO_DAYS.lastIndex ) )     // not heard for 6 months
        ),
        FilterShortcut(
            context.getString( R.string.library_filters_shortcut_rediscover ),
            SongFilters( plays = Span( 4, Scales.PLAYS.lastIndex ), lastPlayed = Span( 5, Scales.AGO_DAYS.lastIndex ) )  // 5+ plays, not for 3 months
        )
    )
}
