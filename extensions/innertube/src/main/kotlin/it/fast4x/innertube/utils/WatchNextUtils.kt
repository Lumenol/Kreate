package it.fast4x.innertube.utils

import it.fast4x.innertube.models.MusicCarouselShelfRenderer
import it.fast4x.innertube.models.NextResponse
import it.fast4x.innertube.models.SectionListRenderer

const val LYRICS_TAB_PREFIX = "MPLY"
const val RELATED_TAB_PREFIX = "MPTR"

/**
 * @return browse id of the watch-next tab whose id starts with [prefix]
 * ([LYRICS_TAB_PREFIX], [RELATED_TAB_PREFIX]).
 *
 * Tabs can't be picked by position: YouTube inserted a "Comments" tab
 * before "Related", which moved it from index 2 to 3.
 */
fun NextResponse.watchNextTabBrowseId( prefix: String ): String? =
    contents
        ?.singleColumnMusicWatchNextResultsRenderer
        ?.tabbedRenderer
        ?.watchNextTabbedResultsRenderer
        ?.tabs
        ?.firstNotNullOfOrNull { tab ->
            tab.tabRenderer?.endpoint?.browseEndpoint?.browseId?.takeIf { it.startsWith( prefix ) }
        }

/**
 * @return first carousel matching [predicate].
 *
 * Use this rather than matching section titles: they're localized
 * ("You might also like" is "Découvrez également" in French).
 */
fun SectionListRenderer?.findCarousel( predicate: (MusicCarouselShelfRenderer) -> Boolean ): MusicCarouselShelfRenderer? =
    this?.contents?.firstNotNullOfOrNull { it.musicCarouselShelfRenderer?.takeIf( predicate ) }

/** Carousel of songs ("You might also like") */
fun MusicCarouselShelfRenderer.hasSongs(): Boolean =
    contents.any { it.musicResponsiveListItemRenderer != null }
