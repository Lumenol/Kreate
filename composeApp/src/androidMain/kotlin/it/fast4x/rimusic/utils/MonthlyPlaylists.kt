package it.fast4x.rimusic.utils

import kotlinx.coroutines.withContext
import app.kreate.android.Preferences
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import app.kreate.android.R
import app.kreate.database.models.Playlist
import it.fast4x.rimusic.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import me.knighthat.utils.TimeDateUtils
import java.time.LocalDate

/** Most played songs kept in a month's playlist, and in a year's */
private const val MONTHLY_SIZE = 50
private const val YEARLY_SIZE = 100

/** Playlist name of a month: year then month without padding, e.g. "20269" for September 2026 */
private fun monthlyName( month: LocalDate ) = "${month.year}${month.monthValue}"

private fun monthOf( name: String ): LocalDate? {
    val digits = name.takeIf { it.all( Char::isDigit ) && it.length in 5..6 } ?: return null
    val month = digits.drop( 4 ).toInt().takeIf { it in 1..12 } ?: return null
    return LocalDate.of( digits.take( 4 ).toInt(), month, 1 )
}

private suspend fun addPlaylistOfMostPlayed( from: LocalDate, to: LocalDate, playlist: Playlist, limit: Int ) {
    val songs = Database.eventTable
                        .findSongsMostPlayedBetween(
                            from = TimeDateUtils.toStartDateMillis( from ),
                            to = TimeDateUtils.toStartDateMillis( to ),
                            limit = limit
                        )
                        .first()
    if( songs.isEmpty() ) return

    Database.asyncTransaction {
        mapIgnore( playlist = playlist, songs = songs.toTypedArray() )
    }
}

/**
 * Monthly playlists used to be named after the month they were made in: on
 * October 1st, September's songs went into "October". Done once: each one is
 * renamed to the month before, oldest first so a name is free when it's taken.
 */
private suspend fun renameOldMonthlyPlaylists() {
    if( Preferences.MONTHLY_PLAYLISTS_RENAMED.value ) return

    Database.playlistTable
            .allMonthly()
            .mapNotNull { playlist -> monthOf( playlist.name )?.let { playlist to it } }
            .sortedBy { it.second }
            .forEach { (playlist, month) ->
                runCatching { Database.playlistTable.rename( playlist.id, monthlyName( month.minusMonths( 1 ) ) ) }
            }
    withContext( Dispatchers.Main ) { Preferences.MONTHLY_PLAYLISTS_RENAMED.value = true }
}

/**
 * Last month's 50 most played songs, named after that month, and last year's
 * 100 most played ("Top 2025"), each made once.
 */
private suspend fun updateRecapPlaylists( context: Context ) {
    renameOldMonthlyPlaylists()

    val firstOfThisMonth = LocalDate.now().withDayOfMonth( 1 )
    val lastMonth = firstOfThisMonth.minusMonths( 1 )
    val monthName = monthlyName( lastMonth )
    if( !Database.playlistTable.exists( monthName ).first() )
        addPlaylistOfMostPlayed( lastMonth, firstOfThisMonth, Playlist( name = monthName, isMonthly = true ), MONTHLY_SIZE )

    val firstOfThisYear = firstOfThisMonth.withMonth( 1 )
    val lastYear = firstOfThisYear.minusYears( 1 )
    val yearName = context.getString( R.string.top_of_year, lastYear.year.toString() )
    if( !Database.playlistTable.exists( yearName ).first() )
        addPlaylistOfMostPlayed( lastYear, firstOfThisYear, Playlist( name = yearName ), YEARLY_SIZE )
}

@Composable
fun CheckMonthlyPlaylist() {
    val context = LocalContext.current
    LaunchedEffect( Unit ) {
        withContext( Dispatchers.IO ) { updateRecapPlaylists( context ) }
    }
}

