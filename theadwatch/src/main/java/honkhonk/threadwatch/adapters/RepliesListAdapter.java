package honkhonk.threadwatch.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;

import honkhonk.threadwatch.R;
import honkhonk.threadwatch.models.PostModel;
import honkhonk.threadwatch.models.ThreadModel;

public class RepliesListAdapter extends BaseExpandableListAdapter {
    private Context context;
    private HashMap<String, ArrayList<PostModel>> replyData;
    private ArrayList<RepliesListAdapterListener> listeners = new ArrayList<>();

    public interface RepliesListAdapterListener {
        void onGroupRemoveClick(final int groupPosition);
    }

    public RepliesListAdapter(Context context, ThreadModel thread) {
        this.context = context;
        this.replyData = thread.replyIds;
    }

    public void addListener(RepliesListAdapterListener listener) {
        listeners.add(listener);
    }

    @Override
    public int getGroupCount() {
        return replyData.keySet().size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        Object[] keys = replyData.keySet().toArray();
        String currentKey = (String)keys[groupPosition];

        ArrayList<PostModel> replies = replyData.get(currentKey);
        return replies.size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        Object[] keys = replyData.keySet().toArray();
        return keys[groupPosition];
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        Object[] keys = replyData.keySet().toArray();
        String currentKey = (String)keys[groupPosition];

        ArrayList<PostModel> replies = replyData.get(currentKey);
        return replies.get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        LayoutInflater layoutInflater =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = (convertView != null) ? convertView :
                layoutInflater.inflate(R.layout.reply_group_item, null);

        view.setBackgroundColor(ContextCompat.getColor(context, R.color.colorArchivedThread));

        Object[] keys = replyData.keySet().toArray();
        String currentKey = (String)keys[groupPosition];
        ArrayList<PostModel> replies = replyData.get(currentKey);

        String display = currentKey;
        String subtext = context.getString(R.string.reply_refresh_needed);
        if (replies != null && replies.size() > 0) {
            display = replies.get(0).number + " (" + (replies.size() - 1) + ")";
            subtext = replies.get(0).getSanitizedComment();

            if (replies.get(0).failed) {
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.colorNotFoundThread));
            }
        }

        TextView textViewLine1 = view.findViewById(android.R.id.text1);
        textViewLine1.setText(display);
        TextView textViewLine2 = view.findViewById(android.R.id.text2);
        textViewLine2.setText(subtext);

        ImageView removeReplyButton = view.findViewById(R.id.removeReplyButton);
        removeReplyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (RepliesListAdapterListener listener : listeners) {
                    listener.onGroupRemoveClick(groupPosition);
                }
            }
        });

        return view;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        LayoutInflater layoutInflater =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = (convertView != null) ? convertView :
                layoutInflater.inflate(android.R.layout.simple_expandable_list_item_1, null);

        Object[] keys = replyData.keySet().toArray();
        String currentKey = (String)keys[groupPosition];

        ArrayList<PostModel> replies = replyData.get(currentKey);
        String display = "\n" + replies.get(childPosition).getSanitizedComment() + "\n";
        TextView textViewLine1 = view.findViewById(android.R.id.text1);
        textViewLine1.setText(display);

        return view;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
