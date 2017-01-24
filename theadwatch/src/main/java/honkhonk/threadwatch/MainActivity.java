package honkhonk.threadwatch;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import honkhonk.threadwatch.adapters.ThreadListAdapter;
import honkhonk.threadwatch.helpers.Common;
import honkhonk.threadwatch.helpers.ThreadSorter;
import honkhonk.threadwatch.models.ThreadModel;
import honkhonk.threadwatch.receivers.UpdatedDataReceiver;
import honkhonk.threadwatch.retrievers.ThreadsRetriever;

public class MainActivity extends AppCompatActivity
        implements ThreadsRetriever.ThreadRetrieverListener,
        ThreadListAdapter.ThreadListAdapterListener,
        UpdatedDataReceiver.UpdatedDataReceiverListener {
    final public static String TAG = MainActivity.class.getSimpleName();

    private UpdatedDataReceiver updatedDataReceiver;
    private SwipeRefreshLayout swipeContainer;
    private ArrayList<ThreadModel> listDataSource = new ArrayList<>();
    private ArrayAdapter<ThreadModel> listAdapter;
    private PendingIntent notificationIntent;
    private HashMap<ThreadModel, Integer> updatedThreads = new HashMap<>();
    private Menu mainMenu;

    private ListView listView;
    private WebView previewWebView;
    private ImageView fadeView;
    private TextView noThreadsText;

    private int fadeDuration;
    private int sortMode = 0;
    private boolean sortAscending = false;
    private boolean notificationsEnabled = true;
    private boolean canVibrate = true;
    private boolean vibrateNotify = true;
    private int refreshRate = 5;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updatedDataReceiver = new UpdatedDataReceiver(this);
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(updatedDataReceiver, new IntentFilter("wtfisthis"));

        fadeDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        listView = (ListView) findViewById(R.id.mainList);
        fadeView = (ImageView) findViewById(R.id.fadeView);
        previewWebView = (WebView) findViewById(R.id.previewWebView);
        noThreadsText = (TextView) findViewById(R.id.noThreadsText);

        final Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        canVibrate = vibrator.hasVibrator();

        previewWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                //view.scrollTo(0, view.getContentHeight());
                //view.pageDown(true);
                ProgressBar spinner = (ProgressBar) view.findViewById(R.id.previewSpinner);
                spinner.setVisibility(ProgressBar.GONE);
            }
        });

        if (!restoreData()) {
            Log.i(TAG, "No previously saved threads found");
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
                final String url = thread.getUrl() + "#p" + Long.toString(thread.lastPostId);

                final Intent browserIntent =
                        new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                thread.replyCountDelta = 0;
                listAdapter.notifyDataSetChanged();
                saveData();
                scheduleAlarm();
                startActivity(browserIntent);
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

        updateNoThreadsText();
        refresh();
        scheduleAlarm();
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        final Intent intent = getIntent();
        handleIntent(intent);
        intent.setData(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Common.SETTINGS_CLOSED_ID) {
            // Update refresh rate
            final SharedPreferences appSettings =
                    PreferenceManager.getDefaultSharedPreferences(this);

            final String refreshValue = appSettings.getString("pref_refresh_rate", "5");
            if (refreshValue.length() > 0) {
                refreshRate = Integer.parseInt(refreshValue);
            }

            vibrateNotify = appSettings.getBoolean("pref_notify_vibrate", true);
            saveData();
            scheduleAlarm();
        }
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        handleIntent(intent);
        intent.setData(null);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        mainMenu = menu;
        setNotificationEnabledState(notificationsEnabled);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_sort:
                showSortMenu();
                return true;
            /*case R.id.menu_refresh:
                refresh();
                return true;*/
            case R.id.menu_notify:
                setNotificationEnabledState(!notificationsEnabled);
                saveData();
                return true;
            case R.id.menu_add:
                showAddThreadDialog();
                return true;
            case R.id.menu_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                settingsIntent.putExtra(Common.PREFS_CAN_VIBRATE, canVibrate);
                startActivityForResult(settingsIntent, Common.SETTINGS_CLOSED_ID);
                return true;
//            case R.id.menu_help:
//                return true;
//            case R.id.menu_about:
//                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    public void scheduleAlarm() {
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (notificationIntent != null) {
            alarm.cancel(notificationIntent);
        }

        Intent intent = new Intent(getApplicationContext(), FetcherService.class);

        final String listDataAsJson = (new Gson()).toJson(listDataSource);
        intent.putExtra(Common.SAVED_THREAD_DATA, listDataAsJson);

        // Create a PendingIntent to be triggered when the alarm goes off
        notificationIntent =
                PendingIntent.getService(this, Common.ALARM_ID,
                        intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                Common.ONE_MINUTE_IN_MILLIS * refreshRate,
                Common.ONE_MINUTE_IN_MILLIS * refreshRate,
                notificationIntent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        // Cancel any in-progress swipe-to-refresh gesture to stop refresh while the menu is up
        final long eventTime = SystemClock.uptimeMillis();
        MotionEvent touchUpEvent =
                MotionEvent.obtain(eventTime, eventTime, MotionEvent.ACTION_UP, 0.0f, 0.0f, 0);

        swipeContainer.dispatchTouchEvent(touchUpEvent);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.thread_action_menu, menu);

        // Set notify toggle state text
        final AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) menuInfo;
        final ThreadModel thread = listDataSource.get(info.position);
        MenuItem toggleItem = menu.findItem(R.id.thread_menu_notify_toggle);
        final String toggleText = thread.disabled ?
                getResources().getString(R.string.thread_menu_disabled) :
                getResources().getString(R.string.thread_menu_enabled);
        toggleItem.setTitle(toggleText);

        // Set delete button color
        MenuItem deleteButton = menu.findItem(R.id.thread_menu_delete);
        SpannableString s = new SpannableString(deleteButton.getTitle());
        s.setSpan(new ForegroundColorSpan(Color.RED), 0, s.length(), 0);
        deleteButton.setTitle(s);
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final ThreadModel thread = listDataSource.get(info.position);

        switch (item.getItemId()) {
            case R.id.thread_menu_info:
                showThreadInfo(info.position);
                return true;
            case R.id.thread_menu_notify_toggle:
                thread.disabled = !thread.disabled;
                listAdapter.notifyDataSetChanged();
                scheduleAlarm();
                return true;
            case R.id.thread_menu_mark_read:
                thread.replyCountDelta = 0;
                listAdapter.notifyDataSetChanged();
                saveData();
                scheduleAlarm();
                return true;
            case R.id.thread_menu_delete:
                deleteThread(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * ThreadRetrieverListener
     */
    public void threadsRetrieved(final ArrayList<ThreadModel> threads) {
        updateList(threads);
        swipeContainer.setRefreshing(false);
        listView.animate().alpha(1.0f).setDuration(fadeDuration);
        listView.setEnabled(true);
    }

    public void threadRetrievalFailed(final ArrayList<ThreadModel> threads) {
        updateList(threads);
        swipeContainer.setRefreshing(false);
        listView.animate().alpha(1.0f).setDuration(fadeDuration);
        listView.setEnabled(true);
    }

    /**
     * ThreadListAdapterListener
     */
    public void onListItemLongPress(final int position) {
        fadeView.setAlpha(0.0f);
        fadeView.animate().alpha(0.5f).setDuration(fadeDuration);

        previewWebView.setVisibility(WebView.VISIBLE);
        fadeView.setVisibility(View.VISIBLE);

        Animation anim = AnimationUtils.loadAnimation(MainActivity.this,
                android.R.anim.slide_in_left);
        anim.setDuration(fadeDuration);
        previewWebView.startAnimation(anim);

        final ThreadModel thread = listDataSource.get(position);
        final long lastPostId = thread.lastPostId;
        previewWebView.loadUrl(thread.getUrl() + "#p" + Long.toString(lastPostId));

        ProgressBar spinner = (ProgressBar) previewWebView.findViewById(R.id.previewSpinner);
        spinner.setVisibility(ProgressBar.VISIBLE);
    }

    /**
     * UpdatedDataReceiverListener
     */
    public void onNewData(final ArrayList<ThreadModel> threads) {
        updateList(threads);
        scheduleAlarm();

        if (!notificationsEnabled) {
            return;
        }

        String updatedThreadsText = "";

        for (final ThreadModel thread : threads) {
            if (thread.newReplyCount > 0 && !thread.disabled) {
                Integer runningTotal = updatedThreads.get(thread);
                if (runningTotal != null) {
                    final Integer newTotal = runningTotal + thread.newReplyCount;
                    updatedThreads.put(thread, newTotal);
                } else {
                    updatedThreads.put(thread, thread.newReplyCount);
                }
            }
        }

        for (Map.Entry<ThreadModel, Integer> entry : updatedThreads.entrySet()) {
            updatedThreadsText += entry.getKey().getTruncatedTitle() +
                    " (+" + entry.getValue() + ")" + "\n";
        }

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtra("cheese", "yes");

        PendingIntent resultPendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            );

        final int defaults = (vibrateNotify && canVibrate) ? NotificationCompat.DEFAULT_ALL :
                NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_SOUND;

        NotificationCompat.Builder builder =
            (android.support.v7.app.NotificationCompat.Builder)
                new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(updatedThreadsText))
                    .setContentTitle("New replies!")
                    .setContentText("New posts in " +
                            Integer.toString(updatedThreads.size()) + " thread(s).")
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
    }

    public void fadeViewClicked(final View view) {
        view.animate().alpha(0.0f).setDuration(fadeDuration);

        Animation anim = AnimationUtils.loadAnimation(MainActivity.this,
                android.R.anim.slide_out_right);
        anim.setDuration(fadeDuration);
        previewWebView.startAnimation(anim);

        new Handler().postDelayed(new Runnable() {
            public void run() {
                previewWebView.loadUrl("about:blank");
                previewWebView.setVisibility(WebView.GONE);
                view.setVisibility(View.GONE);
            }
        }, anim.getDuration());
    }

    /**
     * Private methods
     */
    private void updateList(final ArrayList<ThreadModel> threads) {
        listDataSource.clear();
        listDataSource.addAll(threads);

        // Sort threads
        ThreadSorter.sort(listDataSource, Common.sortOptionsValues[sortMode], sortAscending);

        listAdapter.notifyDataSetChanged();
        scheduleAlarm();
    }

    private void setNotificationEnabledState(final boolean enabled) {
        notificationsEnabled = enabled;

        MenuItem notificationItem = mainMenu.findItem(R.id.menu_notify);
        if (notificationsEnabled) {
            notificationItem.setIcon(R.drawable.ic_notifications_active_white_24dp);
        } else {
            notificationItem.setIcon(R.drawable.ic_notifications_off_white_24dp);
        }
    }

    private void refresh() {
        ConnectivityManager cm =
                (ConnectivityManager) MainActivity.this
                        .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if (!isConnected) {
            Toast.makeText(MainActivity.this, getResources().getString(R.string.refresh_error),
                    Toast.LENGTH_LONG).show();
            swipeContainer.setRefreshing(false);
            return;
        }

        listView.setEnabled(false);
        swipeContainer.setRefreshing(true);

        listView.animate().alpha(0.5f).setDuration(fadeDuration);

        ThreadsRetriever threadsRetriever = new ThreadsRetriever();
        threadsRetriever.addListener(this);
        threadsRetriever.retrieveThreadData(this, listDataSource);
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
                addThread(input.getText().toString());
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
        createDate.setTimeInMillis(thread.time * 1000);

        Calendar latestDate = Calendar.getInstance();
        latestDate.setTimeInMillis(thread.latestTime * 1000);

        final String threadData =
            "Added on: " + thread.dateAdded.getTime() + "\n" +
            "Posted on: " + createDate.getTime() + "\n" +
            "Latest on: " + latestDate.getTime() + "\n" +
            "Replies: " + thread.replyCount +
            " | Images: " + thread.imageCount + "\n\n" +
            thread.getSanitizedComment() + "\n\n";

        builder.setMessage(threadData);

        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteThread(final int position) {
        Animation anim = AnimationUtils.loadAnimation(MainActivity.this,
                android.R.anim.slide_out_right);
        anim.setDuration(fadeDuration);
        listView.getChildAt(position).startAnimation(anim);

        new Handler().postDelayed(new Runnable() {
            public void run() {
                final ThreadModel removedThread = listDataSource.remove(position);
                listAdapter.notifyDataSetChanged();
                updateNoThreadsText();
                scheduleAlarm();

                Snackbar.make(findViewById(android.R.id.content),
                        getResources().getString(R.string.thread_menu_deleted) +  " " +
                                removedThread.getTitle(),
                        Snackbar.LENGTH_LONG)
                        .setAction(R.string.thread_menu_undo, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                listAdapter.insert(removedThread, position);
                                listAdapter.notifyDataSetChanged();
                                updateNoThreadsText();
                                scheduleAlarm();
                            }
                        })
                        .show();
            }

        }, anim.getDuration());

        saveData();
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
        builder.setNeutralButton(R.string.sort_menu_ascend, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final int selectedPosition =
                        ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                ThreadSorter.sort(listDataSource, Common.sortOptionsValues[selectedPosition], true);
                listAdapter.notifyDataSetChanged();
                sortMode = selectedPosition;
                sortAscending = true;
            }
        });

        builder.setPositiveButton(R.string.sort_menu_descend, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final int selectedPosition =
                        ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                ThreadSorter.sort(listDataSource, Common.sortOptionsValues[selectedPosition], false);
                listAdapter.notifyDataSetChanged();
                sortMode = selectedPosition;
                sortAscending = false;
            }
        });

        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                final Button descendingButton =
                        ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                final Button ascendingButton =
                        ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_NEUTRAL);

                if (sortAscending) {
                    ascendingButton.setPaintFlags(ascendingButton.getPaintFlags() |
                            Paint.UNDERLINE_TEXT_FLAG);
                } else {
                    descendingButton.setPaintFlags(descendingButton.getPaintFlags() |
                            Paint.UNDERLINE_TEXT_FLAG);
                }
            }
        });

        dialog.show();
    }

    private void updateNoThreadsText() {
        if (listDataSource.size() == 0) {
            noThreadsText.setVisibility(View.VISIBLE);
        } else {
            noThreadsText.setVisibility(View.GONE);
        }
    }

    private int getThreadIndex(final String board, final String id) {
        for (int i = 0; i < listDataSource.size(); i++) {
            final ThreadModel thread = listDataSource.get(i);
            if (thread.board.equals(board) && thread.id.equals(id)) {
                return i;
            }
        }

        return -1;
    }

    private void saveData() {
        final String listDataAsJson = (new Gson()).toJson(listDataSource);
        SharedPreferences settings = getSharedPreferences(Common.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(Common.SAVED_THREAD_DATA, listDataAsJson);
        editor.putInt(Common.SAVED_SORT_MODE, sortMode);
        editor.putBoolean(Common.SAVED_SORT_ASCENDING, sortAscending);
        editor.putBoolean(Common.SAVED_NOTIFY_ENABLED, notificationsEnabled);
        editor.apply();
    }

    private boolean restoreData() {
        final SharedPreferences savedPrefs = getSharedPreferences(Common.PREFS_NAME, 0);
        final SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(this);
        final String listDataAsJson = savedPrefs.getString(Common.SAVED_THREAD_DATA, null);
        sortMode = savedPrefs.getInt(Common.SAVED_SORT_MODE, 0);
        refreshRate = Integer.parseInt(appSettings.getString("pref_refresh_rate", "5"));
        vibrateNotify = appSettings.getBoolean("pref_notify_vibrate", true);
        sortAscending = savedPrefs.getBoolean(Common.SAVED_SORT_ASCENDING, false);
        notificationsEnabled = savedPrefs.getBoolean(Common.SAVED_NOTIFY_ENABLED, true);

        if (listDataAsJson == null) {
            return false;
        }

        listDataSource = (new Gson()).fromJson(listDataAsJson,
                new TypeToken<ArrayList<ThreadModel>>() {}.getType());
        return true;
    }

    private void addThread(final String threadUrl) {
        if (threadUrl == null || threadUrl.length() == 0) {
            return;
        }

        final Resources resources = MainActivity.this.getResources();
        final Uri url = Uri.parse(threadUrl);
        if (url.getAuthority() == null || !url.getAuthority().equals("boards.4chan.org")) {
            Toast.makeText(MainActivity.this,
                    resources.getString(R.string.invalid_thread_url),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        final String[] pathParts = url.getPath().split("/");

        if (pathParts.length < 4) {
            Toast.makeText(MainActivity.this,
                    resources.getString(R.string.invalid_thread_url),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        final String board = pathParts[1];
        final String id = pathParts[3];

        if (board == null || id == null || board.equals("") || id.equals("")) {
            Toast.makeText(MainActivity.this,
                    resources.getString(R.string.invalid_thread_url),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        final int dupeThreadIndex = getThreadIndex(board, id);
        if (dupeThreadIndex >= 0) {
            Toast.makeText(MainActivity.this,
                    resources.getString(R.string.duplicate_thread),
                    Toast.LENGTH_SHORT).show();
            highlightListItem(dupeThreadIndex, fadeDuration * 4);
            return;
        }

        ThreadModel newThread = new ThreadModel();
        newThread.board = board;
        newThread.id = id;
        newThread.dateAdded = Calendar.getInstance();
        newThread.firstRefresh = true;

        listDataSource.add(newThread);
        listAdapter.notifyDataSetChanged();
        updateNoThreadsText();

        Toast.makeText(MainActivity.this, "Added " + url,
                Toast.LENGTH_SHORT).show();

        saveData();

        refresh();
        scheduleAlarm();
    }

     private void handleIntent(final Intent intent) {
         // Activity may have been started externally e.g. user
         // sent a thread url from the browser
         final String type = intent.getType();
         if (type != null && type.equals("text/plain")) {
             // Mobile Firefox
             String threadUrl = (String)intent.getClipData().getItemAt(0).getText();
             if (threadUrl == null) {
                 // Mobile Chrome
                 threadUrl = intent.getExtras().getString(Common.SHARE_TEXT_KEY, "");
             }

             addThread(threadUrl);
         }

         // Reset count
         updatedThreads.clear();
     }

    private View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

    private void highlightListItem(final int index, final int duration) {
        listView.smoothScrollToPosition(index);
        listView.setSelection(index);
        View itemView = getViewByPosition(index, listView);
        View background = itemView.findViewById(R.id.itemBackground);
        background.setAlpha(1.0f);
        background.animate().alpha(0.0f).setDuration(duration);
    }
}
