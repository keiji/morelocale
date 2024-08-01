package jp.co.c_lis.ccl.morelocale.ui.locale_list

import android.content.Context
import android.os.LocaleList
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.c_lis.ccl.morelocale.entity.LocaleItem
import jp.co.c_lis.ccl.morelocale.entity.createLocale
import jp.co.c_lis.ccl.morelocale.repository.LocaleRepository
import jp.co.c_lis.ccl.morelocale.repository.PreferenceRepository
import jp.co.c_lis.morelocale.MoreLocale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.reflect.InvocationTargetException
import javax.inject.Inject
import jp.co.c_lis.ccl.morelocale.equals

@HiltViewModel
class LocaleListViewModel @Inject constructor(
    private val localeListRepository: LocaleRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    private var lastCurrentLocales = listOf<LocaleItem>()
    val currentLocales = MutableLiveData<MutableList<LocaleItem>>()
    val localeList = MutableLiveData<List<LocaleItem>>()
    val canSave = MutableLiveData(false)

    private val _alertsEvents = MutableSharedFlow<AlertsMoreLocale>()
    val alertsEvents = _alertsEvents.asLiveData()

    private fun loadCurrentLocales(context: Context, localeList: List<LocaleItem>) =
        viewModelScope.launch {
            val localeConfigs = MoreLocale.getLocale(context.resources.configuration)
            val localePreferences = preferenceRepository.loadLocale()

            if (localePreferences != null && !equals(
                    localeConfigs,
                    localePreferences
                )
            ) {
                setLocales(localePreferences) {}
            }
            val locales = localePreferences ?: localeConfigs
            val ls = mutableListOf<LocaleItem>()
            val ids = localeList.map { it.id }.toMutableSet()
            for (i in 0 until locales.size()) {
                val element = createLocale(locales[i], localeList, ids)
                ls.add(element)
                ids.add(element.id)
            }

            currentLocales.postValue(ls)
            lastCurrentLocales = ls.toList()
        }

    private fun checkIfCanSave(ls: MutableList<LocaleItem>) {
        canSave.postValue(ls != lastCurrentLocales)
    }

    fun addToCurrentLocales(localeItem: LocaleItem) {
        viewModelScope.launch {
            val ls = currentLocales.value?.toMutableList() ?: mutableListOf()
            if (ls.contains(localeItem)) {
                return@launch
            }

            ls.add(0, localeItem)
            currentLocales.postValue(ls)
            checkIfCanSave(ls)
        }
    }

    fun delFromCurrentLocales(localeItem: LocaleItem) {
        viewModelScope.launch {
            val ls = currentLocales.value?.toMutableList() ?: mutableListOf()
            if (!ls.contains(localeItem) || ls.size < 2) {
                return@launch
            }

            ls.remove(localeItem)
            currentLocales.postValue(ls)
            checkIfCanSave(ls)
        }
    }

    fun moveInCurrentLocales(localeItem: LocaleItem, isUp: Boolean) {
        viewModelScope.launch {
            val ls = currentLocales.value?.toMutableList() ?: mutableListOf()
            if (!ls.contains(localeItem)) {
                return@launch
            }
            val idx = ls.indexOf(localeItem)
            if (isUp && idx == 0 || (!isUp && idx >= ls.size - 1)) {
                return@launch
            }

            val x = if (isUp) -1 else 1
            val t = ls[idx + x]
            ls[idx + x] = ls[idx]
            ls[idx] = t
            currentLocales.postValue(ls)
            checkIfCanSave(ls)
        }
    }

    fun loadLocaleListAndCurrentLocaleList(context: Context) {
        viewModelScope.launch {
            val all = localeListRepository.getAll(context.assets)
            localeList.postValue(all)
            loadCurrentLocales(context, all)
        }
    }

    fun addLocale(localeItem: LocaleItem) {
        viewModelScope.launch {
            localeListRepository.create(localeItem)
            val list = localeListRepository.findAll()

            localeList.postValue(list)
        }
    }

    fun editLocale(localeItem: LocaleItem) {
        viewModelScope.launch {
            localeListRepository.update(localeItem)
            localeList.postValue(localeListRepository.findAll())
        }
    }

    fun deleteLocale(localeItem: LocaleItem) {
        viewModelScope.launch {
            localeListRepository.delete(localeItem)
            localeList.postValue(localeListRepository.findAll())
        }
    }

    private fun setLocales(locales: LocaleList, whenSucceed: () -> Unit) = viewModelScope.launch {
        try {
            MoreLocale.setLocale(locales)

            if (preferenceRepository.saveLocale(locales)) {
                whenSucceed()
            }
        } catch (e: InvocationTargetException) {
            Timber.e(e, "InvocationTargetException")

            _alertsEvents.emit(AlertsMoreLocale.NEED_PERMISSION)
        }
    }

    fun setLocales() = viewModelScope.launch {
        val ls: List<LocaleItem>? = currentLocales.value
        if (!ls.isNullOrEmpty()) {
            setLocales(LocaleList(*ls.map { it.locale }.toTypedArray())) {
                lastCurrentLocales = ls.toList()
                canSave.postValue(false)
            }
        }
    }
}
