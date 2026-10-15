package app.kreate.android.utils

import app.kreate.android.Preferences

/** Artists the radio and the infinite queue leave out */
object BlockedArtists {

    val ids: Set<String>
        get() = Preferences.BLOCKED_ARTISTS.value.split( ',' ).filterTo( HashSet() ) { it.isNotBlank() }

    fun isBlocked( id: String ): Boolean = id in ids

    /** @return `true` when [id] is blocked afterwards */
    fun toggle( id: String ): Boolean {
        val current = ids
        val blocked = id !in current
        Preferences.BLOCKED_ARTISTS.value = (if( blocked ) current + id else current - id).joinToString( "," )
        return blocked
    }
}
