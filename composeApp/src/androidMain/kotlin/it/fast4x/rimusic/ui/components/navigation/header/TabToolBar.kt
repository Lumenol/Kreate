package it.fast4x.rimusic.ui.components.navigation.header

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.ui.components.tab.toolbar.Button
import it.fast4x.rimusic.ui.components.tab.toolbar.EllipsisMenuComponent
import it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import it.fast4x.rimusic.ui.components.tab.toolbar.MenuHeader
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.BasicText
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.utils.color
import it.fast4x.rimusic.utils.semiBold

object TabToolBar {

    val TOOLBAR_ICON_SIZE = 32.dp
    val HORIZONTAL_PADDING = 12.dp
    val VERTICAL_PADDING = 4.dp

    @Composable
    fun Buttons(
        buttons: List<Button>,
        horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceEvenly,
        verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
        modifier: Modifier = Modifier
    ) {
        val density = LocalDensity.current.density
        var availableWidth by remember { mutableStateOf(0.dp) }

        /*
            `15.dp` is a magic number used to approximate the spacing
            between each item displayed inside `Row` composable.

            Why?
            A 660.dp screen can display up to 10 items without spacing.
            But since [Arrangement.SpaceEvenly] is set, only 7 items
            can be displayed, any other item will be hidden.

            TODO: Implement a more accurate/efficient mathematical equation to calculate spacing
        */
        val sizeWithSpacing = TOOLBAR_ICON_SIZE + 15.dp
        var canDisplay by remember { mutableIntStateOf(0) }
        var isClustered by remember { mutableStateOf(false) }

        LaunchedEffect( availableWidth ) {
            canDisplay = (availableWidth / sizeWithSpacing).toInt()
            isClustered = buttons.size > canDisplay
        }

        val ellipsisMenu = EllipsisMenuComponent.init {
            buttons.takeLast(
                /*
                 * Take what isn't displayed, or 0 if [canDisplay]
                 * is equal or larger than [button]'s size.
                 *
                 * Ellipsis menu will replaces last icon with its icon,
                 * therefore, `1` is added to include that last icon
                 * back to the menu
                 */
                (buttons.size - canDisplay + 1).coerceAtLeast( 0 )
            )
        }

        Row(
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            modifier = modifier.fillMaxWidth()
                               .padding( HORIZONTAL_PADDING, VERTICAL_PADDING )
                               .onGloballyPositioned {
                                   // [it.size.width] returns size in px
                                   val widthDp = it.size.width / density
                                   availableWidth = widthDp.dp - (HORIZONTAL_PADDING * 2)
                               }
        ) {
            // Wait until [availableWidth] is set and [canDisplay]
            // is properly calculated before showing
            if( canDisplay == 0 ) return@Row

            buttons.take(
                if( isClustered )
                // Last item is reserved for ellipsis menu's icon
                    canDisplay - 1
                else
                    buttons.size
            ).forEach { it.ToolBarButton() }

            if( isClustered )
                ellipsisMenu.ToolBarButton()
        }
    }

    /**
     * Toolbar of the redesigned Library: [leading] content (main actions, count)
     * then a "⋮" menu listing every other action with its name, instead of a
     * row of 5 to 10 unlabeled icons. Buttons that can't be menu items stay icons.
     */
    @Composable
    fun Compact(
        buttons: List<Button>,
        modifier: Modifier = Modifier,
        trailing: @Composable RowScope.() -> Unit = {},
        leading: @Composable RowScope.() -> Unit = {}
    ) {
        // Headings go with the menu entries, never in the toolbar
        val menuItems = buttons.filter { it is MenuIcon || it is MenuHeader }
            .takeIf { list -> list.any { it is MenuIcon } }.orEmpty()
        val ellipsisMenu = EllipsisMenuComponent.init { menuItems }

        Row(
            horizontalArrangement = Arrangement.spacedBy( 8.dp ),
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.fillMaxWidth()
                               .padding( start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp )
        ) {
            // The count and filter pills take what's left and scroll sideways if they
            // must: the buttons and "⋮" on the right always stay on screen
            Row(
                horizontalArrangement = Arrangement.spacedBy( 8.dp ),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight( 1f )
                                   .horizontalScroll( androidx.compose.foundation.rememberScrollState() ),
                content = leading
            )
            trailing()
            buttons.filterNot { it is MenuIcon || it is MenuHeader }.forEach { it.ToolBarButton() }
            if( menuItems.isNotEmpty() )
                ellipsisMenu.ToolBarButton()
        }
    }

