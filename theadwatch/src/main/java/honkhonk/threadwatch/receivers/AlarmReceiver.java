package honkhonk.threadwatch.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import honkhonk.threadwatch.FetcherService;
import honkhonk.threadwatch.helpers.Common;

/**
 * Created by Gunbard on 10/10/2016.
 */

public class AlarmReceiver extends BroadcastReceiver {
    // Triggered by the Alarm periodically (starts the service to run task)
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, FetcherService.class);
        i.putExtra(Common.SAVED_THREAD_DATA, intent.getStringExtra(Common.SAVED_THREAD_DATA));
        context.startService(i);
    }
}