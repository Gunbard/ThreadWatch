package honkhonk.threadwatch.models;

import java.util.Calendar;

/**
 * Model of a basic thread
 * Created by Gunbard on 10/13/2016.
 */

public class ThreadModel {
    /**
     * Name of the board
     */
    public String board;

    /**
     * Thread's id number
     */
    public String id;

    /**
     * Poster's name
     */
    public String name;

    /**
     * Subject of the thread
     */
    public String subject;

    /**
     * Opening comment of the thread
     */
    public String comment;

    /**
     * Number of replies in the thread
     */
    public int replyCount;

    /**
     * Number of new posts since the last check
     */
    public int replyCountDelta;

    /**
     * Number of images in the thread
     */
    public int imageCount;

    /**
     * Whether or not the thread is closed
     */
    public boolean closed;

    /**
     * Whether or not the thread has been archived
     */
    public boolean archived;

    /**
     * Whether or not the thread 404'd on the last check
     */
    public boolean notFound;

    /**
     * Whether or not the thread should be checked on refresh
     */
    public boolean disabled;

    /**
     * Whether or not this is the first time the thread has been refreshed.
     * Used to prevent the new post count from updating on add.
     */
    public boolean firstRefresh;

    /**
     * The UNIX timestamp (in seconds) of when the thread was made
     */
    public long time;

    /**
     * The UNIX timestamp (in seconds) of the latest post
     */
    public long latestTime;

    /**
     * The date when the thread was added to the app
     */
    public Calendar dateAdded;

    /**
     * @return The url of the thread, null if the board or id are blank
     */
    public String getUrl() {
        //http(s)://boards.4chan.org/board/thread/num

        if (board != null && id != null) {
            return "https://boards.4chan.org/" + board + "/thread/" + id;
        } else {
            return null;
        }
    }

    /**
     * @return The subject of the thread, the sanitized comment otherwise (which can be blank)
     */
    public String getTitle() {
        if (subject != null && subject.length() > 0) {
            return android.text.Html.fromHtml(subject).toString();
        } else {
            return getSanitizedComment();
        }
    }

    /**
     * @return The name of the board with the slashes
     */
    public String getBoardName() {
        return board.replace("/", "");
    }

    /**
     * @return The comment with any HTML sanitized/escaped, blank string if no comment
     */
    public String getSanitizedComment() {
        if (comment != null && !comment.equals("")) {
            return android.text.Html.fromHtml(comment).toString();
        } else {
            return "";
        }
    }
}
