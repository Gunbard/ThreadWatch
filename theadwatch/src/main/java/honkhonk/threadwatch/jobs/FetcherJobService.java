package honkhonk.threadwatch.jobs;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.HashMap;

import honkhonk.threadwatch.R;
import honkhonk.threadwatch.activities.MainActivity;
import honkhonk.threadwatch.helpers.Common;
import honkhonk.threadwatch.managers.PreferencesDataManager;
import honkhonk.threadwatch.managers.ThreadDataManager;
import honkhonk.threadwatch.models.ThreadModel;
import honkhonk.threadwatch.retrievers.ThreadsRetriever;

/**
 * Job that will actually do the work to get all thread data.
 * This job will actually build and issue the Android notification.
 */
public class FetcherJobService extends JobService implements ThreadsRetriever.ThreadRetrieverListener {
    final public static String TAG = FetcherJobService.class.getSimpleName();
    final public static int FETCHER_JOB_ID = 0;

    public static void scheduleFetcherJobService(Context context, boolean noWait) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        ComponentName serviceComponent = new ComponentName(context, FetcherJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(FETCHER_JOB_ID, serviceComponent);

        if (noWait) {
            builder.setMinimumLatency(1); // Don't wait
            builder.setOverrideDeadline(1); // Perform immediately
        } else {
            final SharedPreferences appSettings =
                    PreferenceManager.getDefaultSharedPreferences(context);

            final String refreshValue = appSettings.getString("pref_refresh_rate", "5");
            int refreshRate = Common.DEFAULT_REFRESH_TIMEOUT;
            if (refreshValue.length() > 0) {
                refreshRate = Integer.parseInt(refreshValue);
            }

            if (refreshRate < 1) {
                sendRefreshFinishedBroadcast(context);
                return;
            }

            builder.setMinimumLatency(Common.ONE_MINUTE_IN_MILLIS * refreshRate); // Minimum wait delay
            builder.setOverrideDeadline((Common.ONE_MINUTE_IN_MILLIS * refreshRate) + 30000); // Maximum delay
        }

        builder.setRequiresCharging(false); // we don't care if the device is charging or not
        jobScheduler.schedule(builder.build());
    }

    public static Boolean fetcherIsScheduled(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        for (JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == FETCHER_JOB_ID) {
                return true;
            }
        }

