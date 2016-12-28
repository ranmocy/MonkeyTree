package me.ranmocy.monkeytree;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Util class used for shared preferences.
 */
final class SharedPreferencesUtil {

    private static final String NAME = "monkey-tree";
    private static final String KEY_MONITORING = "key_monitoring";
    private static final String KEY_AUTO_UPDATE = "key_auto_update";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    static boolean getMonitoringEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_MONITORING, false);
    }

    static void setKeyMonitoring(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_MONITORING, enabled).apply();
    }

    static boolean getAutoUpdateEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_AUTO_UPDATE, false);
    }

    static void setAutoUpdateEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_UPDATE, enabled).apply();
    }
}
