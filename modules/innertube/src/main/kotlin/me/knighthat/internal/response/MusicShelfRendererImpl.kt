package me.knighthat.internal.response

import kotlinx.serialization.Serializable
import me.knighthat.innertube.response.MusicShelfRenderer
import me.knighthat.internal.util.SkipInvalidListSerializer

@Serializable
internal data class MusicShelfRendererImpl(
    override val title: RunsImpl?,
    @Serializable(with = SkipInvalidListSerializer::class) override val contents: List<ContentImpl> = emptyList(),
    override val bottomText: RunsImpl?,
    override val bottomEndpoint: EndpointImpl?,
    override val contentsMultiSelectable: Boolean?,
    @Serializable(with = SkipInvalidListSerializer::class) override val subheaders: List<SubheaderImpl> = emptyList(),
    @Serializable(with = SkipInvalidListSerializer::class) override val continuations: List<ContinuationImpl> = emptyList()
): MusicShelfRenderer {

    @Serializable
    internal data class ContentImpl(
        override val musicResponsiveListItemRenderer: MusicResponsiveListItemRendererImpl?
    ): MusicShelfRenderer.Content

    @Serializable
    internal data class SubheaderImpl(
        override val musicSideAlignedItemRenderer: RendererImpl
    ) : MusicShelfRenderer.Subheader {

        @Serializable
        internal data class RendererImpl(
            @Serializable(with = SkipInvalidListSerializer::class) override val startItems: List<ItemImpl> = emptyList()
        ) : MusicShelfRenderer.Subheader.Renderer {

            @Serializable
            internal data class ItemImpl(
                override val musicSortFilterButtonRenderer: MusicSortFilterButtonRendererImpl
            ) : MusicShelfRenderer.Subheader.Renderer.Item {

                @Serializable
                internal data class MusicSortFilterButtonRendererImpl(
                    override val title: RunsImpl,
                    override val menu: MenuImpl,
                    override val accessibility: AccessibilityImpl
                ) : MusicShelfRenderer.Subheader.Renderer.Item.MusicSortFilterButtonRenderer {

                    @Serializable
                    internal data class MenuImpl(
                        override val musicMultiSelectMenuRenderer: MusicMultiSelectMenuRendererImpl
                    ) : MusicShelfRenderer.Subheader.Renderer.Item.MusicSortFilterButtonRenderer.Menu
                }
            }
        }
    }
}