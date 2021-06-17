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
import honkhonk.threadwatch.models.PostModel;
import honkhonk.threadwatch.models.PostsResponse;
import honkhonk.threadwatch.models.ThreadModel;

/**
 * Retrieves all posts from a thread
 * Created by Gunbard on 10/11/2016.
 */

public class PostsRetriever implements ThumbnailRetriever.ThumbnailRetrieverListener {
    /**
     * List of listeners to notify about retrieval events
     */
    private ArrayList<PostsRetrieverListener> listeners = new ArrayList<>();

    /**
     * Cached context
     */
    private Context context;

    /**
     * Cached response
     */
    private PostsResponse response;

    /**
     * The current thread this post retrieval is for
     */
    private ThreadModel thread;

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
        this.context = context;
        this.thread = thread;

        ConnectivityManager cm =
                (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if (!isConnected) {
            Log.i(this.getClass().getSimpleName(), "No network, so didn't refresh!");
            for (final PostsRetrieverListener listener : listeners) {
                listener.postsRetrievalFailed(context, thread);
            }
            return;
        }

        final String url =
                "https://a.4cdn.org/"+ thread.board + "/thread/" + thread.id + ".json";

        final JsonObjectRequest retrieveRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        PostsRetriever.this.thread.notFound = false;
                        PostsRetriever.this.response =
                                (new Gson()).fromJson(response.toString(), PostsResponse.class);

                        PostsRetriever.this.thread.attachmentId =
                                PostsRetriever.this.response.posts.get(0).attachmentId;

                        // Try to get OP thumbnail, too, if needed
                        if (PostsRetriever.this.thread.thumbnail == null) {
                            ThumbnailRetriever thumbnailRetriever = new ThumbnailRetriever();
                            thumbnailRetriever.addListener(PostsRetriever.this);
                            thumbnailRetriever.retrieveThumbnail(context, PostsRetriever.this.thread);
                        } else {
                            thumbnailRetrievalFinished(PostsRetriever.this.thread,
                                    PostsRetriever.this.thread.thumbnail);
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error != null &&
                            error.networkResponse != null &&
                            error.networkResponse.statusCode == 404) {
                            PostsRetriever.this.thread.notFound = true;
                        }

                        for (final PostsRetrieverListener listener : listeners) {
                            listener.postsRetrievalFailed(context, thread);
                        }
                    }
                });

        ThreadWatch.getInstance(context).addToRequestQueue(retrieveRequest);
    }

    /****************************************
     * ThumbnailRetriever.ThumbnailRetrieverListener
     ****************************************/
    @Override
    public void thumbnailRetrievalFinished(final ThreadModel thread, final String encodedImage) {
        this.thread.thumbnail = encodedImage;

        for (final PostsRetrieverListener listener : listeners) {
            if (response.posts != null) {
                listener.postsRetrieved(context, this.thread, response.posts);
            } else {
                listener.postsRetrievalFailed(context, this.thread);
            }
        }
    }
}
