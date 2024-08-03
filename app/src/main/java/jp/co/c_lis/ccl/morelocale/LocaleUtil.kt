package jp.co.c_lis.ccl.morelocale

import android.os.LocaleList
import jp.co.c_lis.ccl.morelocale.entity.LocaleItem
import java.util.Locale

private fun equals(obj1: Locale, obj2: Locale): Boolean {
    if (obj1 === obj2) {
        return true
    }

    if (obj1.language != obj2.language) {
        return false
    }

    if (obj1.country != obj2.country) {
        return false
    }

    if (obj1.variant != obj2.variant) {
        return false
    }

    if (obj1.script != obj2.script) {
        return false
    }

    return true
}

fun equals(obj1: LocaleList, obj2: LocaleList): Boolean {
    if (obj1.size() != obj2.size()) {
        return false
    } else {
        for (i in 0 until obj1.size()) {
            if (!equals(obj1[i], obj2[i])) {
                return false
            }
        }
    }
    return true
}
