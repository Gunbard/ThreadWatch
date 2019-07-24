package honkhonk.threadwatch.jobs;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;

import java.util.ArrayList;

import honkhonk.threadwatch.ThreadDataManager;
import honkhonk.threadwatch.helpers.Common;
import honkhonk.threadwatch.models.ThreadModel;
import honkhonk.threadwatch.retrievers.ThreadsRetriever;

public class FetcherJobService extends JobService implements ThreadsRetriever.ThreadRetrieverListener {
    final public static String TAG = FetcherJobService.class.getSimpleName();
    final public static int FETCHER_JOB_ID = 0;

    public static void scheduleFetcherJobService(Context context, boolean noWait) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(FETCHER_JOB_ID);

        ComponentName serviceComponent = new ComponentName(context, FetcherJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(FETCHER_JOB_ID, serviceComponent);

        if (noWait) {
            builder.setMinimumLatency(1); // Don't wait
            builder.setOverrideDeadline(1); // Perform immediately
        } else {
            builder.setMinimumLatency(30 * 1000); // Wait at least 30s
            builder.setOverrideDeadline(5 * 60 * 1000); // maximum delay of 5 minutes
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
//        Intent service = new Intent(getApplicationContext(), FetcherService.class);
//        getApplicationContext().startService(service);
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

        Intent newDataIntent = new Intent(Common.FETCH_JOB_BROADCAST_KEY);
        newDataIntent.putExtra(Common.FETCH_JOB_SUCCEEDED_KEY, true);
        LocalBroadcastManager.getInstance(this).sendBroadcast(newDataIntent);

    }
    @Override
    public void threadRetrievalFailed(final ArrayList<ThreadModel> threads) {
        Log.d(TAG, "Couldn't get thread data");

        Intent newDataIntent = new Intent(Common.FETCH_JOB_BROADCAST_KEY);
        newDataIntent.putExtra(Common.FETCH_JOB_SUCCEEDED_KEY, false);
        LocalBroadcastManager.getInstance(this).sendBroadcast(newDataIntent);
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
