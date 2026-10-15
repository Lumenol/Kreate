package app.kreate.android.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.media3.common.Player
import app.kreate.android.R
import co.touchlab.kermit.Logger
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.MainActivity
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.forcePlayAtIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Long press on the app icon: resume, liked songs shuffled, search.
 *
 * Registered at runtime rather than in `res/xml/shortcuts.xml`, whose intents
 * name a fixed package and so did nothing in builds with another application
 * id (nightly, debug).
 */
object LauncherShortcuts {

    const val ACTION_RESUME = "app.kreate.android.action.RESUME"
    const val ACTION_SHUFFLE_FAVORITES = "app.kreate.android.action.SHUFFLE_FAVORITES"

    val actions = setOf( ACTION_RESUME, ACTION_SHUFFLE_FAVORITES )

    fun register( context: Context ) {
        fun shortcut( id: String, action: String, labelId: Int, iconId: Int ) =
            ShortcutInfoCompat.Builder( context, id )
                              .setShortLabel( context.getString( labelId ) )
                              .setIcon( IconCompat.createWithResource( context, iconId ) )
                              .setIntent(
                                  Intent( context, MainActivity::class.java ).setAction( action )
                              )
                              .build()

        runCatching {
            ShortcutManagerCompat.setDynamicShortcuts(
                context,
                listOf(
                    shortcut( "resume", ACTION_RESUME, R.string.shortcut_resume, R.drawable.play ),
                    shortcut( "favorites", ACTION_SHUFFLE_FAVORITES, R.string.shortcut_shuffle_favorites, R.drawable.heart ),
                    shortcut( "search", MainActivity.action_search, R.string.word_search, R.drawable.search )
                )
            )
        }.onFailure { Logger.e( it ) { "failed to register launcher shortcuts" } }
    }

    /** Runs a shortcut [action] once the [player] is available */
    suspend fun run( action: String, player: Player ) {
        when( action ) {
            ACTION_RESUME -> {
                // The saved queue is restored in the background at startup
                val hasQueue = withTimeoutOrNull( 3_000 ) {
                    while( player.mediaItemCount == 0 ) delay( 100 )
                    true
                } ?: false
                if( hasQueue ) player.play() else shuffleFavorites( player )
            }
            ACTION_SHUFFLE_FAVORITES -> shuffleFavorites( player )
        }
    }

    private suspend fun shuffleFavorites( player: Player ) {
        val items = withContext( Dispatchers.IO ) {
            Database.songTable.allFavoritesRandomized().first().map { it.asMediaItem }
        }
        player.forcePlayAtIndex( items, 0 )
    }
}
