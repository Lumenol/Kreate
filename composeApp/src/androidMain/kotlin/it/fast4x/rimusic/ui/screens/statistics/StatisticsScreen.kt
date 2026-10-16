package it.fast4x.rimusic.ui.screens.statistics

import it.fast4x.rimusic.ui.components.ButtonsRow
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

    // A page of its own: "Statistiques" in the bar, the periods as pills on top
    // and the normal bottom bar. The periods used to replace the bottom bar,
    // where they read as navigation ("1 An" was cut, "Tout" off screen)
    app.kreate.android.themed.common.component.DetailSkeleton(
        navController = navController,
        miniPlayer = miniPlayer,
        title = {
            androidx.compose.foundation.text.BasicText(
                text = stringResource( R.string.statistics ),
                style = typography().l.semiBold.color( colorPalette().text )
            )
        }
    ) {
        androidx.compose.foundation.layout.Column {
            ButtonsRow(
                chips = listOf(
                    0 to stringResource( R.string.stats_period_today ),
                    1 to stringResource( R.string.stats_period_week ),
                    2 to stringResource( R.string.stats_period_month ),
                    3 to stringResource( R.string.stats_period_3_months ),
                    4 to stringResource( R.string.stats_period_6_months ),
                    5 to stringResource( R.string.stats_period_year ),
                    6 to stringResource( R.string.all )
                ),
                currentValue = tabIndex,
                onValueUpdate = onTabIndexChanged,
                modifier = androidx.compose.ui.Modifier.padding( bottom = 4.dp )
            )
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
