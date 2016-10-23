package honkhonk.threadwatch.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Response model for getting posts
 * Created by Gunbard on 10/11/2016.
 */

public class PostsResponse {
    /**
     * Array of posts
     */
    @SerializedName("posts")
    public ArrayList<PostModel> posts;
}
