package app.kreate.android.utils

/**
 * YouTube playlist ids, as cards, links and the library give them: bare ("PL…") or
 * with the "VL" the browse call needs ("VLPL…").
 *
 * "RDCLAK…" and "RDTMAK…" are playlists made by YouTube (Home mixes, moods). Any other
 * "RD…" is a radio, endless and built from a song or an artist: it has no page, browsing
 * it gives an empty one.
 */
object PlaylistIds {

    private val PREFIXES = listOf( "PL", "OLAK", "RDCLAK", "RDTMAK", "LM", "UU" )

    fun isRadio( id: String ): Boolean {
        val bare = id.removePrefix( "VL" )
        return bare.startsWith( "RD" ) && !bare.startsWith( "RDCLAK" ) && !bare.startsWith( "RDTMAK" )
    }

    /** A playlist with a page to browse: not a radio, nor a chart or podcast shelf */
    fun isBrowsable( id: String ): Boolean =
        !isRadio( id ) && (id.startsWith( "VL" ) || PREFIXES.any( id::startsWith ))

    /** [id] with the "VL" the browse call needs, when it's a playlist's bare id */
    fun browseId( id: String ): String =
        if( !id.startsWith( "VL" ) && isBrowsable( id ) ) "VL$id" else id
}
