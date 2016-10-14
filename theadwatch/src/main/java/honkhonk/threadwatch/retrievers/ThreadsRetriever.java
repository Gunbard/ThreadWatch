package honkhonk.threadwatch.retrievers;

import android.content.Context;

import java.util.ArrayList;

import honkhonk.threadwatch.models.PostModel;
import honkhonk.threadwatch.models.ThreadModel;

/**
 * Created by Gunbard on 10/13/2016.
 */

public class ThreadsRetriever implements PostsRetriever.PostsRetrieverListener {
    private ArrayList<ThreadRetrieverListener> listeners = new ArrayList<>();
    private ArrayList<ThreadModel> threadsToRetrieve = new ArrayList<>();
    private ArrayList<ThreadModel> retrievedThreads = new ArrayList<>();

    public interface ThreadRetrieverListener {
        void threadsRetrieved(final ArrayList<ThreadModel> threads);
        void threadRetrievalFailed();
    }

    public void addListener(final ThreadRetrieverListener listener) {
        listeners.add(listener);
    }

    public void retrieveThreadData(final Context context, final ArrayList<ThreadModel> threads) {
        threadsToRetrieve = threads;
        processThreadQueue(context);
    }

    /**
     * PostsRetriever.PostsRetrieverListener
     */
    public void postsRetrieved(final Context context,
                               final ThreadModel thread,
                               final ArrayList<PostModel> posts) {
        thread.comment = posts.get(0).comment;
        retrievedThreads.add(thread);
        processThreadQueue(context);
    }

    public void retrievalFailed() {
        for (final ThreadRetrieverListener listener : listeners) {
            listener.threadRetrievalFailed();
        }
    }

    private void processThreadQueue(final Context context) {
        if (threadsToRetrieve.size() > 0) {
            final ThreadModel threadToGet = threadsToRetrieve.remove(0);

            PostsRetriever postsRetriever = new PostsRetriever();
            postsRetriever.addListener(this);
            postsRetriever.retrievePosts(context, threadToGet);
        } else {
            finishedRetrieving();
        }
    }

    private void finishedRetrieving() {
        for (final ThreadRetrieverListener listener : listeners) {
            listener.threadsRetrieved(retrievedThreads);
        }
    }
}
