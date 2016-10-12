package honkhonk.threadwatch.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Gunbard on 10/11/2016.
 */

public class PostModel {
    @SerializedName("sub")
    public String subject;

    @SerializedName("com")
    public String comment;

    @SerializedName("no")
    public int number;

    // 0 indicates thread OP
    @SerializedName("resto")
    public int repliesTo;
}
