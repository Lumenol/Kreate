package it.fast4x.rimusic.ui.screens.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.layout.ContentScale
import app.kreate.android.coil3.ImageFactory
import it.fast4x.rimusic.utils.semiBold
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.kreate.android.R
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.utils.color

/**
 * Rounded search field of the top bar: magnifier, text aligned to the start,
 * grey "clear" cross. [onSearch] null = no search action (library filter).
 */
@Composable
fun SearchField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    onSearch: (() -> Unit)?
) =
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = typography().m.color( colorPalette().text ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = if( onSearch != null ) ImeAction.Search else ImeAction.Done
        ),
        keyboardActions = KeyboardActions( onSearch = { onSearch?.invoke() } ),
        cursorBrush = SolidColor( colorPalette().text ),
        decorationBox = { innerTextField ->
            SearchPill(
                showClear = value.text.isNotEmpty(),
                onClear = { onValueChange( TextFieldValue() ) }
            ) {
                if( value.text.isEmpty() )
                    BasicText(
                        text = stringResource( R.string.word_search ),
                        style = typography().m.color( colorPalette().textSecondary ),
                        maxLines = 1
                    )
                innerTextField()
            }
        },
        modifier = Modifier.fillMaxWidth()
                           .padding( end = 12.dp )
                           .focusRequester( focusRequester )
    )

/** The query of a results page, shown like the field; tapping it edits the search */
@Composable
fun SearchQueryPill( query: String, onEdit: () -> Unit, onClear: () -> Unit ) =
    Box( Modifier.padding( end = 12.dp ).clip( CircleShape ).clickable( onClick = onEdit ) ) {
        SearchPill( showClear = true, onClear = onClear ) {
            BasicText(
                text = query,
                style = typography().m.color( colorPalette().text ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

@Composable
private fun SearchPill(
    showClear: Boolean,
    onClear: () -> Unit,
    text: @Composable () -> Unit
) =
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
                           .height( 44.dp )
                           .clip( CircleShape )
                           .background( colorPalette().background2 )
                           .padding( start = 16.dp, end = 4.dp )
    ) {
        Icon(
            painter = painterResource( R.drawable.search ),
            contentDescription = null,
            tint = colorPalette().textSecondary,
            modifier = Modifier.size( 18.dp )
        )
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.weight( 1f ).padding( horizontal = 10.dp )
        ) { text() }
        if( showClear )
            IconButton( onClick = onClear, modifier = Modifier.size( 40.dp ) ) {
                Icon(
                    painter = painterResource( R.drawable.close ),
                    contentDescription = stringResource( R.string.clear ),
                    tint = colorPalette().textSecondary,
                    modifier = Modifier.size( 18.dp )
                )
            }
    }

/** Artist as a list row (small round photo, name, detail) instead of a big circle */
@Composable
fun ArtistRow( name: String, thumbnailUrl: String?, detail: String, onClick: () -> Unit ) =
    Row(
        horizontalArrangement = Arrangement.spacedBy( 14.dp ),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
                           .clickable( onClick = onClick )
                           .padding( horizontal = 16.dp, vertical = 8.dp )
    ) {
        Image(
            painter = ImageFactory.rememberAsyncImagePainter( thumbnailUrl ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size( 48.dp )
                               .clip( CircleShape )
                               .background( colorPalette().background2 )
        )
        Column {
            BasicText(
                text = name,
                style = typography().s.semiBold.color( colorPalette().text ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            BasicText(
                text = detail,
                style = typography().xs.color( colorPalette().textSecondary ),
                maxLines = 1
            )
        }
    }
