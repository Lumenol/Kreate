package it.fast4x.rimusic.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.NotificationUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import app.kreate.android.R
import app.kreate.android.service.DownloadFailures
import app.kreate.android.service.DownloadHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val JOB_ID = 8888
private const val FOREGROUND_NOTIFICATION_ID = 8989


@UnstableApi
class MyDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    DownloadHelper.DOWNLOAD_NOTIFICATION_CHANNEL_ID,
    R.string.download, 0
), KoinComponent {

    private val downloadHelper: DownloadHelper by inject()

    override fun getDownloadManager(): DownloadManager {
        // This will only happen once, because getDownloadManager is guaranteed to be called only once
        // in the life cycle of the process.
        val downloadManager = downloadHelper.downloadManager
        val downloadNotificationHelper = MyDownloadHelper.getDownloadNotificationHelper()
        downloadManager.addListener(
            TerminalStateNotificationHelper(
                this,
                downloadNotificationHelper,
                FOREGROUND_NOTIFICATION_ID + 1
            )
        )
        DownloadFailures.init( this )
        downloadManager.addListener( DownloadFailures )
        return downloadManager
    }

    override fun getScheduler(): PlatformScheduler? {
        return if(Util.SDK_INT >= 21) PlatformScheduler(this, JOB_ID) else null
    }

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int
    ): Notification {
        fun serviceIntent( intent: Intent, requestCode: Int ) =
            PendingIntent.getService( this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT )

        // Tapping it opens the app
        val openApp = packageManager.getLaunchIntentForPackage( packageName )?.let {
            PendingIntent.getActivity( this, 0, it, PendingIntent.FLAG_IMMUTABLE )
        }

        return NotificationCompat
            .Builder(
                /* context = */ this,
                /* notification = */ MyDownloadHelper
                    .getDownloadNotificationHelper()
                    .buildProgressNotification(
                    /* context            = */ this,
                    /* smallIcon          = */ R.drawable.download_progress,
                    /* contentIntent      = */ openApp,
                    /* message            = */ getString( R.string.downloads_notification, downloads.size ),
                    /* downloads          = */ downloads,
                    /* notMetRequirements = */ notMetRequirements
                )
            )
            .setChannelId(DownloadHelper.DOWNLOAD_NOTIFICATION_CHANNEL_ID)
            .addAction(
                R.drawable.pause,
                getString( R.string.downloads_pause_all ),
                serviceIntent( buildPauseDownloadsIntent( this, MyDownloadService::class.java, false ), 1 )
            )
            .addAction(
                R.drawable.close,
                getString( R.string.downloads_cancel_all ),
                serviceIntent( buildRemoveAllDownloadsIntent( this, MyDownloadService::class.java, false ), 2 )
            )
            .build()
    }

    /**
     * Creates and displays notifications for downloads when they complete or fail.
     *
     *
     * This helper will outlive the lifespan of a single instance of [MyDownloadService].
     * It is static to avoid leaking the first [MyDownloadService] instance.
     */
    private class TerminalStateNotificationHelper(
        private val context: Context,
        private val notificationHelper: DownloadNotificationHelper,
        firstNotificationId: Int
    ) : DownloadManager.Listener {
        private var nextNotificationId: Int = firstNotificationId

        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?
        ) {
            val notification: Notification = when (download.state) {
                Download.STATE_COMPLETED -> {
                    notificationHelper.buildDownloadCompletedNotification(
                        context,
                        R.drawable.downloaded,
                        null,
                        Util.fromUtf8Bytes(download.request.data)
                    )
                }
                Download.STATE_FAILED -> {
                    notificationHelper.buildDownloadFailedNotification(
                        context,
                        R.drawable.alert_circle_not_filled,
                        null,
                        Util.fromUtf8Bytes(download.request.data)
                    )
                }
                else -> return
            }
            NotificationUtil.setNotification(context, nextNotificationId++, notification)

        }


    }

}