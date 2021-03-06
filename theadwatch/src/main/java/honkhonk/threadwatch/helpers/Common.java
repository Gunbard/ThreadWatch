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

    // Unique request code for importing backup files
    final public static int IMPORT_REQUEST_CODE = 42;

    // Unique request code for exporting backup files
    final public static int EXPORT_REQUEST_CODE = 43;

    // Unique ID for accessing the notification
    final public static int NOTIFICATION_ID = 67890;

    // Unique ID for getting a notification that the settings closed
    final public static int SETTINGS_CLOSED_ID = 56789;

    final public static String SETTINGS_CLOSED_SHOULD_REFRESH = "SettingsClosedShouldRefresh";

    // Helper to index SortOptions. This is not built into Java for some reason.
    final public static SortOptions[] sortOptionsValues = SortOptions.values();

    // Shared prefs keys
    final public static String PREFS_NAME = "ThreadWatcherSharedPrefs";
    final public static String SAVED_THREAD_DATA = "SavedThreadData";
    final public static String SAVED_UPDATED_THREADS = "SavedUpdatedThreads";
    final public static String SAVED_SORT_MODE = "SavedSortMode";
    final public static String SAVED_SORT_ASCENDING = "SavedSortAscending";
    final public static String SAVED_NOTIFY_ENABLED = "NotificationsEnabled";
    final public static String PREFS_CAN_VIBRATE = "CanVibrate";

    // Chrome extra key
    final public static String SHARE_TEXT_KEY = "android.intent.extra.TEXT";

    // Common time granularity in milliseconds
    final public static int ONE_MINUTE_IN_MILLIS = 60000;
    final public static int ONE_HOUR_IN_MILLIS = 3600000;
    final public static int ONE_DAY_IN_MILLIS = 86400000;

    // Default 5 minute refresh rate
    final public static int DEFAULT_REFRESH_TIMEOUT = 5;

    final public static String FETCH_JOB_BROADCAST_KEY = "TWFetchJobBroadcastKey";
    final public static String FETCH_JOB_SUCCEEDED_KEY = "TWFetchJobSucceededKey";

    final public static String CHANNEL_ID = "TWNotificationChannel";

    // If a thread is on this page, a deletion/archive warning will be shown.
    // All the boards appear to have a max 10 pages at this time.
    final public static int LAST_PAGE = 10;
}
