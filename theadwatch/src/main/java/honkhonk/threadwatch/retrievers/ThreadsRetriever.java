package honkhonk.threadwatch.retrievers;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;

import honkhonk.threadwatch.models.PostModel;
import honkhonk.threadwatch.models.ThreadModel;

/**
 * Created by Gunbard on 10/13/2016.
 */

public class ThreadsRetriever implements PostsRetriever.PostsRetrieverListener {
    private ArrayList<ThreadRetrieverListener> listeners = new ArrayList<>();

    public interface ThreadRetrieverListener {
        void threadsRetrieved(final ArrayList<ThreadModel> threads);
        void threadRetrievalFailed();
    }

    public void addListener(final ThreadRetrieverListener listener) {
        listeners.add(listener);
    }

    public void retrieveThreadData(final Context context, final ArrayList<ThreadModel> threads) {
        PostsRetriever postsRetriever = new PostsRetriever();
        postsRetriever.addListener(this);
        postsRetriever.retrievePosts(context, threads.get(0));
    }

    /**
     * PostsRetriever.PostsRetrieverListener
     */
    public void postsRetrieved(final ThreadModel thread, final ArrayList<PostModel> posts) {
        thread.comment = posts.get(0).comment;
        for (final ThreadRetrieverListener listener : listeners) {
            listener.threadsRetrieved(new ArrayList<>(Collections.singletonList(thread)));
        }
    }

    public void retrievalFailed() {
        for (final ThreadRetrieverListener listener : listeners) {
            listener.threadRetrievalFailed();
        }
    }
}
