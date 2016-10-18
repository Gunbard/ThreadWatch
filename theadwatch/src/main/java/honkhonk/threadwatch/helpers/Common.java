package honkhonk.threadwatch.helpers;

/**
 * Created by Gunbard on 10/15/2016.
 */

final public class Common {
    public enum SortOptions {
        ADD_DATE,
        BOARD,
        TITLE,
        DATE,
        BUMP_DATE
    }

    public static final SortOptions[] sortOptionsValues = SortOptions.values();

    // Shared prefs keys
    final public static String PREFS_NAME = "ThreadWatcherSharedPrefs";
    final public static String SAVED_THREAD_DATA = "SavedThreadData";
    final public static String SAVED_SORT_MODE = "SavedSortMode";
    final public static String SAVED_SORT_ASCENDING = "SavedSortAscending";
}
