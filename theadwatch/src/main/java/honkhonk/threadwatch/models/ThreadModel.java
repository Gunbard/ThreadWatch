package honkhonk.threadwatch.models;

/**
 * Created by Gunbard on 10/13/2016.
 */

public class ThreadModel {
    public String board;

    public String id;

    public String subject;

    public String comment;

    public int imageCount;

    public String getUrl() {
        //http(s)://boards.4chan.org/board/thread/num

        if (board != null && id != null) {
            return "https://boards.4chan.org/" + board + "/thread/" + id;
        } else {
            return null;
        }

    }
}
