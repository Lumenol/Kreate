package app.kreate.util

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


/**
 * Matches:
 * - dd:hh:mm:ss
 * - hh:mm:ss
 * - mm:ss
 * - ss
 *
 * Single or double-digits are accepted
 */
val DURATION_FORMAT_REGEX = Regex("^\\d+(?::\\d+){0,3}$")

// These are multiplier that turns current unit to seconds
private val TIME_UNITS = arrayOf(1, 60, 3600, 86400)

private fun String.isDigitOnly(): Boolean = Regex("^\\d+").matches( this )

/**
 * Converts current string into [Duration].
 *
 * Returns [Duration.ZERO] if blank,
 * or format is invalid.
 *
 * @return [Duration] instance from [String], [Duration.ZERO] if [String] is `null`
 */
fun String?.toDuration(): Duration {
    if( this.isNullOrBlank() )
        // No data to convert, result to 0
        return Duration.ZERO
    else if( this.isDigitOnly() )
        // Assume it's seconds if only digits present
        return this.toLong().toDuration( DurationUnit.SECONDS )
    else if( DURATION_FORMAT_REGEX.matches( this ) ) {
        var totalSeconds = 0L

        val parts = this.split(":").reversed()
        for( i in 0..parts.lastIndex ) {
            val p = parts[i]

            // Requires all parts to be digits only
            // This also eliminates negative numbers
            if( !p.isDigitOnly() ) return Duration.ZERO

            val value = p.toInt()
            if( (i < 2 && value > 59)
                || (i == 2 && value > 23)
            )
                // Second [0], minute [1] are ranged from 0-59
                // while hour [2] is ranged from 0-23
                return Duration.ZERO

            totalSeconds += value * TIME_UNITS[i]
        }

        return totalSeconds.toDuration( DurationUnit.SECONDS )
    } else
        return Duration.ZERO
}

/**
 * Convert [Duration] to human-readable text
 *
 * Examples:
 * - 128:20:06:28
 * - 7:32:01
 * - 1:02
 *
 * The first unit has no leading zero, the others always 2 digits.
 */
fun Duration.readableText(): String {
    // The first unit as is, the next ones on 2 digits: "1:02", "2:03:35", like
    // the song lists (it was "01:02", "02:03:35")
    val parts = mutableListOf<Long>()
    this.toComponents { days, hours, minutes, seconds, _ ->
        if( days > 0 ) parts.add( days )
        if( hours > 0 || days > 0 ) parts.add( hours.toLong() )
        parts.add( minutes.toLong() )
        parts.add( seconds.toLong() )
    }
    return parts.mapIndexed { i, v -> if( i == 0 ) "$v" else "%02d".format( v ) }
                .joinToString( ":" )
}