package honkhonk.threadwatch.models;

import java.util.Calendar;

/**
 * Created by Gunbard on 10/13/2016.
 */

public class ThreadModel {
    public String board;

    public String id;

    public String name;

    public String subject;

    public String comment;

    public int replyCount;

    public int replyCountDelta;

    public int imageCount;

    public boolean closed;

    public boolean archived;

    public long time;

    public long latestTime;

    public Calendar dateAdded;

    public String getUrl() {
        //http(s)://boards.4chan.org/board/thread/num

        if (board != null && id != null) {
            return "https://boards.4chan.org/" + board + "/thread/" + id;
        } else {
            return null;
        }
    }

    public String getTitle() {
        if (subject != null && subject.length() > 0) {
            return subject;
        } else {
            return getSanitizedComment();
        }
    }

    public String getBoardName() {
        return board.replace("/", "");
    }

    public String getSanitizedComment() {
        if (comment != null && !comment.equals("")) {
            return android.text.Html.fromHtml(comment).toString();
        } else {
            return "";
        }
    }
}
