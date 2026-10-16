package app.kreate.android.themed.rimusic.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.kreate.android.R
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.utils.color
import it.fast4x.rimusic.utils.semiBold
import kotlin.math.roundToInt

/** MOCKUP ONLY (v3): library filters, nothing is filtered */
object LibraryFiltersMock {

    /** A filter over an ordered scale: quick values, or any range with the slider */
    class RangeFilter(
        val title: String,
        val labels: List<String>,
        val presets: List<Pair<String, IntRange>>,
        private val phrase: (lo: String?, hi: String?) -> String,    // null = open end
        expanded: Boolean
    ) {
        val last = labels.lastIndex
        var range by mutableStateOf( 0..last )
        var expanded by mutableStateOf( expanded )
        val isActive get() = range != 0..last
        fun reset() { range = 0..last }

        fun summary(): String = when {
            !isActive                 -> "Tout"
            range.first == 0          -> phrase( null, labels[range.last] )
            range.last == last        -> phrase( labels[range.first], null )
            else                      -> phrase( labels[range.first], labels[range.last] )
        }
    }

    private val ago = listOf( "aujourd'hui", "1 semaine", "2 semaines", "1 mois", "2 mois", "3 mois", "4 mois", "5 mois", "6 mois", "9 mois", "1 an", "2 ans", "3 ans" )
    private fun agoPhrase( lo: String?, hi: String? ) = when {
        lo == null -> "il y a moins de $hi"
        hi == null -> "il y a plus de $lo"
        lo == hi   -> "il y a $lo"
        else       -> "il y a ${lo.substringBefore( ' ' )} à $hi".let { if( lo.substringAfter( ' ' ) != hi.substringAfter( ' ' ) ) "il y a $lo à $hi" else it }
    }
    private val years = (1960..2026).map { it.toString() }
    private fun y( from: Int, to: Int ) = (from - 1960)..(to - 1960)

    val plays = RangeFilter( "Nombre d'écoutes",
        listOf( "0", "1", "2", "3", "5", "10", "20", "50", "100" ),
        listOf( "Jamais" to 0..0, "1 fois" to 1..1, "5 et +" to 4..8, "20 et +" to 6..8 ),
        { lo, hi -> when {
            lo == null && hi == "0" -> "jamais écoutés"
            lo == null -> "$hi écoutes ou moins"
            hi == null -> "$lo écoutes et +"
            lo == hi -> if( lo == "1" ) "1 écoute" else "$lo écoutes"
            else -> "$lo à $hi écoutes"
        } },
        expanded = true )
    val lastPlayed = RangeFilter( "Dernière écoute", ago,
        listOf( "< 1 semaine" to 0..1, "< 1 mois" to 0..3, "> 6 mois" to 8..12, "> 1 an" to 10..12 ),
        ::agoPhrase, expanded = true )
    val firstPlayed = RangeFilter( "Première écoute", ago,
        listOf( "< 1 semaine" to 0..1, "< 1 mois" to 0..3, "< 1 an" to 0..10 ),
        ::agoPhrase, expanded = false )
    val released = RangeFilter( "Première diffusion", years,
        listOf( "2026" to y( 2026, 2026 ), "Années 2020" to y( 2020, 2026 ), "Années 2010" to y( 2010, 2019 ), "Années 2000" to y( 2000, 2009 ), "Avant 2000" to y( 1960, 1999 ) ),
        { lo, hi -> when { lo == null -> "jusqu'à $hi"; hi == null -> "depuis $lo"; lo == hi -> "en $lo"; else -> "de $lo à $hi" } },
        expanded = false )
    val views = RangeFilter( "Popularité", listOf( "0", "10 k", "100 k", "1 M", "10 M", "100 M", "1 Md" ),
        listOf( "< 1 M vues" to 0..3, "> 10 M vues" to 4..6, "> 100 M vues" to 5..6 ),
        { lo, hi -> when { lo == null -> "moins de $hi vues"; hi == null -> "plus de $lo vues"; else -> "$lo à $hi vues" } },
        expanded = false )
    private val ranges = listOf( plays, lastPlayed, firstPlayed, released, views )

