package jp.co.c_lis.ccl.morelocale.repository

import android.content.Context
import android.os.LocaleList
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jp.co.c_lis.ccl.morelocale.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Singleton

class PreferenceRepository(applicationContext: Context) {

    companion object {
        @Deprecated("don't use anymore") private const val KEY_LANGUAGE = "key_language"
        @Deprecated("don't use anymore") private const val KEY_COUNTRY = "key_country"
        @Deprecated("don't use anymore") private const val KEY_VARIANT = "key_variant"
        private const val KEY_LANGUAGES = "key_languages"
    }

    private val pref = applicationContext.getSharedPreferences(
        BuildConfig.PREFERENCES_FILE_NAME, Context.MODE_PRIVATE
    )

    suspend fun saveLocale(locales: LocaleList) = withContext(Dispatchers.IO) {
        pref.edit()
            .putString(KEY_LANGUAGES, locales.toLanguageTags())
            .commit()
    }

    suspend fun loadLocale() = withContext(Dispatchers.IO) {
        val store = pref.getString(KEY_LANGUAGES, null) ?: return@withContext null
        return@withContext LocaleList.forLanguageTags(store)
    }

    @Suppress("DEPRECATION")
    fun clearLocale() {
        pref.edit()
            .putString(KEY_LANGUAGE, null)
            .putString(KEY_COUNTRY, null)
            .putString(KEY_VARIANT, null)
            .putString(KEY_LANGUAGES, null)
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
