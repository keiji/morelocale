package jp.co.c_lis.ccl.morelocale.repository

import android.content.Context
import android.os.LocaleList
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jp.co.c_lis.ccl.morelocale.BuildConfig
import jp.co.c_lis.ccl.morelocale.entity.LocaleItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Singleton

class PreferenceRepository(applicationContext: Context) {

    companion object {
        private const val KEY_LANGUAGE = "key_language"
    }

    private val pref = applicationContext.getSharedPreferences(
        BuildConfig.PREFERENCES_FILE_NAME, Context.MODE_PRIVATE
    )

    suspend fun saveLocale(locales: LocaleList) = withContext(Dispatchers.IO) {
        val ls = mutableListOf<String>()
        for (i in 0 until locales.size()) {
            val e = locales[i]
            ls.add("${e.language}-${e.country}-${e.variant}")
        }

        pref.edit()
            .putString(KEY_LANGUAGE, ls.joinToString(";"))
            .commit()
    }

    suspend fun loadLocale() = withContext(Dispatchers.IO) {
        val store = pref.getString(KEY_LANGUAGE, null) ?: return@withContext null
        val locals = store.split(';')

        return@withContext locals.map {
            val locale = it.split('-')
            val language = locale[0]
            val country = locale[1]
            val variant = locale[2]

            LocaleItem(
                language = language,
                country = country,
                variant = variant
            ).locale
        }.let {
            LocaleList(*it.toTypedArray())
        }
    }

    fun clearLocale() {
        pref.edit()
            .putString(KEY_LANGUAGE, null)
            .apply()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object PreferenceRepositoryModule {

    @Singleton
    @Provides
    fun prividePreferenceRepository(
        @ApplicationContext applicationContext: Context,
    ): PreferenceRepository {
        return PreferenceRepository(
            applicationContext
        )
    }
}
