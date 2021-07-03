package honkhonk.threadwatch.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

import static honkhonk.threadwatch.retrievers.PageDataRetriever.THREAD_PAGE_UNKNOWN;

public class PageDataResponse {
    @SerializedName("page")
    public int page;

    @SerializedName("threads")
    public ArrayList<ThreadPageDataModel> threads;

    public int pageForId(final int id) {
        for (ThreadPageDataModel thread : threads) {
            if (id == thread.id) {
                return page;
            }
        }

        return THREAD_PAGE_UNKNOWN;
    }
}
