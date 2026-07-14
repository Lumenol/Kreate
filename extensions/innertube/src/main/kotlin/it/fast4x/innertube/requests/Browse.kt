package it.fast4x.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.BrowseResponse
import it.fast4x.innertube.models.MusicTwoRowItemRenderer
import it.fast4x.innertube.models.bodies.BrowseBodyWithLocale
import it.fast4x.innertube.utils.from
import it.fast4x.innertube.utils.runCatchingNonCancellable

suspend fun Innertube.browse(body: BrowseBodyWithLocale) = runCatchingNonCancellable {
    val response = client.post(browse) {
        setBody(body)
    }.body<BrowseResponse>()

    BrowseResult(
        title = response.header?.musicImmersiveHeaderRenderer?.title?.text ?: response.header
            ?.musicDetailHeaderRenderer?.title?.text,
        items = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents?.mapNotNull { content ->
                // A section is worth keeping when it has items — not when it happens to
                // carry a header. Pages like FEmusic_mixed_for_you are a single, untitled
                // grid (the name lives in the page header), and requiring a title here
                // silently dropped the whole thing, leaving the screen blank.
                when {
                    content.gridRenderer != null -> {
                        val items = content.gridRenderer.items
                                           ?.mapNotNull { it.musicTwoRowItemRenderer?.toItem() }
                                           .orEmpty()
                        if( items.isEmpty() ) return@mapNotNull null

                        BrowseResult.Item(
                            title = content.gridRenderer.header?.gridHeaderRenderer?.title?.runs
                                ?.firstOrNull()?.text.orEmpty(),
                            items = items
                        )
                    }

                    content.musicCarouselShelfRenderer != null -> {
                        val items = content.musicCarouselShelfRenderer.contents
                                           ?.mapNotNull { it.musicTwoRowItemRenderer?.toItem() }
                                           .orEmpty()
                        if( items.isEmpty() ) return@mapNotNull null

                        BrowseResult.Item(
                            title = content.musicCarouselShelfRenderer.header
                                ?.musicCarouselShelfBasicHeaderRenderer
                                ?.title?.runs?.firstOrNull()?.text.orEmpty(),
                            items = items
                        )
                    }

                    else -> null
                }
            }.orEmpty()
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