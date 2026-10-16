package it.fast4x.rimusic.enums

import androidx.annotation.AnyThread
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class NavRoutes {
    home,
    games,
    gamePacman,
    gameSnake,
    history,
    localPlaylist,
    mood,
    queue,
    search,
    searchResults,
    settings,
    statistics,
    newAlbums,
    moodsPage,
    podcast,
    artistAlbums,
    YT_PLAYLIST,
    YT_ARTIST,
    YT_ALBUM,
    LICENSES,
    SONG_DETAILS,
    PROFILES,
    DOWNLOADS;

    companion object {

        private val ESCAPED_SEQUENCES_REGEX = Regex("^((\\[ntrbf])+)|((\\[ntrbf])+\$)")

        fun current( navController: NavController ) = navController.currentBackStackEntry?.destination?.route

        /** The current route with its arguments in place ("SONG_DETAILS/abc"), as [navigateHere] builds it */
        private fun currentFilled( navController: NavController ): String? {
            val entry = navController.currentBackStackEntry ?: return null
            val pattern = entry.destination.route ?: return null
            val args = entry.arguments
            return Regex( "\\{([^}]+)\\}" ).replace( pattern ) { match ->
                args?.get( match.groupValues[1] )?.toString() ?: match.value
            }
        }
    }

    fun isHere( navController: NavController ) = current( navController )?.startsWith( this.name ) ?: false

    fun isNotHere( navController: NavController ) = !isHere( navController )

    /**
     * Launch a non-blocking task and take user to currently selected route.
     *
     * **NOTE:** This function ensures [NavController.navigate] is run on
     * main thread, so you can safely call it from other threads
     */
    @AnyThread
    fun navigateHere( navController: NavController, path: String? = null ) {
        CoroutineScope( Dispatchers.Default ).launch {
            val cleanPath: String = path?.trim()
                                        ?.replace( ESCAPED_SEQUENCES_REGEX, "" )
                                        ?.let { "/$it" }
                                        .orEmpty()

            withContext( Dispatchers.Main ) {
                val target = "$name$cleanPath"
                // Already on this very page (same arguments): asking again stacked it
                // once more, each needing its own "back"
                if( currentFilled( navController ) == target ) return@withContext
                navController.navigate( target )
            }
        }
    }
}
