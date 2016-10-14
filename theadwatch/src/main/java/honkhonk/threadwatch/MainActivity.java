package honkhonk.threadwatch;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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

        final AdapterView.OnItemClickListener mMessageClickedHandler =
                new AdapterView.OnItemClickListener() {
            public void onItemClick(final AdapterView parent,
                                    final View view,
                                    final int position,
                                    long id) {
                final ThreadModel thread = listDataSource.get(position);
                final String url = thread.getUrl();

                if (url != null) {
                    final Intent browserIntent =
                            new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                }
            }
        };

        listView.setOnItemClickListener(mMessageClickedHandler);

        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);

        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_green_dark);

        refresh();
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
            case R.id.menu_refresh:
                refresh();
                return true;
            case R.id.menu_add:
                addThread();
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

        ThreadsRetriever threadsRetriever = new ThreadsRetriever();
        threadsRetriever.addListener(this);
        threadsRetriever.retrieveThreadData(this, listDataSource);
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

    private void addThread() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_thread_dialog_title);

        final EditText input = new EditText(this);
        input.setHint(R.string.add_thread_dialog_input_hint);

        final int viewPadding =
                getResources().getDimensionPixelSize(R.dimen.add_thread_input_padding);

        builder.setView(input, viewPadding, 0, viewPadding, 0);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                final String errorMessage = "Not a valid thread url";
                final Uri url = Uri.parse(input.getText().toString());
                if (url.getAuthority() == null || !url.getAuthority().equals("boards.4chan.org")) {
                    Toast.makeText(MainActivity.this, errorMessage,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                final String[] pathParts = url.getPath().split("/");
                if (pathParts.length < 4 || pathParts[1] == null || pathParts[3] == null) {
                    Toast.makeText(MainActivity.this, errorMessage,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                ThreadModel newThread = new ThreadModel();
                newThread.board = pathParts[1];
                newThread.id = pathParts[3];

                listDataSource.add(newThread);

                Toast.makeText(MainActivity.this, "Added " + input.getText(),
                        Toast.LENGTH_SHORT).show();

                refresh();
            }
        });

        builder.create().show();
    }
}
