package honkhonk.threadwatch.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by Gunbard on 10/11/2016.
 */

public class PostsResponse {
    @SerializedName("posts")
    public ArrayList<PostModel> posts;
}
