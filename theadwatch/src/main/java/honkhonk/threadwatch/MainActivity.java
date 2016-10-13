package honkhonk.threadwatch;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import honkhonk.threadwatch.models.ThreadModel;
import honkhonk.threadwatch.retrievers.ThreadsRetriever;

public class MainActivity extends AppCompatActivity
        implements ThreadsRetriever.ThreadRetrieverListener {
    final public static String TAG = MainActivity.class.getSimpleName();

    private int fadeDuration;

    private SwipeRefreshLayout swipeContainer;

    private ArrayList<ThreadModel> listDataSource = new ArrayList<>();
    private ArrayAdapter<ThreadModel> listAdapter;

    private ListView listView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //scheduleAlarm();

        fadeDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        listView = (ListView) findViewById(R.id.mainList);

        final ArrayList<ThreadModel> threads = createTestThreads();

        listDataSource.add(threads.get(0));
        listDataSource.add(threads.get(1));

        listAdapter = new ArrayAdapter<ThreadModel>(this,
                R.layout.thread_item, R.id.threadTitle, listDataSource) {
            @Override
            @NonNull
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                final View view = super.getView(position, convertView, parent);
                if (position < listDataSource.size()) {
                    final ThreadModel thread = listDataSource.get(position);

                    final TextView boardName = (TextView) view.findViewById(R.id.boardTitle);
                    boardName.setText("/" + thread.board + "/");

                    final TextView title = (TextView) view.findViewById(R.id.threadTitle);
                    final String comment = thread.comment;

                    if (comment != null && !comment.equals("")) {
                        final String sanitizedComment =
                                android.text.Html.fromHtml(comment)
                                        .toString();

                        title.setText(sanitizedComment);
                    }
                    else
                    {
                        title.setText("");
                    }
                }
                return view;
            }
        };

        listView.setAdapter(listAdapter);

        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);

        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_wat:
                refresh();
                return true;
            case R.id.menu_settings:
                final Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.menu_help:
                return true;
            case R.id.menu_about:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Setup a recurring alarm every half hour
    public void scheduleAlarm() {
        // Construct an intent that will execute the AlarmReceiver
        Intent intent = new Intent(getApplicationContext(), AlarmReceiver.class);

        // Create a PendingIntent to be triggered when the alarm goes off
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(this, AlarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        long firstMillis = System.currentTimeMillis(); // alarm is set right away
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

         alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstMillis,
                AlarmManager.INTERVAL_HALF_HOUR, pendingIntent);
    }

    public void bleh(final View v) {
        NotificationCompat.Builder builder =
            (android.support.v7.app.NotificationCompat.Builder)
                new NotificationCompat.Builder(this)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("New replies!")
                    .setContentText("Some thread")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_VIBRATE);

        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
             (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        mNotifyMgr.notify(345453, builder.build());

        refresh();
    }

    /**
     * ThreadRetrieverListener
     */
    public void threadsRetrieved(final ArrayList<ThreadModel> threads) {
        updateList(threads);
        swipeContainer.setRefreshing(false);
        listView.animate().alpha(1.0f).setDuration(fadeDuration);
    }

    public void threadRetrievalFailed() {
        swipeContainer.setRefreshing(false);
        listView.animate().alpha(1.0f).setDuration(fadeDuration);
    }

    /**
     * Private methods
     */
    private void updateList(final ArrayList<ThreadModel> threads) {
        listDataSource.clear();
        listDataSource.addAll(threads);

        listAdapter.notifyDataSetChanged();
    }


    private void refresh() {
        swipeContainer.setRefreshing(true);
        listView.animate().alpha(0.5f).setDuration(fadeDuration);

        /*PostsRetriever postsRetriever = new PostsRetriever();
        postsRetriever.addListener(this);
        postsRetriever.retrievePosts(this, createTestThreads());*/

        ThreadsRetriever threadsRetriever = new ThreadsRetriever();
        threadsRetriever.addListener(this);
        threadsRetriever.retrieveThreadData(this, createTestThreads());
    }


    private ArrayList<ThreadModel> createTestThreads() {
        ThreadModel newThread1 = new ThreadModel();
        newThread1.board = "jp";
        newThread1.id = "15862023";

        ThreadModel newThread2 = new ThreadModel();
        newThread2.board = "cgl";
        newThread2.id = "9178233";

        return new ArrayList<>(Arrays.asList(newThread1, newThread2));
    }
}
