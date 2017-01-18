package honkhonk.threadwatch.helpers;

/**
 * Global common constants
 * Created by Gunbard on 10/15/2016.
 */

final public class Common {
    // Available sorting options
    public enum SortOptions {
        ADD_DATE,
        BOARD,
        TITLE,
        DATE,
        BUMP_DATE
    }

    // Unique request code for refreshing in the background
    final public static int ALARM_ID = 12345;

    // Unique ID for accessing the notification
    final public static int NOTIFICATION_ID = 67890;

    // Unique ID for getting a notification that the settings closed
    final public static int SETTINGS_CLOSED_ID = 56789;

    // Helper to index SortOptions. This is not built into Java for some reason.
    final public static SortOptions[] sortOptionsValues = SortOptions.values();

    // Shared prefs keys
    final public static String PREFS_NAME = "ThreadWatcherSharedPrefs";
    final public static String SAVED_THREAD_DATA = "SavedThreadData";
    final public static String SAVED_SORT_MODE = "SavedSortMode";
    final public static String SAVED_SORT_ASCENDING = "SavedSortAscending";
    final public static String SAVED_NOTIFY_ENABLED = "NotificationsEnabled";

    // Chrome extra key
    final public static String SHARE_TEXT_KEY = "android.intent.extra.TEXT";

    // One minute in milliseconds
    final public static int ONE_MINUTE_IN_MILLIS = 60000;

    // Default 5 minute refresh rate
    final public static int DEFAULT_REFRESH_TIMEOUT = 5;
}
