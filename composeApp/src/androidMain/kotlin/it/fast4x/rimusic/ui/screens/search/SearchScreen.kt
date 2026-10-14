package it.fast4x.rimusic.ui.screens.search

import app.kreate.android.themed.common.component.DetailSkeleton
import it.fast4x.rimusic.ui.components.ButtonsRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.delay
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.R
import it.fast4x.compose.persist.PersistMapCleanup

@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun SearchScreen(
    navController: NavController,
    miniPlayer: @Composable () -> Unit = {},
    initialTextInput: String,
    onSearch: (String) -> Unit,
    onViewPlaylist: (String) -> Unit,
    onDismiss: (() -> Unit)? = null,
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    val (tabIndex, onTabChanged) = rememberSaveable {
        mutableStateOf(0)
    }

    val (textFieldValue, onTextFieldValueChanged) = rememberSaveable(
        initialTextInput,
        stateSaver = TextFieldValue.Saver
    ) {
        mutableStateOf(
            TextFieldValue(
                text = initialTextInput,
                selection = TextRange(initialTextInput.length)
            )
        )
    }

    PersistMapCleanup(tagPrefix = "search/")

    // The field now sits in the top bar, next to "back"; each mode keeps
    // its own list below. The link mode has a field of its own.
    val decorationBox: @Composable (@Composable () -> Unit) -> Unit = { it() }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect( tabIndex ) {
        if( tabIndex == 2 ) return@LaunchedEffect
        delay( 300 )
        focusRequester.requestFocus()
    }

    DetailSkeleton(
        navController = navController,
        miniPlayer = miniPlayer,
        title = {
            // The link mode has its own title and field
            if( tabIndex != 2 )
                SearchField(
                    value = textFieldValue,
                    onValueChange = onTextFieldValueChanged,
                    focusRequester = focusRequester,
                    onSearch = if( tabIndex == 0 ) {
                        { if( textFieldValue.text.isNotEmpty() ) onSearch( textFieldValue.text ) }
                    } else null
                )
        }
    ) {
        Column( Modifier.fillMaxSize() ) {
            // Used to be a bottom bar of its own
            ButtonsRow(
                chips = listOf(
                    0 to stringResource( R.string.online ),
                    1 to stringResource( R.string.library ),
                    2 to stringResource( R.string.go_to_link )
                ),
                currentValue = tabIndex,
                onValueUpdate = onTabChanged,
                modifier = Modifier.padding( bottom = 4.dp )
            )

            Box( Modifier.weight( 1f ) ) {
                saveableStateHolder.SaveableStateProvider(tabIndex) {
                    when (tabIndex) {
                        0 -> OnlineSearch(
                            navController = navController,
                            textFieldValue = textFieldValue,
                            onTextFieldValueChanged = onTextFieldValueChanged,
                            onSearch = onSearch,
                            decorationBox = decorationBox
                        )

                        1 -> LocalSongSearch(
                            navController = navController,
                            textFieldValue = textFieldValue,
                            onTextFieldValueChanged = onTextFieldValueChanged,
                            decorationBox = decorationBox,
                            onAction1 = { onTabChanged(0) },
                            onAction2 = { onTabChanged(1) },
                            onAction3 = { onTabChanged(2) },
                            onAction4 = {},
                        )

                        2 -> GoToLink(
                            navController = navController,
                            textFieldValue = textFieldValue,
                            onTextFieldValueChanged = onTextFieldValueChanged,
                            decorationBox = decorationBox,
                            onAction1 = { onTabChanged(0) },
                            onAction2 = { onTabChanged(1) },
                            onAction3 = { onTabChanged(2) },
                            onAction4 = {},
                        )
                    }
                }
            }
        }
    }
}
