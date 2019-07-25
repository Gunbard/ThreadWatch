package honkhonk.threadwatch;

import android.content.Context;
import android.content.SharedPreferences;

import honkhonk.threadwatch.helpers.Common;

public class PreferencesDataManager {
    final private static String TAG = PreferencesDataManager.class.getSimpleName();

    public static int getSortMode(Context context) {
        final SharedPreferences savedPrefs = context.getSharedPreferences(Common.PREFS_NAME, 0);
        return savedPrefs.getInt(Common.SAVED_SORT_MODE, 0);
    }

    public static void setSortMode(Context context, int sortMode) {
        final SharedPreferences savedPrefs = context.getSharedPreferences(Common.PREFS_NAME, 0);
        SharedPreferences.Editor editor = savedPrefs.edit();
        editor.putInt(Common.SAVED_SORT_MODE, sortMode);
        editor.apply();
    }

    public static boolean sortAscending(Context context) {
        final SharedPreferences savedPrefs = context.getSharedPreferences(Common.PREFS_NAME, 0);
        return savedPrefs.getBoolean(Common.SAVED_SORT_ASCENDING, false);
    }

    public static void setSortAscending(Context context, boolean sortAscending) {
        final SharedPreferences savedPrefs = context.getSharedPreferences(Common.PREFS_NAME, 0);
        SharedPreferences.Editor editor = savedPrefs.edit();
        editor.putBoolean(Common.SAVED_SORT_ASCENDING, sortAscending);
        editor.apply();
    }

    public static boolean notificationsEnabled(Context context) {
        final SharedPreferences savedPrefs = context.getSharedPreferences(Common.PREFS_NAME, 0);
        return savedPrefs.getBoolean(Common.SAVED_NOTIFY_ENABLED, true);
    }

    public static void setNotificationsEnabled(Context context, boolean notificationsEnabled) {
        final SharedPreferences savedPrefs = context.getSharedPreferences(Common.PREFS_NAME, 0);
        SharedPreferences.Editor editor = savedPrefs.edit();
        editor.putBoolean(Common.SAVED_NOTIFY_ENABLED, notificationsEnabled);
        editor.apply();
    }
}
