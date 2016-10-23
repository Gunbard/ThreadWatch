package honkhonk.threadwatch.retrievers;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.ArrayList;

import honkhonk.threadwatch.ThreadWatch;
import honkhonk.threadwatch.models.PostModel;
import honkhonk.threadwatch.models.PostsResponse;
import honkhonk.threadwatch.models.ThreadModel;

/**
 * Retrieves all posts from a thread
 * Created by Gunbard on 10/11/2016.
 */

public class PostsRetriever {
    /**
     * List of listeners to notify about retrieval events
     */
    private ArrayList<PostsRetrieverListener> listeners = new ArrayList<>();

    /**
     * Retrieval events
     */
    public interface PostsRetrieverListener {
        /**
         * Posts were retrieved successfully
         * @param context Context of the retrieval
         * @param thread The thread the posts were retrieved from
         * @param posts The lists of posts that were retrieved
         */
        void postsRetrieved(final Context context,
                            final ThreadModel thread,
                            final ArrayList<PostModel> posts);

        /**
         * Posts could not be retrieved. Site could be down, thread could have 404'd
         * @param context Context of the retrieval
         * @param thread The thread the posts were supposed to be retrieved from
         */
        void postsRetrievalFailed(final Context context, final ThreadModel thread);
    }

    /**
     * Adds to the list of listeners
     * @param listener PostsRetrieverListener to notify about events
     */
    public void addListener(final PostsRetrieverListener listener) {
        listeners.add(listener);
    }

    /**
     * Makes a request to get posts from a thread
     * @param context Context for the retrieval
     * @param thread The thread to retrieve posts from. Must have the board and id set.
     */
    public void retrievePosts(final Context context, final ThreadModel thread) {
        final String url =
                "https://a.4cdn.org/"+ thread.board + "/thread/" + thread.id + ".json";

        final JsonObjectRequest retrieveRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        final PostsResponse postsResponse =
                                (new Gson()).fromJson(response.toString(), PostsResponse.class);

                        for (final PostsRetrieverListener listener : listeners) {
                            if (postsResponse.posts != null) {
                                listener.postsRetrieved(context, thread, postsResponse.posts);
                            } else {
                                listener.postsRetrievalFailed(context, thread);
                            }
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        for (final PostsRetrieverListener listener : listeners) {
                            listener.postsRetrievalFailed(context, thread);
                        }
                    }
                });

        ThreadWatch.getInstance(context).addToRequestQueue(retrieveRequest);
    }
}
