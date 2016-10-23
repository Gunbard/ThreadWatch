package honkhonk.threadwatch.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import honkhonk.threadwatch.models.ThreadModel;

/**
 * Helper class used to sort threads
 * Created by Gunbard on 10/15/2016.
 */

final public class ThreadSorter {
    /**
     * Sorts a list of threads based on the sort option
     * @param list The list of threads to sort
     * @param sortOption Thread data field to sort by
     * @param ascending Sort by ascending if true, descending otherwise
     */
    public static void sort(ArrayList<ThreadModel> list, final Common.SortOptions sortOption,
                     final boolean ascending) {
        switch (sortOption) {
            case BUMP_DATE:
                sortByLatestUpdated(list, ascending);
                break;
            case BOARD:
                sortByBoard(list, ascending);
                break;
            case TITLE:
                sortByTitle(list, ascending);
                break;
            case DATE:
                sortByDate(list, ascending);
                break;
            default:
            case ADD_DATE:
                sortByAddDate(list, ascending);
                break;

        }
    }

    /**
     * Sorts the list by the date the thread was added in the app
     * @param list The list of threads to sort
     * @param ascending Sort by ascending if true, descending otherwise
     */
    private static void sortByAddDate(ArrayList<ThreadModel> list, final boolean ascending) {
        Collections.sort(list, new Comparator<ThreadModel>() {
            @Override
            public int compare(final ThreadModel lhs, final ThreadModel rhs) {
                if (ascending) {
                    return lhs.dateAdded.compareTo(rhs.dateAdded);
                } else {
                    return rhs.dateAdded.compareTo(lhs.dateAdded);
                }
            }
        });
    }

    /**
     * Sort by board name
     * @param list The list of threads to sort
     * @param ascending Sort by ascending if true, descending otherwise
     */
    private static void sortByBoard(ArrayList<ThreadModel> list, final boolean ascending) {
        Collections.sort(list, new Comparator<ThreadModel>() {
            @Override
            public int compare(final ThreadModel lhs, final ThreadModel rhs) {
                if (ascending) {
                    return lhs.getBoardName().compareTo(rhs.getBoardName());
                } else {
                    return rhs.getBoardName().compareTo(lhs.getBoardName());
                }
            }
        });
    }

    /**
     * Sort by the thread title. Title is the subject if it exists, comment otherwise
     * @param list The list of threads to sort
     * @param ascending Sort by ascending if true, descending otherwise
     */
    private static void sortByTitle(ArrayList<ThreadModel> list, final boolean ascending) {
        Collections.sort(list, new Comparator<ThreadModel>() {
            @Override
            public int compare(final ThreadModel lhs, final ThreadModel rhs) {
                if (ascending) {
                    return lhs.getTitle().compareTo(rhs.getTitle());
                } else {
                    return rhs.getTitle().compareTo(lhs.getTitle());
                }
            }
        });
    }

    /**
     * Sort by the thread's creation date
     * @param list The list of threads to sort
     * @param ascending Sort by ascending if true, descending otherwise
     */
    private static void sortByDate(ArrayList<ThreadModel> list, final boolean ascending) {
        Collections.sort(list, new Comparator<ThreadModel>() {
            @Override
            public int compare(final ThreadModel lhs, final ThreadModel rhs) {
                if (ascending) {
                    return Long.compare(lhs.time, rhs.time);
                } else {
                    return Long.compare(rhs.time, lhs.time);
                }
            }
        });
    }

    /**
     * Sort by the date of the latest post
     * @param list The list of threads to sort
     * @param ascending Sort by ascending if true, descending otherwise
     */
    private static void sortByLatestUpdated(ArrayList<ThreadModel> list, final boolean ascending) {
        Collections.sort(list, new Comparator<ThreadModel>() {
            @Override
            public int compare(final ThreadModel lhs, final ThreadModel rhs) {
                if (ascending) {
                    return Long.compare(lhs.latestTime, rhs.latestTime);
                } else {
                    return Long.compare(rhs.latestTime, lhs.latestTime);
                }
            }
        });
    }
}
