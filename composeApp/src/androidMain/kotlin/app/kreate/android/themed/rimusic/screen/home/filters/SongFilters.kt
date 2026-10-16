package app.kreate.android.themed.rimusic.screen.home.filters

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
    val firstPlayed: Span = Span( 0, Scales.AGO_DAYS.lastIndex )
) {
    val playsActive get() = !plays.isAll( Scales.PLAYS.lastIndex )
    /** "Never listened": the dates of listening mean nothing then */
    val neverPlayed get() = plays == Span( 0, 0 )
    val lastPlayedActive get() = !neverPlayed && !lastPlayed.isAll( Scales.AGO_DAYS.lastIndex )
    val firstPlayedActive get() = !neverPlayed && !firstPlayed.isAll( Scales.AGO_DAYS.lastIndex )

    val activeCount: Int get() =
        listOf( downloaded, cached, liked, explicit != 0, playsActive, lastPlayedActive, firstPlayedActive ).count { it }
    val isActive get() = activeCount > 0
    /** Downloaded or cached ones are looked up only when asked for */
    val needsSources get() = downloaded || cached
    /** These pick songs never played in Kreate too, like the lists they replace */
    val widensLibrary get() = downloaded || cached || liked

    fun matches(
        song: Song,
        stats: SongPlayStats?,
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

        if( playsActive && !Scales.inSpan( Scales.PLAYS, plays, stats?.plays ?: 0 ) ) return false
        if( lastPlayedActive ) {
            val at = stats?.lastPlayed ?: return false
            if( !Scales.inSpan( Scales.AGO_DAYS, lastPlayed, Scales.daysAgo( at, now ) ) ) return false
        }
        if( firstPlayedActive ) {
            val at = stats?.firstPlayed ?: return false
            if( !Scales.inSpan( Scales.AGO_DAYS, firstPlayed, Scales.daysAgo( at, now ) ) ) return false
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

    /** The last step is open ("100 and more", "3 years and more") */
    fun inSpan( scale: IntArray, span: Span, value: Int ): Boolean =
        value >= scale[span.from] && (span.to == scale.lastIndex || value <= scale[span.to])
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
        else runCatching { json.decodeFromString<SongFilters>( text ) }
                .onFailure { Logger.w( it ) { "unreadable library filters, reset" } }
                .getOrNull()

    /** The user's shortcuts; the first time, a few useful ones */
    fun shortcuts( context: Context ): List<FilterShortcut> {
        val text = Preferences.LIBRARY_FILTER_SHORTCUTS.value
        if( text.isNotBlank() )
            runCatching { return json.decodeFromString<List<FilterShortcut>>( text ) }
                .onFailure { Logger.w( it ) { "unreadable filter shortcuts, back to the defaults" } }
        return defaults( context ).also( ::saveShortcuts )
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
