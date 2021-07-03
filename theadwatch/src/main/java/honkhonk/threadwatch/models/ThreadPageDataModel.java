package honkhonk.threadwatch.models;

import com.google.gson.annotations.SerializedName;

public class ThreadPageDataModel {
    @SerializedName("no")
    public int id;

    @SerializedName("last_modified")
    public int lastModified;

    @SerializedName("replies")
    public int replies;
}
