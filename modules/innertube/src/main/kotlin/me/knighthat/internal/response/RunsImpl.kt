package me.knighthat.internal.response

import kotlinx.serialization.Serializable
import me.knighthat.innertube.response.Runs
import me.knighthat.internal.util.SkipInvalidListSerializer

@Serializable
internal data class RunsImpl(
    @Serializable(with = SkipInvalidListSerializer::class) override val runs: List<RunImpl> = emptyList(),
    override val accessibility: AccessibilityImpl?
): Runs {

    @Serializable
    data class RunImpl(
        override val bold: Boolean?,
        override val text: String = "",
        override val navigationEndpoint: EndpointImpl?
    ): Runs.Run
}