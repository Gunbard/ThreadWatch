package honkhonk.threadwatch.retrievers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.ArrayList;

import honkhonk.threadwatch.ThreadWatch;
import honkhonk.threadwatch.models.PageDataResponse;
import honkhonk.threadwatch.models.ThreadModel;

public class PageDataRetriever {
    final public static int THREAD_PAGE_UNKNOWN = -1;

    private ArrayList<PageDataRetrieverListener> listeners = new ArrayList<>();

    public interface PageDataRetrieverListener {
        void pageDataRetrievalFinished(final ThreadModel thread, int pageNumber);
    }

    public void addListener(final PageDataRetrieverListener listener) {
        listeners.add(listener);
    }

    public void retrievePageData(final Context context, final ThreadModel thread) {
        ConnectivityManager cm =
                (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if (!isConnected) {
            Log.i(this.getClass().getSimpleName(), "No network, so didn't retrieve!");
            for (PageDataRetrieverListener listener : listeners) {
                listener.pageDataRetrievalFinished(thread, THREAD_PAGE_UNKNOWN);
            }
            return;
        }

        final String url =
                "https://a.4cdn.org/"+ thread.board + "/threads.json";

        final JsonObjectRequest retrieveRequest = new JsonObjectRequest
            (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    PageDataResponse[] pageDataResponse =
                        (new Gson()).fromJson(response.toString(), PageDataResponse[].class);

                    int pageNumber = THREAD_PAGE_UNKNOWN;
                    // Dig through threads to find the current thread and return the page number
                    for (PageDataResponse pageData : pageDataResponse) {
                        pageNumber = pageData.pageForId(Integer.parseInt(thread.id));
                        if (pageNumber != THREAD_PAGE_UNKNOWN) {
                            break;
                        }
                    }

                    for (PageDataRetrieverListener listener : listeners) {
                        listener.pageDataRetrievalFinished(thread, pageNumber);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    for (PageDataRetrieverListener listener : listeners) {
                        listener.pageDataRetrievalFinished(thread, THREAD_PAGE_UNKNOWN);
                    }
                }
            });

        ThreadWatch.getInstance(context).addToRequestQueue(retrieveRequest);
    }
}
