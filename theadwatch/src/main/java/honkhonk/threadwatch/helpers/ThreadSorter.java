package honkhonk.threadwatch.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import honkhonk.threadwatch.models.ThreadModel;

/**
 * Created by Gunbard on 10/15/2016.
 */

final public class ThreadSorter {
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
