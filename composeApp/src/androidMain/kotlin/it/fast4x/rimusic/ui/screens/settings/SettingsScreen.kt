package it.fast4x.rimusic.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import it.fast4x.rimusic.ui.styling.Dimensions
import app.kreate.android.themed.common.component.DetailSkeleton
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
    // The "Debug" badge of the header opens the debug options directly
    val openDebug = remember { SettingsScreen.consumeOpenDebug() }
    // -1: the list of sections; otherwise the open section
    var tabIndex by rememberSaveable { mutableIntStateOf( if( openDebug ) 6 else -1 ) }

    val topPadding =
        if( UiType.ViMusic.isCurrent() )
            WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        else
            0.dp
    val paddingValues = remember( topPadding ) {
        PaddingValues( 6.dp, topPadding, 6.dp )
    }

    val sections = SettingsSection.all()
    // In a section, "back" (top bar or system) goes up to the list
    BackHandler( enabled = tabIndex >= 0 ) { tabIndex = -1 }

    // Nine chips didn't fit (names cut, some off screen): the page opens on a list
    // of sections, each with what it holds, and a section opens on tap
    DetailSkeleton(
        navController = navController,
        miniPlayer = miniPlayer,
        title = {
            BasicText(
                text = sections.getOrNull( tabIndex )?.title ?: stringResource( R.string.settings ),
                style = typography().l.semiBold.color( colorPalette().text )
            )
        },
        onBack = if( tabIndex >= 0 ) {{ tabIndex = -1 }} else null
    ) {
        Box( Modifier.fillMaxSize() ) {
            if( tabIndex < 0 )
                SettingsSectionList( sections, paddingValues ) { tabIndex = it }
            else
                saveableStateHolder.SaveableStateProvider(tabIndex) {
                    when (tabIndex) {
                        0 -> GeneralSettings( paddingValues )
                        1 -> UiSettings( paddingValues )
                        2 -> AppearanceSettings( paddingValues )
                        3 -> QuickPicksSettings( paddingValues )
                        4 -> DataSettings( paddingValues )
                        5 -> AccountSettings( paddingValues )
                        6 -> OtherSettings( paddingValues, scrollToDebug = openDebug )
                        7 -> NetworkSettings( paddingValues )
                        8 -> About( navController, paddingValues )
                    }
                }
        }
    }

    RestartAppDialog.Render()
}

/** One line of the settings list: its tab, icon, name and what it holds */
class SettingsSection( val title: String, val description: String, val iconId: Int ) {
    companion object {
        @Composable
        fun all() = listOf(
            SettingsSection( stringResource( R.string.tab_general ), stringResource( R.string.settings_section_general ), R.drawable.settings ),
            SettingsSection( stringResource( R.string.ui_tab ), stringResource( R.string.settings_section_ui ), R.drawable.ui ),
            SettingsSection( stringResource( R.string.tab_player ), stringResource( R.string.settings_section_player ), R.drawable.play ),
            SettingsSection(
                stringResource( if( isYouTubeLoggedIn() ) R.string.home else R.string.quick_picks ),
                stringResource( R.string.settings_section_home ),
                R.drawable.sparkles
            ),
            SettingsSection( stringResource( R.string.tab_data ), stringResource( R.string.settings_section_data ), R.drawable.server ),
            SettingsSection( stringResource( R.string.tab_accounts ), stringResource( R.string.settings_section_accounts ), R.drawable.person ),
            SettingsSection( stringResource( R.string.tab_miscellaneous ), stringResource( R.string.settings_section_other ), R.drawable.equalizer ),
            SettingsSection( stringResource( R.string.tab_network ), stringResource( R.string.settings_section_network ), R.drawable.globe ),
            SettingsSection( stringResource( R.string.about ), stringResource( R.string.settings_section_about ), R.drawable.information )
        )
    }
}

@Composable
private fun SettingsSectionList(
    sections: List<SettingsSection>,
    paddingValues: PaddingValues,
    onOpen: (Int) -> Unit
) =
    LazyColumn(
        contentPadding = PaddingValues( bottom = Dimensions.bottomSpacer ),
        modifier = Modifier.fillMaxSize()
                           .background( colorPalette().background0 )
                           .padding( paddingValues )
    ) {
        itemsIndexed( sections ) { index, section ->
            Row(
                horizontalArrangement = Arrangement.spacedBy( 18.dp ),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                                   .clickable { onOpen( index ) }
                                   .padding( horizontal = 16.dp, vertical = 14.dp )
            ) {
                Icon(
                    painter = painterResource( section.iconId ),
                    contentDescription = null,
                    tint = colorPalette().text,
                    modifier = Modifier.size( 24.dp )
                )
                Column( Modifier.weight( 1f ) ) {
                    BasicText(
                        text = section.title,
                        style = typography().s.semiBold.color( colorPalette().text )
                    )
                    BasicText(
                        text = section.description,
                        style = typography().xs.secondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

object SettingsScreen {

    private var openDebug = false

    /** Next time settings open, show the "Miscellaneous" tab scrolled to "Debug" */
    fun requestDebug() { openDebug = true }

    fun consumeOpenDebug(): Boolean = openDebug.also { openDebug = false }
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