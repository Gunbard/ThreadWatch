package honkhonk.threadwatch;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;

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
        // TODO: Keep a running total
        // Only show notification if there's new posts in any thread
        String updatedThreads = "";
        int threadsWithReplies = 0;
        for (final ThreadModel thread : threads) {
            if (thread.newReplyCount > 0) {
                threadsWithReplies++;
                updatedThreads += thread.getTruncatedTitle() +
                        " (" + thread.replyCountDelta + ")" + "\n";
            }
        }

        if (updatedThreads.length() == 0) {
            return;
        }

        // Send new stuff to activity
        Intent newDataIntent = new Intent("wtfisthis");
        final String listDataAsJson = (new Gson()).toJson(threads);
        newDataIntent.putExtra(Common.SAVED_THREAD_DATA, listDataAsJson);
        LocalBroadcastManager.getInstance(this).sendBroadcast(newDataIntent);

        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            );

        NotificationCompat.Builder builder =
            (android.support.v7.app.NotificationCompat.Builder)
                new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(updatedThreads))
                    .setContentTitle("New replies!")
                    .setContentText("New posts in " +
                            Integer.toString(threadsWithReplies) + " thread(s).")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                    .setContentIntent(resultPendingIntent);

        // Gets an instance of the NotificationManager service
        NotificationManager notificationManager =
            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Builds the notification and issues it.
        builder.setAutoCancel(true);
        notificationManager.notify(Common.NOTIFICATION_ID, builder.build());
    }

    public void threadRetrievalFailed(final ArrayList<ThreadModel> threads) {
        threadsRetrieved(threads);
    }
}
