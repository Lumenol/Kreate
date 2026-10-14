package me.knighthat.internal.response

import kotlinx.serialization.Serializable
import me.knighthat.innertube.response.MusicCarouselShelfRenderer
import me.knighthat.internal.util.SkipInvalidListSerializer

@Serializable
internal data class MusicCarouselShelfRendererImpl(
    override val header: HeaderImpl,
    @Serializable(with = SkipInvalidListSerializer::class) override val contents: List<ContentImpl> = emptyList(),
    override val itemSize: String = "",
    override val numItemsPerColumn: String?
): MusicCarouselShelfRenderer {

    @Serializable
    data class HeaderImpl(
        override val musicCarouselShelfBasicHeaderRenderer: MusicCarouselShelfBasicHeaderRendererImpl
    ): MusicCarouselShelfRenderer.Header {

        @Serializable
        data class MusicCarouselShelfBasicHeaderRendererImpl(
            override val title: RunsImpl,
            override val strapline: RunsImpl?,
            override val accessibilityData: AccessibilityImpl?,
            override val headerStyle: String = ""
        ): MusicCarouselShelfRenderer.Header.MusicCarouselShelfBasicHeaderRenderer
    }

    @Serializable
    data class ContentImpl(
        override val musicResponsiveListItemRenderer: MusicResponsiveListItemRendererImpl?,
        override val musicTwoRowItemRenderer: MusicTwoRowItemRendererImpl?
    ): MusicCarouselShelfRenderer.Content
}