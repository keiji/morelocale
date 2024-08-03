package jp.co.c_lis.ccl.morelocale

import android.content.Context
import androidx.multidex.MultiDexApplication
import androidx.room.Room
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import timber.log.Timber.DebugTree

@Suppress("unused")
@HiltAndroidApp
class MainApplication : MultiDexApplication() {

    companion object {

        var db: AppDatabase? = null

        fun getDbInstance(applicationContext: Context): AppDatabase {
            db?.also {
                return it
            }

            return Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    BuildConfig.DATABASE_FILE_NAME
            )
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .build().also {
                        db = it
                    }
        }
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }

    }
}
