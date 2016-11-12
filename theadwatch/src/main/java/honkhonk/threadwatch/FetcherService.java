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
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_FOO = "honkhonk.threadwatch.action.FOO";
    private static final String ACTION_BAZ = "honkhonk.threadwatch.action.BAZ";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "honkhonk.threadwatch.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "honkhonk.threadwatch.extra.PARAM2";

    public FetcherService() {
        super("FetcherService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionFoo(Context context, String param1, String param2) {
        Intent intent = new Intent(context, FetcherService.class);
        intent.setAction(ACTION_FOO);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionBaz(Context context, String param1, String param2) {
        Intent intent = new Intent(context, FetcherService.class);
        intent.setAction(ACTION_BAZ);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
//        if (intent != null) {
//            final String action = intent.getAction();
//            if (ACTION_FOO.equals(action)) {
//                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
//                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
//                handleActionFoo(param1, param2);
//            } else if (ACTION_BAZ.equals(action)) {
//                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
//                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
//                handleActionBaz(param1, param2);
//            }
//        }

        // DO WERK
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if (!isConnected) {
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
        notificationManager.notify(10001, builder.build());
    }

    public void threadRetrievalFailed(final ArrayList<ThreadModel> threads) {
        threadsRetrieved(threads);
    }
}
