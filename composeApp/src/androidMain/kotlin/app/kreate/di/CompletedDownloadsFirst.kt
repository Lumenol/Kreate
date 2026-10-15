@file:androidx.media3.common.util.UnstableApi

package app.kreate.di

import android.net.Uri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener

/**
 * Plays a song from [downloads] only when its download is complete, from
 * [others] otherwise.
 *
 * Both caches are keyed by the song id only. With a download quality of its
 * own, a download still in progress holds part of a stream in another format
 * than the one played: reading its bytes then filling the gaps from the
 * streaming side would mix two formats in one file.
 */
class CompletedDownloadsFirst(
    private val downloads: DataSource.Factory,
    private val others: DataSource.Factory,
    private val isDownloaded: (String) -> Boolean
): DataSource.Factory {

    override fun createDataSource(): DataSource = object : DataSource {

        private val listeners = mutableListOf<TransferListener>()
        private var current: DataSource? = null

        override fun addTransferListener( transferListener: TransferListener ) {
            listeners += transferListener
        }

        override fun open( dataSpec: DataSpec ): Long {
            val factory = if( dataSpec.key?.let( isDownloaded ) == true ) downloads else others
            val source = factory.createDataSource()
            listeners.forEach( source::addTransferListener )
            current = source
            return source.open( dataSpec )
        }

        override fun read( buffer: ByteArray, offset: Int, length: Int ): Int =
            requireNotNull( current ) { "read before open" }.read( buffer, offset, length )

        override fun getUri(): Uri? = current?.uri

        override fun getResponseHeaders(): Map<String, List<String>> = current?.responseHeaders.orEmpty()

        override fun close() {
            try {
                current?.close()
            } finally {
                current = null
            }
        }
    }
}
