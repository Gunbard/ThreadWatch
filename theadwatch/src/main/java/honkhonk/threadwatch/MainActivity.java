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

import honkhonk.threadwatch.models.PostModel;
import honkhonk.threadwatch.retrievers.PostsRetriever;

public class MainActivity extends AppCompatActivity implements PostsRetriever.PostsRetrieverProtocol {
    final public static String TAG = MainActivity.class.getSimpleName();

    private int fadeDuration;

    private SwipeRefreshLayout swipeContainer;

    private ArrayList<PostModel> listDataSource = new ArrayList<>();
    private ArrayAdapter<PostModel> listAdapter;

    private ListView listView;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //scheduleAlarm();

        fadeDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        listView = (ListView) findViewById(R.id.mainList);

        PostModel post1 = new PostModel();
        post1.comment = "what";

        PostModel post2 = new PostModel();
        post2.comment = "sure";

        listDataSource.add(post1);
        listDataSource.add(post2);

        listAdapter = new ArrayAdapter<PostModel>(this,
                R.layout.thread_item, R.id.threadTitle, listDataSource) {
            @Override
            @NonNull
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                final View view = super.getView(position, convertView, parent);
                if (position < listDataSource.size()) {
                    TextView title = (TextView) view.findViewById(R.id.threadTitle);


                    final String comment = listDataSource.get(position).comment;

                    if (comment != null && !comment.equals("")) {
                        final String sanitizedComment =
                                android.text.Html.fromHtml(comment)
                                        .toString();

                        title.setText(sanitizedComment);
                    }
                    //TextView subtitle = (TextView) view.findViewById(android.R.id.text2);
                    //subtitle.setText("what");
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
     * PostsRetrieverProtocol
     */
    public void postsRetrieved(final String board,
                               final String threadId,
                               final ArrayList<PostModel> posts) {
        updateList(posts);
        swipeContainer.setRefreshing(false);
        listView.animate().alpha(1.0f).setDuration(fadeDuration);
    }

    public void retrievalFailed() {
        swipeContainer.setRefreshing(false);
        listView.animate().alpha(1.0f).setDuration(fadeDuration);
    }

    /**
     * Private methods
     */
    private void updateList(final ArrayList<PostModel> posts) {
        listDataSource.clear();
        listDataSource.addAll(posts);

        listAdapter.notifyDataSetChanged();
    }


    private void refresh() {
        swipeContainer.setRefreshing(true);
        listView.animate().alpha(0.5f).setDuration(fadeDuration);

        PostsRetriever postsRetriever = new PostsRetriever();
        postsRetriever.addListener(this);
        postsRetriever.retrievePosts(this, "jp", "15862023");
    }

}
