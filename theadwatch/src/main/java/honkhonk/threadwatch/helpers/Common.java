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

    // Helper to index SortOptions. This is not built into Java for some reason.
    final public static SortOptions[] sortOptionsValues = SortOptions.values();

    // Shared prefs keys
    final public static String PREFS_NAME = "ThreadWatcherSharedPrefs";
    final public static String SAVED_THREAD_DATA = "SavedThreadData";
    final public static String SAVED_SORT_MODE = "SavedSortMode";
    final public static String SAVED_SORT_ASCENDING = "SavedSortAscending";

    // Chrome extra key
    final public static String SHARE_TEXT_KEY = "android.intent.extra.TEXT";

    // Default 60 second timeout
    final public static int DEFAULT_REFRESH_TIMEOUT = 60000;
}