    /**
     * One pill for a list's filters ("All ▾"): it opens the choices instead of a
     * whole row of chips, so the top of the Library takes 2 rows instead of 4.
     */
    @Composable
    fun <E> FilterPill(
        options: List<E>,
        current: E,
        label: @Composable ( E ) -> String,
        onSelect: ( E ) -> Unit,
        // A secondary filter shows only its icon, so two pills fit next to the count
        iconOf: (( E ) -> Int)? = null,
        modifier: Modifier = Modifier
    ) {
        var expanded by remember { mutableStateOf( false ) }
        Box( modifier ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy( 4.dp ),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height( 36.dp )
                                   .clip( CircleShape )
                                   .background( colorPalette().background2 )
                                   // Announced as a button that opens the choices
                                   .clickable( role = androidx.compose.ui.semantics.Role.Button ) { expanded = true }
                                   .padding( start = if( iconOf != null ) 10.dp else 14.dp, end = 10.dp )
            ) {
                if( iconOf != null )
                    androidx.compose.material3.Icon(
                        painter = painterResource( iconOf( current ) ),
                        contentDescription = label( current ),
                        tint = colorPalette().text,
                        modifier = Modifier.size( 18.dp )
                    )
                else
                    BasicText(
                        text = label( current ),
                        style = typography().xs.semiBold.color( colorPalette().text ),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        // Shrinks before the chevron when the width is capped
                        modifier = Modifier.weight( 1f, fill = false )
                    )
                androidx.compose.material3.Icon(
                    painter = painterResource( app.kreate.android.R.drawable.chevron_down ),
                    contentDescription = null,
                    tint = colorPalette().text,
                    modifier = Modifier.size( 16.dp )
                )
            }
            androidx.compose.material3.DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = colorPalette().background1
            ) {
                options.forEach { option ->
                    androidx.compose.material3.DropdownMenuItem(
                        leadingIcon = iconOf?.let { f -> {
                            androidx.compose.material3.Icon(
                                painter = painterResource( f( option ) ),
                                contentDescription = null,
                                tint = if( option == current ) colorPalette().accent else colorPalette().text,
                                modifier = Modifier.size( 20.dp )
                            )
                        } },
                        text = {
                            BasicText(
                                text = label( option ),
                                style = typography().s.semiBold.color(
                                    if( option == current ) colorPalette().accent else colorPalette().text
                                )
                            )
                        },
                        onClick = {
                            expanded = false
                            onSelect( option )
                        }
                    )
                }
            }
        }
    }

    /** Round icon button next to [FilterPill] ("Play all", "Shuffle") */
    @Composable
    fun IconPill(
        iconId: Int,
        description: String,
        filled: Boolean,
        onClick: () -> Unit
    ) =
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size( 36.dp )
                               .clip( CircleShape )
                               .background( if( filled ) colorPalette().text else colorPalette().background2 )
                               .clickable( onClick = onClick )
        ) {
            androidx.compose.material3.Icon(
                painter = painterResource( iconId ),
                contentDescription = description,
                tint = if( filled ) colorPalette().background0 else colorPalette().text,
                modifier = Modifier.size( 18.dp )
            )
        }

    /** Pill button for the main actions of [Compact] ("Play all", "Shuffle") */
    @Composable
    fun PillButton(
        text: String,
        iconId: Int,
        filled: Boolean,
        onClick: () -> Unit
    ) =
        Row(
            horizontalArrangement = Arrangement.spacedBy( 6.dp ),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height( 36.dp )
                               .clip( CircleShape )
                               .background( if( filled ) colorPalette().text else colorPalette().background2 )
                               .clickable( onClick = onClick )
                               .padding( horizontal = 14.dp )
        ) {
            val content = if( filled ) colorPalette().background0 else colorPalette().text
            androidx.compose.material3.Icon(
                painter = painterResource( iconId ),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size( 16.dp )
            )
            BasicText(
                text = text,
                style = typography().xs.semiBold.color( content ),
                maxLines = 1
            )
        }

    @Composable
    fun Buttons(
        vararg buttons: Button,
        horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceEvenly,
        verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
        modifier: Modifier = Modifier
    ) = Buttons( listOf( *buttons ), horizontalArrangement, verticalAlignment, modifier )

    @Composable
    fun Icon(
        icon: Painter,
        tint: Color = colorPalette().text,
        size: Dp = TOOLBAR_ICON_SIZE,
        enabled: Boolean = true,
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {}
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled
        ) {
            androidx.compose.material3.Icon(
                painter = icon,
                null,
                modifier.size( size )
                        .padding( horizontal = 4.dp ),
                tint
            )
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Icon(
        icon: Painter,
        tint: Color = colorPalette().text,
        size: Dp = TOOLBAR_ICON_SIZE,
        enabled: Boolean = true,
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {},
        onLongClick: () -> Unit = {}
    ) {
        Icon(
            icon,
            tint,
            size,
            enabled,
            modifier.combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        )
    }

    @Composable
    fun Icon(
        @DrawableRes iconId: Int,
        tint: Color = colorPalette().text,
        size: Dp = TOOLBAR_ICON_SIZE,
        enabled: Boolean = true,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled
        ) {
            androidx.compose.material3.Icon(
                painter = painterResource( iconId ),
                tint = tint,
                contentDescription = null,
                modifier = modifier
                    .size(size)
                    .padding(horizontal = 4.dp)
            )
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Icon(
        @DrawableRes iconId: Int,
        tint: Color = colorPalette().text,
        size: Dp = TOOLBAR_ICON_SIZE,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        onShortClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        Icon(
            iconId,
            tint,
            size,
            enabled,
            modifier.combinedClickable (
                onClick = onShortClick,
                onLongClick = onLongClick
            )
        ) { }
    }

    @Composable
    fun Toggleable(
        @DrawableRes onIconId: Int,
        @DrawableRes offIconId: Int,
        toggleCondition: Boolean,
        tint: Color = colorPalette().text,
        size: Dp = TOOLBAR_ICON_SIZE,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        Icon(
            iconId = if( toggleCondition ) onIconId else offIconId,
            tint = tint,
            size = size,
            modifier = modifier,
            onClick = onClick
        )
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Toggleable(
        @DrawableRes onIconId: Int,
        @DrawableRes offIconId: Int,
        toggleCondition: Boolean,
        tint: Color = colorPalette().text,
        size: Dp = TOOLBAR_ICON_SIZE,
        modifier: Modifier = Modifier,
        onShortClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        Toggleable(
            onIconId,
            offIconId,
            toggleCondition,
            tint,
            size,
            modifier.combinedClickable (
                onClick = onShortClick,
                onLongClick = onLongClick
            )
        ) { }
    }

    @Composable
    fun Toggleable(
        @DrawableRes iconId: Int,
        tintOn: Color = colorPalette().text,
        tintOff: Color = colorPalette().textDisabled,
        toggleCondition: Boolean,
        enabled: Boolean,
        size: Dp = TOOLBAR_ICON_SIZE,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        Icon(
            iconId = iconId,
            tint = if( toggleCondition ) tintOn else tintOff,
            size = size,
            enabled = enabled,
            modifier = modifier,
            onClick = onClick
        )
    }

    @ExperimentalFoundationApi
    @Composable
    fun Toggleable(
        @DrawableRes iconId: Int,
        tintOn: Color = colorPalette().text,
        tintOff: Color = colorPalette().textDisabled,
        toggleCondition: Boolean,
        enabled: Boolean,
        size: Dp = TOOLBAR_ICON_SIZE,
        modifier: Modifier = Modifier,
        onShortClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        Toggleable(
            iconId,
            tintOn,
            tintOff,
            toggleCondition,
            enabled,
            size,
            modifier.combinedClickable (
                onClick = onShortClick,
                onLongClick = onLongClick
            )
        ) { }
    }
}