        return false;
    }

    public static void stopFetcher(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(FETCHER_JOB_ID);
        Log.i(TAG, "JobService stopped!");
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i(TAG, "Started to fetch threads");
        refreshThreadData();
        scheduleFetcherJobService(getApplicationContext(), false); // reschedule the job
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.i(TAG, "Stopped fetching threads");
        return true;
    }

    // ThreadRetrieverListener

    @Override
    public void threadsRetrieved(final ArrayList<ThreadModel> threads) {
        if (threads.size() == 0) {
            sendRefreshFinishedBroadcast(this);
            return;
        }

        final SharedPreferences appSettings =
                PreferenceManager.getDefaultSharedPreferences(this);

        final boolean shouldNotifyAboutLastPage =
                appSettings.getBoolean("pref_notify_on_last_page", true);

        Log.d(TAG, "Got thread data");
        ThreadDataManager.updateThreadList(this, threads);
        HashMap<String, Integer> updatedThreads = ThreadDataManager.getUpdatedThreads(this);

        boolean threadWasUpdated = false;
        for (final ThreadModel thread : threads) {
            if ((thread.newReplyCount > 0 && !thread.disabled && thread.isAvailable()) ||
                    (shouldNotifyAboutLastPage && thread.isNowOnLastPage)) {
                threadWasUpdated = true;
                break;
            }
        }

        if (!threadWasUpdated || !PreferencesDataManager.notificationsEnabled(this)) {
            sendRefreshFinishedBroadcast(this);
            return;
        }

        // Update running totals for the notification
        StringBuilder updatedThreadsText = new StringBuilder();
        int newThreadCount = 0;
        for (final ThreadModel thread : threads) {
            // Skip disabled threads
            if (thread.disabled) {
                continue;
            }

            if (thread.newReplyCount > 0 && thread.replyCountDelta > 0) {
                // Bypass notification if user only cares about (You)s, but
                // only if there are (You)s/tracked replies. Otherwise, no notifications will come
                // through since the user isn't tracking any replies.
                if (!thread.replyIds.isEmpty() &&
                    (thread.notifyOnlyIfRepliesToYou && !thread.newRepliesToYou))
                {
                    continue;
                }

                updatedThreadsText.append(thread.getTruncatedTitle());
                updatedThreadsText.append(" (+");

                Integer runningTotal = updatedThreads.get(thread.board + thread.id);
                if (runningTotal != null) {
                    final Integer newTotal = runningTotal + thread.newReplyCount;
                    updatedThreads.put(thread.board + thread.id, newTotal);
                    updatedThreadsText.append(newTotal);
                } else {
                    updatedThreads.put(thread.board + thread.id, thread.newReplyCount);
                    updatedThreadsText.append(thread.newReplyCount);
                }

                if (thread.newRepliesToYou) {
                    updatedThreadsText.append(") (You) ");
                } else {
                    updatedThreadsText.append(") ");
                }

                // This will cover autosaging where a thread will no longer bump
                if (thread.currentPage >= Common.LAST_PAGE) {
                    updatedThreadsText.append(getString(R.string.last_page_warning_indicator));
                }

                updatedThreadsText.append("\n");
                newThreadCount++;
            } else if (shouldNotifyAboutLastPage && thread.isNowOnLastPage) {
                updatedThreadsText.append(thread.getTruncatedTitle());
                updatedThreadsText.append(" ");
                updatedThreadsText.append(getString(R.string.last_page_warning_indicator));
                updatedThreadsText.append("\n");
            }
        }

        // Even if a thread does not have new replies, notify the user that a thread
        // hit the last page

        // Save for persistence
        ThreadDataManager.setUpdatedThreads(this, updatedThreads);

        // Only notify if the notification was populated
        if (updatedThreadsText.length() == 0) {
            sendRefreshFinishedBroadcast(this);
            return;
        }

        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

        final boolean vibrateNotify = appSettings.getBoolean("pref_notify_vibrate", true);
        final Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        boolean canVibrate = vibrator.hasVibrator();

        final int defaults = (vibrateNotify && canVibrate) ? NotificationCompat.DEFAULT_ALL :
                NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_SOUND;

        String title = "";
        String titleText = "";

        // Update titles based on what actually updated
        if (newThreadCount == 0 && updatedThreadsText.length() > 0) {
            // Only last page warning(s)
            title = getString(R.string.notification_title_last_page);
            titleText = getString(R.string.notification_title_last_page_content);
        } else {
            // Could be a mix of thread replies and last page warnings
            title = getString(R.string.notification_title);
            titleText = getString(R.string.notification_content, newThreadCount);
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, Common.CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(updatedThreadsText.toString()))
                        .setContentTitle(title)
                        .setContentText(titleText)
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
        sendRefreshFinishedBroadcast(this);
    }
    @Override
    public void threadRetrievalFailed(final ArrayList<ThreadModel> threads) {
        threadsRetrieved(threads);
    }

    // Private methods

    private void refreshThreadData() {
        final ArrayList<ThreadModel> threadList = ThreadDataManager.getThreadList(this);
        if (threadList == null || threadList.isEmpty()) {
            return;
        }

        ThreadsRetriever threadsRetriever = new ThreadsRetriever();
        threadsRetriever.addListener(this);
        threadsRetriever.retrieveThreadData(this, threadList);
    }

    private static void sendRefreshFinishedBroadcast(final Context context) {
        Intent fetchFinishedIntent = new Intent(Common.FETCH_JOB_BROADCAST_KEY);
        fetchFinishedIntent.putExtra(Common.FETCH_JOB_SUCCEEDED_KEY, true);
        LocalBroadcastManager.getInstance(context).sendBroadcast(fetchFinishedIntent);
    }
}
