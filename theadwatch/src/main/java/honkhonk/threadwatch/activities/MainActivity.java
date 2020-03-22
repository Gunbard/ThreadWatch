package honkhonk.threadwatch.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;

import honkhonk.threadwatch.R;
import honkhonk.threadwatch.adapters.ThreadListAdapter;
import honkhonk.threadwatch.fragments.RepliesFragment;
import honkhonk.threadwatch.helpers.Common;
import honkhonk.threadwatch.helpers.ThreadSorter;
import honkhonk.threadwatch.jobs.FetcherJobService;
import honkhonk.threadwatch.managers.PreferencesDataManager;
import honkhonk.threadwatch.managers.ThreadDataManager;
import honkhonk.threadwatch.models.PostModel;
import honkhonk.threadwatch.models.ThreadModel;

public class MainActivity extends AppCompatActivity
        implements
        ThreadListAdapter.ThreadListAdapterListener {
    final public static String TAG = MainActivity.class.getSimpleName();

    final private BroadcastReceiver updatedThreadsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Threads were updated!");

            swipeContainer.setRefreshing(false);
            listView.animate().alpha(1.0f).setDuration(fadeDuration);
            listView.setEnabled(true);
            refreshList();
        }
    };

    private SwipeRefreshLayout swipeContainer;
    private ArrayList<ThreadModel> listDataSource = new ArrayList<>();
    private ArrayAdapter<ThreadModel> listAdapter;
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
    private int refreshRate = 5;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(updatedThreadsReceiver,
                        new IntentFilter(Common.FETCH_JOB_BROADCAST_KEY));

        fadeDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        listView = findViewById(R.id.mainList);
        fadeView = findViewById(R.id.fadeView);
        previewWebView = findViewById(R.id.previewWebView);
        noThreadsText = findViewById(R.id.noThreadsText);

        final Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        canVibrate = vibrator.hasVibrator();

        previewWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                ProgressBar spinner = view.findViewById(R.id.previewSpinner);
                spinner.setVisibility(ProgressBar.GONE);
            }
        });

        restoreData();

        listAdapter = new ThreadListAdapter(this,
                R.layout.thread_item, R.id.threadTitle, listDataSource);
        listView.setAdapter(listAdapter);

        final AdapterView.OnItemClickListener messageClickedHandler =
                new AdapterView.OnItemClickListener() {
            public void onItemClick(final AdapterView parent,
                                    final View view,
                                    final int position,
                                    long id) {
                final ThreadModel thread = listDataSource.get(position);
                final String url = thread.getUrl() + "#p" + thread.lastPostId;

                final Intent browserIntent =
                        new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                thread.replyCountDelta = 0;
                thread.newRepliesToYou = false;
                ThreadDataManager.updateThread(MainActivity.this, thread);
                listAdapter.notifyDataSetChanged();
                startActivity(browserIntent);
            }
        };

        listView.setOnItemClickListener(messageClickedHandler);

        // Show the thread action menu on long click
        registerForContextMenu(listView);

        swipeContainer = findViewById(R.id.swipeContainer);

        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_green_dark);

        refreshList();

        if (!FetcherJobService.fetcherIsScheduled(this)) {
            FetcherJobService.scheduleFetcherJobService(this, false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        final Intent intent = getIntent();
        handleIntent(intent);
        intent.setData(null);

        refreshList();
        ThreadDataManager.clearUpdatedThreads(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updatedThreadsReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != Common.SETTINGS_CLOSED_ID) {
            return;
        }

        final boolean shouldRefresh = (data != null) &&
                data.getBooleanExtra(Common.SETTINGS_CLOSED_SHOULD_REFRESH, false);
        final int previousRefreshRate = refreshRate;

        // Update refresh rate
        final SharedPreferences appSettings =
                PreferenceManager.getDefaultSharedPreferences(this);

        final String refreshValue = appSettings.getString("pref_refresh_rate", "5");
        if (refreshValue.length() > 0) {
            refreshRate = Integer.parseInt(refreshValue);
        }

        // Update job if refresh rate changed or an import occurred
        if ((previousRefreshRate != refreshRate && refreshRate > 0) || shouldRefresh) {
            FetcherJobService.scheduleFetcherJobService(this, false);
        } else if (refreshRate == 0){
            FetcherJobService.stopFetcher(this);
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
                return true;
            case R.id.menu_add:
                showAddThreadDialog();
                return true;
            case R.id.menu_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                settingsIntent.putExtra(Common.PREFS_CAN_VIBRATE, canVibrate);
                startActivityForResult(settingsIntent, Common.SETTINGS_CLOSED_ID);
                return true;
            case R.id.menu_help:
                showHelp();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
                ThreadDataManager.updateThread(MainActivity.this, thread);
                refreshList();
                return true;
            case R.id.thread_menu_replies:
                showThreadReplies(info.position);
                return true;
            case R.id.thread_menu_mark_read:
                thread.replyCountDelta = 0;
                thread.newRepliesToYou = false;
                ThreadDataManager.updateThread(MainActivity.this, thread);
                refreshList();
                return true;
            case R.id.thread_menu_delete:
                deleteThread(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
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
        previewWebView.loadUrl(thread.getUrl() + "#p" + lastPostId);

        ProgressBar spinner = previewWebView.findViewById(R.id.previewSpinner);
        spinner.setVisibility(ProgressBar.VISIBLE);
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

    private void setNotificationEnabledState(final boolean enabled) {
        notificationsEnabled = enabled;
        PreferencesDataManager.setNotificationsEnabled(this, enabled);

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

        if (ThreadDataManager.getThreadList(this).size() == 0) {
            swipeContainer.setRefreshing(false);
            return;
        }

        listView.setEnabled(false);
        swipeContainer.setRefreshing(true);
        listView.animate().alpha(0.5f).setDuration(fadeDuration);

        FetcherJobService.scheduleFetcherJobService(this, true);
    }

    private void showAddThreadDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_thread_dialog_title);

        final EditText input = new EditText(this);
        input.setHint(R.string.add_thread_dialog_input_hint);
        builder.setView(input);

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
            getString(R.string.info_added, getElapsedTime(thread.dateAdded),
                    thread.dateAdded.getTime()) + "\n\n" +
            getString(R.string.info_posted, getElapsedTime(createDate),
                    createDate.getTime()) + "\n\n" +
            getString(R.string.info_latest, getElapsedTime(latestDate),
                    latestDate.getTime()) + "\n\n" +
            getString(R.string.info_stats, thread.replyCount, thread.imageCount) + "\n\n" +
            thread.getSanitizedComment() + "\n\n";

        builder.setMessage(threadData);

        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showThreadReplies(final int position) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Fragment existingFragment = getSupportFragmentManager().findFragmentByTag("repliesDetails");
        if (existingFragment != null) {
            transaction.remove(existingFragment);
        }
        transaction.addToBackStack(null);

        RepliesFragment repliesFragment = new RepliesFragment(this, listDataSource.get(position));
        repliesFragment.show(transaction, "repliesDetails");
    }

    private String getElapsedTime(Calendar date) {
        final Calendar currentDate = Calendar.getInstance();

        final double minuteInMillis = Common.ONE_MINUTE_IN_MILLIS;
        final int diffInMinutes = (int)Math.floor((((currentDate.getTimeInMillis() / minuteInMillis)
                - (date.getTimeInMillis() / minuteInMillis))));
        if (diffInMinutes < 60) {
            return getString(R.string.minutes_label, diffInMinutes);
        }

        final double hourInMillis = Common.ONE_HOUR_IN_MILLIS;
        final int diffInHours = (int)Math.floor((((currentDate.getTimeInMillis() / hourInMillis)
                - (date.getTimeInMillis() / hourInMillis))));
        if (diffInHours < 24) {
            return getString(R.string.hours_label, diffInHours);
        }

        final double dayInMillis = Common.ONE_DAY_IN_MILLIS;
        final int diffInDays = (int)Math.floor((((currentDate.getTimeInMillis() / dayInMillis)
                - (date.getTimeInMillis() / dayInMillis))));
        return getString(R.string.days_label, diffInDays);
    }


    private void deleteThread(final int position) {
        Animation anim = AnimationUtils.loadAnimation(MainActivity.this,
                android.R.anim.slide_out_right);
        anim.setDuration(fadeDuration);
        listView.getChildAt(position).startAnimation(anim);

        new Handler().postDelayed(new Runnable() {
            public void run() {
                final ThreadModel removedThread = listDataSource.remove(position);
                ThreadDataManager.deleteThread(MainActivity.this, removedThread.board, removedThread.id);
                refreshList();

                Snackbar.make(findViewById(android.R.id.content),
                        getResources().getString(R.string.thread_menu_deleted) +  " \"" +
                                removedThread.getTitle() + "\"",
                        Snackbar.LENGTH_LONG)
                        .setAction(R.string.thread_menu_undo, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                listAdapter.insert(removedThread, position);
                                ThreadDataManager.addThread(MainActivity.this, removedThread);
                                refreshList();
                            }
                        })
                        .show();
            }

        }, anim.getDuration());

        refreshList();
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
                PreferencesDataManager.setSortMode(MainActivity.this, sortMode);
                PreferencesDataManager.setSortAscending(MainActivity.this, sortAscending);
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
                PreferencesDataManager.setSortMode(MainActivity.this, sortMode);
                PreferencesDataManager.setSortAscending(MainActivity.this, sortAscending);
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
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.notification_channel_name_updated_thread);
            String description = getString(R.string.notification_channel_desc_updated_thread);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(Common.CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
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

    private void restoreData() {
        final SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(this);
        refreshRate = Integer.parseInt(appSettings.getString("pref_refresh_rate", "5"));
        sortMode = PreferencesDataManager.getSortMode(this);
        sortAscending = PreferencesDataManager.sortAscending(this);
        notificationsEnabled = PreferencesDataManager.notificationsEnabled(this);
        listDataSource = ThreadDataManager.getThreadList(this);
    }

    private void refreshList() {
        listDataSource.clear();
        listDataSource.addAll(ThreadDataManager.getThreadList(this));

        // Sort threads
        ThreadSorter.sort(listDataSource, Common.sortOptionsValues[sortMode], sortAscending);

        listAdapter.notifyDataSetChanged();
        updateNoThreadsText();
    }

    private void addThread(final String threadUrl) {
        if (threadUrl == null || threadUrl.length() == 0) {
            return;
        }

        final Resources resources = MainActivity.this.getResources();
        final Uri url = Uri.parse(threadUrl);
        if (url.getAuthority() == null || (!url.getAuthority().equals("boards.4chan.org")
                && !url.getAuthority().equals("boards.4channel.org"))) {
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

        // Can be used for tracking (you)s
        String replyId = null;
        String urlFragment = url.getEncodedFragment();
        if (urlFragment != null) {
            // Discard first letter, which can be a 'p' or 'q'
            replyId = urlFragment.substring(1);
        }

        final int dupeThreadIndex = getThreadIndex(board, id);
        if (dupeThreadIndex > -1) {
            if (replyId == null) {
                Toast.makeText(MainActivity.this,
                        resources.getString(R.string.duplicate_thread),
                        Toast.LENGTH_LONG).show();
                highlightListItem(dupeThreadIndex, fadeDuration * 4);
            } else {
                ThreadModel thread = listDataSource.get(dupeThreadIndex);
                if (!thread.replyIds.containsKey(replyId)) {
                    thread.replyIds.put(replyId, new ArrayList<PostModel>());
                    ThreadDataManager.updateThread(this, thread);
                    Toast.makeText(MainActivity.this,
                            resources.getString(R.string.reply_tracked, replyId),
                            Toast.LENGTH_LONG).show();
                    refresh();
                } else {
                    Toast.makeText(MainActivity.this,
                            resources.getString(R.string.reply_already_tracked),
                            Toast.LENGTH_LONG).show();
                }
            }
            return;
        }

        ThreadModel newThread = new ThreadModel();
        newThread.board = board;
        newThread.id = id;
        newThread.dateAdded = Calendar.getInstance();
        newThread.firstRefresh = true;

        if (replyId != null) {
            newThread.replyIds.put(replyId, new ArrayList<PostModel>());
        }

        ThreadDataManager.addThread(this, newThread);

        String toastMessage = getString(R.string.add_thread_message, url);
        if (replyId != null) {
            toastMessage = getString(R.string.add_thread_track_reply,
                    threadUrl.replace('#' + urlFragment, ""), replyId);
        }

        Toast.makeText(MainActivity.this, toastMessage, Toast.LENGTH_LONG).show();

        refresh();
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

    private void showHelp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.menu_help);
        builder.setMessage(R.string.help_summary);
        builder.setNeutralButton(R.string.help_github, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final Intent browserIntent =
                        new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Gunbard/ThreadWatch"));
                startActivity(browserIntent);
            }
        });
        builder.setNegativeButton(R.string.action_close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();
    }
}
