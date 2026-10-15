package app.kreate.util

import coil3.Uri
import coil3.toUri


fun String?.thumbnail(size: Int): String? {
    return when {
        this?.startsWith("https://lh3.googleusercontent.com") == true -> "$this-w$size-h$size"
        this?.startsWith("https://yt3.ggpht.com") == true -> "$this-w$size-h$size-s$size"
        this?.startsWith("https://yt3.googleusercontent.com") == true -> "$this-w$size-h$size-s$size"
        else -> this
    }
}
/**
 * [this] Google thumbnail url at [size] px. Unlike [thumbnail], which appends,
 * the size YouTube put in the url is replaced: appended sizes pile up.
 */
fun String?.thumbnailResized( size: Int ): String? {
    if( this == null ) return null

    val base = substringBefore( '=' )
    return when {
        base.startsWith( "https://lh3.googleusercontent.com" ) -> "$base=w$size-h$size"
        base.startsWith( "https://yt3.ggpht.com" ) ||
        base.startsWith( "https://yt3.googleusercontent.com" ) -> "$base=w$size-h$size-s$size"
        else -> this
    }
}
fun String?.thumbnail(): String? {
    return this
}
fun Uri?.thumbnail(size: Int): Uri? {
    return toString().thumbnail(size)?.toUri()
}