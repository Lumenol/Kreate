package app.kreate.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.kreate.database.models.SongYoutubeInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface SongYoutubeInfoTable {

    @Upsert
    fun upsert( info: SongYoutubeInfo )

    @Query("SELECT * FROM song_youtube_info")
    fun all(): Flow<List<SongYoutubeInfo>>

    @Query("SELECT * FROM song_youtube_info WHERE song_id = :songId")
    fun get( songId: String ): SongYoutubeInfo?

    @Query("SELECT fetched_at FROM song_youtube_info WHERE song_id = :songId")
    fun fetchedAt( songId: String ): Long?

    @Query("SELECT EXISTS(SELECT 1 FROM songs WHERE id = :songId)")
    fun songExists( songId: String ): Boolean

    /**
     * Online songs of the library with nothing asked yet: the ones played, liked or in a
     * playlist (not every song ever seen in a search).
     */
    @Query("""
        SELECT S.id FROM songs S
        WHERE S.is_local = 0
          AND NOT EXISTS (SELECT 1 FROM song_youtube_info I WHERE I.song_id = S.id)
          AND ( S.total_playtime > 0 OR S.liked_at > 0
                OR EXISTS (SELECT 1 FROM song_playlist_map P WHERE P.song_id = S.id)
                OR EXISTS (SELECT 1 FROM playback_history E WHERE E.song_id = S.id) )
        ORDER BY S.ROWID DESC
    """)
    fun missing(): List<String>

    /** Asked before [before] (views change): oldest first */
    @Query("SELECT song_id FROM song_youtube_info WHERE fetched_at < :before ORDER BY fetched_at")
    fun staleBefore( before: Long ): List<String>

    @Query("""
        SELECT COUNT(*) FROM songs S
        WHERE S.is_local = 0
          AND ( S.total_playtime > 0 OR S.liked_at > 0
                OR EXISTS (SELECT 1 FROM song_playlist_map P WHERE P.song_id = S.id)
                OR EXISTS (SELECT 1 FROM playback_history E WHERE E.song_id = S.id) )
    """)
    fun libraryCount(): Flow<Int>

    /** Songs of the library already asked (same songs as [libraryCount]) */
    @Query("""
        SELECT COUNT(*) FROM songs S
        WHERE S.is_local = 0
          AND EXISTS (SELECT 1 FROM song_youtube_info I WHERE I.song_id = S.id)
          AND ( S.total_playtime > 0 OR S.liked_at > 0
                OR EXISTS (SELECT 1 FROM song_playlist_map P WHERE P.song_id = S.id)
                OR EXISTS (SELECT 1 FROM playback_history E WHERE E.song_id = S.id) )
    """)
    fun knownCount(): Flow<Int>
}
