package it.fast4x.rimusic.enums

import androidx.annotation.DrawableRes
import app.kreate.android.R

/** Where the Library's artists and albums come from; [iconId] for the filter pill */
enum class FilterBy( @field:DrawableRes val iconId: Int ) {
    All( R.drawable.library ),
    Local( R.drawable.devices ),
    YoutubeLibrary( R.drawable.ytmusic );
}
