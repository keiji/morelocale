package jp.co.c_lis.ccl.morelocale.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint
import jp.co.c_lis.ccl.morelocale.equals
import jp.co.c_lis.ccl.morelocale.repository.PreferenceRepository
import jp.co.c_lis.ccl.morelocale.service.RestoreLocaleService
import jp.co.c_lis.morelocale.MoreLocale
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        intent ?: return

        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        runBlocking {
            val currentLocale = MoreLocale.getLocale(context.resources.configuration)
            val savedLocale = PreferenceRepository(context).loadLocale()
                    ?: return@runBlocking

            if (equals(currentLocale, savedLocale)) {
                return@runBlocking
            }

            startService(context, RestoreLocaleService.getIntent(context))
        }
    }

    private fun startService(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
