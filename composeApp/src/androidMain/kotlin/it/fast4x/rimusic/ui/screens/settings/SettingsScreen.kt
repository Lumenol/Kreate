package it.fast4x.rimusic.ui.screens.settings

import app.kreate.android.themed.common.component.DetailSkeleton
import it.fast4x.rimusic.ui.components.ButtonsRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import it.fast4x.rimusic.utils.color
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.R
import app.kreate.android.themed.common.screens.settings.About
import app.kreate.android.themed.common.screens.settings.AccountSettings
import app.kreate.android.themed.common.screens.settings.AppearanceSettings
import app.kreate.android.themed.common.screens.settings.DataSettings
import app.kreate.android.themed.common.screens.settings.GeneralSettings
import app.kreate.android.themed.common.screens.settings.NetworkSettings
import app.kreate.android.themed.common.screens.settings.OtherSettings
import app.kreate.android.themed.common.screens.settings.QuickPicksSettings
import app.kreate.android.themed.common.screens.settings.UiSettings
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.UiType
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.themed.StringListDialog
import it.fast4x.rimusic.utils.secondary
import it.fast4x.rimusic.utils.semiBold
import me.knighthat.component.dialog.RestartAppDialog

@UnstableApi
@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun SettingsScreen(
    navController: NavController,
    miniPlayer: @Composable () -> Unit = {},
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val (tabIndex, onTabChanged) = rememberSaveable { mutableIntStateOf(0) }

    val topPadding =
        if( UiType.ViMusic.isCurrent() )
            WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        else
            0.dp
    val paddingValues = remember( topPadding ) {
        PaddingValues( 6.dp, topPadding, 6.dp )
    }

    // Nine tabs did not fit in a bottom bar (names cut, some off screen):
    // they become chips under the title, the main tabs stay at the bottom
    DetailSkeleton(
        navController = navController,
        miniPlayer = miniPlayer,
        title = {
            BasicText(
                text = stringResource( R.string.settings ),
                style = typography().l.semiBold.color( colorPalette().text )
            )
        }
    ) {
        Column( Modifier.fillMaxSize() ) {
            ButtonsRow(
                chips = listOf(
                    0 to stringResource( R.string.tab_general ),
                    1 to stringResource( R.string.ui_tab ),
                    2 to stringResource( R.string.player_appearance ),
                    3 to stringResource( if( isYouTubeLoggedIn() ) R.string.home else R.string.quick_picks ),
                    4 to stringResource( R.string.tab_data ),
                    5 to stringResource( R.string.tab_accounts ),
                    6 to stringResource( R.string.tab_miscellaneous ),
                    7 to stringResource( R.string.tab_network ),
                    8 to stringResource( R.string.about )
                ),
                currentValue = tabIndex,
                onValueUpdate = onTabChanged,
                modifier = Modifier.padding( bottom = 4.dp )
            )

            Box( Modifier.weight( 1f ) ) {
                saveableStateHolder.SaveableStateProvider(tabIndex) {
                    when (tabIndex) {
                        0 -> GeneralSettings( paddingValues )
                        1 -> UiSettings( paddingValues )
                        2 -> AppearanceSettings( paddingValues )
                        3 -> QuickPicksSettings( paddingValues )
                        4 -> DataSettings( paddingValues )
                        5 -> AccountSettings( paddingValues )
                        6 -> OtherSettings( paddingValues )
                        7 -> NetworkSettings( paddingValues )
                        8 -> About( navController, paddingValues )
                    }
                }
            }
        }
    }

    RestartAppDialog.Render()
}

@Composable
inline fun StringListValueSelectorSettingsEntry(
    title: String,
    text: String,
    addTitle: String,
    addPlaceholder: String,
    conflictTitle: String,
    removeTitle: String,
    context: Context,
    list: List<String>,
    crossinline add: (String) -> Unit,
    crossinline remove: (String) -> Unit
) {
    var showStringListDialog by remember {
        mutableStateOf(false)
    }


    if (showStringListDialog) {
        StringListDialog(
            title = title,
            addTitle = addTitle,
            addPlaceholder = addPlaceholder,
            removeTitle = removeTitle,
            conflictTitle = conflictTitle,
            list = list,
            add = add,
            remove = remove,
            onDismiss = { showStringListDialog = false },
        )
    }
    SettingsEntry(
        title = title,
        text = text,
        onClick = {
            showStringListDialog = true
        }
    )
}

@Composable
fun SettingsEntry(
    title: String,
    titleSecondary: String? = null,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isEnabled: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable(enabled = isEnabled, onClick = onClick)
            .alpha(if (isEnabled) 1f else 0.5f)
            //.padding(start = 16.dp)
            //.padding(all = 16.dp)
            .padding(all = 12.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            BasicText(
                text = title,
                style = typography().xs.semiBold.copy(color = colorPalette().text),
                modifier = Modifier
                    .padding(bottom = 4.dp)
            )
            if (text != "")
                BasicText(
                    text = text,
                    style = typography().xs.semiBold.copy(color = colorPalette().textSecondary),
                )
        }

        trailingContent?.invoke()

        if (titleSecondary != null) {
            BasicText(
                text = titleSecondary,
                style = typography().xxs.secondary,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                //modifier = Modifier
                //    .padding(vertical = 8.dp, horizontal = 24.dp)
            )
        }
    }
}