package it.fast4x.rimusic.enums

import androidx.compose.runtime.Composable
import me.knighthat.enums.TextView

/** How long a song's preview (tap on its thumbnail) plays before the music resumes */
enum class PreviewLength( val asSeconds: Int ): TextView {

    `10s`( 10 ),

    `15s`( 15 ),

    `30s`( 30 ),

    `60s`( 60 );

    val asMillis: Long = this.asSeconds * 1000L

    override val text: String
        @Composable
        get() = this.name
}
