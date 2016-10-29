package honkhonk.threadwatch.adapters;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import honkhonk.threadwatch.R;
import honkhonk.threadwatch.models.ThreadModel;

/**
 * Handles display of threads in the list
 * Created by Gunbard on 10/15/2016.
 */

public class ThreadListAdapter extends ArrayAdapter<ThreadModel> {
    /**
     * Tag for logging
     */
    final private static String TAG = ThreadListAdapter.class.getSimpleName();

    /**
     * List of threads
     */
    private ArrayList<ThreadModel> threads;

    /**
     * Context for getting resources
     */
    private Context context;

    public ThreadListAdapter(Context context, @LayoutRes int resource, @IdRes int textViewResourceId,
                        @NonNull ArrayList<ThreadModel> threads) {
        super(context, resource, textViewResourceId, threads);
        this.threads = threads;
        this.context = context;
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final View view = super.getView(position, convertView, parent);

        if (position >= threads.size()) {
            Log.e(TAG, "Tried to show a thread outside of bounds of data source");
            return view;
        }

        final ThreadModel thread = threads.get(position);

        final TextView boardName = (TextView) view.findViewById(R.id.boardTitle);
        boardName.setText("/" + thread.board + "/");

        final TextView title = (TextView) view.findViewById(R.id.threadTitle);
        final String titleText = thread.getTitle();

        if (titleText != null && !titleText.equals("") && !thread.firstRefresh) {
            title.setText(thread.getTitle());
        } else {
            title.setText(context.getResources().getString(R.string.no_thread_data));
        }

        final TextView newPosts = (TextView) view.findViewById(R.id.newPosts);
        if (thread.replyCountDelta != 0) {
            newPosts.setText("(" + thread.replyCountDelta + ")") ;
        } else {
            newPosts.setText("");
        }

        if (thread.archived) {
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.colorArchivedThread));
        } else if (thread.closed) {
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.colorClosedThread));
        } else if (thread.notFound) {
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.colorNotFoundThread));
        } else {
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.colorThreadBackground));
        }

        // Set up preview event
        boardName.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getContext(), boardName.getText(), Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        return view;
    }
}
