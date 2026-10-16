package it.fast4x.rimusic.ui.components.tab.toolbar

import androidx.compose.runtime.Composable
import app.kreate.android.R
import androidx.compose.ui.res.stringResource
import kotlin.random.Random

/**
 * "Ouvrir au hasard": an entry of the ⋯ menu. It was a dice icon in the toolbar,
 * where it pushed the Library's filter pills off screen.
 */
interface Randomizer<T>: MenuIcon {

    companion object {
        /**
         * To ensure true randomness, Random must not
         * be a constant variable
         */
        fun nextInt( until: Int ) =
            Random( System.currentTimeMillis() ).nextInt( until )
    }

    fun getItems(): List<T>

    fun onClick( index: Int )

    override val iconId: Int
        get() = R.drawable.dice

    override val menuIconTitle: String
        @Composable
        get() = stringResource( R.string.open_random_item )

    override val isEnabled: Boolean
        get() = getItems().isNotEmpty()

    override fun onShortClick() = onClick( nextInt( getItems().size ) )
}
