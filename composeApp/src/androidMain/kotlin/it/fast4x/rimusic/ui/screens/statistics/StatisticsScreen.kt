package it.fast4x.rimusic.ui.screens.statistics

import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import it.fast4x.rimusic.ui.components.navigation.header.TabToolBar
import androidx.compose.foundation.layout.width
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.colorPalette
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.compose.persist.PersistMapCleanup
import app.kreate.android.R
import it.fast4x.rimusic.enums.StatisticsType
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import it.fast4x.rimusic.utils.color
import it.fast4x.rimusic.utils.semiBold

@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun StatisticsScreen(
    navController: NavController,
    statisticsType: StatisticsType,
    miniPlayer: @Composable () -> Unit = {},
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    val (tabIndex, onTabIndexChanged) = rememberSaveable {
        mutableStateOf(when (statisticsType) {
            StatisticsType.Today -> 0
            StatisticsType.OneWeek -> 1
            StatisticsType.OneMonth -> 2
            StatisticsType.ThreeMonths -> 3
            StatisticsType.SixMonths -> 4
            StatisticsType.OneYear -> 5
            StatisticsType.All -> 6
        })
    }

    PersistMapCleanup(tagPrefix = "${statisticsType.name}/")

    val periods = listOf(
        0 to stringResource( R.string.stats_period_today ),
        1 to stringResource( R.string.stats_period_week ),
        2 to stringResource( R.string.stats_period_month ),
        3 to stringResource( R.string.stats_period_3_months ),
        4 to stringResource( R.string.stats_period_6_months ),
        5 to stringResource( R.string.stats_period_year ),
        6 to stringResource( R.string.all )
    )

    // A page of its own: "Statistiques" and the period as one pill ("Mois ▾") in
    // the bar, like the Library's filters. Only the Titres/Artistes/Albums/
    // Playlists row is left above the list (two rows of pills were cut).
    app.kreate.android.themed.common.component.DetailSkeleton(
        navController = navController,
        miniPlayer = miniPlayer,
        title = {
            androidx.compose.foundation.text.BasicText(
                text = stringResource( R.string.statistics ),
                style = typography().l.semiBold.color( colorPalette().text ),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        },
        actions = {
            TabToolBar.FilterPill(
                options = periods.map { p -> p.first },
                current = tabIndex,
                // Never throws on a stale saved index: falls back to "Tout" like the content
                label = { index -> periods.firstOrNull { p -> p.first == index }?.second ?: periods.last().second },
                onSelect = onTabIndexChanged
            )
            Spacer( Modifier.width( 8.dp ) )
        }
    ) {
        // A little air between the bar and the Titres/Artistes… row
        Box( Modifier.padding( top = 4.dp ) ) {
            saveableStateHolder.SaveableStateProvider( key = tabIndex ) {
                val type: StatisticsType =
                    when( tabIndex ) {
                        0 -> StatisticsType.Today
                        1 -> StatisticsType.OneWeek
                        2 -> StatisticsType.OneMonth
                        3 -> StatisticsType.ThreeMonths
                        4 -> StatisticsType.SixMonths
                        5 -> StatisticsType.OneYear
                        else -> StatisticsType.All
                    }

                StatisticsPage( navController, type )
            }
        }
    }
}
