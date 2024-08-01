package jp.co.c_lis.ccl.morelocale.repository

import android.content.Context
import android.content.res.AssetManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jp.co.c_lis.ccl.morelocale.MainApplication
import jp.co.c_lis.ccl.morelocale.entity.LocaleItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Singleton

class LocaleRepository(applicationContext: Context) {

    private val db = MainApplication.getDbInstance(applicationContext)

    suspend fun getAll(assetManager: AssetManager) = withContext(Dispatchers.IO) {
        val localeList = findAll()
        if (localeList.isNotEmpty()) {
            return@withContext localeList
        }

        val localeItems = Locale.getAvailableLocales()
//assetManager.locales
            .filterNotNull()
            .reversed()
            .map { LocaleItem.from(it).also { it.isPreset = true } }
        db.localeItemDao()
            .insertAll(localeItems)

        return@withContext findAll()
    }

    suspend fun findAll() = withContext(Dispatchers.IO) {
        return@withContext db.localeItemDao().findAll()
    }

    suspend fun create(locale: LocaleItem) = withContext(Dispatchers.IO) {
        db.localeItemDao()
            .insert(locale)
    }

    suspend fun update(locale: LocaleItem) = withContext(Dispatchers.IO) {
        db.localeItemDao()
            .update(locale)
    }

    suspend fun delete(locale: LocaleItem) = withContext(Dispatchers.IO) {
        db.localeItemDao()
            .delete(locale)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object LocaleRepositoryModule {

    @Singleton
    @Provides
    fun provideLocaleRepository(
        @ApplicationContext applicationContext: Context,
    ): LocaleRepository {
        return LocaleRepository(
            applicationContext
        )
    }
}
