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
    val script: String? = null,
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

        fun from(locale: Locale): LocaleItem {
            return LocaleItem(
                language = locale.language,
                country = locale.country,
                variant = locale.variant,
                script = locale.script,
            )
        }
    }

    @IgnoredOnParcel
    @Ignore
    val locale: Locale =
        Locale.Builder().setLanguage(language).setRegion(country).setVariant(variant)
            .setScript(script).build()

    val displayName: String
        get() = label ?: locale.displayName

    val displayNameInLocal: String
        get() = locale.getDisplayName(locale)

    @IgnoredOnParcel
    @Ignore
    val langCode = locale.toString()

    val displayFull: String
        get() {
            return "$displayName ($langCode)\n$displayNameInLocal"
        }
}

fun createLocale(locale: Locale, locales: List<LocaleItem>, ids: Set<Int>): LocaleItem {
    val firstOrNull = locales.firstOrNull {
        (it.language ?: "") == locale.language &&
        (it.country ?: "") == locale.country &&
        (it.variant ?: "") == locale.variant &&
        (it.script ?: "") == locale.script
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
