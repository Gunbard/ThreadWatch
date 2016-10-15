package honkhonk.threadwatch.adapters;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import honkhonk.threadwatch.R;
import honkhonk.threadwatch.models.ThreadModel;

/**
 * Created by Gunbard on 10/15/2016.
 */

public class ThreadListAdapter extends ArrayAdapter<ThreadModel> {
    private ArrayList<ThreadModel> threads;

    public ThreadListAdapter(Context context, @LayoutRes int resource, @IdRes int textViewResourceId,
                        @NonNull ArrayList<ThreadModel> threads) {
        super(context, resource, textViewResourceId, threads);
        this.threads = threads;
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final View view = super.getView(position, convertView, parent);
        if (position < threads.size()) {
            final ThreadModel thread = threads.get(position);

            final TextView boardName = (TextView) view.findViewById(R.id.boardTitle);
            boardName.setText("/" + thread.board + "/");

            final TextView title = (TextView) view.findViewById(R.id.threadTitle);
            title.setText(thread.getTitle());

            if (thread.archived) {
                view.setBackgroundColor(Color.LTGRAY);
            } else {
                view.setBackgroundColor(Color.WHITE);
            }
        }
        return view;
    }
}
