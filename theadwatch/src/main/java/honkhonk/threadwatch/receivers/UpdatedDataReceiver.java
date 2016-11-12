package honkhonk.threadwatch.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

import honkhonk.threadwatch.helpers.Common;
import honkhonk.threadwatch.models.ThreadModel;

/**
 * Created by Gunbard on 11/9/2016.
 */

public class UpdatedDataReceiver extends BroadcastReceiver {
    private UpdatedDataReceiverListener listener;

    public interface UpdatedDataReceiverListener {
        void onNewData(final ArrayList<ThreadModel> threads);
    }

    public UpdatedDataReceiver(final UpdatedDataReceiverListener listener) {
        super();
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String listDataAsJson = intent.getStringExtra(Common.SAVED_THREAD_DATA);
        final ArrayList<ThreadModel> threads = (new Gson()).fromJson(listDataAsJson,
                new TypeToken<ArrayList<ThreadModel>>() {}.getType());
        listener.onNewData(threads);
    }
}
