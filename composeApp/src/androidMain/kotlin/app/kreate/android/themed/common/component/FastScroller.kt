package app.kreate.android.themed.common.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.utils.color
import it.fast4x.rimusic.utils.semiBold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Below this, scrolling by hand is quick enough */
private const val MIN_ITEMS = 40

/**
 * Thumb on the right edge of a long list: shown while it scrolls, drag it to
 * jump anywhere. While dragging, a bubble shows [label] of the item under the
 * thumb (its first letter when the list is sorted by name), none if null.
 */
@Composable
fun BoxScope.FastScroller( state: LazyListState, label: (Int) -> String? = { null } ) =
    FastScroller(
        firstVisible = { state.firstVisibleItemIndex },
        total = { state.layoutInfo.totalItemsCount },
        visible = { state.layoutInfo.visibleItemsInfo.size },
        isScrolling = { state.isScrollInProgress },
        scrollTo = state::scrollToItem,
        label = label
    )

@Composable
fun BoxScope.FastScroller( state: LazyGridState, label: (Int) -> String? = { null } ) =
    FastScroller(
        firstVisible = { state.firstVisibleItemIndex },
        total = { state.layoutInfo.totalItemsCount },
        visible = { state.layoutInfo.visibleItemsInfo.size },
        isScrolling = { state.isScrollInProgress },
        scrollTo = state::scrollToItem,
        label = label
    )

/** First letter of [text] for the bubble, "#" for anything else */
fun initialOf( text: String? ): String {
    val first = text?.trim()?.firstOrNull() ?: return "#"
    return if( first.isLetter() ) first.uppercase() else "#"
}

@Composable
private fun BoxScope.FastScroller(
    firstVisible: () -> Int,
    total: () -> Int,
    visible: () -> Int,
    isScrolling: () -> Boolean,
    scrollTo: suspend (Int) -> Unit,
    label: (Int) -> String?
) {
    // Many items, or few but big ones (mood tiles): at least 3 screens long
    val isLong by remember { derivedStateOf { total() >= MIN_ITEMS || (visible() > 0 && total() >= visible() * 3) } }
    if( !isLong ) return

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var isShown by remember { mutableStateOf(false) }

    val scrolling by remember { derivedStateOf( isScrolling ) }
    LaunchedEffect( scrolling, isDragging ) {
        if( scrolling || isDragging )
            isShown = true
        else {
            delay( 1_500 )
            isShown = false
        }
    }

    val listFraction by remember {
        derivedStateOf {
            val range = (total() - visible()).coerceAtLeast( 1 )
            (firstVisible().toFloat() / range).coerceIn( 0f, 1f )
        }
    }

    // Takes touches only while the thumb shows: the strip lies over the list's
    // right edge (drag handles, swipes, buttons), which must keep working
    val dragArea = if( isShown ) Modifier.pointerInput( Unit ) {
                    fun jumpTo( y: Float ) {
                        dragFraction = (y / size.height).coerceIn( 0f, 1f )
                        val index = (dragFraction * (total() - 1)).roundToInt()
                        scope.launch { scrollTo( index ) }
                    }
                    detectVerticalDragGestures(
                        onDragStart = { isDragging = true; jumpTo( it.y ) },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false }
                    ) { change, _ -> jumpTo( change.position.y ) }
                } else Modifier

    BoxWithConstraints(
        Modifier.align( Alignment.CenterEnd )
                .fillMaxHeight()
                .padding( vertical = 8.dp )
                .width( 24.dp )
                .then( dragArea )
    ) {
        val thumbHeight = 48.dp
        val travel = with( density ) { (maxHeight - thumbHeight).toPx() }
        val fraction = if( isDragging ) dragFraction else listFraction
        val thumbY = (fraction * travel).roundToInt()

        AnimatedVisibility(
            visible = isShown,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align( Alignment.TopEnd )
        ) {
            Box( Modifier.offset { IntOffset( 0, thumbY ) } ) {
                Box(
                    Modifier.padding( end = 4.dp )
                            .size( width = if( isDragging ) 8.dp else 5.dp, height = thumbHeight )
                            .clip( RoundedCornerShape( 4.dp ) )
                            .background( colorPalette().text.copy( alpha = if( isDragging ) .9f else .45f ) )
                )
            }
        }

        val bubble = if( isDragging ) label( (dragFraction * (total() - 1)).roundToInt() ) else null
        // Left of the durations and download icons, so it hides no text that matters
        if( bubble != null )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.offset { IntOffset( -with( density ) { 128.dp.roundToPx() }, thumbY - with( density ) { 4.dp.roundToPx() } ) }
                                   .size( 56.dp )
                                   .clip( CircleShape )
                                   .background( colorPalette().text )
            ) {
                BasicText(
                    text = bubble,
                    style = typography().xl.semiBold.color( colorPalette().background0 ).copy( fontSize = 24.sp )
                )
            }
    }
}
