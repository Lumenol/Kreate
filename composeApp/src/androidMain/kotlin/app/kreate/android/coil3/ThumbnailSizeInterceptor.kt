package app.kreate.android.coil3

import app.kreate.util.thumbnailResized
import coil3.Uri
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.size.pxOrElse
import kotlin.math.max

/**
 * Asks Google's image servers for the size actually drawn: a small row used to
 * download and decode the 900 px cover. A target without a size (full screen
 * player) keeps [ImageFactory.THUMBNAIL_SIZE]. From Craeckie/Kreate 42f2230b.
 *
 * Each size gets its own disk cache key so a row and a cover don't overwrite
 * each other (Coil's own memory key already includes size and transformations). When the resized image can't be fetched (offline), the request goes on
 * unchanged, so covers cached before keep showing.
 */
class ThumbnailSizeInterceptor : Interceptor {

    override suspend fun intercept( chain: Interceptor.Chain ): ImageResult {
        val url = when( val data = chain.request.data ) {
            is String -> data
            is Uri    -> data.toString()
            else      -> null
        }

        val px = max(
            chain.size.width.pxOrElse { 0 },
            chain.size.height.pxOrElse { 0 }
        ).takeIf { it > 0 }?.coerceAtMost( ImageFactory.THUMBNAIL_SIZE ) ?: ImageFactory.THUMBNAIL_SIZE
        val resized = url.thumbnailResized( px )
        if( url == null || resized == null || resized == url )
            return chain.proceed()

        val request = chain.request
                           .newBuilder()
                           .data( resized )
                           .diskCacheKey( "$url@$px" )
                           .build()
        val result = chain.withRequest( request ).proceed()
        return if( result is ErrorResult ) chain.proceed() else result
    }
}
