package jp.co.c_lis.ccl.morelocale.entity

import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.util.Locale
import kotlin.random.Random

@Parcelize
@Entity
data class LocaleItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val label: String? = null,
    val language: String? = null,
    val country: String? = null,
    val variant: String? = null,
    var isPreset: Boolean = false,
) : Parcelable {
    companion object {
        val itemDiffCallback = object : DiffUtil.ItemCallback<LocaleItem>() {
            override fun areItemsTheSame(oldItem: LocaleItem, newItem: LocaleItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: LocaleItem, newItem: LocaleItem): Boolean {
                return oldItem.isPreset == newItem.isPreset
            }
        }

        fun from(locale: Locale) {
            LocaleItem(
                language = locale.language,
                country = locale.country,
                variant = locale.variant
            )
        }
    }

    @IgnoredOnParcel
    @Ignore
    val locale: Locale = if (language != null && country != null && variant != null) {
        Locale(language, country, variant)
    } else if (language != null && country != null && variant == null) {
        Locale(language, country)
    } else if (language != null && country == null && variant == null) {
        Locale(language)
    } else {
        Locale("")
    }

    val displayName: String
        get() = label ?: locale.displayName

    val displayFull: String
        get() {
            fun defaultValue(value: String?): String {
                return if (value.isNullOrEmpty()) "_" else value
            }

            val language = defaultValue(language)
            val country = defaultValue(country)
            val variant = defaultValue(variant)
            return "$displayName ($language-$country-$variant)"
        }
}

fun createLocale(localeStr: String): LocaleItem {
    val localeTokens = localeStr.split("-")
    return when (localeTokens.size) {
        0 -> {
            LocaleItem(language = localeStr)
        }

        1 -> {
            LocaleItem(language = localeTokens[0])
        }

        2 -> {
            LocaleItem(
                language = localeTokens[0],
                country = localeTokens[1]
            )
        }

        else -> {
            LocaleItem(
                language = localeTokens[0],
                country = localeTokens[1],
                variant = localeTokens[2]
            )
        }
    }
}

fun createLocale(locale: Locale, locales: List<LocaleItem>, ids: Set<Int>): LocaleItem {
    val firstOrNull = locales.firstOrNull {
        (it.language ?: "") == locale.language &&
                (it.country ?: "") == locale.country &&
                (it.variant ?: "") == locale.variant
    }
    return if (firstOrNull != null) {
        firstOrNull
    } else {
        var id: Int
        do {
            id = Random.nextInt()
        } while (ids.contains(id))
        
        LocaleItem(
            id = id,
            language = locale.language,
            country = locale.country,
            variant = locale.variant
        )
    }
}
