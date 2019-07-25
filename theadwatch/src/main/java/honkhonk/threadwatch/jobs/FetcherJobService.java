package honkhonk.threadwatch.jobs;

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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import honkhonk.threadwatch.helpers.Common;
import honkhonk.threadwatch.managers.PreferencesDataManager;
import honkhonk.threadwatch.managers.ThreadDataManager;
import honkhonk.threadwatch.models.ThreadModel;
import honkhonk.threadwatch.retrievers.ThreadsRetriever;

public class FetcherJobService extends JobService implements ThreadsRetriever.ThreadRetrieverListener {
    final public static String TAG = FetcherJobService.class.getSimpleName();
    final public static int FETCHER_JOB_ID = 0;

    private HashMap<ThreadModel, Integer> updatedThreads = new HashMap<>();

    public static void scheduleFetcherJobService(Context context, boolean noWait) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(FETCHER_JOB_ID);

        ComponentName serviceComponent = new ComponentName(context, FetcherJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(FETCHER_JOB_ID, serviceComponent);

        if (noWait) {
            builder.setMinimumLatency(1); // Don't wait
            builder.setOverrideDeadline(1); // Perform immediately
        } else {
            final SharedPreferences appSettings =
                    PreferenceManager.getDefaultSharedPreferences(context);

            final String refreshValue = appSettings.getString("pref_refresh_rate", "5");
            int refreshRate = 5;
            if (refreshValue.length() > 0) {
                refreshRate = Integer.parseInt(refreshValue);
            }
            builder.setMinimumLatency(Common.ONE_MINUTE_IN_MILLIS * refreshRate); // Minimum wait delay
            builder.setOverrideDeadline((Common.ONE_MINUTE_IN_MILLIS * refreshRate) + 30000); // Maximum delay
        }

        builder.setRequiresCharging(false); // we don't care if the device is charging or not
        jobScheduler.schedule(builder.build());
    }

    public static Boolean fetcherIsRunning(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        for ( JobInfo jobInfo : jobScheduler.getAllPendingJobs() ) {
            if ( jobInfo.getId() == FETCHER_JOB_ID ) {
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
        //scheduleFetcherJobService(getApplicationContext(), false); // reschedule the job
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
        Log.d(TAG, "Got thread data");
        ThreadDataManager.updateThreadList(this, threads);

        boolean threadWasUpdated = false;
        for (final ThreadModel thread : threads) {
            if (thread.newReplyCount > 0 && !thread.disabled) {
                threadWasUpdated = true;
                break;
            }
        }

        Intent newDataIntent = new Intent(Common.FETCH_JOB_BROADCAST_KEY);
        newDataIntent.putExtra(Common.FETCH_JOB_SUCCEEDED_KEY, true);
        LocalBroadcastManager.getInstance(this).sendBroadcast(newDataIntent);

        if (!threadWasUpdated || !PreferencesDataManager.notificationsEnabled(this)) {
            return;
        }

        final Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        boolean canVibrate = vibrator.hasVibrator();


        //blah notification crap

    }
    @Override
    public void threadRetrievalFailed(final ArrayList<ThreadModel> threads) {
        threadsRetrieved(threads);
//        Intent newDataIntent = new Intent(Common.FETCH_JOB_BROADCAST_KEY);
//        newDataIntent.putExtra(Common.FETCH_JOB_SUCCEEDED_KEY, false);
//        LocalBroadcastManager.getInstance(this).sendBroadcast(newDataIntent);
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
}