    /** Where the song is: any of the ticked ones (OR) */
    val sources: SnapshotStateList<String> = mutableStateListOf()
    private val sourceOptions = listOf( "Téléchargés", "En cache", "Sur l'appareil" )
    var likedOnly by mutableStateOf( false )
    /** 0 = all, 1 = without, 2 = only */
    var explicit by mutableStateOf( 0 )

    private class Shortcut( val label: String, val apply: () -> Unit, val matches: () -> Boolean )
    private val shortcuts = listOf(
        Shortcut( "Découvertes du mois", { clear(); firstPlayed.range = 0..3; firstPlayed.expanded = true }, { firstPlayed.range == 0..3 } ),
        Shortcut( "Aimés mais oubliés", { clear(); likedOnly = true; lastPlayed.range = 8..12 }, { likedOnly && lastPlayed.range == 8..12 } ),
        Shortcut( "Pépites", { clear(); views.range = 0..3; views.expanded = true }, { views.range == 0..3 } )
    )

    init {
        // The mockup's example: listened 5 to 20 times, last time 3 to 5 months ago
        plays.range = 4..6
        lastPlayed.range = 5..7
    }

    val activeCount get() = ranges.count { it.isActive } + sources.size + (if( likedOnly ) 1 else 0) + (if( explicit != 0 ) 1 else 0)
    fun clear() { ranges.forEach { it.reset() }; sources.clear(); likedOnly = false; explicit = 0 }

