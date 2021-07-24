package honkhonk.threadwatch.models;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Model of a basic thread
 * Created by Gunbard on 10/13/2016.
 */

public class ThreadModel {
    /**
     * Longest allowed length of a truncated string
     */
    final private static int maxTruncatedLength = 30;

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
     * Number of new posts since the last reset
     */
    public int replyCountDelta;

    /**
     * Number of new posts since the last check
     */
    public int newReplyCount;

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
     * Whether or not the thread has new replies to (you)
     */
    public boolean newRepliesToYou;

    /**
     * Whether or not to notify only if the thread has new replies to (you)
     */
    public boolean notifyOnlyIfRepliesToYou;

    /**
     * Whether or not the thread hit the last page on the current refresh.
     */
    public boolean isNowOnLastPage;

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
     * The last post's id
     */
    public long lastPostId;

    /**
     * The UNIX timestamp + microtime of the OP attachment/image
     */
    public long attachmentId;

    /**
     * Base64 encoded string of OP's image as a JPEG thumbnail, if it exists
     */
    public String thumbnail;

    /**
     * The current page index the thread is on
     */
    public int currentPage;

    /**
     * List of reply ids to track using the post number as the key and the value
     * as a list of comment strings that reference it
     */
    public HashMap<String, ArrayList<PostModel>> replyIds = new HashMap<>();

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
        if (subject != null && !subject.equals("")) {
            return android.text.Html.fromHtml(subject)
                    .toString().replaceAll(System.getProperty("line.separator"), " ");

        } else {
            return getSanitizedComment();
        }
    }

    /**
     * Truncated title text
     * @return Truncated title with ellipsis
     */
    public String getTruncatedTitle() {
        String title = getTitle();
        if (title.length() > maxTruncatedLength) {
            title = title.substring(0, maxTruncatedLength) + "...";
        }

        return title;
    }

    /**
     * @return The name of the board with the slashes
     */
    public String getBoardName() {
        return board.replace("/", "");
    }

    /**
     * @return The comment with any HTML sanitized/escaped and new lines removed,
     * blank string if no comment
     */
    public String getSanitizedComment() {
        if (comment != null && !comment.equals("")) {
            return android.text.Html.fromHtml(comment)
                    .toString().replaceAll(System.getProperty("line.separator"), " ");
        } else {
            return "";
        }
    }

    /**
     * @return Whether the thread is available or not
     */
    public boolean isAvailable() {
        return !(closed || archived || disabled || notFound);
    }

    @Override
    public int hashCode()
    {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ThreadModel) {
            ThreadModel object = (ThreadModel) o;
            return id.equals(object.id);
        } else {
            return false;
        }
    }
}