private val MONTH_TITLES = intArrayOf(
    R.string.month_january_s, R.string.month_february_s, R.string.month_march_s,
    R.string.month_april_s, R.string.month_may_s, R.string.month_june_s,
    R.string.month_july_s, R.string.month_august_s, R.string.month_september_s,
    R.string.month_october_s, R.string.month_november_s, R.string.month_december_s
)

/**
 * Monthly playlists are named year then month without padding
 * ("20268" is August 2026, "202610" October 2026, see [monthlyName]).
 *
 * @return the localized "Month Year" title, `null` if [name] isn't one
 */
@Composable
fun monthlyPlaylistTitle( name: String ): String? {
    val digits = name.filter( Char::isDigit )
    if( digits.length !in 5..6 ) return null

    val month = digits.drop( 4 ).toIntOrNull()?.takeIf { it in 1..12 } ?: return null
    return stringResource( MONTH_TITLES[month - 1] ).format( digits.take( 4 ) )
}

@Composable
fun getTitleMonthlyPlaylist(playlist: String): String {

    val y = playlist.substring(0,4)
    val m = playlist.substring(5,7).toInt()
    return when (m) {
        1 -> stringResource(R.string.month_january_s).format(y)
        2 -> stringResource(R.string.month_february_s).format(y)
        3 -> stringResource(R.string.month_march_s).format(y)
        4 -> stringResource(R.string.month_april_s).format(y)
        5 -> stringResource(R.string.month_may_s).format(y)
        6 -> stringResource(R.string.month_june_s).format(y)
        7 -> stringResource(R.string.month_july_s).format(y)
        8 -> stringResource(R.string.month_august_s).format(y)
        9 -> stringResource(R.string.month_september_s).format(y)
        10 -> stringResource(R.string.month_october_s).format(y)
        11 -> stringResource(R.string.month_november_s).format(y)
        12 -> stringResource(R.string.month_december_s).format(y)
        else -> playlist
    }
}

fun getTitleMonthlyPlaylist( context: Context, playlist: String ): String {
    
    fun getMonthString( @DrawableRes monthId: Int, year: String ) =
        context.resources.getString( monthId ).format( year )

    val y = playlist.substring(0,4)
    val m = playlist.substring(5,7).toInt()
    return when( m ) {
        1 -> getMonthString( R.string.month_january_s, y )
        2 -> getMonthString( R.string.month_february_s, y )
        3 -> getMonthString( R.string.month_march_s, y )
        4 -> getMonthString( R.string.month_april_s, y )
        5 -> getMonthString( R.string.month_may_s, y )
        6 -> getMonthString( R.string.month_june_s, y )
        7 -> getMonthString( R.string.month_july_s, y )
        8 -> getMonthString( R.string.month_august_s, y )
        9 -> getMonthString( R.string.month_september_s, y )
        10 -> getMonthString( R.string.month_october_s, y )
        11 -> getMonthString( R.string.month_november_s, y )
        12 -> getMonthString( R.string.month_december_s, y )
        else -> playlist
    }
}


fun getTitleMonthlyPlaylistFromContext(playlist: String, context: Context): String {

    val y = playlist.substring(0,4)
    val m = playlist.substring(5,7).toInt()
    return when (m) {
        1 -> context.resources.getString(R.string.month_january_s).format(y)
        2 -> context.resources.getString(R.string.month_february_s).format(y)
        3 -> context.resources.getString(R.string.month_march_s).format(y)
        4 -> context.resources.getString(R.string.month_april_s).format(y)
        5 -> context.resources.getString(R.string.month_may_s).format(y)
        6 -> context.resources.getString(R.string.month_june_s).format(y)
        7 -> context.resources.getString(R.string.month_july_s).format(y)
        8 -> context.resources.getString(R.string.month_august_s).format(y)
        9 -> context.resources.getString(R.string.month_september_s).format(y)
        10 -> context.resources.getString(R.string.month_october_s).format(y)
        11 -> context.resources.getString(R.string.month_november_s).format(y)
        12 -> context.resources.getString(R.string.month_december_s).format(y)
        else -> playlist
    }
}