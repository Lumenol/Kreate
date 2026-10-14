package it.fast4x.innertube.utils

/**
 * Card subtitles end with the year only sometimes ("Album • 2024"),
 * often with the artist ("Single • BEN plg"), which then showed twice.
 *
 * @return this text if it's a year, `null` otherwise
 */
fun String?.asYear(): String? = this?.trim()?.takeIf { it.length == 4 && it.all( Char::isDigit ) }
