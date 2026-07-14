package it.fast4x.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.MusicTwoRowItemRenderer
import it.fast4x.innertube.models.bodies.BrowseBodyWithLocale
import it.fast4x.innertube.utils.from
import it.fast4x.innertube.utils.runCatchingNonCancellable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

// Only the fields toItem()/from() actually read are needed. The full item carries
// a rich `menu` and `thumbnailOverlay` that we ignore but that decode strictly —
// a single unmodelled endpoint type in there made the whole page fail to parse
// (e.g. FEmusic_mixed_for_you). Decoding item by item, without those heavy parts,
// keeps one odd item from taking the page down.
private val BROWSE_JSON = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}

private fun JsonElement?.child( key: String ): JsonElement? =
    (this as? JsonObject)?.get( key )

private fun JsonElement?.array(): List<JsonElement> =
    (this as? JsonArray).orEmpty()

private fun JsonElement?.string(): String? =
    (this as? JsonPrimitive)?.contentOrNull

private fun twoRowItem( element: JsonElement ): Innertube.Item? {
    val renderer = element.child( "musicTwoRowItemRenderer" ) as? JsonObject ?: return null
    // Drop the parts we don't use; they're the fragile ones.
    val slimmed = JsonObject( renderer - "menu" - "thumbnailOverlay" )
    return runCatching {
        BROWSE_JSON.decodeFromJsonElement( MusicTwoRowItemRenderer.serializer(), slimmed ).toItem()
    }.getOrNull()
}

private fun sectionItem( section: JsonElement, gridKey: String, titlePath: List<String> ): BrowseResult.Item? {
    val renderer = section.child( gridKey ) ?: return null
    val itemsArray = ( renderer.child( "items" ) ?: renderer.child( "contents" ) ).array()
    val items = itemsArray.mapNotNull( ::twoRowItem )
    if( items.isEmpty() ) return null

    var title: JsonElement? = renderer.child( "header" )
    for( key in titlePath ) title = title.child( key )
    return BrowseResult.Item(
        title = title.child( "runs" ).array().firstOrNull().child( "text" ).string().orEmpty(),
        items = items
    )
}

suspend fun Innertube.browse(body: BrowseBodyWithLocale) = runCatchingNonCancellable {
    val root = client.post(browse) {
        setBody(body)
    }.body<JsonObject>()

    val header = root.child( "header" )
    val sections = root.child( "contents" )
                       .child( "singleColumnBrowseResultsRenderer" )
                       .child( "tabs" ).array().firstOrNull()
                       .child( "tabRenderer" )
                       .child( "content" )
                       .child( "sectionListRenderer" )
                       .child( "contents" ).array()

    BrowseResult(
        title = ( header.child( "musicImmersiveHeaderRenderer" ) ?: header.child( "musicDetailHeaderRenderer" ) )
            .child( "title" ).child( "runs" ).array().firstOrNull().child( "text" ).string(),
        items = sections.mapNotNull { section ->
            sectionItem( section, "gridRenderer", listOf( "gridHeaderRenderer", "title" ) )
                ?: sectionItem( section, "musicCarouselShelfRenderer", listOf( "musicCarouselShelfBasicHeaderRenderer", "title" ) )
        }
    )
}

data class BrowseResult(
    val title: String?,
    val items: List<Item>
) {
    data class Item(
        val title: String,
        val items: List<Innertube.Item>
    )
}

fun MusicTwoRowItemRenderer.toItem() = when {
    isAlbum -> Innertube.AlbumItem.from(this)
    isPlaylist -> Innertube.PlaylistItem.from(this)
    isArtist -> Innertube.ArtistItem.from(this)
    else -> null
}