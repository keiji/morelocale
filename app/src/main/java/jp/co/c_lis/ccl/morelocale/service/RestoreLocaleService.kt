package jp.co.c_lis.ccl.morelocale.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.LocaleList
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import dagger.hilt.android.AndroidEntryPoint
import jp.co.c_lis.ccl.morelocale.BuildConfig
import jp.co.c_lis.ccl.morelocale.equals
import jp.co.c_lis.ccl.morelocale.R
import jp.co.c_lis.ccl.morelocale.repository.PreferenceRepository
import jp.co.c_lis.ccl.morelocale.ui.MainActivity
import jp.co.c_lis.morelocale.MoreLocale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.lang.reflect.InvocationTargetException
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class RestoreLocaleService : Service() {

    companion object {
        private const val KEY_REQUEST = "key_request"

        private const val NOTIFICATION_CHANNEL_ID =
            "morelocale_restore_locale-${BuildConfig.VERSION_NAME}"
        private val OLD_NOTIFICATION_CHANNEL_IDS = listOf(
            "morelocale_restore_locale",
        )

        private const val NOTIFICATION_ID_PROGRESS = 0x128
        private const val NOTIFICATION_ID_CANCELED = 0x129
        private const val NOTIFICATION_ID_ERROR = 0x130

        private const val NO_ICON = 0x0

        private const val DELAY_IN_MILLIS = 10 * 1000
        private const val INTERVAL_PROGRESS_UPDATE_IN_MILLIS = 100L

        private const val REQUEST_OPEN_FROM_PERMISSION_ERROR = 0x1234

        private const val REQUEST_CODE_NOTIFY = 0
        private const val REQUEST_CODE_RESTORE = 1
        private const val REQUEST_CODE_CANCEL = -1
        private const val REQUEST_CODE_NEVER = -2

        fun getIntent(context: Context): Intent {
            return Intent(context, RestoreLocaleService::class.java).apply {
                putExtra(KEY_REQUEST, REQUEST_CODE_NOTIFY)
            }
        }

        fun getCancelIntent(context: Context): Intent {
            return getIntent(context).apply {
                putExtra(KEY_REQUEST, REQUEST_CODE_CANCEL)
            }
        }

        fun getNeverIntent(context: Context): Intent {
            return getIntent(context).apply {
                putExtra(KEY_REQUEST, REQUEST_CODE_NEVER)
            }
        }

        fun getRestoreIntent(context: Context): Intent {
            return getIntent(context).apply {
                putExtra(KEY_REQUEST, REQUEST_CODE_RESTORE)
            }
        }
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    @Inject
    lateinit var preferenceRepository: PreferenceRepository

    private var startTimeInMillis = -1L
    private var job: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        intent ?: return super.onStartCommand(intent, flags, startId)

        val requestCode = intent.getIntExtra(KEY_REQUEST, REQUEST_CODE_NOTIFY)
        Timber.d("requestCode: $requestCode")

        val notificationManager = NotificationManagerCompat.from(this)

        removeOldNotificationChannels(
            notificationManager,
            OLD_NOTIFICATION_CHANNEL_IDS
        )

        createNotificationChannel(
                notificationManager,
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name_restore_locale)
        )

        notificationManager.cancelAll()

        val currentLocale = MoreLocale.getLocale(resources.configuration)

        val locale = runBlocking {
            preferenceRepository.loadLocale()
        } ?: return START_STICKY_COMPATIBILITY

        if (equals(currentLocale, locale)) {
            return START_STICKY_COMPATIBILITY
        }

        when (requestCode) {
            REQUEST_CODE_RESTORE -> {
                restoreLocale(locale)
                return START_STICKY_COMPATIBILITY
            }
            REQUEST_CODE_CANCEL -> {
                cancelRestore()
                return START_STICKY_COMPATIBILITY
            }
            REQUEST_CODE_NEVER -> {
                setNeverRestore(preferenceRepository, notificationManager)
                return START_STICKY_COMPATIBILITY
            }
        }

        startForeground(NOTIFICATION_ID_PROGRESS, createProgressNotification(this))

        if (startTimeInMillis < 0) {
            startTimeInMillis = System.currentTimeMillis()
        }

        job?.cancel()
        job = coroutineScope.launch(Dispatchers.Default) {
            do {
                val elapsed = System.currentTimeMillis() - startTimeInMillis

                showProgressNotification(this@RestoreLocaleService)

                delay(INTERVAL_PROGRESS_UPDATE_IN_MILLIS)

            } while (elapsed < DELAY_IN_MILLIS)

            stopForeground(true)

            restoreLocale(locale)

            stopSelf()
        }

        return START_STICKY_COMPATIBILITY
    }

    private fun setNeverRestore(
            preferenceRepository: PreferenceRepository,
            notificationManager: NotificationManagerCompat
    ) {
        runBlocking {
            preferenceRepository.clearLocale()
            notificationManager.cancel(NOTIFICATION_ID_CANCELED)
            stopSelf()
        }
    }

    private fun cancelRestore() {
        stopForeground(true)
        showCanceledNotification(this)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")

        startTimeInMillis = -1
        job?.cancel()
        coroutineScope.cancel()
    }

    private fun restoreLocale(locales: LocaleList): Boolean {
        Timber.d("Restore locale")

        try {
            MoreLocale.setLocale(locales)
            return true
        } catch (e: InvocationTargetException) {
            Timber.e(e)
            showPermissionErrorNotification(this@RestoreLocaleService)
        }

        return false
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun removeOldNotificationChannels(
        notificationManager: NotificationManagerCompat,
        oldChannelIds: List<String>
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        oldChannelIds.forEach { channelId ->
            notificationManager.deleteNotificationChannel(channelId)
        }
    }

    private fun createNotificationChannel(
            notificationManager: NotificationManagerCompat,
            channelId: String,
            name: String
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
                channelId,
                name,
                NotificationManager.IMPORTANCE_DEFAULT
        ).also {
            it.setSound(null, null)
        }
        notificationManager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    private fun showProgressNotification(context: Context) {
        val notify = createProgressNotification(context)
        NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_PROGRESS, notify)
    }

    private fun createProgressNotification(context: Context): Notification {
        val cancelPendingIntent = PendingIntent.getService(
                context,
                REQUEST_CODE_CANCEL,
                getCancelIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT)

        val elapsed = System.currentTimeMillis() - startTimeInMillis
        val percentage = ((elapsed.toFloat() / DELAY_IN_MILLIS) * 100).roundToInt()

        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.settings_backup_restore)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(getText(R.string.restoring_locale))
                .setContentText(getText(R.string.tap_to_cancel))
                .setTicker(getText(R.string.restoring_locale))
                .setProgress(100, percentage, false)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(cancelPendingIntent)
                .setDefaults(0)
                .setSound(null)
                .build()

    }

    @SuppressLint("MissingPermission")
    private fun showCanceledNotification(context: Context) {
        val notify = createCanceledNotification(context)
        NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_CANCELED, notify)
    }

    private fun createCanceledNotification(context: Context): Notification {
        val neverPendingIntent = PendingIntent.getService(
                context,
                REQUEST_CODE_NEVER,
                getNeverIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT)
        val neverAction = NotificationCompat.Action(
                NO_ICON,
                getText(R.string.never),
                neverPendingIntent
        )

        val restorePendingIntent = PendingIntentCompat.getService(
                context,
                REQUEST_CODE_RESTORE,
                getRestoreIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT,
                false,
            )
        val restoreAction = NotificationCompat.Action(
                NO_ICON,
                getText(R.string.restore),
                restorePendingIntent
        )

        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_menu_3d_globe)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(getText(R.string.restore_canceled))
                .setTicker(getText(R.string.restore_canceled))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(neverAction)
                .addAction(restoreAction)
                .build()

    }

    @SuppressLint("MissingPermission")
    private fun showPermissionErrorNotification(context: Context) {
        val notify = createPermissionErrorNotification(context)
        NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_ERROR, notify)
    }

    private fun createPermissionErrorNotification(context: Context): Notification {
        val pendingIntent = PendingIntentCompat.getActivity(
                context,
                REQUEST_OPEN_FROM_PERMISSION_ERROR,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_CANCEL_CURRENT,
                false,
        )

        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(getText(R.string.permission_required))
                .setContentText(getText(R.string.tap_to_open))
                .setTicker(getText(R.string.permission_required))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .build()

    }

}
