package honkhonk.threadwatch.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import java.util.ArrayList;

import honkhonk.threadwatch.R;
import honkhonk.threadwatch.adapters.RepliesListAdapter;
import honkhonk.threadwatch.managers.ThreadDataManager;
import honkhonk.threadwatch.models.PostModel;
import honkhonk.threadwatch.models.ThreadModel;

public class RepliesFragment extends AppCompatDialogFragment
        implements RepliesListAdapter.RepliesListAdapterListener {
    private Context context;
    private ThreadModel currentThread;
    private ExpandableListView expandableListView;
    private RepliesListAdapter repliesListAdapter;
    private View view;

    public RepliesFragment(final Context context, final ThreadModel thread) {
        this.context = context;
        this.currentThread = thread;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setTitle(getString(R.string.thread_replies_dialog_title));
        view = inflater.inflate(R.layout.replies, container, false);
        expandableListView = view.findViewById(R.id.repliesList);

        repliesListAdapter = new RepliesListAdapter(context, currentThread);
        repliesListAdapter.addListener(this);
        expandableListView.setAdapter(repliesListAdapter);
        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                        int childPosition, long id) {
                Object[] keys = currentThread.replyIds.keySet().toArray();
                String currentKey = (String)keys[groupPosition];
                ArrayList<PostModel> replies = currentThread.replyIds.get(currentKey);

                PostModel reply = replies.get(childPosition);
                if (reply.failed) {
                    return false;
                }

                final String url = currentThread.getUrl() + "#p" + reply.number;

                final Intent browserIntent =
                        new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);

                return false;
            }
        });

        refreshList();

        final TextView addReplyField = view.findViewById(R.id.addReplyField);
        final ImageButton addReplyButton = view.findViewById(R.id.addReplyButton);
        addReplyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String reply = addReplyField.getText().toString();
                if (!reply.isEmpty()) {
                    // Pull just the digits
                    String sanitizedReplyNumber = reply.replaceAll("\\D+","");
                    trackReply(sanitizedReplyNumber);
                    addReplyField.setText("");

                    // Hide keyboard
                    InputMethodManager inputManager =
                            (InputMethodManager) getActivity()
                                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(v.getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        });

        return view;
    }

    /**
     * RepliesListAdapterListener
     */

    public void onGroupRemoveClick(final int groupPosition) {
        final Object[] keys = currentThread.replyIds.keySet().toArray();
        final String currentKey = (String)keys[groupPosition];

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.reply_remove_title);
        builder.setMessage(getString(R.string.reply_remove_confirm, currentKey));
        builder.setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                currentThread.replyIds.remove(currentKey);
                ThreadDataManager.updateThread(context, currentThread);
                refreshList();
            }
        });
        builder.setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void trackReply(final String reply) {
        currentThread.replyIds.put(reply, new ArrayList<PostModel>());
        ThreadDataManager.updateThread(context, currentThread);
        refreshList();
    }

    private void refreshList() {
        repliesListAdapter.notifyDataSetChanged();

        View noRepliesLabel = view.findViewById(R.id.noTrackedRepliesLabel);
        if (currentThread.replyIds.isEmpty()) {
            noRepliesLabel.setVisibility(View.VISIBLE);
        } else {
            noRepliesLabel.setVisibility(View.GONE);
        }
    }
}
