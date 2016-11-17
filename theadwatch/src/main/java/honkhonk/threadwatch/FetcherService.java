package honkhonk.threadwatch;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

import honkhonk.threadwatch.helpers.Common;
import honkhonk.threadwatch.models.ThreadModel;
import honkhonk.threadwatch.retrievers.ThreadsRetriever;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class FetcherService extends IntentService
        implements ThreadsRetriever.ThreadRetrieverListener {
    public FetcherService() {
        super(FetcherService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if (!isConnected) {
            // Don't refresh if offline
            return;
        }

        final String listDataAsJson = intent.getStringExtra(Common.SAVED_THREAD_DATA);
        final ArrayList<ThreadModel> listDataSource = (new Gson()).fromJson(listDataAsJson,
                new TypeToken<ArrayList<ThreadModel>>() {}.getType());

        ThreadsRetriever threadsRetriever = new ThreadsRetriever();
        threadsRetriever.addListener(this);
        threadsRetriever.retrieveThreadData(this, listDataSource);
    }

    /**
     * ThreadRetrieverListener
     */
    public void threadsRetrieved(final ArrayList<ThreadModel> threads) {
        boolean threadWasUpdated = false;
        for (final ThreadModel thread : threads) {
            if (thread.newReplyCount > 0 && !thread.disabled) {
                threadWasUpdated = true;
                break;
            }
        }

        if (!threadWasUpdated) {
            return;
        }

        // Send new stuff to activity
        Intent newDataIntent = new Intent("wtfisthis");
        final String listDataAsJson = (new Gson()).toJson(threads);
        newDataIntent.putExtra(Common.SAVED_THREAD_DATA, listDataAsJson);
        LocalBroadcastManager.getInstance(this).sendBroadcast(newDataIntent);
    }

    public void threadRetrievalFailed(final ArrayList<ThreadModel> threads) {
        threadsRetrieved(threads);
    }
}
