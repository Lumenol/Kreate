package it.fast4x.rimusic.enums

import androidx.annotation.StringRes
import app.kreate.android.R
import me.knighthat.enums.TextView

/** Quality of downloads, apart from the one songs are played in */
enum class DownloadQuality(
    @field:StringRes override val androidTextId: Int,
    /** `null`: the playback quality */
    val format: AudioQualityFormat?
): TextView {

    SameAsPlayback( R.string.download_quality_same_as_playback, null ),

    High( R.string.audio_quality_format_high, AudioQualityFormat.High ),

    Low( R.string.audio_quality_format_low, AudioQualityFormat.Low );
}
