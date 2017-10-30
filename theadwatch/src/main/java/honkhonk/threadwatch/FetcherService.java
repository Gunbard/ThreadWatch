package honkhonk.threadwatch;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
    final public static String TAG = FetcherService.class.getSimpleName();

    private HashMap<ThreadModel, Integer> updatedThreads = new HashMap<>();
    private boolean canVibrate;
    final private BroadcastReceiver resetUpdatedThreadsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updatedThreads.clear();
        }
    };

    public FetcherService() {
        super(FetcherService.class.getSimpleName());
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(resetUpdatedThreadsReceiver, new IntentFilter("wtfistreds"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(resetUpdatedThreadsReceiver);
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
            Log.d(TAG, "Can't refresh, no network!");
            return;
        }

        final Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        canVibrate = vibrator.hasVibrator();

        final SharedPreferences savedPrefs = getSharedPreferences(Common.PREFS_NAME, 0);
        final String listDataAsJson = savedPrefs.getString(Common.SAVED_THREAD_DATA, null);
        if (listDataAsJson == null) {
            Log.d(TAG, "No threads to fetch.");
            return;
        }

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

        final String listDataAsJson = (new Gson()).toJson(threads);

        SharedPreferences savedPrefs = getSharedPreferences(Common.PREFS_NAME, 0);
        SharedPreferences.Editor editor = savedPrefs.edit();
        editor.putString(Common.SAVED_THREAD_DATA, listDataAsJson);
        editor.apply();

        final SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean vibrateNotify = appSettings.getBoolean("pref_notify_vibrate", true);
        final boolean notificationsEnabled =
                savedPrefs.getBoolean(Common.SAVED_NOTIFY_ENABLED, true);

        if (!notificationsEnabled) {
            return;
        }

        String updatedThreadsText = "";

        for (final ThreadModel thread : threads) {
            if (thread.newReplyCount > 0 && !thread.disabled) {
                Integer runningTotal = updatedThreads.get(thread);
                if (runningTotal != null) {
                    final Integer newTotal = runningTotal + thread.newReplyCount;
                    updatedThreads.put(thread, newTotal);
                } else {
                    updatedThreads.put(thread, thread.newReplyCount);
                }
            }
        }

        Intent newDataIntent = new Intent("wtfisthis");
        final String updatedThreadsAsJson = (new Gson()).toJson(updatedThreads);
        newDataIntent.putExtra("updatedthreads", updatedThreadsAsJson);
        LocalBroadcastManager.getInstance(this).sendBroadcast(newDataIntent);

        for (Map.Entry<ThreadModel, Integer> entry : updatedThreads.entrySet()) {
            updatedThreadsText += entry.getKey().getTruncatedTitle() +
                    " (+" + entry.getValue() + ")" + "\n";
        }

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtra("cheese", "yes");

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        final int defaults = (vibrateNotify && canVibrate) ? NotificationCompat.DEFAULT_ALL :
                NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_SOUND;

        NotificationCompat.Builder builder =
                (android.support.v7.app.NotificationCompat.Builder)
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setStyle(new NotificationCompat.BigTextStyle().bigText(updatedThreadsText))
                                .setContentTitle("New replies!")
                                .setContentText("New posts in " +
                                        Integer.toString(updatedThreads.size()) + " thread(s).")
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setDefaults(defaults)
                                .setContentIntent(resultPendingIntent);

        if (!vibrateNotify) {
            // Workaround to prevent vibrate even when the device is in vibrate mode
            builder.setVibrate(new long[]{0L});
        }
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
