package honkhonk.threadwatch;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import honkhonk.threadwatch.adapters.ThreadListAdapter;
import honkhonk.threadwatch.helpers.Common;
import honkhonk.threadwatch.helpers.ThreadSorter;
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

    // TODO: Move to SharedPrefs
    private int sortMode = 0;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //scheduleAlarm();

        fadeDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        listView = (ListView) findViewById(R.id.mainList);

        final ArrayList<ThreadModel> threads = createTestThreads();
        for (final ThreadModel thread : threads) {
            listDataSource.add(thread);
        }

        listAdapter = new ThreadListAdapter(this,
                R.layout.thread_item, R.id.threadTitle, listDataSource);
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

        // Show the thread action menu on long click
        registerForContextMenu(listView);

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
            case R.id.menu_sort:
                showSortMenu();
                return true;
            case R.id.menu_refresh:
                refresh();
                return true;
            case R.id.menu_add:
                showAddThreadDialog();
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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.thread_action_menu, menu);

        // Set delete button color
        MenuItem deleteButton = menu.findItem(R.id.thread_menu_delete);
        SpannableString s = new SpannableString(deleteButton.getTitle());
        s.setSpan(new ForegroundColorSpan(Color.RED), 0, s.length(), 0);
        deleteButton.setTitle(s);
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

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.thread_menu_info:
                showThreadInfo(info.position);
                return true;
            case R.id.thread_menu_delete:
                deleteThread(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
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
        Calendar date = Calendar.getInstance();

        ThreadModel newThread1 = new ThreadModel();
        newThread1.board = "jp";
        newThread1.id = "15862023";
        newThread1.dateAdded = date;

        Calendar date2 = Calendar.getInstance();
        date2.add(Calendar.DAY_OF_MONTH, -5);

        ThreadModel newThread2 = new ThreadModel();
        newThread2.board = "cgl";
        newThread2.id = "9178233";
        newThread2.dateAdded = date2;

        Calendar date3 = Calendar.getInstance();
        date3.add(Calendar.DAY_OF_MONTH, -2);

        ThreadModel newThread3 = new ThreadModel();
        newThread3.board = "cgl";
        newThread3.id = "9201659";
        newThread3.dateAdded = date3;

        return new ArrayList<>(Arrays.asList(newThread1, newThread2, newThread3));
    }

    private void showAddThreadDialog() {
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
                newThread.dateAdded = Calendar.getInstance();

                listDataSource.add(newThread);

                Toast.makeText(MainActivity.this, "Added " + input.getText(),
                        Toast.LENGTH_SHORT).show();

                refresh();
            }
        });

        final AlertDialog dialog = builder.create();

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Stub
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                final Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setEnabled(!(s.toString().trim().length() == 0));
            }
        });

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    }

    private void showThreadInfo(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.thread_info_dialog_title);

        final ThreadModel thread = listDataSource.get(position);

        Calendar createDate = Calendar.getInstance();
        createDate.setTimeInMillis(thread.time);

        Calendar latestDate = Calendar.getInstance();
        latestDate.setTimeInMillis(thread.latestTime);

        final String threadData =
            thread.getUrl() + "\n\n" +
            "Added on: " + thread.dateAdded.getTime() + "\n\n" +
            thread.getSanitizedComment() + "\n\n" +
            "Posted on: " + createDate.getTime() + "\n\n" +
            "Latest on: " + latestDate.getTime() + "\n\n" +
            "Replies: " + thread.replyCount + "\n" +
            "Images: " + thread.imageCount;
        builder.setMessage(threadData);

        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteThread(final int position) {
        final ThreadModel removedThread = listDataSource.remove(position);
        listAdapter.notifyDataSetChanged();

        Snackbar.make(findViewById(android.R.id.content),
            getResources().getString(R.string.thread_menu_deleted) +  " " + removedThread.comment,
                Snackbar.LENGTH_LONG)
            .setAction(R.string.thread_menu_undo, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listAdapter.insert(removedThread, position);
                    listAdapter.notifyDataSetChanged();
                }
            })
            .show();
    }

    private void showSortMenu() {
        final String[] options = {
            getResources().getString(R.string.sort_menu_add_date),
            getResources().getString(R.string.sort_menu_board),
            getResources().getString(R.string.sort_menu_title),
            getResources().getString(R.string.sort_menu_date),
            getResources().getString(R.string.sort_menu_bump_date)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.menu_sort);
        builder.setSingleChoiceItems(options, sortMode, null);
        builder.setPositiveButton(R.string.sort_menu_ascend, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final int selectedPosition =
                        ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                ThreadSorter.sort(listDataSource, Common.sortOptionsValues[selectedPosition], true);
                listAdapter.notifyDataSetChanged();
                sortMode = selectedPosition;
            }
        });

        builder.setNeutralButton(R.string.sort_menu_descend, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final int selectedPosition =
                        ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                ThreadSorter.sort(listDataSource, Common.sortOptionsValues[selectedPosition], false);
                listAdapter.notifyDataSetChanged();
                sortMode = selectedPosition;
            }
        });

        builder.show();
    }
}
