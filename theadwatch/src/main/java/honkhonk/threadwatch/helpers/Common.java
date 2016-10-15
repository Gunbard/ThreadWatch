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
}