    @Composable
    private fun Chip( text: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit ) {
        val shape = RoundedCornerShape( 50 )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.alpha( if( enabled ) 1f else 0.4f )
                               .clip( shape )
                               .background( if( selected ) colorPalette().accent else colorPalette().background2, shape )
                               .border( 1.dp, if( selected ) colorPalette().accent else colorPalette().background3, shape )
                               .clickable( enabled = enabled, onClick = onClick )
                               .padding( horizontal = 14.dp, vertical = 8.dp )
        ) {
            if( selected ) {
                Icon( painterResource( R.drawable.checkmark ), null, tint = colorPalette().background0, modifier = Modifier.size( 14.dp ) )
                Spacer( Modifier.width( 6.dp ) )
            }
            BasicText( text, style = typography().xs.semiBold.color( if( selected ) colorPalette().background0 else colorPalette().text ) )
        }
    }

    @Composable
    private fun SectionTitle( title: String, value: String?, modifier: Modifier = Modifier, chevron: Boolean? = null ) =
        Row( verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth() ) {
            BasicText( title.uppercase(), style = typography().xxs.semiBold.color( colorPalette().textSecondary ), modifier = Modifier.weight( 1f ) )
            if( value != null )
                BasicText( value, style = typography().xs.semiBold.color( if( value == "Tout" ) colorPalette().textSecondary else colorPalette().accent ) )
            if( chevron != null ) {
                Spacer( Modifier.width( 6.dp ) )
                BasicText( if( chevron ) "⌄" else "›", style = typography().s.semiBold.color( colorPalette().textSecondary ) )
            }
        }

    @OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
    @Composable
    private fun Range( filter: RangeFilter, enabled: Boolean = true ) {
        Column( verticalArrangement = Arrangement.spacedBy( 6.dp ), modifier = Modifier.alpha( if( enabled ) 1f else 0.4f ) ) {
            SectionTitle(
                filter.title, if( enabled ) filter.summary() else "sans objet",
                Modifier.clickable { filter.expanded = !filter.expanded }.padding( vertical = 6.dp ),
                chevron = filter.expanded
            )
            if( !filter.expanded ) return@Column
            FlowRow( horizontalArrangement = Arrangement.spacedBy( 8.dp ), verticalArrangement = Arrangement.spacedBy( 8.dp ) ) {
                filter.presets.forEach { (label, range) ->
                    Chip( label, filter.range == range, enabled ) {
                        filter.range = if( filter.range == range ) 0..filter.last else range
                    }
                }
            }
            val on = filter.isActive
            RangeSlider(
                value = filter.range.first.toFloat()..filter.range.last.toFloat(),
                onValueChange = { filter.range = it.start.roundToInt()..it.endInclusive.roundToInt() },
                valueRange = 0f..filter.last.toFloat(),
                steps = (filter.last - 1).coerceAtLeast( 0 ),
                enabled = enabled,
                // Grey while it lets everything through, green once it filters; no tick dots
                colors = SliderDefaults.colors(
                    thumbColor = if( on ) colorPalette().accent else colorPalette().textSecondary,
                    activeTrackColor = if( on ) colorPalette().accent else colorPalette().textSecondary.copy( alpha = 0.45f ),
                    inactiveTrackColor = colorPalette().background3,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                )
            )
            Row( Modifier.fillMaxWidth() ) {
                BasicText( filter.labels.first(), style = typography().xxs.color( colorPalette().textSecondary ), modifier = Modifier.weight( 1f ) )
                BasicText( filter.labels.last() + " +", style = typography().xxs.color( colorPalette().textSecondary ) )
            }
        }
    }

    @Composable
    private fun Segmented( options: List<String>, selected: Int, onSelect: (Int) -> Unit ) {
        val shape = RoundedCornerShape( 50 )
        Row( Modifier.clip( shape ).background( colorPalette().background2 ).border( 1.dp, colorPalette().background3, shape ).padding( 3.dp ) ) {
            options.forEachIndexed { i, label ->
                BasicText(
                    label,
                    style = typography().xs.semiBold.color( if( i == selected ) colorPalette().background0 else colorPalette().text ),
                    modifier = Modifier.clip( shape )
                                       .background( if( i == selected ) ( if( i == 0 ) colorPalette().textSecondary else colorPalette().accent ) else Color.Transparent )
                                       .clickable { onSelect( i ) }
                                       .padding( horizontal = 14.dp, vertical = 7.dp )
                )
            }
        }
    }

    /** The sheet opened by the filter button */
    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun Sheet( onDone: () -> Unit ) {
        Column(
            Modifier.padding( top = 48.dp )
                    .fillMaxWidth()
                    .background( colorPalette().background1, RoundedCornerShape( topStart = 20.dp, topEnd = 20.dp ) )
                    .navigationBarsPadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding( start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp )
            ) {
                BasicText( "Filtres", style = typography().l.semiBold.color( colorPalette().text ), modifier = Modifier.weight( 1f ) )
                val any = activeCount > 0
                BasicText(
                    "Tout effacer",
                    style = typography().xs.semiBold.color( if( any ) colorPalette().accent else colorPalette().textSecondary ),
                    modifier = Modifier.alpha( if( any ) 1f else 0.5f ).clickable( enabled = any ) { clear() }.padding( 8.dp )
                )
            }
            // Shortcuts: one scrolling line, lit while the filters match them
            Row(
                horizontalArrangement = Arrangement.spacedBy( 8.dp ),
                modifier = Modifier.horizontalScroll( rememberScrollState() ).padding( horizontal = 20.dp, vertical = 6.dp )
            ) {
                shortcuts.forEach { Chip( it.label, it.matches() ) { it.apply() } }
            }
            Box( Modifier.weight( 1f, false ) ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy( 18.dp ),
                    modifier = Modifier.verticalScroll( rememberScrollState() )
                                       .padding( start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp )
                ) {
                    Column( verticalArrangement = Arrangement.spacedBy( 8.dp ) ) {
                        SectionTitle( "Source", if( sources.isEmpty() ) "Tout" else sources.joinToString( " ou " ) )
                        FlowRow( horizontalArrangement = Arrangement.spacedBy( 8.dp ), verticalArrangement = Arrangement.spacedBy( 8.dp ) ) {
                            sourceOptions.forEach { t -> Chip( t, t in sources ) { if( t in sources ) sources.remove( t ) else sources.add( t ) } }
                        }
                    }
                    Row( verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy( 12.dp ) ) {
                        Chip( "♥ Aimés seulement", likedOnly ) { likedOnly = !likedOnly }
                        Spacer( Modifier.weight( 1f ) )
                    }
                    Row( verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth() ) {
                        BasicText( "EXPLICITES", style = typography().xxs.semiBold.color( colorPalette().textSecondary ), modifier = Modifier.weight( 1f ) )
                        Segmented( listOf( "Tous", "Sans", "Seulement" ), explicit ) { explicit = it }
                    }
                    Range( plays )
                    val heard = plays.range != 0..0
                    Range( lastPlayed, heard )
                    Range( firstPlayed, heard )
                    Range( released )
                    Range( views )
                }
                // Fades the list out above the button
                Box(
                    Modifier.align( Alignment.BottomCenter ).fillMaxWidth().height( 28.dp )
                            .background( Brush.verticalGradient( listOf( Color.Transparent, colorPalette().background1 ) ) )
                )
            }
            BasicText(
                text = "Afficher 42 titres",
                style = typography().s.semiBold.color( colorPalette().background0 ).copy( textAlign = TextAlign.Center ),
                modifier = Modifier.padding( start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp )
                                   .fillMaxWidth()
                                   .clip( RoundedCornerShape( 50 ) )
                                   .background( colorPalette().text )
                                   .clickable( onClick = onDone )
                                   .padding( vertical = 14.dp )
            )
        }
    }

    /** The toolbar's filter button: green with the number of active filters */
    @Composable
    fun FilterButton( onClick: () -> Unit ) {
        val count = activeCount
        Box {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size( 36.dp )
                                   .clip( CircleShape )
                                   .background( if( count > 0 ) colorPalette().accent else colorPalette().background2 )
                                   .clickable( onClick = onClick )
            ) {
                Icon( painterResource( R.drawable.filter ), "Filtres", tint = if( count > 0 ) colorPalette().background0 else colorPalette().text, modifier = Modifier.size( 18.dp ) )
            }
            if( count > 0 )
                BasicText(
                    count.toString(),
                    style = typography().xxs.semiBold.color( colorPalette().accent ).copy( textAlign = TextAlign.Center ),
                    modifier = Modifier.align( Alignment.TopEnd ).offset( x = 4.dp, y = (-4).dp )
                                       .size( 16.dp ).clip( CircleShape ).background( colorPalette().background0 )
                )
        }
    }

    /** Active filters under the toolbar: one scrolling line, each removable */
    @Composable
    fun ActiveRow() {
        val pills = sources.map { it to { sources.remove( it ); Unit } } +
                    listOfNotNull(
                        ( "Aimés" to { likedOnly = false } ).takeIf { likedOnly },
                        ( (if( explicit == 1 ) "Sans explicites" else "Explicites seulement") to { explicit = 0 } ).takeIf { explicit != 0 }
                    ) +
                    ranges.filter { it.isActive }.map { "${it.title} : ${it.summary()}" to { it.reset() } }
        if( pills.isEmpty() ) return
        Row(
            horizontalArrangement = Arrangement.spacedBy( 8.dp ),
            modifier = Modifier.horizontalScroll( rememberScrollState() ).padding( horizontal = 12.dp ).padding( bottom = 8.dp )
        ) {
            pills.forEach { (label, remove) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip( CircleShape )
                                       .background( colorPalette().accent.copy( alpha = 0.18f ) )
                                       .clickable { remove() }
                                       .padding( start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp )
                ) {
                    BasicText( label, style = typography().xs.semiBold.color( colorPalette().accent ) )
                    Spacer( Modifier.width( 6.dp ) )
                    Icon( painterResource( R.drawable.close ), null, tint = colorPalette().accent, modifier = Modifier.size( 12.dp ) )
                }
            }
        }
    }
}
