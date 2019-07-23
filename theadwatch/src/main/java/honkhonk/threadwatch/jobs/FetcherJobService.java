package honkhonk.threadwatch.jobs;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

public class FetcherJobService extends JobService {
    final public static int FETCHER_JOB_ID = 0;

    public static void scheduleFetcherJobService(Context context, Boolean noWait) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(FETCHER_JOB_ID)

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

    public static void scheduleFetcherJobServiceNow(Context context) {
        scheduleFetcherJobService(context, true);
    }

    public static JobInfo getJob() {
        return null;
    }

    @Override
    public boolean onStartJob(JobParameters params) {
//        Intent service = new Intent(getApplicationContext(), FetcherService.class);
//        getApplicationContext().startService(service);
        scheduleFetcherJobService(getApplicationContext(), false); // reschedule the job
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
