package app.kreate.database.models

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * What YouTube says of a song beyond its title: when it was put online and how many
 * times it was viewed. For the library filters; kept apart from [Song] so the songs
 * table stays as upstream has it.
 */
@Immutable
@Entity(
    tableName = "song_youtube_info",
    foreignKeys = [
        ForeignKey(
            entity = Song::class,
            parentColumns = ["id"],
            childColumns = ["song_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SongYoutubeInfo(
    @PrimaryKey
    @ColumnInfo(name = "song_id")
    val songId: String,

    /** Put online on YouTube, epoch millis; `null` when YouTube didn't say */
    @ColumnInfo(name = "published_at")
    val publishedAt: Long? = null,

    @ColumnInfo(name = "view_count")
    val viewCount: Long? = null,

    /** When it was asked to YouTube: views change, they're asked again after a while */
    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Long
)
