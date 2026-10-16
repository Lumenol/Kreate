package app.kreate.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import app.kreate.database.ext.EventWithSong
import app.kreate.database.models.Album
import app.kreate.database.models.Artist
import app.kreate.database.models.Event
import app.kreate.database.models.PlaylistPreview
import app.kreate.database.models.Song
import app.kreate.database.table.DatabaseTable
import kotlinx.coroutines.flow.Flow

/** How a song was listened to, from the whole history: what the library filters read */
data class SongPlayStats(
    @ColumnInfo(name = "song_id") val songId: String,
    @ColumnInfo(name = "plays") val plays: Int,
    @ColumnInfo(name = "first_played") val firstPlayed: Long,
    @ColumnInfo(name = "last_played") val lastPlayed: Long
)

@Dao
@RewriteQueriesToDropUnusedColumns
interface EventTable: DatabaseTable<Event> {

    /** Plays, first and last listen of every song ever listened to */
    @Query("""
        SELECT song_id, COUNT(*) AS plays, MIN(created_at) AS first_played, MAX(created_at) AS last_played
        FROM playback_history
        GROUP BY song_id
    """)
    fun playStats(): Flow<List<SongPlayStats>>

    override val tableName: String
        get() = "playback_history"

    /** Time listened between [from] and [to], in milliseconds */
    @Query("SELECT COALESCE(SUM(time_spent), 0) FROM playback_history WHERE created_at BETWEEN :from AND :to")
    fun totalTimeSpentBetween( from: Long, to: Long = System.currentTimeMillis() ): Flow<Long>

    @Query("SELECT COUNT(*) FROM playback_history")
    fun countAll(): Flow<Long>

    /** When the first song was ever played, `null` with no history */
    @Query("SELECT MIN(created_at) FROM playback_history")
    fun firstPlayedAt(): Long?

    /** How many different songs were played between [from] and [to] */
    @Query("SELECT COUNT(DISTINCT song_id) FROM playback_history WHERE created_at BETWEEN :from AND :to")
    fun countSongsBetween( from: Long, to: Long = Long.MAX_VALUE ): Flow<Int>

    @Transaction
    @Query("SELECT DISTINCT * FROM playback_history LIMIT :limit")
    fun allWithSong( limit: Int = Int.MAX_VALUE ): Flow<List<EventWithSong>>

    /**
     * Return a list of songs that were listened to by user.
     *
     * Songs must be listened at least once within [from] and [to]
     * be included in the results.
     *
     * Results are sorted from most listened to least listened to.
     *
     * By default, only [from] is required, [to] is set to current time.
     * Meaning fetch all from [from] to present.
     *
     * @param from beginning of period to query in epoch millis format
     * @param to the end of period to query in epoch millis format
     * @param limit trim result to have maximum size of this value
     *
     * @return [Song]s that were listened to at least once in period in descending order
     */
    @Query("""
        SELECT DISTINCT S.*
        FROM songs S
        JOIN playback_history E ON E.song_id = S.id
        WHERE E.created_at BETWEEN :from AND :to
        GROUP BY E.song_id 
        ORDER BY SUM(E.time_spent) DESC
        LIMIT :limit
    """)
    fun findSongsMostPlayedBetween(
        from: Long,
        to: Long = System.currentTimeMillis(),
        limit: Int = Int.MAX_VALUE
    ): Flow<List<Song>>

    /** Every song of [findSongsMostPlayedBetween], read once (to play or save them all), off the main thread */
    @Query("""
        SELECT S.*
        FROM songs S
        JOIN playback_history E ON E.song_id = S.id
        WHERE E.created_at BETWEEN :from AND :to
        GROUP BY E.song_id
        ORDER BY SUM(E.time_spent) DESC
    """)
    fun allSongsMostPlayedBetween( from: Long, to: Long = Long.MAX_VALUE ): List<Song>

    /**
     * Return a list of artists that have their songs listened to by user.
     *
     * Songs must be listened at least once within [from] and [to]
     * be included in the results.
     *
     * Results are sorted by total [Song.totalPlayTimeMs] in descending order.
     *
     * By default, only [from] is required, [to] is set to current time.
     * Meaning fetch all from [from] to present.
     *
     * @param from beginning of period to query in epoch millis format
     * @param to the end of period to query in epoch millis format
     * @param limit trim result to have maximum size of this value
     *
     * @return [Artist]s that have their songs listened to at least once in period in descending order
     */
    @Query("""
        SELECT DISTINCT A.*
        FROM artists A
        JOIN song_artist_map SAM ON SAM.artist_id = A.id
        JOIN playback_history E ON E.song_id = SAM.song_id
        WHERE E.created_at BETWEEN :from AND :to
        GROUP BY A.id
        ORDER BY SUM(E.time_spent) DESC
        LIMIT :limit
    """)
    fun findArtistsMostPlayedBetween(
        from: Long,
        to: Long = System.currentTimeMillis(),
        limit: Int = Int.MAX_VALUE
    ): Flow<List<Artist>>

    /**
     * Return a list of albums that have their songs listened to by user.
     *
     * Songs must be listened at least once within [from] and [to]
     * be included in the results.
     *
     * Results are sorted by total [Song.totalPlayTimeMs] in descending order.
     *
     * By default, only [from] is required, [to] is set to current time.
     * Meaning fetch all from [from] to present.
     *
     * @param from beginning of period to query in epoch millis format
     * @param to the end of period to query in epoch millis format
     * @param limit trim result to have maximum size of this value
     *
     * @return [Album]s that have their songs listened to at least once in period in descending order
     */
    @Query("""
        SELECT DISTINCT A.*
        FROM albums A
        JOIN song_album_map SAM ON SAM.album_id = A.id
        JOIN playback_history E ON E.song_id = SAM.song_id
        WHERE E.created_at BETWEEN :from AND :to
        GROUP BY A.id
        ORDER BY SUM(E.time_spent) DESC
        LIMIT :limit
    """)
    fun findAlbumsMostPlayedBetween(
        from: Long,
        to: Long = System.currentTimeMillis(),
        limit: Int = Int.MAX_VALUE
    ): Flow<List<Album>>

    /**
     * Return a list of playlists that have their songs were listened to by user.
     *
     * Songs must be listened at least once within [from] and [to]
     * be included in the results.
     *
     * Results are converted into [PlaylistPreview] and
     * sorted by total [Song.totalPlayTimeMs] in descending order.
     *
     * By default, only [from] is required, [to] is set to current time.
     * Meaning fetch all from [from] to present.
     *
     * @param from beginning of period to query in epoch millis format
     * @param to the end of period to query in epoch millis format
     * @param limit trim result to have maximum size of this value
     *
     * @return [PlaylistPreview] that their songs were listened to at least once in period in descending order
     */
    @Query("""
        SELECT DISTINCT P.*, COUNT(SPM.song_id) AS songCount
        FROM playlists P
        JOIN song_playlist_map SPM ON SPM.playlist_id = P.id
        JOIN playback_history E ON E.song_id = SPM.song_id
        WHERE E.created_at BETWEEN :from AND :to
        GROUP BY P.id
        ORDER BY SUM(E.time_spent) DESC
        LIMIT :limit
    """)
    fun findPlaylistMostPlayedBetweenAsPreview(
        from: Long,
        to: Long = System.currentTimeMillis(),
        limit: Int = Int.MAX_VALUE
    ): Flow<List<PlaylistPreview>>

    @Query("DELETE FROM playback_history")
    fun deleteAll(): Int
}