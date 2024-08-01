/*
 * Copyright (C) 2009-2020 C-LIS CO., LTD.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.c_lis.morelocale;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.LocaleList;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MoreLocale {
    private static final String LOG_TAG = MoreLocale.class.getSimpleName();

    @SuppressWarnings({"UnnecessaryBoxing", "rawtypes"})
    private static void setUserSetLocale(Configuration configuration, boolean value) {
        Class c = configuration.getClass();
        try {
            Field f = c.getField("userSetLocale");
            f.set(configuration, Boolean.valueOf(value));
        } catch (NoSuchFieldException e) {
            if (BuildConfig.DEBUG) {
                Log.e(LOG_TAG, "NoSuchFieldException", e);
            }
        } catch (IllegalAccessException e) {
            if (BuildConfig.DEBUG) {
                Log.e(LOG_TAG, "IllegalAccessException", e);
            }
        }
    }

    /**
     * 言語を取得.
     *
     * @return Locale
     */
    public static LocaleList getLocale(Configuration configuration) {
        return configuration.getLocales();
    }

    /**
     * 言語を設定.
     *
     * @param locales
     */
    public static boolean setLocale(LocaleList locales) throws InvocationTargetException {
        return setLocaleList(locales);
    }

    /**
     * 言語を設定.
     * RequiresApi N
     *
     * @param locales
     */
    @SuppressLint("NewApi")
    @SuppressWarnings({"RedundantArrayCreation", "ConfusingArgumentToVarargsMethod"})
    private static boolean setLocaleList(LocaleList locales) throws InvocationTargetException {
        try {
            @SuppressLint("PrivateApi") Class<?> activityManagerNative = Class.forName("android.app.ActivityManagerNative");

            // ActivityManagerNative.getDefault();
            Method getDefault = activityManagerNative.getMethod("getDefault", null);
            Object am = getDefault.invoke(activityManagerNative, null);

            // am.getConfiguration();
            Method getConfiguration = am.getClass().getMethod("getConfiguration", null);
            Configuration config = (Configuration) getConfiguration.invoke(am, null);

            config.setLocales(locales);
            setUserSetLocale(config, true);

            // am.updateConfiguration(config);
            Method updateConfiguration = am.getClass().getMethod("updatePersistentConfiguration", new Class[]{Configuration.class});
            updateConfiguration.invoke(am, new Object[]{config});

            return true;

        } catch (ClassNotFoundException e) {
            if (BuildConfig.DEBUG) {
                Log.e(LOG_TAG, "ClassNotFoundException", e);
            }
        } catch (NoSuchMethodException e) {
            if (BuildConfig.DEBUG) {
                Log.e(LOG_TAG, "NoSuchMethodException", e);
            }
        } catch (IllegalAccessException e) {
            if (BuildConfig.DEBUG) {
                Log.e(LOG_TAG, "IllegalAccessException", e);
            }
        }
        return false;
    }
}
