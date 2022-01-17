package honkhonk.threadwatch.retrievers;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import honkhonk.threadwatch.ThreadWatch;
import honkhonk.threadwatch.models.ThreadModel;

/**
 * Retrieves the thumbnail version of an image from 4chan's CDN
 * Created by Gunbard on 6/17/2021
 */
public class ThumbnailRetriever {
    // Max 48x48 pixels
    final static int MAX_THUMBNAIL_DIMENSION = 48;

    // List of listeners to notify
    private ArrayList<ThumbnailRetrieverListener> listeners = new ArrayList<>();

    public interface ThumbnailRetrieverListener {
        /**
         * Callback for when the retrieval finishes
         * @param thread The thread the retrieval was based on
         * @param encodedThumbnail A base 64 encoded string of the thumbnail JPEG or null
         *                         if thread did not have an image/attachment for OP
         */
        void thumbnailRetrievalFinished(final ThreadModel thread, final String encodedThumbnail);
    }

    /**
     * Adds to the list of listeners
     * @param listener ThumbnailRetrieverListener to notify about events
     */
    public void addListener(final ThumbnailRetrieverListener listener) {
        listeners.add(listener);
    }

    /**
     * Makes a request
     * @param context Context for the retrieval
     * @param thread The thread to get the thumbnail from
     */
    public void retrieveThumbnail(final Context context, final ThreadModel thread) {
        if (thread.attachmentId == 0) {
            Log.i(this.getClass().getSimpleName(),
                    "Thread does not have OP image, so didn't retrieve!");
            for (ThumbnailRetrieverListener listener : listeners) {
                listener.thumbnailRetrievalFinished(thread, null);
            }
            return;
        }

        ConnectivityManager cm =
                (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if (!isConnected) {
            Log.i(this.getClass().getSimpleName(), "No network, so didn't retrieve!");
            for (ThumbnailRetrieverListener listener : listeners) {
                listener.thumbnailRetrievalFinished(thread, null);
            }
            return;
        }

        final String url =
                "https://i.4cdn.org/"+ thread.board + "/" + thread.attachmentId + "s.jpg";

        final ImageRequest retrieveRequest = new ImageRequest(url,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap responseImage) {
                        // Base64 encode image to string. This should make it
                        // easier to cache/backup locally.
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        responseImage.compress(Bitmap.CompressFormat.JPEG,
                                90, byteArrayOutputStream);

                        for (ThumbnailRetrieverListener listener : listeners) {
                            listener.thumbnailRetrievalFinished(thread,
                                    Base64.encodeToString(byteArrayOutputStream.toByteArray(),
                                            Base64.DEFAULT));
                        }
                    }
                }, MAX_THUMBNAIL_DIMENSION, MAX_THUMBNAIL_DIMENSION,
                ImageView.ScaleType.FIT_CENTER,
                Bitmap.Config.ARGB_8888,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        for (ThumbnailRetrieverListener listener : listeners) {
                            listener.thumbnailRetrievalFinished(thread, null);
                        }
                    }
                });

        retrieveRequest.setShouldCache(false);
        ThreadWatch.getInstance(context).addToRequestQueue(retrieveRequest);
    }
}
