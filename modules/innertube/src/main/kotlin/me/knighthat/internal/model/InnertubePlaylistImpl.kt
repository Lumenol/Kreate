package me.knighthat.internal.model

import kotlinx.serialization.Serializable
import me.knighthat.innertube.model.InnertubePlaylist
import me.knighthat.innertube.model.InnertubeSong
import me.knighthat.innertube.response.BrowseResponse.Contents.TwoColumnBrowseResultsRenderer
import me.knighthat.innertube.response.Continuation
import me.knighthat.innertube.response.MusicResponsiveListItemRenderer
import me.knighthat.innertube.response.MusicTwoRowItemRenderer
import me.knighthat.innertube.response.Runs
import me.knighthat.innertube.response.Tabs
import me.knighthat.innertube.response.Thumbnails

@Serializable
internal data class InnertubePlaylistImpl(
    override val id: String,
    override val name: String,
    override val thumbnails: List<Thumbnails.Thumbnail>,
    override val description: String?,
    override var subtitle: Runs?,
    override var continuations: List<Continuation>,
    override var songs: List<InnertubeSong>,
    override var songContinuation: String?,
    override var visitorData: String?
): InnertubePlaylist {

    companion object {

        fun from( renderer: MusicTwoRowItemRenderer ): InnertubePlaylist {
            val run = renderer.title.runs.first()       // Requires not null to proceed
            return InnertubePlaylistImpl(
                // [id] must be a non-null value
                run.navigationEndpoint!!.browseEndpoint!!.browseId,
                run.text,
                renderer.thumbnailRenderer.toThumbnailList(),
                null,
                null,
                emptyList(),
                emptyList(),
                null,
                null
            )
        }

        /** @param browseId requested, the response doesn't always repeat it */
        fun from( visitorData: String?, renderer: TwoColumnBrowseResultsRenderer, browseId: String ): InnertubePlaylist {
            // YouTube stopped sending the header (title, cover...) of some playlists,
            // at least without login: the songs are still there, so don't fail the
            // whole page for it (it showed every such playlist empty)
            val content = renderer.tabs
                                  .firstNotNullOfOrNull( Tabs.Tab::tabRenderer )
                                  ?.content
                                  ?.sectionListRenderer
                                  ?.contents
                                  ?.firstOrNull()
            val headerRenderer = content?.musicResponsiveHeaderRenderer
                ?: content?.musicEditablePlaylistDetailHeaderRenderer
                          ?.header
                          ?.musicResponsiveHeaderRenderer

            val sectionListRenderer = renderer.secondaryContents!!.sectionListRenderer
            val playlistShelfRenderer = sectionListRenderer.contents.first().musicPlaylistShelfRenderer
            val playlistId = playlistShelfRenderer!!.playlistId ?: browseId
            val continuedPlaylist = ContinuedPlaylistImpl.from( playlistShelfRenderer.contents )
            val description = headerRenderer?.description?.musicDescriptionShelfRenderer?.description?.firstText

            return InnertubePlaylistImpl(
                // Add "VL" in case it's not there
                if( playlistId.startsWith("VL") ) playlistId else "VL$playlistId",
                    headerRenderer?.title?.firstText.orEmpty(),
                    // Without header, the first song's cover stands for the playlist
                    headerRenderer?.thumbnail?.toThumbnailList() ?: continuedPlaylist.songs.firstOrNull()?.thumbnails.orEmpty(),
                                         description,
                                         headerRenderer?.secondSubtitle,      // Contains delimiters by default
                                         sectionListRenderer.continuations,
                                         continuedPlaylist.songs,
                                         continuedPlaylist.continuation,
                                         visitorData
            )
        }

        fun from( renderer: MusicResponsiveListItemRenderer): InnertubePlaylist {
            val id = requireNotNull(
                renderer.navigationEndpoint?.browseEndpoint?.browseId
            ) { "MusicResponsiveListItemRenderer doesn't contain id" }

            val info = renderer.flexColumns
            require( info.isNotEmpty() ) { "Playlist doesn't contain any information" }
            val name = info.first().musicResponsiveListItemFlexColumnRenderer?.text?.firstText.orEmpty()

            return InnertubePlaylistImpl(
                id = id,
                name = name,
                thumbnails = renderer.thumbnail.toThumbnailList(),
                description = null,
                subtitle = info.getOrNull( 1 )?.musicResponsiveListItemFlexColumnRenderer?.text,
                continuations = emptyList(),
                songs = emptyList(),
                songContinuation = null,
                visitorData = null
            )
        }
    }

    override val subtitleText: String? by lazy { subtitle?.runs?.joinToString( "" ) { it.text } }

    override fun shareUrl( host: String ): String {
        require( host.isYouTubeHost )

        return "$host/playlist?list=$id"
    }
}
