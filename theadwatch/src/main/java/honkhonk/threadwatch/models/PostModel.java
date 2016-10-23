package honkhonk.threadwatch.models;

import com.google.gson.annotations.SerializedName;

/**
 * Model of a basic post. Could be OP or any other post in a thread
 * Created by Gunbard on 10/11/2016.
 */

public class PostModel {
    /**
     * The poster's name
     */
    @SerializedName("name")
    public String name;

    /**
     * The subject of the post
     */
    @SerializedName("sub")
    public String subject;

    /**
     * The post's comment
     */
    @SerializedName("com")
    public String comment;

    /**
     * Post number
     */
    @SerializedName("no")
    public int number;

    /**
     * The thread/post this post is replying to.
     * 0 indicates thread OP
     */
    @SerializedName("resto")
    public int repliesTo;

    /**
     * Total number of replies in the thread, populated in OP only
     */
    @SerializedName("replies")
    public int replyCount;

    /**
     * Total number of images in the thread, populated in OP only
     */
    @SerializedName("images")
    public int imageCount;

    /**
     * Whether or not the thread is closed, populated in OP only
     */
    @SerializedName("closed")
    public int closed;

    /**
     * Whether or not the thread is archived, populated in OP only
     */
    @SerializedName("archived")
    public int archived;

    /**
     * The UNIX timestamp (in seconds) of when the post was made
     */
    @SerializedName("time")
    public int time;
}
