package app.kreate.android.utils

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.DocumentsContract
import app.kreate.android.Preferences
import app.kreate.android.R
import co.touchlab.kermit.Logger
import it.fast4x.rimusic.Database
import me.knighthat.component.export.ExportSettingsDialog
import me.knighthat.utils.Toaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class BackupInterval( val days: Long, val textId: Int ) {
    DAILY( 1, R.string.backup_every_day ),
    WEEKLY( 7, R.string.backup_every_week ),
    MONTHLY( 30, R.string.backup_every_month );
}

/**
 * Backs up the database and/or the settings into a folder picked in the
 * settings, keeping the last [Preferences.AUTO_BACKUP_KEEP] of each.
 * Never the audio cache. Login data stays out: it lives in the encrypted
 * preferences, which the settings export doesn't read.
 *
 * Runs when the app starts and a backup is due: the data only changes while
 * the app is used, so no background job is needed.
 */
object AutoBackup {

    private const val TAG = "AutoBackup"
    private const val DATABASE_PREFIX = "Kreate_database_"
    private const val SETTINGS_PREFIX = "Kreate_settings_"

    fun isDue(): Boolean {
        if( !Preferences.AUTO_BACKUP.value || Preferences.AUTO_BACKUP_FOLDER.value.isBlank() ) return false
        val interval = TimeUnit.DAYS.toMillis( Preferences.AUTO_BACKUP_INTERVAL.value.days )
        return System.currentTimeMillis() - Preferences.AUTO_BACKUP_LAST.value >= interval
    }

    /** Blocking: call it off the main thread */
    suspend fun runIfDue( context: Context ) {
        if( isDue() ) backup( context ).onFailure { Toaster.e( R.string.backup_failed ) }
    }

    /** Remembers [treeUri] (from the folder picker) with permanent access */
    fun setFolder( context: Context, treeUri: Uri ) {
        context.contentResolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        Preferences.AUTO_BACKUP_FOLDER.value = treeUri.toString()
    }

    /** Name of the folder for the settings screen, `null` if none or no access anymore */
    fun folderName( context: Context ): String? {
        val tree = Preferences.AUTO_BACKUP_FOLDER.value.takeIf( String::isNotBlank )?.let( Uri::parse ) ?: return null
        if( context.contentResolver.persistedUriPermissions.none { it.uri == tree && it.isWritePermission } ) return null
        return DocumentsContract.getTreeDocumentId( tree ).substringAfterLast( ':' ).ifBlank { "/" }
    }

    /** Blocking: call it off the main thread */
    suspend fun backup( context: Context ): Result<Unit> = runCatching {
        val tree = Uri.parse( Preferences.AUTO_BACKUP_FOLDER.value )
        val folder = DocumentsContract.buildDocumentUriUsingTree( tree, DocumentsContract.getTreeDocumentId( tree ) )
        val stamp = SimpleDateFormat( "yyyyMMdd-HHmmss", Locale.ROOT ).format( Date() )

        if( Preferences.AUTO_BACKUP_DATABASE.value ) {
            val copy = File( context.cacheDir, "backup.sqlite" ).apply { delete() }
            try {
                Database.vacuumInto( copy )
                if( !Preferences.AUTO_BACKUP_HISTORY.value )
                    SQLiteDatabase.openDatabase( copy.path, null, SQLiteDatabase.OPEN_READWRITE ).use {
                        it.execSQL( "DELETE FROM playback_history" )
                        it.execSQL( "VACUUM" )
                    }
                write( context, folder, "$DATABASE_PREFIX$stamp.sqlite", "application/vnd.sqlite3" ) { out ->
                    copy.inputStream().use { it.copyTo( out ) }
                }
            } finally {
                copy.delete()
            }
            prune( context, tree, DATABASE_PREFIX )
        }

        if( Preferences.AUTO_BACKUP_SETTINGS.value ) {
            write( context, folder, "$SETTINGS_PREFIX$stamp.csv", "text/csv" ) { ExportSettingsDialog.onExportToCsv( it ) }
            prune( context, tree, SETTINGS_PREFIX )
        }

        // On the main thread, so the settings screen shows it right away
        withContext( Dispatchers.Main ) { Preferences.AUTO_BACKUP_LAST.value = System.currentTimeMillis() }
        Logger.i( TAG ) { "backup done ($stamp)" }
    }.onFailure { Logger.e( it, TAG ) { "backup failed" } }

    private fun write( context: Context, folder: Uri, name: String, mime: String, content: (java.io.OutputStream) -> Unit ) {
        val document = requireNotNull( DocumentsContract.createDocument( context.contentResolver, folder, mime, name ) ) {
            "couldn't create $name"
        }
        requireNotNull( context.contentResolver.openOutputStream( document ) ) { "couldn't open $name" }.use( content )
    }

    /** Keeps the newest backups starting with [prefix] */
    private fun prune( context: Context, tree: Uri, prefix: String ) {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree( tree, DocumentsContract.getTreeDocumentId( tree ) )
        val backups = mutableListOf<Pair<String, String>>()       // name to document id
        context.contentResolver.query(
            children,
            arrayOf( DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_DOCUMENT_ID ),
            null, null, null
        )?.use { cursor ->
            while( cursor.moveToNext() ) {
                val name = cursor.getString( 0 ) ?: continue
                if( name.startsWith( prefix ) ) backups += name to cursor.getString( 1 )
            }
        }
        // Names hold the date: sorting them sorts by age
        backups.sortedByDescending { it.first }
               .drop( Preferences.AUTO_BACKUP_KEEP.value.coerceAtLeast( 1 ) )
               .forEach { (name, id) ->
                   runCatching {
                       DocumentsContract.deleteDocument( context.contentResolver, DocumentsContract.buildDocumentUriUsingTree( tree, id ) )
                   }.onFailure { Logger.w( it, TAG ) { "couldn't delete old backup $name" } }
               }
    }
}
