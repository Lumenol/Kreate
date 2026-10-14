package me.knighthat.internal.response

import kotlinx.serialization.Serializable
import me.knighthat.innertube.response.MusicPlaylistShelfRenderer
import me.knighthat.internal.util.SkipInvalidListSerializer

@Serializable
internal data class MusicPlaylistShelfRendererImpl(
    override val playlistId: String?,
    @Serializable(with = SkipInvalidListSerializer::class) override val contents: List<ContentImpl> = emptyList(),
    override val collapsedItemCount: Int = 0,
    override val contentsMultiSelectable: Boolean = false,
    override val targetId: String?
): MusicPlaylistShelfRenderer {

    @Serializable
    internal data class ContentImpl(
        override val musicResponsiveListItemRenderer: MusicResponsiveListItemRendererImpl?,
        override val continuationItemRenderer: ContinuationItemRendererImpl?
    ): MusicPlaylistShelfRenderer.Content {

        @Serializable
        internal data class ContinuationItemRendererImpl(
            override val trigger: String = "",
            override val continuationEndpoint: ContinuationImpl
        ): MusicPlaylistShelfRenderer.Content.ContinuationItemRenderer {

            @Serializable
            internal data class ContinuationImpl(
                override val continuationCommand: CommandImpl
            ): MusicPlaylistShelfRenderer.Content.ContinuationItemRenderer.Continuation {

                @Serializable
                internal data class CommandImpl(
                    override val token: String = "",
                    override val request: String = ""
                ): MusicPlaylistShelfRenderer.Content.ContinuationItemRenderer.Continuation.Command
            }
        }
    }
}