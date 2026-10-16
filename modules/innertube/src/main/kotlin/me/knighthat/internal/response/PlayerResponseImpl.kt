package me.knighthat.internal.response

import kotlinx.serialization.Serializable
import me.knighthat.innertube.response.PlayerResponse
import me.knighthat.internal.util.SkipInvalidListSerializer

@Serializable
internal data class PlayerResponseImpl(
    override val playabilityStatus: PlayabilityStatusImpl,
    override val streamingData: StreamingDataImpl?,
    override val videoDetails: VideoDetailsImpl?,
    override val playerConfig: PlayerConfigImpl?,
    override val microformat: MicroformatImpl?,
    override val responseContext: InnertubeResponseImpl.ContextImpl
): PlayerResponse {

    @Serializable
    data class PlayabilityStatusImpl(
        override val status: String = "",
        override val reason: String?,
        override val playableInEmbed: Boolean?,
        override val audioOnlyPlayability: AudioOnlyPlayabilityImpl?,
        override val contextParams: String?
    ): PlayerResponse.PlayabilityStatus {

        @Serializable
        data class AudioOnlyPlayabilityImpl(
            override val audioOnlyPlayabilityRenderer: AudioOnlyPlayabilityRendererImpl
        ): PlayerResponse.PlayabilityStatus.AudioOnlyPlayability {

            @Serializable
            data class AudioOnlyPlayabilityRendererImpl(
                override val audioOnlyAvailability: String = ""
            ): PlayerResponse.PlayabilityStatus.AudioOnlyPlayability.AudioOnlyPlayabilityRenderer
        }
    }

    @Serializable
    data class StreamingDataImpl(
        override val expiresInSeconds: String = "",
        @Serializable(with = SkipInvalidListSerializer::class) override val formats: List<FormatImpl> = emptyList(),
        @Serializable(with = SkipInvalidListSerializer::class) override val adaptiveFormats: List<FormatImpl> = emptyList(),
        override val serverAbrStreamingUrl: String?
    ): PlayerResponse.StreamingData {

        @Serializable
        data class FormatImpl(
            override val itag: Short,
            override val url: String?,
            override val mimeType: String = "",
            override val bitrate: Int = 0,
            override val width: Short?,
            override val height: Short?,
            override val lastModified: String = "",
            override val contentLength: String?,
            override val quality: String = "",
            override val fps: Byte?,
            override val qualityLabel: String?,
            override val projectionType: String = "",
            override val averageBitrate: Int?,
            override val highReplication: Boolean?,
            override val audioQuality: String?,
            override val approxDurationMs: String = "",
            override val audioSampleRate: String?,
            override val audioChannels: Byte?,
            override val loudnessDb: Float?,
            override val signatureCipher: String?
        ): PlayerResponse.StreamingData.Format
    }

    @Serializable
    data class VideoDetailsImpl(
        override val videoId: String = "",
        override val title: String = "",
        override val lengthSeconds: String = "",
        override val channelId: String = "",
        override val isOwnerViewing: Boolean = false,
        override val isCrawlable: Boolean = false,
        override val thumbnail: ThumbnailsImpl,
        override val allowRatings: Boolean = false,
        override val viewCount: String = "",
        override val author: String = "",
        override val isPrivate: Boolean = false,
        override val isUnpluggedCorpus: Boolean = false,
        override val musicVideoType: String?,
        override val isLiveContent: Boolean = false,
        override val shortDescription: String? = null
    ): PlayerResponse.VideoDetails

    @Serializable
    data class PlayerConfigImpl(
        override val audioConfig: AudioConfigImpl
    ): PlayerResponse.PlayerConfig {

        @Serializable
        data class AudioConfigImpl(
            override val loudnessDb: Float?,
            override val perceptualLoudnessDb: Float = 0f,
            override val enablePerFormatLoudness: Boolean?
        ): PlayerResponse.PlayerConfig.AudioConfig
    }
}